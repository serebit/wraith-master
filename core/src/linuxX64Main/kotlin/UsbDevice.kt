@file:UseExperimental(ExperimentalUnsignedTypes::class)

package com.serebit.wraith.core

import cnames.structs.libusb_device
import kotlinx.cinterop.*
import libusb.*

private const val COOLER_MASTER_VENDOR_ID: UShort = 0x2516u
private const val WRAITH_PRISM_PRODUCT_ID: UShort = 0x51u

fun obtainWraithPrism(): WraithPrism? = memScoped {
    val init = libusb_init(null)
    check(init == 0) { "Failed to initialize libusb." }
    findWraithPrism() ?: null.also { libusb_exit(null) }
}

private fun MemScope.findWraithPrism(): WraithPrism? {
    val cDevices = loadUsbDevices()
    val descriptors = getUsbDeviceDescriptors(cDevices.map { it.ptr })
    val devices = descriptors.zip(cDevices).map { UsbDevice(it.first, it.second) }

    return devices.mapNotNull { it.open() }.singleOrNull()
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
    if (err != 0) println("Failed to load device descriptor with error code $err")
    descriptor
}

private class UsbDevice(private val descriptor: libusb_device_descriptor, private val device: libusb_device) {
    @UseExperimental(ExperimentalUnsignedTypes::class)
    private val isWraithPrism
        get() = descriptor.idVendor == COOLER_MASTER_VENDOR_ID && descriptor.idProduct == WRAITH_PRISM_PRODUCT_ID

    fun open(): WraithPrism? = if (isWraithPrism) WraithPrism(device) else null
}
