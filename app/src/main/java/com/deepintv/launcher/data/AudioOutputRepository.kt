package com.deepintv.launcher.data

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.deepintv.launcher.R

data class AudioOutput(val label: String, val isBluetooth: Boolean)

/** Lists the TV's audio outputs. Read-only, like WifiRepository: a normal app can't force media
 * routing to a specific output (AudioManager.setCommunicationDevice only covers the VoIP/call
 * use case), so this informs rather than pretends to switch. */
class AudioOutputRepository(private val context: Context) {

    fun listOutputs(): List<AudioOutput> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter { it.type in RELEVANT_TYPES }
            .map { AudioOutput(label = labelFor(it), isBluetooth = it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) }
            .distinctBy { it.label }
    }

    /** Calls [onChange] when an output is plugged in or removed (Bluetooth speaker, HDMI ARC...).
     * Returns the function that stops listening. */
    fun observe(onChange: () -> Unit): () -> Unit {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = onChange()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = onChange()
        }
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        return { audioManager.unregisterAudioDeviceCallback(callback) }
    }

    private fun labelFor(device: AudioDeviceInfo): String = when (device.type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> context.getString(R.string.audio_tv_speakers)
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> device.productName?.toString() ?: "Bluetooth"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI (ARC)"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> context.getString(R.string.audio_wired_headphones)
        else -> device.productName?.toString() ?: context.getString(R.string.audio_output)
    }

    companion object {
        private val RELEVANT_TYPES = setOf(
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
        )
    }
}
