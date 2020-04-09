package com.serebit.wraith.core.prism

import cnames.structs.libusb_device_handle
import kotlinx.cinterop.*
import libusb.libusb_close
import libusb.libusb_interrupt_transfer
import libusb.libusb_release_interface
import kotlin.math.floor

@OptIn(ExperimentalUnsignedTypes::class)
class WraithPrism(private val handle: CPointer<libusb_device_handle>) {
    val components get() = listOf(logo, fan, ring)
    val logo: PrismLogoComponent
    val fan: PrismFanComponent
    val ring: PrismRingComponent
    var onApply: () -> Unit = { }

    init {
        sendBytes(0x41, 0x80) // power on
        sendBytes(0x51, 0x96) // magic bytes
        restore()
        apply()
        val channels = getChannels()
        logo = PrismLogoComponent(getChannelValues(channels[8]))
        fan = PrismFanComponent(getChannelValues(channels[9]))
        ring = PrismRingComponent(getChannelValues(channels[10]))
    }

    fun sendBytes(vararg bytes: Int, filler: Int = 0): List<Int> {
        transfer(ENDPOINT_OUT, UByteArray(64) { (bytes.getOrNull(it) ?: filler).toUByte() })
        return transfer(ENDPOINT_IN, UByteArray(64))
    }

    private fun transfer(endpoint: UByte, bytes: UByteArray) = memScoped {
        val byteValues = bytes.toCValues().ptr
        libusb_interrupt_transfer(handle, endpoint, byteValues, bytes.size, null, 1000u).let { err ->
            check(err == 0) { "Failed to transfer to device endpoint $endpoint with error code $err." }
        }
        byteValues.pointed.readValues(bytes.size)
            .getBytes()
            .toUByteArray()
            .map { byte -> byte.toInt() }
    }

    fun setChannelValues(component: PrismComponent) =
        sendBytes(0x51, 0x2C, 1, 0, *component.byteValues.toIntArray(), 0, 0, 0, filler = 0xFF)

    fun assignChannels() =
        sendBytes(0x51, 0xA0, 1, 0, 0, 3, 0, 0, logo.channel, fan.channel, *IntArray(15) { ring.mode.channel })

    fun getChannels() = sendBytes(0x52, 0xA0, 1, 0, 0, 3, 0, 0)
    fun getChannelValues(channel: Int) = ChannelValues(sendBytes(0x52, 0x2C, 1, 0, channel))

    fun close() {
        restore()
        apply(runCallback = false)
        libusb_release_interface(handle, HID_INTERFACE)
        libusb_close(handle)
    }

    companion object {
        private const val ENDPOINT_IN: UByte = 0x83u
        private const val ENDPOINT_OUT: UByte = 4u
        private const val HID_INTERFACE: Int = 1
    }
}

class ChannelValues(private val values: List<Int>) {
    val channel get() = values[4]
    val speed get() = values[5]
    val colorSource get() = values[6]
    val mode get() = values[7]
    val brightness get() = values[9]
    val color get() = Color(values[10], values[11], values[12])
}

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

fun WraithPrism.enableFanMirage(redFreq: Int, greenFreq: Int, blueFreq: Int): List<Int> {
    val (rm, ri, rd) = redFreq.mirageFreqBytes()
    val (gm, gi, gd) = greenFreq.mirageFreqBytes()
    val (bm, bi, bd) = blueFreq.mirageFreqBytes()
    return sendBytes(0x51, 0x71, 0, 0, 1, 0, 0xFF, 0x4A, 2, rm, ri, rd, 3, gm, gi, gd, 4, bm, bi, bd)
}

fun WraithPrism.disableFanMirage() =
    sendBytes(0x51, 0x71, 0, 0, 1, 0, 0xFF, 0x4A, 2, 0, 0xFF, 0x4A, 3, 0, 0xFF, 0x4A, 4, 0, 0xFF, 0x4A)

fun WraithPrism.updateRingMorseText(text: String) {
    val chunks = text.parseMorseOrTextToBytes().chunked(60)
    val firstChunk = chunks[0].toIntArray()
    val secondChunk = if (chunks.size > 1) chunks[1].toIntArray() else intArrayOf()
    sendBytes(0x51, 0x73, 0, 0, *firstChunk)
    sendBytes(0x51, 0x73, 1, 0, *secondChunk)
}
