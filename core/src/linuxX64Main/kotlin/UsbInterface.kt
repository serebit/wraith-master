package com.serebit.wraith.core

import cnames.structs.libusb_device_handle
import kotlinx.cinterop.*
import libusb.*

class TransferError(val code: Int) : Throwable(libusb_strerror(code)!!.toKString())

internal class UsbInterface(private val requestSize: Int, private val handle: CPointer<libusb_device_handle>) {
    fun sendBytes(vararg bytes: UByte, filler: UByte = 0u): List<UByte> = memScoped {
        val outBytes = UByteArray(requestSize) { (bytes.getOrNull(it) ?: filler).toUByte() }
        transfer(ENDPOINT_OUT, outBytes.toCValues().ptr)
        return transfer(ENDPOINT_IN, allocArray(requestSize)).toList()
    }

    fun close() {
        libusb_release_interface(handle, HID_INTERFACE)
        libusb_close(handle)
    }

    private fun transfer(endpoint: UByte, bytes: CPointer<UByteVar>): UByteArray {
        val err = libusb_interrupt_transfer(handle, endpoint, bytes, requestSize, null, TIMEOUT)
        if (err != LIBUSB_SUCCESS) {
            throw TransferError(err)
        }
        return bytes.pointed.readValues(requestSize).getBytes().toUByteArray()
    }

    companion object {
        private const val ENDPOINT_IN: UByte = 0x83u
        private const val ENDPOINT_OUT: UByte = 4u
        private const val HID_INTERFACE: Int = 1
        private const val TIMEOUT: UInt = 1000u
    }
}
