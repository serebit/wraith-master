package com.serebit.wraith.core

import cnames.structs.libusb_device_handle
import kotlinx.cinterop.*
import libusb.libusb_close
import libusb.libusb_interrupt_transfer
import libusb.libusb_release_interface

@OptIn(ExperimentalUnsignedTypes::class)
class UsbInterface(private val requestSize: Int, private val handle: CPointer<libusb_device_handle>) {
    fun sendBytes(vararg bytes: Int, filler: Int = 0): List<Int> {
        return transfer(UByteArray(requestSize) { (bytes.getOrNull(it) ?: filler).toUByte() }).map { it.toInt() }
    }

    private fun transfer(outBytes: UByteArray): UByteArray = memScoped {
        libusb_interrupt_transfer(handle, ENDPOINT_OUT, outBytes.toCValues(), requestSize, null, TIMEOUT)

        allocArray<UByteVar>(requestSize).also { inBytes ->
            libusb_interrupt_transfer(handle, ENDPOINT_IN, inBytes, requestSize, null, TIMEOUT)
        }.pointed.readValues(requestSize).getBytes().toUByteArray()
    }

    fun close() {
        libusb_release_interface(handle, HID_INTERFACE)
        libusb_close(handle)
    }

    companion object {
        private const val ENDPOINT_IN: UByte = 0x83u
        private const val ENDPOINT_OUT: UByte = 4u
        private const val HID_INTERFACE: Int = 1
        private const val TIMEOUT: UInt = 1000u
    }
}
