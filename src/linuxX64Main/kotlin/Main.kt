@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package com.serebit.wraith

import cnames.structs.libusb_device
import kotlinx.cinterop.*
import libusb.*

const val COOLER_MASTER_VENDOR_ID: UShort = 0x2516u
const val WRAITH_PRISM_PRODUCT_ID: UShort = 0x51u

const val ENDPOINT_IN: UByte = 0x83u
const val ENDPOINT_OUT: UByte = 0x04u

val device = memScoped {
    print("Finding and opening Wraith Prism USB device... ")
    val init = libusb_init(null)
    check(init == 0) { "Failed to initialize libusb." }
    findWraithPrism()?.apply { initialize() } ?: error("Failed to find Wraith Prism USB device.")
}

fun main() = memScoped {
    gtkMain(emptyArray())

    UByteArray(64).let { outData ->
        ubyteArrayOf(0x51u, 0xa0u, 0x01u, 0u, 0u, 0x03u, 0u, 0u, 0x05u, 0x06u).copyInto(outData)
        UByteArray(3) { 0xFEu }.copyInto(outData, destinationOffset = 10)
        device.sendBytes(outData)
    }

    device.close()
    libusb_exit(null)
}

fun findWraithPrism(): WraithPrism? = memScoped {
    val cDevices = loadUsbDevices()
    val descriptors = getUsbDeviceDescriptors(cDevices.map { it.ptr })
    val devices = descriptors.zip(cDevices).map { UsbDevice(it.first, it.second) }

    return devices.mapNotNull { it.open() }.singleOrNull()
}

fun loadUsbDevices(): List<libusb_device> = memScoped {
    val devicesPtr = allocPointerTo<CArrayPointerVar<libusb_device>>().ptr
    val len = libusb_get_device_list(null, devicesPtr)

    val devicesArray = devicesPtr.pointed.value!!
    return (0 until len).map { devicesArray[it]!!.pointed }.also { libusb_free_device_list(devicesArray, 1) }
}

fun MemScope.getUsbDeviceDescriptors(devices: List<CPointer<libusb_device>>) = devices.map {
    val descriptor = alloc<libusb_device_descriptor>()
    val err = libusb_get_device_descriptor(it, descriptor.ptr)
    if (err != 0) println("Failed to load device descriptor with error code $err")
    descriptor
}

class UsbDevice(private val descriptor: libusb_device_descriptor, private val device: libusb_device) {
    private val isWraithPrism
        get() = descriptor.idVendor == COOLER_MASTER_VENDOR_ID && descriptor.idProduct == WRAITH_PRISM_PRODUCT_ID

    fun open(): WraithPrism? = if (isWraithPrism) WraithPrism(device) else null
}
