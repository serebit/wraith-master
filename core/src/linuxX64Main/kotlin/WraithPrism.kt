@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package com.serebit.wraith.core

import cnames.structs.libusb_device
import cnames.structs.libusb_device_handle
import kotlinx.cinterop.*
import libusb.*

private const val ENDPOINT_IN: UByte = 0x83u
private const val ENDPOINT_OUT: UByte = 0x04u

private fun UByteArray.indexOfOrNull(value: UByte) = indexOf(value).let { if (it == -1) null else it }

enum class LedMode(
    val mode: UByte,
    val brightnesses: UByteArray = ubyteArrayOf(0x4Cu, 0x99u, 0xFFu),
    val speeds: UByteArray = ubyteArrayOf(),
    val supportsColor: Boolean = false
) {
    OFF(0x00u, ubyteArrayOf()),
    STATIC(0x01u, supportsColor = true),
    CYCLE(0x02u, ubyteArrayOf(0x10u, 0x40u, 0x7Fu), ubyteArrayOf(0x96u, 0x8Cu, 0x80u, 0x6Eu, 0x68u)),
    BREATHE(0x03u, speeds = ubyteArrayOf(0x3Cu, 0x37u, 0x31u, 0x2Cu, 0x26u), supportsColor = true)
}

enum class RingMode(
    val channel: UByte, val mode: UByte,
    val brightnesses: UByteArray = ubyteArrayOf(0x4Cu, 0x99u, 0xFFu),
    val speeds: UByteArray = ubyteArrayOf(),
    val supportsColor: Boolean = false,
    val colorSource: UByte = 0x20u
) {
    OFF(0xFEu, 0xFFu, ubyteArrayOf(), ubyteArrayOf()),
    STATIC(0x00u, 0xFFu, supportsColor = true),
    RAINBOW(0x07u, 0x05u, speeds = ubyteArrayOf(0x72u, 0x68u, 0x64u, 0x62u, 0x61u), colorSource = 0u),
    SWIRL(
        0x0Au, 0x4Au, speeds = ubyteArrayOf(0x77u, 0x74u, 0x6Eu, 0x6Bu, 0x67u),
        supportsColor = true, colorSource = 0u
    ),
    CHASE(
        0x09u, 0xC3u, speeds = ubyteArrayOf(0x77u, 0x74u, 0x6Eu, 0x6Bu, 0x67u),
        supportsColor = true, colorSource = 0u
    ),
    BOUNCE(0x08u, 0xFFu, speeds = ubyteArrayOf(0x77u, 0x74u, 0x6Eu, 0x6Bu, 0x67u), colorSource = 0x80u),
    MORSE(0x0Bu, 0x05u, supportsColor = true, colorSource = 0u),
    CYCLE(0x02u, 0xFFu, ubyteArrayOf(0x10u, 0x40u, 0x7Fu), ubyteArrayOf(0x96u, 0x8Cu, 0x80u, 0x6Eu, 0x68u)),
    BREATHE(0x01u, 0xFFu, speeds = ubyteArrayOf(0x3Cu, 0x37u, 0x31u, 0x2Cu, 0x26u), supportsColor = true)
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
    val logo: BasicLedDevice
    val fan: BasicLedDevice
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
        logo = BasicLedDevice(getChannelValues(channels[8]).sliceArray(4..12))
        fan = BasicLedDevice(getChannelValues(channels[9]).sliceArray(4..12))
        ring = Ring(getChannelValues(channels[10]).sliceArray(4..12))
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

    fun setChannelValues(device: LedDevice) =
        sendBytes(0x51u, 0x2Cu, 0x01u, 0u, *device.values, 0u, 0u, 0u, filler = 0xFFu)

    fun assignChannels() = sendBytes(
        0x51u, 0xA0u, 0x01u, 0u, 0u, 0x03u, 0u, 0u, logo.channel, fan.channel,
        *UByteArray(15) { ring.mode.channel }
    )

    fun close() {
        libusb_close(handle.ptr)
        libusb_exit(null)
    }
}

interface LedDevice {
    val values: UByteArray

    var color: Color
    var speed: UByte
    var brightness: UByte
}

class BasicLedDevice(initialValues: UByteArray) : LedDevice {
    val channel: UByte = initialValues[0]
    var mode: LedMode = LedMode.values().first { it.mode == initialValues[3] }
    override var color = initialValues.let { if (mode.supportsColor) Color(it[6], it[7], it[8]) else Color(0u, 0u, 0u) }
    override var speed = mode.speeds.indexOfOrNull(initialValues[1])?.plus(1)?.toUByte() ?: 3u
    override var brightness = mode.brightnesses.indexOfOrNull(initialValues[5])?.plus(1)?.toUByte() ?: 2u

    override val values: UByteArray
        get() {
            val brightness = mode.brightnesses.elementAtOrNull(brightness.toInt() - 1) ?: 0u
            val speed = mode.speeds.elementAtOrNull(speed.toInt() - 1) ?: 0x2Cu
            return ubyteArrayOf(channel, speed, 0x20u, mode.mode, 0xFFu, brightness, color.r, color.g, color.b)
        }
}

class Ring(initialValues: UByteArray) : LedDevice {
    var mode: RingMode = RingMode.values().first { it.channel == initialValues[0] }
    override var color = initialValues.let { if (mode.supportsColor) Color(it[6], it[7], it[8]) else Color(0u, 0u, 0u) }
    override var speed: UByte = mode.speeds.indexOfOrNull(initialValues[1])?.plus(1)?.toUByte() ?: 3u
    override var brightness: UByte = mode.brightnesses.indexOfOrNull(initialValues[5])?.plus(1)?.toUByte() ?: 2u

    override val values: UByteArray
        get() {
            val brightness = mode.brightnesses.elementAtOrNull(brightness.toInt() - 1) ?: 0x99u
            val speed = mode.speeds.elementAtOrNull(speed.toInt() - 1) ?: 0xFFu
            return ubyteArrayOf(
                mode.channel, speed, mode.colorSource, mode.mode,
                0xFFu, brightness, color.r, color.g, color.b
            )
        }
}

fun WraithPrism.sendBytes(vararg bytes: UByte, bufferSize: Int = 64, filler: UByte = 0x0u) =
    sendBytes(bytes.copyInto(UByteArray(bufferSize) { filler }))

fun WraithPrism.getChannelValues(channel: UByte) = sendBytes(0x52u, 0x2Cu, 0x01u, 0u, channel)
fun WraithPrism.save() = sendBytes(0x50u, 0x55u)
fun WraithPrism.apply() = sendBytes(0x51u, 0x28u, 0u, 0u, 0xE0u)

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

inline fun <T : LedDevice> WraithPrism.update(device: T, update: T.() -> Unit) {
    device.update()
    setChannelValues(device)
    assignChannels()
    apply()
}
