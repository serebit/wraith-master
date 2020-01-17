package com.serebit.wraith.core

import cnames.structs.libusb_device
import cnames.structs.libusb_device_handle
import kotlinx.cinterop.*
import libusb.*

sealed class WraithPrismResult {
    class Success(val device: WraithPrism) : WraithPrismResult()
    class Failure(val message: String) : WraithPrismResult()
}

private fun success(handle: libusb_device_handle, device: libusb_device) =
    WraithPrismResult.Success(WraithPrism(handle, device))

private fun failure(message: String) = WraithPrismResult.Failure(message)

fun obtainWraithPrism(): WraithPrismResult = memScoped {
    val init = libusb_init(null)
    if (init != LIBUSB_SUCCESS) return failure("Libusb initialization returned error code ${libusb_error_name(init)}.")

    val cDevices = loadUsbDevices()
    val descriptors = getUsbDeviceDescriptors(cDevices.map { it.ptr })
    val devices = descriptors.zip(cDevices).map { UsbDevice(it.first, it.second) }

    devices.singleOrNull { it.isWraithPrism }?.open()
        ?: failure("Couldn't find Wraith Prism. Make sure the internal USB 2.0 header is connected.")
            .also { libusb_exit(null) }
}

private fun MemScope.loadUsbDevices(): List<libusb_device> {
    val devicesPtr = allocPointerTo<CArrayPointerVar<libusb_device>>().ptr
    val len = libusb_get_device_list(null, devicesPtr)

    val devicesArray = devicesPtr.pointed.value!!
    return (0 until len).map { devicesArray[it]!!.pointed }.also { libusb_free_device_list(devicesArray, 1) }
}

private fun MemScope.getUsbDeviceDescriptors(devices: List<CPointer<libusb_device>>) = devices.map {
    val descriptor = alloc<libusb_device_descriptor>()
    val err = libusb_get_device_descriptor(it, descriptor.ptr)
    if (err != LIBUSB_SUCCESS) println("Failed to load device descriptor with error code $err")
    descriptor
}

private class UsbDevice(private val descriptor: libusb_device_descriptor, private val device: libusb_device) {
    @UseExperimental(ExperimentalUnsignedTypes::class)
    val isWraithPrism
        get() = descriptor.idVendor == COOLER_MASTER_VENDOR_ID && descriptor.idProduct == WRAITH_PRISM_PRODUCT_ID

    fun open(): WraithPrismResult = memScoped {
        val handlePtr = allocPointerTo<libusb_device_handle>()
        when (val err = libusb_open(device.ptr, handlePtr.ptr)) {
            LIBUSB_SUCCESS -> success(handlePtr.value!!.pointed, device)
            LIBUSB_ERROR_ACCESS -> failure("Found a Wraith Prism, but don't have permission to connect. Try with sudo.")
            else -> {
                val name = libusb_error_name(err)
                failure("Found a Wraith Prism, but encountered $name when trying to open a connection.")
            }
        }
    }

    @UseExperimental(ExperimentalUnsignedTypes::class)
    companion object {
        private const val COOLER_MASTER_VENDOR_ID: UShort = 0x2516u
        private const val WRAITH_PRISM_PRODUCT_ID: UShort = 0x51u
    }
}
