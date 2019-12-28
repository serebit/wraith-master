@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package com.serebit.wraith

import cnames.structs.libusb_device
import cnames.structs.libusb_device_handle
import kotlinx.cinterop.*
import libusb.*

const val COOLER_MASTER_VENDOR_ID: UShort = 0x2516u
const val WRAITH_PRISM_PRODUCT_ID: UShort = 0x51u

const val ENDPOINT_IN: UByte = 0x83u
const val ENDPOINT_OUT: UByte = 0x04u

fun main() = memScoped {
    val init = libusb_init(null)
    check(init == 0) { "Failed to initialize libusb." }

    print("Finding and opening Wraith Prism USB device... ")
    val device = findWraithPrismDevice()?.open() ?: error("Failed to find Wraith Prism USB device.")
    println("Done.")

    print("Resetting device... ")
    libusb_reset_device(device.handle.ptr)
    println("Done.")

    print("Claiming interfaces... ")
    device.claimInterfaces()
    println("Done.")

    val outData = UByteArray(64)

    ubyteArrayOf(0x41u, 0x80u).copyInto(outData)
    device.transferOut(outData)
    device.transferIn(64)

    ubyteArrayOf(0x51u, 0x96u).copyInto(outData)
    device.transferOut(outData)
    device.transferIn(64)

    ubyteArrayOf(0x51u, 0x28u, 0u, 0u, 0xe0u).copyInto(outData)
    device.transferOut(outData)
    device.transferIn(64)

    outData.fill(0xffu)
    ubyteArrayOf(0x51u, 0x2cu, 0x01u, 0u, 0x06u, 0xffu, 0u, 0x01u, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0u, 0u, 0u)
        .copyInto(outData)
    device.transferOut(outData)
    device.transferIn(64)

    device.transferOut(outData)
    device.transferIn(64)

    outData.fill(0u)
    ubyteArrayOf(0x51u, 0xa0u, 0x01u, 0u, 0u, 0x03u, 0u, 0u, 0x05u, 0x06u).copyInto(outData)
    UByteArray(14) { 0xfeu }.copyInto(outData, destinationOffset = 10)
    device.transferOut(outData)
    device.transferIn(64)

    device.close()
    libusb_exit(null)
}

fun findWraithPrismDevice(): UsbDevice? = memScoped {
    val cDevices = loadUsbDevices()
    val descriptors = getUsbDeviceDescriptors(cDevices.map { it.ptr })
    val devices = descriptors.zip(cDevices).map { UsbDevice(it.first, it.second) }

    return devices.find(UsbDevice::isWraithPrism)
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
    val isWraithPrism
        get() = descriptor.idVendor == COOLER_MASTER_VENDOR_ID && descriptor.idProduct == WRAITH_PRISM_PRODUCT_ID

    fun open() = OpenedUsbDevice(device)
}

class OpenedUsbDevice(device: libusb_device) {
    private val activeConfig = memScoped {
        val configPtr = allocPointerTo<libusb_config_descriptor>()
        val err = libusb_get_active_config_descriptor(device.ptr, configPtr.ptr)

        check(err == 0) { "Failed to fetch active configuration for USB device with error code $err" }
        configPtr.value!!.pointed
    }
    val handle = memScoped {
        val handlePtr = allocPointerTo<libusb_device_handle>()
        val err = libusb_open(device.ptr, handlePtr.ptr)

        check(err == 0) { "Failed to open Cooler Master device with error code $err" }
        handlePtr.value!!.pointed
    }

    fun claimInterfaces() = memScoped {
        libusb_set_auto_detach_kernel_driver(handle.ptr, 1)
        for (i in 0 until activeConfig.bNumInterfaces.toInt()) {
            val err3 = libusb_claim_interface(handle.ptr, i)
            check(err3 == 0) { "Failed to claim interface $i with error $err3." }
        }
    }

    fun close() = libusb_close(handle.ptr)
}

fun OpenedUsbDevice.transferIn(numBytes: Int): UByteArray = memScoped {
    val bytes = UByteArray(numBytes)
    val err = libusb_interrupt_transfer(handle.ptr, ENDPOINT_IN, bytes.toCValues().ptr, bytes.size, null, 1000u)
    check(err == 0) { "Failed to transfer bytes to device OUT endpoint." }
    bytes
}

fun OpenedUsbDevice.transferOut(bytes: UByteArray) = memScoped {
    val err = libusb_interrupt_transfer(handle.ptr, ENDPOINT_OUT, bytes.toCValues().ptr, bytes.size, null, 1000u)
    check(err == 0) { "Failed to transfer bytes to device OUT endpoint." }
}
