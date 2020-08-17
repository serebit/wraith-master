package com.serebit.wraith.core

import com.serebit.wraith.core.prism.WraithPrism
import hidapi.hid_enumerate
import hidapi.hid_free_enumeration
import hidapi.hid_init
import hidapi.hid_open

@OptIn(ExperimentalUnsignedTypes::class)
private const val COOLER_MASTER_VENDOR_ID: UShort = 0x2516u

@OptIn(ExperimentalUnsignedTypes::class)
private const val WRAITH_PRISM_PRODUCT_ID: UShort = 0x51u

sealed class DeviceResult {
    class Success(val prism: WraithPrism) : DeviceResult()
    class Failure(val message: String) : DeviceResult()
}

@OptIn(ExperimentalUnsignedTypes::class)
fun obtainWraithPrism(): DeviceResult {
    if (hid_init() != 0) {
        return DeviceResult.Failure("HIDAPI failed to initialize.")
    }

    var foundDevice = false
    hid_enumerate(COOLER_MASTER_VENDOR_ID, WRAITH_PRISM_PRODUCT_ID)?.let {
        foundDevice = true
        hid_free_enumeration(it)
    }

    return if (foundDevice) {
        hid_open(COOLER_MASTER_VENDOR_ID, WRAITH_PRISM_PRODUCT_ID, null)
            ?.let { DeviceResult.Success(WraithPrism(it)) }
            ?: DeviceResult.Failure("Found a Wraith Prism, but couldn't connect to it. Try using sudo.")
    } else {
        DeviceResult.Failure("Couldn't find a Wraith Prism. Make sure the internal USB 2.0 cable is connected.")
    }
}
