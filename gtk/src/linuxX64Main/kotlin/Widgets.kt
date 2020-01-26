package com.serebit.wraith.gtk

import com.serebit.wraith.core.*
import gtk3.GtkWidget
import gtk3.gtk_combo_box_text_get_active_text
import gtk3.gtk_switch_new
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString

internal val logoColorButton by lazyGridColorButton(logo.color, logo.mode.supportsColor,
    staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateColor(logo, it) })

internal val logoBrightnessScale by lazyGridScale(logo.brightness, 3, logo.mode.supportsBrightness,
    staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateBrightness(logo, it) })

internal val logoSpeedScale by lazyGridScale(logo.speed, 5, logo.mode.supportsSpeed,
    staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateSpeed(logo, it) })

internal val fanColorButton by lazyGridColorButton(fan.color, fan.mode.supportsColor,
    staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateColor(fan, it) })

internal val fanBrightnessScale by lazyGridScale(fan.brightness, 3, fan.mode.supportsBrightness,
    staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateBrightness(fan, it) })

internal val fanSpeedScale by lazyGridScale(fan.speed, 5, fan.mode.supportsSpeed,
    staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateSpeed(fan, it) })

internal val fanMirageToggle by lazy {
    gtk_switch_new()!!.apply {
        setSensitive(wraith.fan.mode != LedMode.OFF)
        connectSignal("state-set", staticCFunction<CPointer<GtkWidget>, Int, Boolean> { _, state ->
            wraith.fan.mirage = state != 0
            wraith.updateFanMirage()
            false
        })
    }
}

internal val ringColorButton by lazyGridColorButton(ring.color, ring.mode.supportsColor,
    staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateColor(ring, it) })

internal val ringBrightnessScale by lazyGridScale(ring.brightness, 3, ring.mode.supportsBrightness,
    staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateBrightness(ring, it) })

internal val ringSpeedScale by lazyGridScale(ring.speed, 5, ring.mode.supportsSpeed,
    staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateSpeed(ring, it) })

internal val ringDirectionComboBox by lazyGridComboBox(
    ring.direction, RotationDirection.values(), ring.mode.supportsDirection,
    staticCFunction<CPointer<GtkWidget>, Unit> {
        val text = gtk_combo_box_text_get_active_text(it.reinterpret())!!.toKString()
        wraith.update(ring) {
            direction = RotationDirection.valueOf(text.toUpperCase())
        }
    })
