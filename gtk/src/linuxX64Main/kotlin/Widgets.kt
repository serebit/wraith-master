package com.serebit.wraith.gtk

import com.serebit.wraith.core.*
import gtk3.*
import kotlinx.cinterop.*

private data class CallbackData<W : ComponentWidgets<*>>(val wraith: WraithPrism, val widgets: W)

private inline fun <reified W : ComponentWidgets<*>> COpaquePointer.useWith(task: (CallbackData<W>) -> Unit) {
    val ref = asStableRef<CallbackData<W>>()
    task(ref.get())
    ref.dispose()
}

private fun COpaquePointer.use(task: (CallbackData<*>) -> Unit) = useWith<ComponentWidgets<*>>(task)

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

private val String.hintText: String
    get() = when {
        isBlank() -> "Enter either morse code (dots and dashes) or text"
        parseMorseOrTextToBytes().size > 120 -> "Maximum length exceeded"
        isMorseCode -> "Parsing as morse code"
        isValidMorseText -> "Parsing as text"
        else -> "Invalid characters detected: $invalidMorseChars"
    }

sealed class ComponentWidgets<C : LedComponent>(device: WraithPrism, val component: C) {
    protected val ptr: COpaquePointer by lazy { StableRef.create(CallbackData(device, this)).asCPointer() }
    open val widgets by lazy { listOf(modeBox, colorBox, brightnessScale, speedScale) }

    open val modeBox = gridComboBox(component.mode.name, LedMode.values.map { it.name }, true).apply {
        connectSignalWithData("changed", ptr, onModeChange)
    }

    val randomizeColorCheckbox = gtk_check_button_new_with_label("Randomize?")!!.apply {
        setSensitive(component.mode.colorSupport == ColorSupport.ALL)
        gtk_toggle_button_set_active(reinterpret(), component.useRandomColor.toByte().toInt())
        connectSignalWithData("toggled", ptr, onRandomizeChange)
    }
    val colorButton = gridColorButton(
        component.colorOrBlack, component.mode.colorSupport != ColorSupport.NONE && !component.useRandomColor
    ).apply {
        connectSignalWithData("color-set", ptr, onColorChange)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    val colorBox = gtk_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, 4)!!.apply {
        gtk_box_pack_end(reinterpret(), colorButton, 0, 0, 0u)
        gtk_box_pack_end(reinterpret(), randomizeColorCheckbox, 0, 0, 0u)
    }
    val brightnessScale = gridScale(component.brightness, 3, component.mode.supportsBrightness, ptr, onBrightnessChange)
    val speedScale = gridScale(component.speed, 5, component.mode.supportsSpeed, ptr, onSpeedChange)

    open fun fullReload() = basicReload()

    protected val onModeChange: CallbackCFunction
        get() = staticCFunction { it, ptr ->
            ptr.use { data -> data.wraith.updateMode(data.widgets.component, it); data.widgets.fullReload() }
        }
    private val onColorChange: CallbackCFunction
        get() = staticCFunction { it, ptr ->
            ptr.use { data -> data.wraith.updateColor(data.widgets.component, it) }
        }
    private val onBrightnessChange: CallbackCFunction
        get() = staticCFunction { it, ptr ->
            ptr.use { data -> data.wraith.updateBrightness(data.widgets.component, it) }
        }
    private val onSpeedChange: CallbackCFunction
        get() = staticCFunction { it, ptr ->
            ptr.use { data -> data.wraith.updateSpeed(data.widgets.component, it) }
        }
    private val onRandomizeChange: CallbackCFunction
        get() = staticCFunction { it, ptr ->
            ptr.use { (wraith, widgets) ->
                wraith.updateRandomize(widgets.component, widgets.randomizeColorCheckbox, it)
            }
        }
}

class LogoWidgets(wraith: WraithPrism) : ComponentWidgets<LogoComponent>(wraith, wraith.logo)

