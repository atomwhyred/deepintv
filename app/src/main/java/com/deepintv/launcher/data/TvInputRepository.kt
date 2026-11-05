package com.deepintv.launcher.data

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager

/** What kind of connector an input is, for picking its icon. */
enum class InputKind { HDMI, ANALOG_COMPONENT, ANALOG_COMPOSITE, TUNER, OTHER }

/** How an input is wired, from [TvInputInfo.getType]. Pure, so it can be unit-tested. */
fun inputKindFor(type: Int): InputKind = when (type) {
    TvInputInfo.TYPE_HDMI, TvInputInfo.TYPE_DISPLAY_PORT, TvInputInfo.TYPE_DVI -> InputKind.HDMI
    TvInputInfo.TYPE_COMPONENT, TvInputInfo.TYPE_VGA -> InputKind.ANALOG_COMPONENT
    TvInputInfo.TYPE_COMPOSITE, TvInputInfo.TYPE_SCART, TvInputInfo.TYPE_SVIDEO -> InputKind.ANALOG_COMPOSITE
    TvInputInfo.TYPE_TUNER -> InputKind.TUNER
    else -> InputKind.OTHER
}

/** Whether a source is plugged in, from [TvInputManager.getInputState]. */
enum class InputState { CONNECTED, STANDBY, DISCONNECTED }

fun inputStateFor(state: Int): InputState? = when (state) {
    TvInputManager.INPUT_STATE_CONNECTED -> InputState.CONNECTED
    TvInputManager.INPUT_STATE_CONNECTED_STANDBY -> InputState.STANDBY
    TvInputManager.INPUT_STATE_DISCONNECTED -> InputState.DISCONNECTED
    else -> null
}

data class TvInputItem(
    val inputId: String,
    val label: String,
    val switchIntent: Intent,
    val kind: InputKind = InputKind.OTHER,
    /** Null for the tuner, or when the TV doesn't say. */
    val state: InputState? = null,
)

/** Lists the TV's physical inputs (HDMI, AV, the tuner) for the Inputs pill, same framework the
 * stock Live TV app uses: starting ACTION_VIEW on a passthrough channel URI switches to it. */
class TvInputRepository(private val context: Context) {

    fun listPhysicalInputs(): List<TvInputItem> {
        val tvInputManager =
            context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return emptyList()

        return tvInputManager.tvInputList
            .filter { isPassthroughInputType(it.type) }
            // An input the user hid in the TV's settings stays hidden here too.
            .filterNot { info -> runCatching { info.isHidden(context) }.getOrDefault(false) }
            .sortedBy { it.type }
            .map { info ->
                val kind = inputKindFor(info.type)
                TvInputItem(
                    inputId = info.id,
                    label = labelFor(info),
                    switchIntent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(info.id)),
                    kind = kind,
                    state = if (kind == InputKind.TUNER) null
                    else runCatching { inputStateFor(tvInputManager.getInputState(info.id)) }.getOrNull(),
                )
            }
    }

    /** The name the user gave the input in the TV's settings, else its own label, else its id. */
    private fun labelFor(info: TvInputInfo): String =
        runCatching { info.loadCustomLabel(context)?.toString() }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: runCatching { info.loadLabel(context)?.toString() }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: info.id

    private fun isPassthroughInputType(type: Int): Boolean = when (type) {
        TvInputInfo.TYPE_HDMI,
        TvInputInfo.TYPE_COMPOSITE,
        TvInputInfo.TYPE_COMPONENT,
        TvInputInfo.TYPE_SCART,
        TvInputInfo.TYPE_SVIDEO,
        TvInputInfo.TYPE_VGA,
        TvInputInfo.TYPE_DVI,
        TvInputInfo.TYPE_DISPLAY_PORT,
        TvInputInfo.TYPE_TUNER,
        -> true
        else -> false
    }
}
