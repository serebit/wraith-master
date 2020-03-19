package com.serebit.wraith.gtk

import com.serebit.wraith.core.*
import gtk3.*
import kotlinx.cinterop.*

sealed class ComponentWidgets<C : LedComponent>(
    private val device: WraithPrism, val component: C, modes: List<Mode> = LedMode.values
) {
    protected val ptr get() = StableRef.create(CallbackData(device, this)).asCPointer()

    val modeBox = comboBox(modes.map { it.name }, ptr, staticCFunction { widget, ptr ->
        ptr.use { it.wraith.updateMode(it.widgets, widget) }
    })
    val colorButton = colorButton(ptr, staticCFunction { widget, ptr ->
        ptr.use { it.wraith.updateColor(it.widgets.component, widget) }
    })
    val randomizeColorCheckbox = checkButton("Randomize?", ptr, staticCFunction { widget, ptr ->
        ptr.use { (wraith, widgets) -> wraith.updateRandomize(widgets.component, widget, widgets.colorButton) }
    })
    val brightnessScale = gridScale(3, ptr, staticCFunction { widget, ptr ->
        ptr.use { it.wraith.updateBrightness(it.widgets.component, widget) }
    })
    val speedScale = gridScale(5, ptr, staticCFunction { widget, ptr ->
        ptr.use { it.wraith.updateSpeed(it.widgets.component, widget) }
    })

    @OptIn(ExperimentalUnsignedTypes::class)
    val colorBox = gtk_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, 4)!!.apply {
        gtk_box_pack_end(reinterpret(), colorButton, 0, 0, 0u)
        gtk_box_pack_end(reinterpret(), randomizeColorCheckbox, 0, 0, 0u)
    }

    private val baseLabels = listOf("Mode", "Color", "Brightness", "Speed")
    protected val baseWidgets = listOf(modeBox, colorBox, brightnessScale, speedScale)

    protected open val extraLabels = emptyList<String>()
    protected open val extraWidgets = emptyList<Widget>()

    fun initialize(grid: Widget) {
        grid.gridAttach(baseLabels + extraLabels, baseWidgets + extraWidgets)
        attachExtraWidgets(grid)
        reload()
    }

    open fun extraReload() = Unit

    protected open fun attachExtraWidgets(grid: Widget) = Unit
}

class LogoWidgets(wraith: WraithPrism) : ComponentWidgets<LogoComponent>(wraith, wraith.logo)

