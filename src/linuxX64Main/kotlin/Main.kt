package com.serebit.wraith

import cnames.structs.libusb_device
import cnames.structs.libusb_device_handle
import kotlinx.cinterop.*
import libusb.*

fun main() = memScoped {
    val init = libusb_init(null)
    check(init == 0) { "Failed to initialize libusb" }

    val data = listOf(
        0x12, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    ).map { it.toUByte() }.toUByteArray().toCValues()

    val data2 = listOf(
        0x12, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    ).map { it.toUByte() }.toUByteArray().toCValues()

    val devices = getDeviceInfo()

    val coolerMasterDevices = devices.filter { it.second.idVendor.toInt() == 0x2516 }
        .also { println("Found ${it.size} Cooler Master devices.") }

    val wraithPrism = coolerMasterDevices.find { it.second.idProduct.toInt() == 0x51 }

    val handle = wraithPrism?.let { getHandle(it.first) }
    println("Obtained device handle for Wraith Prism")

    for (i in 0..2) {
        if (libusb_kernel_driver_active(handle, i) == 1) {
            val err = libusb_detach_kernel_driver(handle, i)
            check(err == 0) { "Failed to detach kernel driver from interface $i with error code $err"}
            val err3 = libusb_claim_interface(handle, i)
            check(err3 == 0) { "Failed to claim interface $i with error $err3" }
        }
    }

    println("Claimed interfaces for Wraith Prism")

    val transferred = allocArray<IntVar>(64)

    val err4 = libusb_interrupt_transfer(
        handle,
        0x04,
        data,
        64,
        transferred,
        1000
    )

    val transferred2 = allocArray<IntVar>(64)

    val err5 = libusb_interrupt_transfer(
        handle,
        0x04,
        data2,
        64,
        transferred2,
        1000
    )

    libusb_exit(null)
}

fun MemScope.getDeviceInfo(): List<Pair<CPointer<libusb_device>, libusb_device_descriptor>> {
    val devices = loadUsbDevices()
    val descriptors = getUsbDeviceDescriptors(devices)
    return devices.zip(descriptors)
}

fun MemScope.loadUsbDevices(): List<CPointer<libusb_device>> {
    val devicesPtr = allocPointerTo<CArrayPointerVar<libusb_device>>().ptr
    val len = libusb_get_device_list(null, devicesPtr)
    println("Found $len USB devices.")
    val devicesArray = devicesPtr.pointed.value!!
    return (0 until len).map { devicesArray[it]!! }.also { libusb_free_device_list(devicesArray, 1) }
}

fun MemScope.getUsbDeviceDescriptors(devices: List<CPointer<libusb_device>>) = devices.map {
    val descriptor = alloc<libusb_device_descriptor>()
    val err = libusb_get_device_descriptor(it, descriptor.ptr)
    if (err != 0) println("Failed to load device with error code $err")
    descriptor
}

fun MemScope.getHandle(device: CPointer<libusb_device>): CPointer<libusb_device_handle> {
    val handle = allocPointerTo<libusb_device_handle>()
    val err = libusb_open(device, handle.ptr)

    check(err == 0) { "Failed to open Cooler Master device with error code $err" }
    return handle.value!!
}
