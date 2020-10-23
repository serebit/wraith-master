package com.serebit.wraith.core

import cnames.structs.libusb_device
import cnames.structs.libusb_device_handle
import com.serebit.wraith.core.prism.WraithPrism
import kotlinx.cinterop.*
import libusb.*

private fun initLibusb() = libusb_init(null).also {
    if (it != LIBUSB_SUCCESS) error("Libusb initialization returned error code ${libusb_error_name(it)!!.toKString()}.")
}

sealed class DeviceResult {
    class Success(val prism: WraithPrism) : DeviceResult()
    class Failure(val message: String) : DeviceResult()
}

fun obtainWraithPrism() = memScoped {
    initLibusb()

    val cDevices = loadUsbDevices()
    val descriptors = getUsbDeviceDescriptors(cDevices)
    val devices = descriptors.zip(cDevices).map { UsbDevice(it.first.idVendor, it.first.idProduct, it.second) }

    fun success(handle: CPointer<libusb_device_handle>) = DeviceResult.Success(WraithPrism(UsbInterface(64, handle)))
    fun failure(message: String) = DeviceResult.Failure(message)

    val device = devices.singleOrNull { it.isWraithPrism }?.device
        ?: return@memScoped failure("Couldn't find a Wraith Prism. Make sure the internal USB 2.0 cable is connected.")

    val handlePtr = allocPointerTo<libusb_device_handle>()
    libusb_open(device, handlePtr.ptr).also {
        when {
            it == LIBUSB_ERROR_ACCESS -> return failure("Found a Wraith Prism, but don't have permission to connect. Try with sudo.")
            it != LIBUSB_SUCCESS -> return failure("Found a Wraith Prism, but encountered ${libusb_error_name(it)!!.toKString()} when trying to connect.")
        }
    }
    val handle = handlePtr.value!!

    libusb_set_auto_detach_kernel_driver(handle, 1)

    when (val err = libusb_claim_interface(handle, 1)) {
        LIBUSB_SUCCESS -> success(handle)
        LIBUSB_ERROR_BUSY -> failure("Failed to claim the Wraith Prism's USB interface, as it is currently in use by another process.")
        else -> failure("Failed to claim the Wraith Prism's USB interface due to error ${libusb_error_name(err)!!.toKString()}.")
    }
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
    if (err != LIBUSB_SUCCESS) {
        println("Failed to load device descriptor due to error ${libusb_error_name(err)!!.toKString()}")
    }
    descriptor
}

private class UsbDevice(val vendorID: UShort, val productID: UShort, val device: CPointer<libusb_device>) {
    val isWraithPrism get() = vendorID == COOLER_MASTER_VENDOR_ID && productID == WRAITH_PRISM_PRODUCT_ID

    companion object {
        private const val COOLER_MASTER_VENDOR_ID: UShort = 0x2516u
        private const val WRAITH_PRISM_PRODUCT_ID: UShort = 0x51u
    }
}
