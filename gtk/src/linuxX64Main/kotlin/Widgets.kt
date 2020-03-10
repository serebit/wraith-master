package com.serebit.wraith.gtk

import com.serebit.wraith.core.*
import gtk3.*
import kotlinx.cinterop.*

private inline fun <reified C : ComponentWidgets<*>> COpaquePointer.useWith(task: C.() -> Unit) {
    val ref = asStableRef<C>()
    ref.get().task()
    ref.dispose()
}

private fun COpaquePointer.use(task: ComponentWidgets<*>.() -> Unit) = useWith(task)

private inline fun ComponentWidgets<*>.basicReload(additional: () -> Unit = {}) {
    gtk_combo_box_set_active(modeBox.reinterpret(), component.mode.index)
    memScoped { gtk_color_button_set_rgba(colorButton.reinterpret(), gdkRgba(component.colorOrBlack).ptr) }

    val useRandomColor = component.mode.colorSupport == ColorSupport.ALL && component.useRandomColor
    gtk_toggle_button_set_active(randomizeColorCheckbox.reinterpret(), useRandomColor.toByte().toInt())

    randomizeColorCheckbox.setSensitive(component.mode.colorSupport == ColorSupport.ALL)
    colorButton.setSensitive(component.mode.colorSupport != ColorSupport.NONE && !component.useRandomColor)
    brightnessScale.setSensitive(component.mode.supportsBrightness)
    speedScale.setSensitive(component.mode.supportsSpeed)
    additional()
}

sealed class ComponentWidgets<C : LedComponent>(val component: C) {
    protected val ptr: COpaquePointer get() = StableRef.create(this).asCPointer()
    open val widgets get() = listOf(modeBox, colorBox, brightnessScale, speedScale)

    open val modeBox by lazy {
        gridComboBox(component.mode.name, LedMode.values.map { it.name }, true).apply {
            connectSignalWithData("changed", ptr, onModeChange)
        }
    }

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
            connectSignalWithData("toggled", ptr, onRandomizeChange)
        }
    }
    val colorButton by lazy {
        gridColorButton(
            component.colorOrBlack, component.mode.colorSupport != ColorSupport.NONE && !component.useRandomColor
        ).apply {
            connectSignalWithData("color-set", ptr, onColorChange)
        }
    }
    val brightnessScale by lazy {
        gridScale(component.brightness, 3, component.mode.supportsBrightness, ptr, onBrightnessChange)
    }
    val speedScale by lazy {
        gridScale(component.speed, 5, component.mode.supportsSpeed, ptr, onSpeedChange)
    }

    open fun fullReload() = basicReload()

    protected val onModeChange: CmpCallbackFunction
        get() = staticCFunction { it, ptr ->
            ptr.use { wraith.updateMode(component, it); fullReload() }
        }
    private val onColorChange: CmpCallbackFunction
        get() = staticCFunction { it, ptr ->
            ptr.use { wraith.updateColor(component, it) }
        }
    private val onBrightnessChange: CmpCallbackFunction
        get() = staticCFunction { it, ptr ->
            ptr.use { wraith.updateBrightness(component, it) }
        }
    private val onSpeedChange: CmpCallbackFunction
        get() = staticCFunction { it, ptr ->
            ptr.use { wraith.updateSpeed(component, it) }
        }
    private val onRandomizeChange: CmpCallbackFunction
        get() = staticCFunction { it, ptr ->
            ptr.use { wraith.updateRandomize(component, randomizeColorCheckbox, it) }
        }
}

class LogoWidgets : ComponentWidgets<LogoComponent>(wraith.logo)

