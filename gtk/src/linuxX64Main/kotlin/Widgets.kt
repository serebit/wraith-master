package com.serebit.wraith.gtk

import com.serebit.wraith.core.*
import gtk3.*
import kotlinx.cinterop.*

internal val logoColorButton by lazy {
    gridColorButton(logo.color, logo.mode.supportsColor,
        staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateColor(logo, it) })
}

internal val logoBrightnessScale by lazy {
    gridScale(logo.brightness, 3, logo.mode.supportsBrightness,
        staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateBrightness(logo, it) })
}

internal val logoSpeedScale by lazy {
    gridScale(logo.speed, 5, logo.mode.supportsSpeed,
        staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateSpeed(logo, it) })
}

internal val fanColorButton by lazy {
    gridColorButton(fan.color, fan.mode.supportsColor,
        staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateColor(fan, it) })
}

internal val fanBrightnessScale by lazy {
    gridScale(fan.brightness, 3, fan.mode.supportsBrightness,
        staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateBrightness(fan, it) })
}

internal val fanSpeedScale by lazy {
    gridScale(fan.speed, 5, fan.mode.supportsSpeed,
        staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateSpeed(fan, it) })
}

internal val fanMirageToggle by lazy {
    gtk_switch_new()!!.apply {
        setSensitive(fan.mode != LedMode.OFF)
        connectSignal("state-set", staticCFunction<CPointer<GtkWidget>, Int, Boolean> { _, state ->
            fan.mirage = state != 0
            wraith.updateFanMirage()
            false
        })
    }
}

internal val ringColorButton by lazy {
    gridColorButton(ring.color, ring.mode.supportsColor,
        staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateColor(ring, it) })
}

internal val ringBrightnessScale by lazy {
    gridScale(ring.brightness, 3, ring.mode.supportsBrightness,
        staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateBrightness(ring, it) })
}

internal val ringSpeedScale by lazy {
    gridScale(ring.speed, 5, ring.mode.supportsSpeed,
        staticCFunction<CPointer<GtkWidget>, Unit> { wraith.updateSpeed(ring, it) })
}

internal val ringDirectionComboBox by lazy {
    gridComboBox(
        ring.direction, RotationDirection.values(), ring.mode.supportsDirection,
        staticCFunction<CPointer<GtkWidget>, Unit> {
            val text = gtk_combo_box_text_get_active_text(it.reinterpret())!!.toKString()
            wraith.update(ring) { direction = RotationDirection.valueOf(text.toUpperCase()) }
        }
    )
}

internal val ringMorseTextBox by lazy {
    gtk_entry_new()!!
}

internal val morseReloadTextButton by lazy {
    gtk_button_new_from_icon_name("gtk-ok", GtkIconSize.GTK_ICON_SIZE_BUTTON)!!.apply {
        addCss("button { min-height: unset; }")
    }
}

internal val ringMorseBox by lazy {
    gtk_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, 2)!!.apply {
        gtk_box_pack_end(reinterpret(), morseReloadTextButton, 0, 0, 0u)
        gtk_box_pack_end(reinterpret(), ringMorseTextBox, 0, 0, 0u)
        setSensitive(ring.mode == RingMode.MORSE)
    }
}
