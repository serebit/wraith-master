package com.serebit.wraith.core

import cnames.structs.libusb_device_handle
import kotlinx.cinterop.*
import libusb.*
import kotlin.math.floor
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

@OptIn(ExperimentalUnsignedTypes::class)
private data class TransferData(
    val handle: CPointer<libusb_device_handle>, val endpoint: UByte, val bytes: UByteArray
)

@OptIn(ExperimentalUnsignedTypes::class)
class WraithPrism(private val handle: CPointer<libusb_device_handle>, private val numInterfaces: Int) {
    private val worker = Worker.start(name = "wraith-master-io")
    val components get() = listOf(logo, fan, ring)
    val logo: LogoComponent
    val fan: FanComponent
    val ring: RingComponent

    init {
        libusb_reset_device(handle)
        claimInterfaces()
        powerOn()
        sendBytes(0x51, 0x96) // magic bytes
        apply()
        val channels = getChannels().result
        logo = LogoComponent(getChannelValues(channels[8]))
        fan = FanComponent(getChannelValues(channels[9]))
        ring = RingComponent(getChannelValues(channels[10]))
    }

    private fun claimInterfaces() {
        libusb_set_auto_detach_kernel_driver(handle, 1)
        for (i in 0 until numInterfaces) {
            val err3 = libusb_claim_interface(handle, i)
            check(err3 == 0) { "Failed to claim interface $i with error $err3." }
        }
    }

    private fun transfer(endpoint: UByte, bytes: UByteArray) =
        worker.execute(TransferMode.SAFE, { TransferData(handle, endpoint, bytes).freeze() }) {
            memScoped {
                val byteValues = it.bytes.toCValues().ptr
                libusb_interrupt_transfer(it.handle, it.endpoint, byteValues, it.bytes.size, null, 1000u).let { err ->
                    check(err == 0) { "Failed to transfer to device endpoint ${it.endpoint} with error code $err." }
                }
                byteValues.pointed.readValues(it.bytes.size)
                    .getBytes()
                    .toUByteArray()
                    .map { byte -> byte.toInt() }
            }
        }

    fun sendBytes(bytes: List<Int>): Future<List<Int>> {
        transfer(ENDPOINT_OUT, bytes.map { it.toUByte() }.toUByteArray())
        return transfer(ENDPOINT_IN, UByteArray(64))
    }

    fun setChannelValues(component: LedComponent) =
        sendBytes(0x51, 0x2C, 1, 0, *component.byteValues.toIntArray(), 0, 0, 0, filler = 0xFF)

    fun assignChannels() = sendBytes(0x51, 0xA0, 1, 0, 0, 3, 0, 0, logo.channel, fan.channel,
        *IntArray(15) { ring.mode.channel })

    fun close() {
        (0 until numInterfaces).forEach { libusb_release_interface(handle, it) }
        libusb_close(handle)
        libusb_exit(null)
    }

    companion object {
        private const val ENDPOINT_IN: UByte = 0x83u
        private const val ENDPOINT_OUT: UByte = 4u
    }
}

fun WraithPrism.sendBytes(vararg bytes: Int, filler: Int = 0) =
    sendBytes(IntArray(64) { bytes.getOrNull(it) ?: filler }.toList())

class ChannelValues(private val values: List<Int>) {
    val channel get() = values[4]
    val speed get() = values[5]
    val colorSource get() = values[6]
    val mode get() = values[7]
    val brightness get() = values[9]
    val color get() = Color(values[10], values[11], values[12])
}

fun WraithPrism.save() = sendBytes(0x50, 0x55)
fun WraithPrism.apply() = sendBytes(0x51, 0x28, 0, 0, 0xE0)
fun WraithPrism.load() = sendBytes(0x50)
fun WraithPrism.restore() = sendBytes(0, 0x41)
fun WraithPrism.powerOff() = sendBytes(0x41, 3)
fun WraithPrism.powerOn() = sendBytes(0x41, 0x80)
fun WraithPrism.getChannels() = sendBytes(0x52, 0xA0, 1, 0, 0, 3, 0, 0)
fun WraithPrism.getChannelValues(channel: Int) = ChannelValues(sendBytes(0x52, 0x2C, 1, 0, channel).result)

private fun Int.mirageFreqBytes(): List<Int> {
    val initial = 187_498f / this

    val multiplicand = floor(initial / 256)
    val rem = initial / (multiplicand + 1)

    return listOf(multiplicand, floor(rem % 1 * 256), floor(rem)).map { it.toInt() }
}

fun WraithPrism.enableFanMirage(redFreq: Int, greenFreq: Int, blueFreq: Int): Future<List<Int>> {
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

fun WraithPrism.reset() {
    load()
    restore()
    apply()

    // update existing components with reset values
    val channels = getChannels().result
    components.forEachIndexed { i, it -> it.assignValuesFromChannel(getChannelValues(channels[i + 8])) }
}

inline fun <C : LedComponent> WraithPrism.update(component: C, task: C.() -> Unit) {
    component.task()
    setChannelValues(component)
    assignChannels()
    apply()
}
