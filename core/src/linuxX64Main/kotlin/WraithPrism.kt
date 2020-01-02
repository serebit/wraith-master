@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package com.serebit.wraith.core

import cnames.structs.libusb_device
import cnames.structs.libusb_device_handle
import kotlinx.cinterop.*
import libusb.*

private const val ENDPOINT_IN: UByte = 0x83u
private const val ENDPOINT_OUT: UByte = 0x04u

enum class LedMode(val value: UByte) {
    OFF(0x00u), STATIC(0x01u), CYCLE(0x02u), BREATHE(0x03u)
}

enum class RingMode(val channel: UByte, val mode: UByte) {
    OFF(0xFEu, 0xFFu), STATIC(0x00u, 0xFFu), BREATHE(0x01u, 0xFFu), SWIRL(0x0Au, 0x4Au)
}

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
    val logo: Logo
    val fan: Fan
    val ring: Ring

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
        logo = Logo(getChannel(channels[8]))
        fan = Fan(getChannel(channels[9]))
        ring = Ring(getChannel(channels[10]))
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

    fun setChannel(channel: UByte, speed: UByte, mode: UByte, brightness: UByte, color: Color) = sendBytes(
        0x51u, 0x2Cu, 0x01u, 0u, channel, speed, 0x20u, mode, 0xFFu, brightness, color.r, color.g, color.b,
        0u, 0u, 0u, filler = 0xFFu
    )

    fun close() {
        libusb_close(handle.ptr)
        libusb_exit(null)
    }

    inner class Logo internal constructor(private var channel: UByteArray) {
        var color: Color
            get() = channel.let { Color(it[10], it[11], it[12]) }
            set(value) {
                channel = setChannel(0x05u, speed, mode.value, brightness, value)
            }

        var brightness: UByte
            get() = channel[9]
            set(value) {
                channel = setChannel(0x05u, speed, mode.value, value, color)
            }

        var mode: LedMode
            get() = LedMode.values().first { it.value == channel[7] }
            set(value) {
                channel = setChannel(0x05u, speed, value.value, brightness, color)
            }

        var speed: UByte
            get() = channel[5]
            set(value) {
                channel = setChannel(0x05u, value, mode.value, brightness, color)
            }
    }

    inner class Fan internal constructor(private var channel: UByteArray) {
        var color: Color
            get() = channel.let { Color(it[10], it[11], it[12]) }
            set(value) {
                channel = setChannel(0x06u, speed, mode.value, brightness, value)
            }

        var brightness: UByte
            get() = channel[9]
            set(value) {
                channel = setChannel(0x06u, speed, mode.value, value, color)
            }

        var mode: LedMode
            get() = LedMode.values().first { it.value == channel[7] }
            set(value) {
                channel = setChannel(0x06u, speed, value.value, brightness, color)
            }

        var speed: UByte
            get() = channel[5]
            set(value) {
                channel = setChannel(0x06u, value, mode.value, brightness, color)
            }
    }

    inner class Ring internal constructor(private var channel: UByteArray) {
        var color: Color
            get() = channel.let { Color(it[10], it[11], it[12]) }
            set(value) {
                channel = setChannel(mode.channel, speed, mode.mode, brightness, value)
            }

        var brightness: UByte
            get() = channel[9]
            set(value) {
                channel = setChannel(mode.channel, speed, mode.mode, value, color)
            }

        var mode: RingMode
            get() = RingMode.values().first { it.channel == channel[4] }
            set(value) {
                channel = setChannel(value.channel, speed, value.mode, brightness, color)
            }

        var speed: UByte
            get() = channel[5]
            set(value) {
                channel = setChannel(mode.channel, value, mode.mode, brightness, color)
            }
    }
}

fun WraithPrism.sendBytes(vararg bytes: UByte, bufferSize: Int = 64, filler: UByte = 0x0u) =
    sendBytes(bytes.copyInto(UByteArray(bufferSize) { filler }))

fun WraithPrism.getChannel(channel: UByte) = sendBytes(0x52u, 0x2Cu, 0x01u, 0u, channel)

fun WraithPrism.save() {
    sendBytes(0x50u, 0x55u)
}

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

fun WraithPrism.apply() = sendBytes(0x51u, 0x28u, 0u, 0u, 0xE0u)