class FanWidgets : ComponentWidgets<FanComponent>(wraith.fan) {
    override val widgets get() = listOf(modeBox, colorBox, brightnessScale, speedScale, mirageToggle)
    val mirageToggle by lazy {
        gtk_switch_new()!!.apply {
            setSensitive(component.mode != LedMode.OFF)
            gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_END)
            gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
            connectSignalWithData(
                "state-set", ptr,
                staticCFunction<Widget, Int, COpaquePointer, Boolean> { _, state, ptr ->
                    ptr.useWith<FanWidgets> {
                        component.mirage = state != 0
                        wraith.updateFanMirage()
                    }
                    false
                })
        }
    }

    override fun fullReload() = basicReload {
        mirageToggle.setSensitive(component.mode != LedMode.OFF)
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
class RingWidgets : ComponentWidgets<RingComponent>(wraith.ring) {
    override val widgets
        get() = listOf(modeBox, colorBox, brightnessScale, speedScale, directionComboBox, morseContainer)
    private var morseTextBoxHintLabel: Widget? = null
    var morseTextBoxHint: Widget? = null

    override val modeBox by lazy {
        gridComboBox(component.mode.name, RingMode.values.map { it.name }, true).apply {
            connectSignalWithData("changed", ptr, onModeChange)
        }
    }

    val directionComboBox by lazy {
        gridComboBox(
            component.direction.name, RotationDirection.values().map { it.name }, component.mode.supportsDirection
        ).apply {
            connectSignalWithData("changed", ptr, staticCFunction<Widget, COpaquePointer, Unit> { it, ptr ->
                val text = gtk_combo_box_text_get_active_text(it.reinterpret())!!.toKString()
                ptr.useWith<RingWidgets> {
                    wraith.update(component) { direction = RotationDirection.valueOf(text.toUpperCase()) }
                }
            })
        }
    }

    val morseContainer by lazy {
        gtk_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, 4)!!.apply {
            gtk_box_pack_end(reinterpret(), morseReloadButton, 0, 0, 0u)
            gtk_box_pack_end(reinterpret(), morseTextBox, 0, 0, 0u)
            setSensitive(component.mode == RingMode.MORSE)
        }
    }

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

            connectSignalWithData("changed", ptr, staticCFunction<Widget, COpaquePointer, Unit> { it, ptr ->
                ptr.useWith<RingWidgets> { this.changeCallback(it) }
            })
            connectSignalWithData("icon-press", ptr, staticCFunction<Widget, COpaquePointer, Unit> { it, ptr ->
                ptr.useWith<RingWidgets> {
                    when {
                        morseTextBoxHint == null -> {
                            morseTextBoxHintLabel = gtk_label_new(it.text.hintText)!!
                            morseTextBoxHint = gtk_popover_new(it)!!.apply {
                                gtk_popover_set_modal(reinterpret(), 0)
                                gtk_container_set_border_width(reinterpret(), 8u)
                                gtk_container_add(reinterpret(), morseTextBoxHintLabel)
                                gtk_widget_show_all(this)
                            }
                        }
                        gtk_widget_is_visible(morseTextBoxHint) == 1 -> gtk_widget_hide(morseTextBoxHint)
                        else -> gtk_widget_show(morseTextBoxHint)
                    }
                }
            })
        }
    }

    private val morseReloadButton by lazy {
        gtk_button_new_from_icon_name("gtk-ok", GtkIconSize.GTK_ICON_SIZE_BUTTON)!!.apply {
            gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
            connectSignalWithData("clicked", ptr, staticCFunction<Widget, COpaquePointer, Unit> { _, ptr ->
                ptr.useWith<RingWidgets> { wraith.updateRingMorseText(morseTextBox.text) }
            })
        }
    }

    private fun changeCallback(pointer: Widget) {
        morseTextBoxHintLabel?.apply {
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

    override fun fullReload() = basicReload {
        gtk_range_set_value(brightnessScale.reinterpret(), component.brightness.toDouble())
        gtk_range_set_value(speedScale.reinterpret(), component.speed.toDouble())
        gtk_combo_box_set_active(directionComboBox.reinterpret(), component.direction.value.toInt())
        directionComboBox.setSensitive(component.mode.supportsDirection)
        morseContainer.setSensitive(component.mode == RingMode.MORSE)
        if (component.mode != RingMode.MORSE) morseTextBoxHint?.let { hint -> gtk_widget_hide(hint) }
    }
}