@OptIn(ExperimentalUnsignedTypes::class)
class FanWidgets(wraith: WraithPrism) : ComponentWidgets<FanComponent>(wraith, wraith.fan) {
    private val mirageLabels = listOf("Red", "Green", "Blue").map { gtk_label_new("$it (Hz)")!! }
    private val mirageToggle = gtk_switch_new()!!.apply {
        gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_START)
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER) // override for some themes, like default
        connectSignalWithData("state-set", ptr, staticCFunction<Widget, Int, COpaquePointer, Boolean> { _, state, ptr ->
            ptr.useWith<FanWidgets> { (_, widgets) ->
                widgets.component.mirage = state != 0
                (widgets.mirageFreqWidgets + widgets.mirageLabels).forEach {
                    it.setSensitive(widgets.component.mode != LedMode.OFF && widgets.component.mirage)
                }
            }
            false
        })
    }

    private val mirageRedFrequency = frequencySpinButton(ptr, staticCFunction { _, _, _ -> })
    private val mirageGreenFrequency = frequencySpinButton(ptr, staticCFunction { _, _, _ -> })
    private val mirageBlueFrequency = frequencySpinButton(ptr, staticCFunction { _, _, _ -> })
    private val mirageFreqWidgets = listOf(mirageRedFrequency, mirageGreenFrequency, mirageBlueFrequency)

    private val mirageReload = gtk_button_new_from_icon_name("gtk-ok", GtkIconSize.GTK_ICON_SIZE_BUTTON)!!.apply {
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
        gtk_button_set_label(reinterpret(), "Apply")
        gtk_button_set_always_show_image(reinterpret(), 1)
        connectSignalWithData("clicked", ptr, staticCFunction<Widget, COpaquePointer, Unit> { _, ptr ->
            ptr.useWith<FanWidgets> { (wraith, widgets) ->
                if (gtk_switch_get_active(widgets.mirageToggle.reinterpret()) == 1) {
                    val redFreq = gtk_spin_button_get_value_as_int(widgets.mirageRedFrequency.reinterpret())
                    val greenFreq = gtk_spin_button_get_value_as_int(widgets.mirageGreenFrequency.reinterpret())
                    val blueFreq = gtk_spin_button_get_value_as_int(widgets.mirageBlueFrequency.reinterpret())
                    wraith.enableFanMirage(redFreq, greenFreq, blueFreq)
                } else {
                    wraith.disableFanMirage()
                }
            }
        })
    }

    private val mirageGrid = gtk_grid_new()!!.apply {
        gtk_grid_set_column_spacing(reinterpret(), 4u)
        gtk_grid_set_row_spacing(reinterpret(), 4u)
        gtk_grid_attach(reinterpret(), mirageToggle, 0, 0, 1, 1)
        gtk_grid_attach(reinterpret(), mirageReload, 2, 0, 1, 1)
        mirageFreqWidgets.forEachIndexed { i, it -> gtk_grid_attach(reinterpret(), it, i, 1, 1, 1) }
        mirageLabels.forEachIndexed { i, it -> gtk_grid_attach(reinterpret(), it, i, 2, 1, 1) }
    }

    override fun extraReload() {
        mirageToggle.setSensitive(component.mode != LedMode.OFF)
        mirageReload.setSensitive(component.mode != LedMode.OFF)
        (mirageFreqWidgets + mirageLabels).forEach {
            it.setSensitive(component.mode != LedMode.OFF && component.mirage)
        }
    }

    override val extraLabels = listOf("Mirage")
    override fun attachExtraWidgets(grid: Widget) {
        gtk_grid_attach(grid.reinterpret(), mirageGrid, 1, baseWidgets.lastIndex + 1, 1, 2)
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
class RingWidgets(prism: WraithPrism) : ComponentWidgets<RingComponent>(prism, prism.ring, RingMode.values) {
    private var morseTextBoxHintLabel: Widget? = null
    private var morseTextBoxHint: Widget? = null

    private val directionComboBox =
        comboBox(RotationDirection.values().map { it.name }, ptr, staticCFunction { it, ptr ->
            val text = gtk_combo_box_text_get_active_text(it.reinterpret())!!.toKString()
            ptr.useWith<RingWidgets> { (wraith, widgets) ->
                wraith.update(widgets.component) { direction = RotationDirection.valueOf(text.toUpperCase()) }
            }
        })

    private val morseTextBox = gtk_entry_new()!!.apply {
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
        gtk_entry_set_icon_from_icon_name(
            reinterpret(),
            GtkEntryIconPosition.GTK_ENTRY_ICON_SECONDARY,
            "dialog-information"
        )
        gtk_widget_set_size_request(this, 192, -1)

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
        gtk_widget_set_hexpand(this, 1)
    }

    private fun changeCallback(pointer: Widget) {
        morseTextBoxHintLabel?.apply {
            val entryText = pointer.text
            gtk_label_set_text(reinterpret(), entryText.hintText)
            val isInvalid = !entryText.isValidMorseText && !entryText.isMorseCode
            morseReloadButton.setSensitive(!(entryText.parseMorseOrTextToBytes().size > 120 || isInvalid))
        }
    }

    override val extraLabels = listOf("Rotation Direction", "Morse Text")
    override val extraWidgets = listOf(directionComboBox, morseContainer)

    override fun extraReload() {
        gtk_combo_box_set_active(directionComboBox.reinterpret(), component.direction.value)
        directionComboBox.setSensitive(component.mode.supportsDirection)
        morseContainer.setSensitive(component.mode == RingMode.MORSE)
        if (component.mode != RingMode.MORSE) morseTextBoxHint?.let { hint -> gtk_widget_hide(hint) }
    }
}

fun ComponentWidgets<*>.reload() {
    gtk_combo_box_set_active(modeBox.reinterpret(), component.mode.index)
    memScoped { gtk_color_button_set_rgba(colorButton.reinterpret(), gdkRgba(component.colorOrBlack).ptr) }

    val useRandomColor = component.mode.colorSupport == ColorSupport.ALL && component.useRandomColor
    gtk_toggle_button_set_active(randomizeColorCheckbox.reinterpret(), useRandomColor.toByte().toInt())

    gtk_range_set_value(brightnessScale.reinterpret(), component.brightness.toDouble())
    gtk_range_set_value(speedScale.reinterpret(), component.speed.toDouble())

    randomizeColorCheckbox.setSensitive(component.mode.colorSupport == ColorSupport.ALL)
    colorButton.setSensitive(component.mode.colorSupport != ColorSupport.NONE && !useRandomColor)
    brightnessScale.setSensitive(component.mode.supportsBrightness)
    speedScale.setSensitive(component.mode.supportsSpeed)
    extraReload()
}

private data class CallbackData<W : ComponentWidgets<*>>(val wraith: WraithPrism, val widgets: W)

private inline fun <reified W : ComponentWidgets<*>> COpaquePointer.useWith(task: (CallbackData<W>) -> Unit) {
    val ref = asStableRef<CallbackData<W>>()
    task(ref.get())
    ref.dispose()
}

private fun COpaquePointer.use(task: (CallbackData<*>) -> Unit) = useWith<ComponentWidgets<*>>(task)

private fun Widget.gridAttach(labels: List<String>, widgets: List<Widget>) {
    labels.forEachIndexed { i, it -> newGridLabel(i, it) }
    widgets.forEachIndexed { i, it -> gridAttachRight(it, i) }
}

private val String.hintText: String
    get() = when {
        isBlank() -> "Enter either morse code (dots and dashes) or text"
        parseMorseOrTextToBytes().size > 120 -> "Maximum length exceeded"
        isMorseCode -> "Parsing as morse code"
        isValidMorseText -> "Parsing as text"
        else -> "Invalid characters detected: $invalidMorseChars"
    }
