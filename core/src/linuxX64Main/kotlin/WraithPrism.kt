package com.serebit.wraith.core

import cnames.structs.libusb_device_handle
import kotlinx.cinterop.*
import libusb.*

@OptIn(ExperimentalUnsignedTypes::class)
class WraithPrism(private val handle: CPointer<libusb_device_handle>, private val numInterfaces: Int) {
    val logo: LogoComponent
    val fan: FanComponent
    val ring: RingComponent

    init {
        libusb_reset_device(handle)
        claimInterfaces()
        // turn on
        sendBytes(0x41u, 0x80u)
        // send magic bytes
        sendBytes(0x51u, 0x96u)
        // apply changes
        apply()
        val channels = sendBytes(0x52u, 0xA0u, 0x01u, 0u, 0u, 0x03u, 0u, 0u)
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

    private fun transfer(endpoint: UByte, bytes: UByteArray, timeout: UInt) = memScoped {
        val byteValues = bytes.toCValues().ptr
        libusb_interrupt_transfer(handle, endpoint, byteValues, bytes.size, null, timeout).let { err ->
            check(err == 0) { "Failed to transfer to device endpoint $endpoint with error code $err." }
        }
        byteValues.pointed.readValues(bytes.size).getBytes().toUByteArray()
    }

    fun sendBytes(bytes: UByteArray): UByteArray = memScoped {
        transfer(ENDPOINT_OUT, bytes, 1000u)
        transfer(ENDPOINT_IN, UByteArray(64), 1000u)
    }

    fun setChannelValues(component: LedComponent) =
        sendBytes(0x51u, 0x2Cu, 0x01u, 0u, *component.values, 0u, 0u, 0u, filler = 0xFFu)

    fun assignChannels() = sendBytes(0x51u, 0xA0u, 0x01u, 0u, 0u, 0x03u, 0u, 0u, logo.channel, fan.channel,
        *UByteArray(15) { ring.mode.channel })

    fun close() {
        (0 until numInterfaces).forEach { libusb_release_interface(handle, it) }
        libusb_close(handle)
        libusb_exit(null)
    }

    companion object {
        private const val ENDPOINT_IN: UByte = 0x83u
        private const val ENDPOINT_OUT: UByte = 0x04u
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun WraithPrism.sendBytes(vararg bytes: UByte, bufferSize: Int = 64, filler: UByte = 0x0u) =
    sendBytes(bytes.copyInto(UByteArray(bufferSize) { filler }))

@OptIn(ExperimentalUnsignedTypes::class)
class ChannelValues(private val array: UByteArray) {
    val channel get() = array[4]
    val speed get() = array[5]
    val colorSource get() = array[6]
    val mode get() = array[7]
    val brightness get() = array[9]
    val color get() = Color(array[10], array[11], array[12])
}

@OptIn(ExperimentalUnsignedTypes::class)
fun WraithPrism.getChannelValues(channel: UByte) = ChannelValues(sendBytes(0x52u, 0x2Cu, 0x01u, 0u, channel))

@OptIn(ExperimentalUnsignedTypes::class)
fun WraithPrism.save() = sendBytes(0x50u, 0x55u)

@OptIn(ExperimentalUnsignedTypes::class)
fun WraithPrism.apply() = sendBytes(0x51u, 0x28u, 0u, 0u, 0xE0u)

@OptIn(ExperimentalUnsignedTypes::class)
fun WraithPrism.updateFanMirage() = if (fan.mirage) sendBytes(
    0x51u, 0x71u, 0u, 0u, 0x01u, 0u, 0xFFu, 0x4Au, 0x02u, 0x02u, 0x63u, 0xBDu, 0x03u, 0x02u, 0x63u, 0xBDu,
    0x04u, 0x02u, 0x63u, 0xBDu
) else sendBytes(
    0x51u, 0x71u, 0u, 0u, 0x01u, 0u, 0xFFu, 0x4Au, 0x02u, 0u, 0xFFu, 0x4Au, 0x03u, 0u, 0xFFu, 0x4Au,
    0x04u, 0u, 0xFFu, 0x4Au
)

@OptIn(ExperimentalUnsignedTypes::class)
fun WraithPrism.updateRingMorseText(text: String) {
    val chunks = text.parseMorseOrTextToBytes().chunked(60)
    val firstChunk = chunks[0].toUByteArray()
    val secondChunk = if (chunks.size > 1) chunks[1].toUByteArray() else ubyteArrayOf()
    sendBytes(0x51u, 0x73u, 0u, 0u, *firstChunk)
    sendBytes(0x51u, 0x73u, 0x01u, 0u, *secondChunk)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun WraithPrism.reset() {
    // load
    sendBytes(0x50u)
    // power off
    sendBytes(0x41u, 0x03u)
    // restore
    sendBytes(0u, 0x41u)
    // power on
    sendBytes(0x41u, 0x80u)
    // apply changes
    apply()

    // update existing components with reset values
    val channels = sendBytes(0x52u, 0xA0u, 0x01u, 0u, 0u, 0x03u, 0u, 0u)
    logo.assignValuesFromChannel(getChannelValues(channels[8]))
    fan.assignValuesFromChannel(getChannelValues(channels[9]))
    ring.assignValuesFromChannel(getChannelValues(channels[10]))
}

inline fun <C : LedComponent> WraithPrism.update(component: C, update: C.() -> Unit) {
    component.update()
    setChannelValues(component)
    assignChannels()
    apply()
}
