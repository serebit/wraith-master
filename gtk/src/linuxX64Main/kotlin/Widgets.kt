package com.serebit.wraith.gtk

import com.serebit.wraith.core.RotationDirection
import com.serebit.wraith.core.supportsBrightness
import com.serebit.wraith.core.supportsSpeed
import com.serebit.wraith.core.update
import gtk3.GtkWidget
import gtk3.gtk_combo_box_text_get_active_text
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString

val logo get() = wraith.logo
val fan get() = wraith.fan
val ring get() = wraith.ring

internal val logoColorButton by lazy {
    gridColorButton(logo.color, logo.mode.supportsColor, staticCFunction<CPointer<GtkWidget>, Unit> {
        wraith.updateColor(logo, it)
    })
}

internal val logoBrightnessScale by lazy {
    gridScale(logo.brightness, 3, logo.mode.supportsBrightness, staticCFunction<CPointer<GtkWidget>, Unit> {
        wraith.updateBrightness(logo, it)
    })
}

internal val logoSpeedScale by lazy {
    gridScale(logo.speed, 5, logo.mode.supportsSpeed, staticCFunction<CPointer<GtkWidget>, Unit> {
        wraith.updateSpeed(logo, it)
    })
}

internal val fanColorButton by lazy {
    gridColorButton(fan.color, fan.mode.supportsColor, staticCFunction<CPointer<GtkWidget>, Unit> {
        wraith.updateColor(fan, it)
    })
}

internal val fanBrightnessScale by lazy {
    gridScale(fan.brightness, 3, fan.mode.supportsBrightness, staticCFunction<CPointer<GtkWidget>, Unit> {
        wraith.updateBrightness(fan, it)
    })
}

internal val fanSpeedScale by lazy {
    gridScale(fan.speed, 5, fan.mode.supportsSpeed, staticCFunction<CPointer<GtkWidget>, Unit> {
        wraith.updateSpeed(fan, it)
    })
}

internal val ringColorButton by lazy {
    gridColorButton(ring.color, ring.mode.supportsColor, staticCFunction<CPointer<GtkWidget>, Unit> {
        wraith.updateColor(ring, it)
    })
}

internal val ringBrightnessScale by lazy {
    gridScale(ring.brightness, 3, ring.mode.supportsBrightness, staticCFunction<CPointer<GtkWidget>, Unit> {
        wraith.updateBrightness(ring, it)
    })
}

internal val ringSpeedScale by lazy {
    gridScale(ring.speed, 5, ring.mode.supportsSpeed, staticCFunction<CPointer<GtkWidget>, Unit> {
        wraith.updateSpeed(ring, it)
    })
}

internal val ringDirectionComboBox by lazy {
    val callback = staticCFunction<CPointer<GtkWidget>, Unit> {
        val text = gtk_combo_box_text_get_active_text(it.reinterpret())!!.toKString()
        wraith.update(ring) {
            direction = RotationDirection.valueOf(text.toUpperCase())
        }
    }
    gridComboBox(ring.direction, RotationDirection.values(), ring.mode.supportsDirection, callback)
}
