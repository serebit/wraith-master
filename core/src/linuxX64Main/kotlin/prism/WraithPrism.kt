package com.serebit.wraith.core.prism

import com.serebit.wraith.core.UsbInterface
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.nanosleep
import platform.posix.timespec
import kotlin.math.floor

class WraithPrism internal constructor(private val usb: UsbInterface) {
    val components get() = listOf(logo, fan, ring)
    val logo: PrismLogoComponent
    val fan: PrismFanComponent
    val ring: PrismRingComponent
    var onApply: () -> Unit = { }

    var enso: Boolean
        get() = usb.sendBytes(0x52u, 0x96u)[4] == 0x10.toUByte()
        set(value) {
            if (value) {
                usb.sendBytes(0x51u, 0x96u, 0u, 0u, 0x10u)
                save()
            } else {
                usb.sendBytes(0x51u, 0x96u)
            }
        }

    init {
        usb.sendBytes(0x41u, 0x80u) // power on
        restore()
        apply()
        val channels = usb.sendBytes(0x52u, 0xA0u, 1u, 0u, 0u, 3u)
        logo = PrismLogoComponent(usb, channels[8])
        fan = PrismFanComponent(usb, channels[9])
        ring = PrismRingComponent(usb, channels[10])
    }

    fun assignChannels() = usb.sendBytes(
        0x51u, 0xA0u, 1u, 0u, 0u, 3u, 0u, 0u, logo.channel, fan.channel, *UByteArray(15) { ring.mode.channel }
    )

    fun save() {
        usb.sendBytes(0x50u, 0x55u)
        components.forEach { it.savedByteValues = it.byteValues }
    }

    fun restore() {
        usb.sendBytes(0x50u)
    }

    fun apply(runCallback: Boolean = true) {
        usb.sendBytes(0x51u, 0x28u, 0u, 0u, 0xE0u)
        if (runCallback) onApply()
    }

    fun pushFanMirageState() = fan.mirageState.let { state ->
        // always send the disable bytes
        usb.sendBytes(
            0x51u, 0x71u, 0u, 0u, 1u, 0u, 0xFFu, 0x4Au, 2u, 0u, 0xFFu, 0x4Au, 3u, 0u, 0xFFu, 0x4Au, 4u, 0u, 0xFFu, 0x4Au
        )

        if (state is MirageState.On) {
            // fix weird offsets by waiting for a bit before re-enabling
            memScoped {
                nanosleep(alloc<timespec>().apply { tv_nsec = 22200000 }.ptr, null)
            }
            val (rm, ri, rd) = state.redFreq.mirageFreqBytes()
            val (gm, gi, gd) = state.greenFreq.mirageFreqBytes()
            val (bm, bi, bd) = state.blueFreq.mirageFreqBytes()
            usb.sendBytes(0x51u, 0x71u, 0u, 0u, 1u, 0u, 0xFFu, 0x4Au, 2u, rm, ri, rd, 3u, gm, gi, gd, 4u, bm, bi, bd)
        }
    }

    fun updateRingMorseText(text: String) {
        val chunks = text.parseMorseOrTextToBytes().also { ring.savedMorseBytes = it }.chunked(60)
        val firstChunk = chunks[0].toUByteArray()
        val secondChunk = if (chunks.size > 1) chunks[1].toUByteArray() else ubyteArrayOf()
        usb.sendBytes(0x51u, 0x73u, 0u, 0u, *firstChunk)
        usb.sendBytes(0x51u, 0x73u, 1u, 0u, *secondChunk)
        apply(runCallback = false)
    }

    fun requestFirmwareVersion(): String = usb.sendBytes(0x12u, 0x20u)
        .subList(8, 34)
        .filter { it != 0.toUByte() }
        .map { it.toByte().toChar() }
        .joinToString("")
        .toLowerCase()

    fun close() = usb.close()
}

internal class ChannelValues(private val values: List<UByte>) {
    val channel get() = values[4]
    val speed get() = values[5]
    val colorSource get() = values[6]
    val mode get() = values[7]
    val brightness get() = values[9]
    val color get() = Color(values[10], values[11], values[12])
}

val WraithPrism.hasUnsavedChanges get() = components.any { it.savedByteValues != it.byteValues }

private fun Int.mirageFreqBytes(): List<UByte> {
    val initial = 187_498f / this

    val multiplicand = floor(initial / 256)
    val rem = initial / (multiplicand + 1)

    return listOf(multiplicand, floor(rem % 1 * 256), floor(rem)).map { it.toInt().toUByte() }
}

fun WraithPrism.resetToDefault() {
    enso = false

    listOf(fan, logo).forEach {
        it.mode = BasicPrismMode.CYCLE
        it.speed = Speed.MEDIUM
        it.brightness = Brightness.HIGH
    }
    ring.apply {
        mode = PrismRingMode.RAINBOW
        speed = Speed.MEDIUM
        brightness = Brightness.HIGH
    }
    fan.mirageState = MirageState.DEFAULT
    pushFanMirageState()

    components.forEach { it.submitValues() }
    assignChannels()
    apply()
}
