package com.deepintv.launcher.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.deepintv.launcher.R
import com.deepintv.launcher.data.AVAILABLE_COINS
import com.deepintv.launcher.data.ConfigCodec
import com.deepintv.launcher.data.ConfigParse
import com.deepintv.launcher.data.ConfigStore
import com.deepintv.launcher.data.LauncherSettingsRepository
import com.deepintv.launcher.data.PairingSession
import com.deepintv.launcher.data.RefreshPrefsRepository
import com.deepintv.launcher.data.ThemeMode
import com.deepintv.launcher.data.WidgetKind
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * A tiny web server so a phone can change the launcher's settings: it opens only while the
 * pairing screen is showing. Every request is checked: the phone must be on a private address,
 * the Host header must be this TV's own, the page needs the one-time token from the QR, and
 * the API needs the session that token was exchanged for. It closes after the first successful
 * change, when the screen is left, or after a few minutes.
 */
object ConfigServer {
    class Info(val url: String, val expiresAtMs: Long)

    private const val TAG = "ConfigServer"
    private const val PREFERRED_PORT = 8765
    private const val MAX_HEADER_BYTES = 8 * 1024
    private const val READ_TIMEOUT_MS = 2_000
    private const val REQUEST_DEADLINE_NS = 5_000_000_000L
    private const val HARD_LIMIT_MS = 10 * 60_000L
    private val TOKEN_PATH = Regex("/[0-9a-f]{32}")

    /** The only files served besides the page: the bundled 3D scene (three.js, MIT; source in web/). */
    private val STATIC_FILES = mapOf("/static/scene.js" to R.raw.config_scene)

