package com.serebit.wraith.gtk

import com.serebit.wraith.core.*
import gtk3.*
import kotlinx.cinterop.*
import kotlin.math.absoluteValue

internal val logoColorBox by lazy {
    gtk_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, 4)!!.apply {
        gtk_box_pack_end(reinterpret(), logoColorButton, 0, 0, 0u)
        gtk_box_pack_end(reinterpret(), logoRandomColorCheckbox, 0, 0, 0u)
    }
}

internal val logoRandomColorCheckbox by lazy {
    gtk_check_button_new_with_label("Randomize?")!!.apply {
        setSensitive(logo.mode.colorSupport == ColorSupport.ALL)
        gtk_toggle_button_set_active(reinterpret(), logo.useRandomColor.toByte().toInt())
        connectSignal("toggled", staticCFunction<CPointer<GtkWidget>, Unit> {
            val isActive = gtk_toggle_button_get_active(it.reinterpret())
            if (logo.mode.colorSupport == ColorSupport.ALL)
                wraith.update(logo) { useRandomColor = isActive == 1 }
            gtk_widget_set_sensitive(logoColorButton, (isActive - 1).absoluteValue)
        })
    }
}

internal val logoColorButton by lazy {
    gridColorButton(logo.color, logo.mode.colorSupport != ColorSupport.NONE,
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

internal val fanColorBox by lazy {
    gtk_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, 4)!!.apply {
        gtk_box_pack_end(reinterpret(), fanColorButton, 0, 0, 0u)
        gtk_box_pack_end(reinterpret(), fanRandomColorCheckbox, 0, 0, 0u)
    }
}

internal val fanRandomColorCheckbox by lazy {
    gtk_check_button_new_with_label("Randomize?")!!.apply {
        setSensitive(fan.mode.colorSupport == ColorSupport.ALL)
        gtk_toggle_button_set_active(reinterpret(), fan.useRandomColor.toByte().toInt())
        connectSignal("toggled", staticCFunction<CPointer<GtkWidget>, Unit> {
            val isActive = gtk_toggle_button_get_active(it.reinterpret())
            if (fan.mode.colorSupport == ColorSupport.ALL)
                wraith.update(fan) { useRandomColor = isActive == 1 }
            gtk_widget_set_sensitive(fanColorButton, (isActive - 1).absoluteValue)
        })
    }
}

internal val fanColorButton by lazy {
    gridColorButton(fan.color, fan.mode.colorSupport != ColorSupport.NONE,
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
        gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_END)
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
        connectSignal("state-set", staticCFunction<CPointer<GtkWidget>, Int, Boolean> { _, state ->
            fan.mirage = state != 0
            wraith.updateFanMirage()
            false
        })
    }
}

internal val ringColorBox by lazy {
    gtk_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, 4)!!.apply {
        gtk_box_pack_end(reinterpret(), ringColorButton, 0, 0, 0u)
        gtk_box_pack_end(reinterpret(), ringRandomColorCheckbox, 0, 0, 0u)
    }
}

internal val ringRandomColorCheckbox by lazy {
    gtk_check_button_new_with_label("Randomize?")!!.apply {
        setSensitive(ring.mode.colorSupport == ColorSupport.ALL)
        gtk_toggle_button_set_active(reinterpret(), ring.useRandomColor.toByte().toInt())
        connectSignal("toggled", staticCFunction<CPointer<GtkWidget>, Unit> {
            val isActive = gtk_toggle_button_get_active(it.reinterpret())
            if (ring.mode.colorSupport == ColorSupport.ALL)
                wraith.update(ring) { useRandomColor = isActive == 1 }
            gtk_widget_set_sensitive(ringColorButton, (isActive - 1).absoluteValue)
        })
    }
}

internal val ringColorButton by lazy {
    gridColorButton(ring.color, ring.mode.colorSupport != ColorSupport.NONE,
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

@OptIn(ExperimentalUnsignedTypes::class)
internal val ringMorseBox by lazy {
    gtk_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, 4)!!.apply {
        gtk_box_pack_end(reinterpret(), morseReloadTextButton, 0, 0, 0u)
        gtk_box_pack_end(reinterpret(), ringMorseTextBox, 0, 0, 0u)
        setSensitive(ring.mode == RingMode.MORSE)
    }
}

private var ringMorseTextBoxHintLabel: CPointer<GtkWidget>? = null
internal var ringMorseTextBoxHint: CPointer<GtkWidget>? = null

@OptIn(ExperimentalUnsignedTypes::class)
private val String.hintText: String
    get() = when {
        isBlank() -> "Enter either morse code (dots and dashes) or text"
        parseMorseOrTextToBytes().size > 120 -> "Maximum length exceeded"
        isMorseCode -> "Parsing as morse code"
        isValidMorseText -> "Parsing as text"
        else -> "Invalid characters detected: $invalidMorseChars"
    }

private fun changeCallback(pointer: CPointer<GtkWidget>) {
    ringMorseTextBoxHintLabel?.apply {
        val entryText = pointer.text
        gtk_label_set_text(reinterpret(), entryText.hintText)
        val isInvalid = !entryText.isValidMorseText && !entryText.isMorseCode
        if (entryText.parseMorseOrTextToBytes().size > 120 || isInvalid) {
            gtk_widget_set_sensitive(morseReloadTextButton, 0)
        } else {
            gtk_widget_set_sensitive(morseReloadTextButton, 1)
        }
    }
}

internal val ringMorseTextBox by lazy {
    gtk_entry_new()!!.apply {
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
        gtk_entry_set_icon_from_icon_name(
            reinterpret(),
            GtkEntryIconPosition.GTK_ENTRY_ICON_SECONDARY,
            "dialog-information"
        )

        connectSignal("changed", staticCFunction<CPointer<GtkWidget>, Unit> { changeCallback(it) })
        connectSignal("icon-press", staticCFunction<CPointer<GtkWidget>, Unit> {
            when {
                ringMorseTextBoxHint == null -> {
                    ringMorseTextBoxHintLabel = gtk_label_new(it.text.hintText)!!
                    ringMorseTextBoxHint = gtk_popover_new(it)!!.apply {
                        gtk_popover_set_modal(reinterpret(), 0)
                        gtk_container_set_border_width(reinterpret(), 8u)
                        gtk_container_add(reinterpret(), ringMorseTextBoxHintLabel)
                        gtk_widget_show_all(this)
                    }
                }
                gtk_widget_is_visible(ringMorseTextBoxHint) == 1 -> gtk_widget_hide(ringMorseTextBoxHint)
                else -> gtk_widget_show(ringMorseTextBoxHint)
            }
        })
    }
}

internal val morseReloadTextButton by lazy {
    gtk_button_new_from_icon_name("gtk-ok", GtkIconSize.GTK_ICON_SIZE_BUTTON)!!.apply {
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
        connectSignal("clicked", staticCFunction<CPointer<GtkWidget>, Unit> {
            wraith.updateRingMorseText(ringMorseTextBox.text)
        })
    }
}
