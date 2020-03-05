package com.serebit.wraith.gtk

import com.serebit.wraith.core.*
import gtk3.*
import kotlinx.cinterop.*

sealed class ComponentWidgets<C : LedComponent> {
    abstract val component: C
    open val widgets by lazy { listOf(colorBox, brightnessScale, speedScale) }

    @OptIn(ExperimentalUnsignedTypes::class)
    val colorBox by lazy {
        gtk_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, 4)!!.apply {
            gtk_box_pack_end(reinterpret(), colorButton, 0, 0, 0u)
            gtk_box_pack_end(reinterpret(), randomizeColorCheckbox, 0, 0, 0u)
        }
    }
    val randomizeColorCheckbox by lazy {
        gtk_check_button_new_with_label("Randomize?")!!.apply {
            setSensitive(component.mode.colorSupport == ColorSupport.ALL)
            gtk_toggle_button_set_active(reinterpret(), component.useRandomColor.toByte().toInt())
            connectSignal("toggled", onRandomizeChange)
        }
    }
    val colorButton by lazy {
        gridColorButton(component.colorOrBlack, component.mode.colorSupport != ColorSupport.NONE, onColorChange)
    }
    val brightnessScale by lazy {
        gridScale(component.brightness, 3, component.mode.supportsBrightness, onBrightnessChange)
    }
    val speedScale by lazy {
        gridScale(component.speed, 5, component.mode.supportsSpeed, onSpeedChange)
    }

    abstract val onColorChange: GtkCallbackFunction
    abstract val onBrightnessChange: GtkCallbackFunction
    abstract val onSpeedChange: GtkCallbackFunction
    abstract val onRandomizeChange: GtkCallbackFunction
}

object LogoWidgets : ComponentWidgets<LogoComponent>() {
    override val component get() = wraith.logo
    override val onColorChange = staticCFunction<Widget, Unit> { wraith.updateColor(logo, it) }
    override val onBrightnessChange = staticCFunction<Widget, Unit> { wraith.updateBrightness(logo, it) }
    override val onSpeedChange = staticCFunction<Widget, Unit> { wraith.updateSpeed(logo, it) }
    override val onRandomizeChange =
        staticCFunction<Widget, Unit> { wraith.updateRandomize(logo, it, colorButton) }
}

object FanWidgets : ComponentWidgets<FanComponent>() {
    override val component get() = wraith.fan
    override val widgets by lazy { listOf(colorBox, brightnessScale, speedScale, mirageToggle) }
    val mirageToggle by lazy {
        gtk_switch_new()!!.apply {
            setSensitive(fan.mode != LedMode.OFF)
            gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_END)
            gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
            connectSignal("state-set", staticCFunction<Widget, Int, Boolean> { _, state ->
                fan.mirage = state != 0
                wraith.updateFanMirage()
                false
            })
        }
    }

    override val onColorChange = staticCFunction<Widget, Unit> { wraith.updateColor(fan, it) }
    override val onBrightnessChange = staticCFunction<Widget, Unit> { wraith.updateBrightness(fan, it) }
    override val onSpeedChange = staticCFunction<Widget, Unit> { wraith.updateSpeed(fan, it) }
    override val onRandomizeChange =
        staticCFunction<Widget, Unit> { wraith.updateRandomize(fan, it, colorButton) }
}

object RingWidgets : ComponentWidgets<RingComponent>() {
    override val component get() = wraith.ring
    override val widgets by lazy { listOf(colorBox, brightnessScale, speedScale, morseContainer) }
    private var ringMorseTextBoxHintLabel: Widget? = null
    var morseTextBoxHint: Widget? = null

    val directionComboBox by lazy {
        gridComboBox(
            ring.direction.name, RotationDirection.values().map { it.name }, ring.mode.supportsDirection,
            staticCFunction<Widget, Unit> {
                val text = gtk_combo_box_text_get_active_text(it.reinterpret())!!.toKString()
                wraith.update(ring) { direction = RotationDirection.valueOf(text.toUpperCase()) }
            }
        )
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    val morseContainer by lazy {
        gtk_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, 4)!!.apply {
            gtk_box_pack_end(reinterpret(), morseReloadButton, 0, 0, 0u)
            gtk_box_pack_end(reinterpret(), morseTextBox, 0, 0, 0u)
            setSensitive(ring.mode == RingMode.MORSE)
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private val String.hintText: String
        get() = when {
            isBlank() -> "Enter either morse code (dots and dashes) or text"
            parseMorseOrTextToBytes().size > 120 -> "Maximum length exceeded"
            isMorseCode -> "Parsing as morse code"
            isValidMorseText -> "Parsing as text"
            else -> "Invalid characters detected: $invalidMorseChars"
        }

    private val morseTextBox by lazy {
        gtk_entry_new()!!.apply {
            gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
            gtk_entry_set_icon_from_icon_name(
                reinterpret(),
                GtkEntryIconPosition.GTK_ENTRY_ICON_SECONDARY,
                "dialog-information"
            )

            connectSignal("changed", staticCFunction<Widget, Unit> { changeCallback(it) })
            connectSignal("icon-press", staticCFunction<Widget, Unit> {
                when {
                    morseTextBoxHint == null -> {
                        ringMorseTextBoxHintLabel = gtk_label_new(it.text.hintText)!!
                        morseTextBoxHint = gtk_popover_new(it)!!.apply {
                            gtk_popover_set_modal(reinterpret(), 0)
                            gtk_container_set_border_width(reinterpret(), 8u)
                            gtk_container_add(reinterpret(), ringMorseTextBoxHintLabel)
                            gtk_widget_show_all(this)
                        }
                    }
                    gtk_widget_is_visible(morseTextBoxHint) == 1 -> gtk_widget_hide(morseTextBoxHint)
                    else -> gtk_widget_show(morseTextBoxHint)
                }
            })
        }
    }

    private val morseReloadButton by lazy {
        gtk_button_new_from_icon_name("gtk-ok", GtkIconSize.GTK_ICON_SIZE_BUTTON)!!.apply {
            gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
            connectSignal("clicked", staticCFunction<Widget, Unit> {
                wraith.updateRingMorseText(morseTextBox.text)
            })
        }
    }

    private fun changeCallback(pointer: Widget) {
        ringMorseTextBoxHintLabel?.apply {
            val entryText = pointer.text
            gtk_label_set_text(reinterpret(), entryText.hintText)
            val isInvalid = !entryText.isValidMorseText && !entryText.isMorseCode
            if (entryText.parseMorseOrTextToBytes().size > 120 || isInvalid) {
                gtk_widget_set_sensitive(morseReloadButton, 0)
            } else {
                gtk_widget_set_sensitive(morseReloadButton, 1)
            }
        }
    }

    override val onColorChange = staticCFunction<Widget, Unit> { wraith.updateColor(ring, it) }
    override val onBrightnessChange = staticCFunction<Widget, Unit> { wraith.updateBrightness(ring, it) }
    override val onSpeedChange = staticCFunction<Widget, Unit> { wraith.updateSpeed(ring, it) }
    override val onRandomizeChange =
        staticCFunction<Widget, Unit> { wraith.updateRandomize(ring, it, colorButton) }
}
