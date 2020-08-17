package com.serebit.wraith.core.prism

import hidapi.hid_close
import hidapi.hid_device
import hidapi.hid_read
import hidapi.hid_write
import kotlinx.cinterop.*
import platform.posix.nanosleep
import platform.posix.timespec
import kotlin.math.floor

@OptIn(ExperimentalUnsignedTypes::class)
class WraithPrism(private val handle: CPointer<hid_device>) {
    val components get() = listOf(logo, fan, ring)
    val logo: PrismLogoComponent
    val fan: PrismFanComponent
    val ring: PrismRingComponent
    var onApply: () -> Unit = { }

    init {
        sendBytes(0x41, 0x80) // power on
        restore()
        apply()
        val channels = getChannels()
        logo = PrismLogoComponent(getChannelValues(channels[8]))
        fan = PrismFanComponent(getChannelValues(channels[9]))
        ring = PrismRingComponent(getChannelValues(channels[10]))
    }

    fun sendBytes(vararg bytes: Int, filler: Int = 0): List<Int> {
        return transfer(UByteArray(64) { (bytes.getOrNull(it) ?: filler).toUByte() }).map { it.toInt() }
    }

    private fun transfer(writeBytes: UByteArray): UByteArray = memScoped {
        hid_write(handle, writeBytes.toCValues().ptr, writeBytes.size.toULong())
        val readBytes = UByteArray(writeBytes.size).toCValues().ptr
        hid_read(handle, readBytes, writeBytes.size.toULong())
        readBytes.pointed.readValues(writeBytes.size).getBytes().toUByteArray()
    }

    fun setChannelValues(component: PrismComponent) =
        sendBytes(0x51, 0x2C, 1, 0, *component.byteValues.toIntArray(), 0, 0, 0, filler = 0xFF)

    fun assignChannels() =
        sendBytes(0x51, 0xA0, 1, 0, 0, 3, 0, 0, logo.channel, fan.channel, *IntArray(15) { ring.mode.channel })

    fun getChannels() = sendBytes(0x52, 0xA0, 1, 0, 0, 3, 0, 0)
    fun getChannelValues(channel: Int) = ChannelValues(sendBytes(0x52, 0x2C, 1, 0, channel))

    fun close() = hid_close(handle)
}

class ChannelValues(private val values: List<Int>) {
    val channel get() = values[4]
    val speed get() = values[5]
    val colorSource get() = values[6]
    val mode get() = values[7]
    val brightness get() = values[9]
    val color get() = Color(values[10], values[11], values[12])
}

val WraithPrism.hasUnsavedChanges get() = components.any { it.savedByteValues != it.byteValues }

fun WraithPrism.save() {
    sendBytes(0x50, 0x55)
    components.forEach { it.savedByteValues = it.byteValues }
}

fun WraithPrism.restore() = sendBytes(0x50)

fun WraithPrism.apply(runCallback: Boolean = true) {
    sendBytes(0x51, 0x28, 0, 0, 0xE0)
    if (runCallback) onApply()
}

private fun Int.mirageFreqBytes(): List<Int> {
    val initial = 187_498f / this

    val multiplicand = floor(initial / 256)
    val rem = initial / (multiplicand + 1)

    return listOf(multiplicand, floor(rem % 1 * 256), floor(rem)).map { it.toInt() }
}

fun WraithPrism.pushFanMirageState() = fan.mirageState.let { state ->
    // always send the disable bytes
    sendBytes(0x51, 0x71, 0, 0, 1, 0, 0xFF, 0x4A, 2, 0, 0xFF, 0x4A, 3, 0, 0xFF, 0x4A, 4, 0, 0xFF, 0x4A)

    if (state is MirageState.On) {
        // fix weird offsets by waiting for a bit before re-enabling
        memScoped {
            nanosleep(alloc<timespec>().apply { tv_nsec = 22200000 }.ptr, null)
        }
        val (rm, ri, rd) = state.redFreq.mirageFreqBytes()
        val (gm, gi, gd) = state.greenFreq.mirageFreqBytes()
        val (bm, bi, bd) = state.blueFreq.mirageFreqBytes()
        sendBytes(0x51, 0x71, 0, 0, 1, 0, 0xFF, 0x4A, 2, rm, ri, rd, 3, gm, gi, gd, 4, bm, bi, bd)
    }
}

fun WraithPrism.updateRingMorseText(text: String) {
    val chunks = text.parseMorseOrTextToBytes().also { ring.savedMorseBytes = it }.chunked(60)
    val firstChunk = chunks[0].toIntArray()
    val secondChunk = if (chunks.size > 1) chunks[1].toIntArray() else intArrayOf()
    sendBytes(0x51, 0x73, 0, 0, *firstChunk)
    sendBytes(0x51, 0x73, 1, 0, *secondChunk)
    apply(runCallback = false)
}

var WraithPrism.enso: Boolean
    get() = sendBytes(0x52, 0x96)[4] == 0x10
    set(value) {
        if (value) {
            sendBytes(0x51, 0x96, 0, 0, 0x10)
            save()
        } else {
            sendBytes(0x51, 0x96)
        }
    }

fun WraithPrism.requestFirmwareVersion(): String = sendBytes(0x12, 0x20)
    .subList(8, 34)
    .filter { it != 0 }
    .map { it.toChar() }
    .joinToString("")
    .toLowerCase()

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

    components.forEach { setChannelValues(it) }
    assignChannels()
    apply()
}