class FanWidgets(wraith: WraithPrism) : ComponentWidgets<FanComponent>(wraith, wraith.fan) {
    private val mirageToggle = gtk_switch_new()!!.apply {
        setSensitive(component.mode != LedMode.OFF)
        gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_END)
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
        connectSignalWithData(
            "state-set", ptr,
            staticCFunction<Widget, Int, COpaquePointer, Boolean> { _, state, ptr ->
                ptr.useWith<FanWidgets> { (wraith, widgets) ->
                    widgets.component.mirage = state != 0
                    wraith.updateFanMirage()
                }
                false
            })
    }

    override val widgets = listOf(modeBox, colorBox, brightnessScale, speedScale, mirageToggle)

    override fun fullReload() = basicReload {
        mirageToggle.setSensitive(component.mode != LedMode.OFF)
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
class RingWidgets(prism: WraithPrism) : ComponentWidgets<RingComponent>(prism, prism.ring) {
    var morseTextBoxHintLabel: Widget? = null
    var morseTextBoxHint: Widget? = null

    override val modeBox = gridComboBox(component.mode.name, RingMode.values.map { it.name }, true).apply {
        connectSignalWithData("changed", ptr, onModeChange)
    }

    private val directionComboBox = gridComboBox(
        component.direction.name, RotationDirection.values().map { it.name }, component.mode.supportsDirection
    ).apply {
        connectSignalWithData("changed", ptr, staticCFunction<Widget, COpaquePointer, Unit> { it, ptr ->
            val text = gtk_combo_box_text_get_active_text(it.reinterpret())!!.toKString()
            ptr.useWith<RingWidgets> { data ->
                data.wraith.update(data.widgets.component) { direction = RotationDirection.valueOf(text.toUpperCase()) }
            }
        })
    }


    private val morseTextBox = gtk_entry_new()!!.apply {
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
        gtk_entry_set_icon_from_icon_name(
            reinterpret(),
            GtkEntryIconPosition.GTK_ENTRY_ICON_SECONDARY,
            "dialog-information"
        )

        connectSignalWithData("changed", ptr, staticCFunction<Widget, COpaquePointer, Unit> { it, ptr ->
            ptr.useWith<RingWidgets> { (_, widgets) -> widgets.changeCallback(it) }
        })
        connectSignalWithData("icon-press", ptr, staticCFunction<Widget, COpaquePointer, Unit> { it, ptr ->
            ptr.useWith<RingWidgets> { (_, widgets) ->
                when {
                    widgets.morseTextBoxHint == null -> {
                        widgets.morseTextBoxHintLabel = gtk_label_new(it.text.hintText)!!
                        widgets.morseTextBoxHint = gtk_popover_new(it)!!.apply {
                            gtk_popover_set_modal(reinterpret(), 0)
                            gtk_container_set_border_width(reinterpret(), 8u)
                            gtk_container_add(reinterpret(), widgets.morseTextBoxHintLabel)
                            gtk_widget_show_all(this)
                        }
                    }
                    gtk_widget_is_visible(widgets.morseTextBoxHint) == 1 -> gtk_widget_hide(widgets.morseTextBoxHint)
                    else -> gtk_widget_show(widgets.morseTextBoxHint)
                }
            }
        })
    }

    private val morseReloadButton = gtk_button_new_from_icon_name("gtk-ok", GtkIconSize.GTK_ICON_SIZE_BUTTON)!!.apply {
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
        connectSignalWithData("clicked", ptr, staticCFunction<Widget, COpaquePointer, Unit> { _, ptr ->
            ptr.useWith<RingWidgets> { (wraith, widgets) -> wraith.updateRingMorseText(widgets.morseTextBox.text) }
        })
    }

    private val morseContainer = gtk_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, 4)!!.apply {
        gtk_box_pack_end(reinterpret(), morseReloadButton, 0, 0, 0u)
        gtk_box_pack_end(reinterpret(), morseTextBox, 0, 0, 0u)
        setSensitive(component.mode == RingMode.MORSE)
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

    override val widgets = listOf(modeBox, colorBox, brightnessScale, speedScale, directionComboBox, morseContainer)

    override fun fullReload() = basicReload {
        gtk_range_set_value(brightnessScale.reinterpret(), component.brightness.toDouble())
        gtk_range_set_value(speedScale.reinterpret(), component.speed.toDouble())
        gtk_combo_box_set_active(directionComboBox.reinterpret(), component.direction.value.toInt())
        directionComboBox.setSensitive(component.mode.supportsDirection)
        morseContainer.setSensitive(component.mode == RingMode.MORSE)
        if (component.mode != RingMode.MORSE) morseTextBoxHint?.let { hint -> gtk_widget_hide(hint) }
    }
}
