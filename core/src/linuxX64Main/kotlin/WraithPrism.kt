package com.serebit.wraith.core

import cnames.structs.libusb_device
import cnames.structs.libusb_device_handle
import kotlinx.cinterop.*
import libusb.*

@UseExperimental(ExperimentalUnsignedTypes::class)
private const val ENDPOINT_IN: UByte = 0x83u
@UseExperimental(ExperimentalUnsignedTypes::class)
private const val ENDPOINT_OUT: UByte = 0x04u

@UseExperimental(ExperimentalUnsignedTypes::class)
class WraithPrism(handle: libusb_device_handle, device: libusb_device) {
    private val activeConfig = memScoped {
        val configPtr = allocPointerTo<libusb_config_descriptor>()
        val err = libusb_get_active_config_descriptor(device.ptr, configPtr.ptr)

        check(err == 0) { "Failed to fetch active configuration for USB device with error code $err" }
        configPtr.value!!.pointed
    }
    private val handle = memScoped {
        val handlePtr = allocPointerTo<libusb_device_handle>()
        val err = libusb_open(device.ptr, handlePtr.ptr)

        check(err == 0) { "Failed to open Cooler Master device with error code $err" }
        handlePtr.value!!.pointed
    }
    val logo: LogoComponent
    val fan: FanComponent
    val ring: RingComponent

    init {
        libusb_reset_device(handle.ptr)
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

    private fun claimInterfaces() = memScoped {
        libusb_set_auto_detach_kernel_driver(handle.ptr, 1)
        for (i in 0 until activeConfig.bNumInterfaces.toInt()) {
            val err3 = libusb_claim_interface(handle.ptr, i)
            check(err3 == 0) { "Failed to claim interface $i with error $err3." }
        }
    }

    private fun transfer(endpoint: UByte, bytes: UByteArray, timeout: UInt) = memScoped {
        val byteValues = bytes.toCValues().ptr
        libusb_interrupt_transfer(handle.ptr, endpoint, byteValues, bytes.size, null, timeout).let { err ->
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

    fun assignChannels() = sendBytes(
        0x51u, 0xA0u, 0x01u, 0u, 0u, 0x03u, 0u, 0u, logo.channel, fan.channel,
        *UByteArray(15) { ring.mode.channel }
    )

    fun close() {
        libusb_close(handle.ptr)
        libusb_exit(null)
    }
}

@UseExperimental(ExperimentalUnsignedTypes::class)
fun WraithPrism.sendBytes(vararg bytes: UByte, bufferSize: Int = 64, filler: UByte = 0x0u) =
    sendBytes(bytes.copyInto(UByteArray(bufferSize) { filler }))

@UseExperimental(ExperimentalUnsignedTypes::class)
class ChannelValues(val array: UByteArray) {
    val channel get() = array[4]
    val speed get() = array[5]
    val colorSource get() = array[6]
    val mode get() = array[7]
    val brightness get() = array[9]
    val color get() = Color(array[10], array[11], array[12])
}

@UseExperimental(ExperimentalUnsignedTypes::class)
fun WraithPrism.getChannelValues(channel: UByte) = ChannelValues(sendBytes(0x52u, 0x2Cu, 0x01u, 0u, channel))

@UseExperimental(ExperimentalUnsignedTypes::class)
fun WraithPrism.save() = sendBytes(0x50u, 0x55u)

@UseExperimental(ExperimentalUnsignedTypes::class)
fun WraithPrism.apply() = sendBytes(0x51u, 0x28u, 0u, 0u, 0xE0u)

@UseExperimental(ExperimentalUnsignedTypes::class)
fun WraithPrism.updateFanMirage() = if (fan.mirage) sendBytes(
    0x51u, 0x71u, 0u, 0u, 0x01u, 0u, 0xFFu, 0x4Au, 0x02u, 0x02u, 0x63u, 0xBDu, 0x03u, 0x02u, 0x63u, 0xBDu,
    0x04u, 0x02u, 0x63u, 0xBDu
) else sendBytes(
    0x51u, 0x71u, 0u, 0u, 0x01u, 0u, 0xFFu, 0x4Au, 0x02u, 0u, 0xFFu, 0x4Au, 0x03u, 0u, 0xFFu, 0x4Au,
    0x04u, 0u, 0xFFu, 0x4Au
)

@UseExperimental(ExperimentalUnsignedTypes::class)
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
}

inline fun <T : LedComponent> WraithPrism.update(device: T, update: T.() -> Unit) {
    device.update()
    setChannelValues(device)
    assignChannels()
    apply()
}
