package com.serebit.wraith

import cnames.structs.libusb_device
import cnames.structs.libusb_device_handle
import kotlinx.cinterop.*
import libusb.*

class WraithPrism(device: libusb_device) {
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

    init {
        reset()
        claimInterfaces()
    }

    private fun claimInterfaces() = memScoped {
        libusb_set_auto_detach_kernel_driver(handle.ptr, 1)
        for (i in 0 until activeConfig.bNumInterfaces.toInt()) {
            val err3 = libusb_claim_interface(handle.ptr, i)
            check(err3 == 0) { "Failed to claim interface $i with error $err3." }
        }
    }

    fun sendBytes(bytes: UByteArray) = memScoped {
        libusb_interrupt_transfer(handle.ptr, ENDPOINT_OUT, bytes.toCValues().ptr, bytes.size, null, 1000u).let { err ->
            check(err == 0) { "Failed to transfer bytes to device OUT endpoint with error code $err." }
        }
        val inBytes = UByteArray(64).toCValues()
        libusb_interrupt_transfer(handle.ptr, ENDPOINT_IN, inBytes, inBytes.size, null, 1000u).let { err ->
            check(err == 0) { "Failed to transfer bytes from device IN endpoint with error code $err." }
        }
    }

    fun reset() = libusb_reset_device(handle.ptr)

    fun close() = libusb_close(handle.ptr)
}

fun WraithPrism.sendBytes(vararg bytes: UByte, bufferSize: Int = 64, filler: UByte = 0x0u) {
    sendBytes(bytes.copyInto(UByteArray(bufferSize) { filler }))
}

fun WraithPrism.setChannel(
    channel: UByte,
    speed: UByte,
    colorSource: UByte,
    mode: UByte,
    brightness: UByte,
    r: UByte, g: UByte, b: UByte
) {
    sendBytes(
        0x51u, 0x2Cu, 0x01u, 0u, channel, speed, colorSource, mode, 0xFFu, brightness, r, g, b, 0u, 0u, 0u,
        filler = 0xFFu
    )
}

fun WraithPrism.initialize() {
    // turn on
    sendBytes(0x41u, 0x80u)
    // send magic bytes
    sendBytes(0x51u, 0x96u)
    // apply changes
    apply()
}

fun WraithPrism.apply() = sendBytes(0x51u, 0x28u, 0u, 0u, 0xE0u)
