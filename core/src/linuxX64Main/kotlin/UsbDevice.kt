package com.serebit.wraith.core

import cnames.structs.libusb_device
import cnames.structs.libusb_device_handle
import kotlinx.cinterop.*
import libusb.*

sealed class WraithPrismResult {
    class Success(val device: WraithPrism) : WraithPrismResult()
    class Failure(val message: String) : WraithPrismResult()
}

private fun success(handle: CPointer<libusb_device_handle>, numInterfaces: Int) =
    WraithPrismResult.Success(WraithPrism(handle, numInterfaces))

private fun failure(message: String) = WraithPrismResult.Failure(message)

fun obtainWraithPrism(): WraithPrismResult = memScoped {
    val init = libusb_init(null)
    if (init != LIBUSB_SUCCESS) return failure("Libusb initialization returned error code ${libusb_error_name(init)}.")

    val cDevices = loadUsbDevices()
    val descriptors = getUsbDeviceDescriptors(cDevices)
    val devices = descriptors.zip(cDevices)
        .map { UsbDevice(it.first.idVendor, it.first.idProduct, it.second) }

    devices.singleOrNull { it.isWraithPrism }?.open()
        ?: failure("Couldn't find Wraith Prism. Make sure the internal USB 2.0 header is connected.")
            .also { libusb_exit(null) }
}

private fun MemScope.loadUsbDevices(): List<CPointer<libusb_device>> {
    val devicesPtr = allocPointerTo<CArrayPointerVar<libusb_device>>().ptr
    val len = libusb_get_device_list(null, devicesPtr)

    val devicesArray = devicesPtr.pointed.value!!
    return (0 until len).map { devicesArray[it]!! }.also { libusb_free_device_list(devicesArray, 1) }
}

private fun MemScope.getUsbDeviceDescriptors(devices: List<CPointer<libusb_device>>) = devices.map {
    val descriptor = alloc<libusb_device_descriptor>()
    val err = libusb_get_device_descriptor(it, descriptor.ptr)
    if (err != LIBUSB_SUCCESS) println("Failed to load device descriptor with error code $err")
    descriptor
}

@OptIn(ExperimentalUnsignedTypes::class)
private class UsbDevice(val vendorID: UShort, val productID: UShort, private val device: CPointer<libusb_device>) {
    val isWraithPrism get() = vendorID == COOLER_MASTER_VENDOR_ID && productID == WRAITH_PRISM_PRODUCT_ID

    val numInterfaces
        get() = memScoped {
            val configPtr = allocPointerTo<libusb_config_descriptor>()
            val err = libusb_get_active_config_descriptor(device, configPtr.ptr)

            check(err == 0) { "Failed to fetch active configuration for USB device with error code $err" }
            configPtr.value!!.pointed.bNumInterfaces.toInt().also { libusb_free_config_descriptor(configPtr.value) }
        }

    fun open(): WraithPrismResult = memScoped {
        val handlePtr = allocPointerTo<libusb_device_handle>()
        when (val err = libusb_open(device, handlePtr.ptr)) {
            LIBUSB_SUCCESS -> success(handlePtr.value!!, numInterfaces)
            LIBUSB_ERROR_ACCESS -> failure("Found a Wraith Prism, but don't have permission to connect. Try with sudo.")
            else -> {
                val name = libusb_error_name(err)
                failure("Found a Wraith Prism, but encountered $name when trying to open a connection.")
            }
        }
    }

    companion object {
        private const val COOLER_MASTER_VENDOR_ID: UShort = 0x2516u
        private const val WRAITH_PRISM_PRODUCT_ID: UShort = 0x51u
    }
}