    private val main = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stop() }

    @Volatile private var server: ServerSocket? = null
    @Volatile private var session: PairingSession? = null

    /** Starts listening and returns what the QR must say, or null if the TV has no local network. */
    @Synchronized
    fun start(context: Context, onApplied: () -> Unit): Info? {
        stop()
        val address = localAddress() ?: return null
        val socket = bind(address) ?: return null
        val pairing = PairingSession()
        val appContext = context.applicationContext
        val page = appContext.resources.openRawResource(R.raw.config_page).use { it.readBytes() }
        val host = "${address.hostAddress}:${socket.localPort}"
        server = socket
        session = pairing
        Thread({ serve(socket, pairing, appContext, page, host, onApplied) }, "deepintv-config").apply { isDaemon = true }.start()
        main.removeCallbacks(stopRunnable)
        main.postDelayed(stopRunnable, HARD_LIMIT_MS)
        return Info(url = "http://$host/${pairing.token}", expiresAtMs = System.currentTimeMillis() + PairingSession.DEFAULT_TTL_MS)
    }

    @Synchronized
    fun stop() {
        main.removeCallbacks(stopRunnable)
        runCatching { server?.close() }
        server = null
        session = null
    }

    private fun bind(address: InetAddress): ServerSocket? =
        runCatching { ServerSocket(PREFERRED_PORT, 4, address) }.getOrNull()
            ?: runCatching { ServerSocket(0, 4, address) }.getOrNull()

    /** The TV's address on the local network (Wi-Fi or Ethernet), never a VPN or loopback one. */
    private fun localAddress(): InetAddress? {
        val candidates = runCatching { NetworkInterface.getNetworkInterfaces().toList() }.getOrDefault(emptyList())
            .filter { runCatching { it.isUp && !it.isLoopback && !it.isVirtual }.getOrDefault(false) }
            .flatMap { nic -> nic.inetAddresses.toList().filter { it is Inet4Address && it.isSiteLocalAddress }.map { nic.name to it } }
        return (candidates.firstOrNull { (name, _) -> name.startsWith("wlan") || name.startsWith("eth") } ?: candidates.firstOrNull())?.second
    }

    private fun serve(socket: ServerSocket, pairing: PairingSession, context: Context, page: ByteArray, host: String, onApplied: () -> Unit) {
        while (!socket.isClosed) {
            val client = try {
                socket.accept()
            } catch (e: IOException) {
                break
            }
            val finished = runCatching { handle(client, pairing, context, page, host) }
                .onFailure { Log.w(TAG, "Request failed", it) }
                .getOrDefault(false)
            runCatching { client.close() }
            if (finished) {
                main.post { onApplied() }
                stop()
                break
            }
            if (pairing.isLocked() || pairing.isExpired()) {
                stop()
                break
            }
        }
    }

    /** Answers one request. True when it was a successful settings change (the server is done). */
    private fun handle(client: Socket, pairing: PairingSession, context: Context, page: ByteArray, host: String): Boolean {
        client.soTimeout = READ_TIMEOUT_MS
        val out = client.getOutputStream()
        val ip = client.inetAddress
        if (!(ip.isSiteLocalAddress || ip.isLinkLocalAddress)) return reply(out, 403, "text/plain", "forbidden")
        val request = try {
            readRequest(BufferedInputStream(client.getInputStream()))
        } catch (e: SocketTimeoutException) {
            return false
        } ?: return reply(out, 400, "text/plain", "bad request")

        // A page on another site can make a browser call this address; its Host header would differ.
        if (request.headers["host"] != host) return reply(out, 400, "text/plain", "bad host")
        val clientIp = ip.hostAddress.orEmpty()
        val sessionCookie = request.cookie("s")

        return when {
            // Any 32-hex path is a token attempt, so wrong guesses count against the lock.
            request.method == "GET" && TOKEN_PATH.matches(request.path) -> {
                val id = pairing.redeem(request.path.removePrefix("/"), clientIp)
                if (id == null) {
                    reply(out, 403, "text/plain", "This code was already used or has expired.")
                } else {
                    reply(out, 200, "text/html; charset=utf-8", page, "Set-Cookie: s=$id; Path=/; HttpOnly; SameSite=Strict")
                }
            }
            request.method == "GET" && request.path == "/api/config" -> {
                if (!pairing.isAuthorized(sessionCookie, clientIp)) return reply(out, 403, "text/plain", "forbidden")
                reply(out, 200, "application/json", configJson(context).put("expiresInMs", pairing.remainingMs()).toString())
            }
            request.method == "GET" && request.path.startsWith("/static/") -> {
                if (!pairing.isAuthorized(sessionCookie, clientIp)) return reply(out, 403, "text/plain", "forbidden")
                val res = STATIC_FILES[request.path] ?: return reply(out, 404, "text/plain", "not found")
                // Read per request rather than kept: it is only fetched once per pairing.
                val body = context.resources.openRawResource(res).use { it.readBytes() }
                reply(out, 200, "text/javascript; charset=utf-8", body)
            }
            request.method == "POST" && request.path == "/api/config" -> {
                if (!pairing.isAuthorized(sessionCookie, clientIp)) return reply(out, 403, "text/plain", "forbidden")
                // A plain cross-site form cannot set this header or this content type.
                if (request.headers["x-requested-with"] != "deepintv" || request.headers["content-type"]?.startsWith("application/json") != true) {
                    return reply(out, 400, "text/plain", "bad request")
                }
                val store = ConfigStore(context)
                when (val parsed = ConfigCodec.parse(String(request.body, Charsets.UTF_8), store.rules())) {
                    is ConfigParse.Ok -> {
                        store.apply(parsed.config)
                        reply(out, 200, "application/json", """{"ok":true}""")
                        true
                    }
                    is ConfigParse.Invalid -> reply(out, 400, "application/json", JSONObject().put("ok", false).put("error", parsed.reason).toString())
                }
            }
            else -> reply(out, 404, "text/plain", "not found")
        }
    }

    private fun configJson(context: Context): JSONObject {
        val store = ConfigStore(context)
        val options = JSONObject()
            .put("themes", JSONArray(ThemeMode.entries.map { it.name }))
            .put("dockSize", JSONArray(listOf(LauncherSettingsRepository.MIN_SIZE, LauncherSettingsRepository.MAX_SIZE)))
            .put("coins", JSONArray(AVAILABLE_COINS.map { JSONObject().put("id", it.id).put("symbol", it.symbol) }))
            .put("widgets", JSONArray(WidgetKind.entries.map { JSONObject().put("name", it.name).put("label", context.getString(it.labelRes)) }))
            .put("refreshWeather", JSONArray(RefreshPrefsRepository.WEATHER_OPTIONS))
            .put("refreshCrypto", JSONArray(RefreshPrefsRepository.CRYPTO_OPTIONS))
            .put("refreshCalendar", JSONArray(RefreshPrefsRepository.CALENDAR_OPTIONS))
            .put("refreshSystem", JSONArray(RefreshPrefsRepository.SYSTEM_OPTIONS))
            .put("refreshWatchNext", JSONArray(RefreshPrefsRepository.WATCH_NEXT_OPTIONS))
        return JSONObject().put("config", ConfigCodec.toJson(store.read())).put("options", options)
    }

    private class Request(val method: String, val path: String, val headers: Map<String, String>, val body: ByteArray) {
        fun cookie(name: String): String? =
            headers["cookie"]?.split(';')?.map { it.trim() }?.firstOrNull { it.startsWith("$name=") }?.substringAfter('=')
    }

    private fun readRequest(input: BufferedInputStream): Request? {
        // soTimeout limits each read; this limits the whole request, so a client trickling one byte
        // at a time cannot hold the single serving thread (and block the real phone) for minutes.
        val deadline = System.nanoTime() + REQUEST_DEADLINE_NS
        fun late() = System.nanoTime() > deadline
        val head = ByteArray(MAX_HEADER_BYTES)
        var length = 0
        while (true) {
            if (late()) return null
            val b = input.read()
            if (b < 0 || length >= head.size) return null
            head[length++] = b.toByte()
            if (length >= 4 && head[length - 4] == '\r'.code.toByte() && head[length - 3] == '\n'.code.toByte() &&
                head[length - 2] == '\r'.code.toByte() && head[length - 1] == '\n'.code.toByte()
            ) break
        }
        val lines = String(head, 0, length, Charsets.ISO_8859_1).split("\r\n")
        val parts = lines.firstOrNull()?.split(' ') ?: return null
        if (parts.size < 3) return null
        val headers = lines.drop(1).filter { it.contains(':') }
            .associate { it.substringBefore(':').trim().lowercase() to it.substringAfter(':').trim() }
        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
        if (contentLength < 0 || contentLength > ConfigCodec.MAX_BODY_CHARS) return null
        val body = ByteArray(contentLength)
        var read = 0
        while (read < contentLength) {
            if (late()) return null
            val n = input.read(body, read, contentLength - read)
            if (n < 0) return null
            read += n
        }
        return Request(parts[0], parts[1].substringBefore('?'), headers, body)
    }

    private fun reply(out: OutputStream, status: Int, type: String, body: String, extra: String? = null): Boolean =
        reply(out, status, type, body.toByteArray(Charsets.UTF_8), extra)

    private fun reply(out: OutputStream, status: Int, type: String, body: ByteArray, extra: String? = null): Boolean {
        val reason = when (status) { 200 -> "OK"; 400 -> "Bad Request"; 403 -> "Forbidden"; else -> "Not Found" }
        val head = buildString {
            append("HTTP/1.1 $status $reason\r\n")
            append("Content-Type: $type\r\nContent-Length: ${body.size}\r\nConnection: close\r\n")
            append("Cache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\n")
            append("Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; script-src 'self' 'unsafe-inline'; connect-src 'self'\r\n")
            extra?.let { append(it).append("\r\n") }
            append("\r\n")
        }
        out.write(head.toByteArray(Charsets.ISO_8859_1))
        out.write(body)
        out.flush()
        return false
    }
}
