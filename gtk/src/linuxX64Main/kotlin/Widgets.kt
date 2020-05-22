package com.serebit.wraith.gtk

import com.serebit.wraith.core.prism.*
import gtk3.*
import kotlinx.cinterop.*

sealed class PrismComponentWidgets<C : PrismComponent>(protected val device: WraithPrism, val component: C) {
    protected val ptr by lazy { StableRef.create(CallbackData(device, this)).asCPointer() }
    var isReloading = false

    private val modeBox = comboBox(component.modes.map { it.name }, ptr, staticCFunction { widget, ptr ->
        ptr.use { it.wraith.updateMode(it.widgets, widget) }
    })
    private val colorButton = colorButton(ptr, staticCFunction { widget, ptr ->
        ptr.useUpdateBasic {
            color = memScoped {
                alloc<GdkRGBA>().also { gtk_color_button_get_rgba(widget.reinterpret(), it.ptr) }.toColor()
            }
        }
    })
    private val randomizeColorCheckbox = checkButton("Randomize?", ptr, staticCFunction { widget, ptr ->
        ptr.useUpdateBasic {
            val isActive = gtk_toggle_button_get_active(widget.reinterpret())
            if (it.component.mode.colorSupport == ColorSupport.ALL) {
                useRandomColor = isActive == 1
            }
            it.colorButton.setSensitive(isActive == 0)
        }
    })
    private val brightnessScale = gridScale(Brightness.values().size, ptr, staticCFunction { widget, ptr ->
        ptr.useUpdateBasic {
            brightness = Brightness.values()[gtk_adjustment_get_value(widget.reinterpret()).toInt()]
        }
    })
    private val speedScale = gridScale(Speed.values().size, ptr, staticCFunction { widget, ptr ->
        ptr.useUpdateBasic {
            speed = Speed.values()[gtk_adjustment_get_value(widget.reinterpret()).toInt()]
        }
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
        (baseLabels + extraLabels).forEachIndexed { i, it -> grid.newGridLabel(i, it) }
        (baseWidgets + extraWidgets).forEachIndexed { i, it -> gtk_grid_attach(grid.reinterpret(), it, 1, i, 1, 1) }
        attachExtraWidgets(grid)
        reload()
    }

    fun reload() {
        isReloading = true
        if (device.enso) {
            gtk_combo_box_set_active(modeBox.reinterpret(), -1)
            (baseWidgets + extraWidgets).forEach { it.setSensitive(false) }
        } else {
            modeBox.setSensitive(true)
            gtk_combo_box_set_active(modeBox.reinterpret(), component.mode.ordinal)

            val colorOrBlack = if (component.mode.colorSupport != ColorSupport.NONE) component.color else Color(0, 0, 0)
            memScoped { gtk_color_button_set_rgba(colorButton.reinterpret(), gdkRgba(colorOrBlack).ptr) }

            val useRandomColor = component.mode.colorSupport == ColorSupport.ALL && component.useRandomColor
            gtk_toggle_button_set_active(randomizeColorCheckbox.reinterpret(), useRandomColor.toByte().toInt())

            gtk_range_set_value(brightnessScale.reinterpret(), component.brightness.ordinal.toDouble())
            gtk_range_set_value(speedScale.reinterpret(), component.speed.ordinal.toDouble())

            randomizeColorCheckbox.setSensitive(component.mode.colorSupport == ColorSupport.ALL)
            colorButton.setSensitive(component.mode.colorSupport != ColorSupport.NONE && !useRandomColor)
            brightnessScale.setSensitive(component.mode.supportsBrightness)
            speedScale.setSensitive(component.mode.supportsSpeed)
        }
        extraReload()

        isReloading = false
    }

    protected open fun extraReload() = Unit

    protected open fun attachExtraWidgets(grid: Widget) = Unit

    fun close() = ptr.asStableRef<CallbackData<*>>().dispose()
}

class LogoWidgets(wraith: WraithPrism) : PrismComponentWidgets<PrismLogoComponent>(wraith, wraith.logo)

@OptIn(ExperimentalUnsignedTypes::class)
class FanWidgets(wraith: WraithPrism) : PrismComponentWidgets<PrismFanComponent>(wraith, wraith.fan) {
    private val mirageLabels = listOf("Red", "Green", "Blue").map { gtk_label_new("$it (Hz)")!! }
    private val mirageToggle = gtk_switch_new()!!.apply {
        gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_START)
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER) // override for some themes, like default
        connectSignalWithData("state-set", ptr, staticCFunction<Widget, Int, COpaquePointer, Boolean> { _, state, ptr ->
            ptr.useWith<FanWidgets> { (_, widgets) ->
                widgets.component.mirage = state != 0
                (widgets.mirageFreqWidgets + widgets.mirageLabels).forEach {
                    it.setSensitive(widgets.component.mode != BasicPrismMode.OFF && widgets.component.mirage)
                }
            }
            false
        })
    }

    private val mirageRedFrequency = frequencySpinButton(ptr, staticCFunction { _, _, _ -> })
    private val mirageGreenFrequency = frequencySpinButton(ptr, staticCFunction { _, _, _ -> })
    private val mirageBlueFrequency = frequencySpinButton(ptr, staticCFunction { _, _, _ -> })
    private val mirageFreqWidgets = listOf(mirageRedFrequency, mirageGreenFrequency, mirageBlueFrequency)

    private val mirageReload = iconButton("gtk-ok", "Apply", ptr, staticCFunction { _, ptr ->
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

    private val mirageGrid = gtk_grid_new()!!.apply {
        gtk_grid_set_column_spacing(reinterpret(), 4u)
        gtk_grid_set_row_spacing(reinterpret(), 4u)
        gtk_grid_attach(reinterpret(), mirageToggle, 0, 0, 1, 1)
        gtk_grid_attach(reinterpret(), mirageReload, 2, 0, 1, 1)
        mirageFreqWidgets.forEachIndexed { i, it -> gtk_grid_attach(reinterpret(), it, i, 1, 1, 1) }
        mirageLabels.forEachIndexed { i, it -> gtk_grid_attach(reinterpret(), it, i, 2, 1, 1) }
    }

    override fun extraReload() {
        mirageToggle.setSensitive(component.mode != BasicPrismMode.OFF && !device.enso)
        mirageReload.setSensitive(component.mode != BasicPrismMode.OFF && !device.enso)
        (mirageFreqWidgets + mirageLabels).forEach {
            it.setSensitive(component.mode != BasicPrismMode.OFF && component.mirage && !device.enso)
        }
    }

    override val extraLabels = listOf("Mirage")
    override fun attachExtraWidgets(grid: Widget) {
        gtk_grid_attach(grid.reinterpret(), mirageGrid, 1, baseWidgets.lastIndex + 1, 1, 2)
    }

    fun setMirageEnabled(redFreq: Int, greenFreq: Int, blueFreq: Int) {
        gtk_switch_set_state(mirageToggle.reinterpret(), 1)
        gtk_spin_button_set_value(mirageRedFrequency.reinterpret(), redFreq.toDouble())
        gtk_spin_button_set_value(mirageGreenFrequency.reinterpret(), greenFreq.toDouble())
        gtk_spin_button_set_value(mirageBlueFrequency.reinterpret(), blueFreq.toDouble())
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
class RingWidgets(prism: WraithPrism) : PrismComponentWidgets<PrismRingComponent>(prism, prism.ring) {
    private var morseHintLabel: Widget? = null
    private var morseHint: Widget? = null

    private val directionComboBox =
        comboBox(RotationDirection.values().map { it.name }, ptr, staticCFunction { it, ptr ->
            val text = gtk_combo_box_text_get_active_text(it.reinterpret())!!.toKString()
            ptr.useUpdate<RingWidgets, PrismRingComponent> {
                direction = RotationDirection.valueOf(text.toUpperCase())
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
        connectSignalWithData("icon-press", ptr,
            staticCFunction<Widget, GtkEntryIconPosition, CPointer<GdkEvent>, COpaquePointer, Unit> { it, _, _, ptr ->
                ptr.useWith<RingWidgets> { (_, widgets) ->
                    when {
                        widgets.morseHint == null -> {
                            widgets.morseHintLabel = gtk_label_new(it.text.hintText)!!
                            widgets.morseHint = gtk_popover_new(it)!!.apply {
                                gtk_popover_set_modal(reinterpret(), 0)
                                gtk_container_set_border_width(reinterpret(), 8u)
                                gtk_container_add(reinterpret(), widgets.morseHintLabel)
                                gtk_widget_show_all(this)
                            }
                        }
                        gtk_widget_is_visible(widgets.morseHint) == 1 -> gtk_widget_hide(widgets.morseHint)
                        else -> gtk_widget_show(widgets.morseHint)
                    }
                }
            })
    }

    private val morseReloadButton = gtk_button_new_from_icon_name("gtk-ok", GtkIconSize.GTK_ICON_SIZE_BUTTON)!!.apply {
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
        connectSignalWithData("clicked", ptr, staticCFunction<Widget, COpaquePointer, Unit> { it, ptr ->
            ptr.useWith<RingWidgets> { (wraith, widgets) ->
                gtk_entry_set_text(widgets.morseTextBox.reinterpret(), widgets.morseTextBox.text.trim())
                wraith.updateRingMorseText(widgets.morseTextBox.text)
                it.setSensitive(false)
            }
        })
    }

    private val morseContainer = gtk_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, 4)!!.apply {
        gtk_box_pack_end(reinterpret(), morseReloadButton, 0, 0, 0u)
        gtk_box_pack_end(reinterpret(), morseTextBox, 0, 0, 0u)
        gtk_widget_set_hexpand(this, 1)
    }

    private fun changeCallback(pointer: Widget) {
        val entryText = pointer.text
        morseHintLabel?.apply {
            gtk_label_set_text(reinterpret(), entryText.hintText)
        }
        val isInvalid = !entryText.isValidMorseText && !entryText.isMorseCode
        val parsed = entryText.parseMorseOrTextToBytes()
        val equalsSaved = parsed == component.savedMorseBytes
        morseReloadButton.setSensitive(!(parsed.size > 120 || isInvalid) && !equalsSaved)
    }

    override val extraLabels = listOf("Rotation Direction", "Morse Text")
    override val extraWidgets = listOf(directionComboBox, morseContainer)

    override fun extraReload() {
        gtk_combo_box_set_active(directionComboBox.reinterpret(), component.direction.ordinal)
        directionComboBox.setSensitive(component.mode.supportsDirection && !device.enso)
        morseContainer.setSensitive(component.mode == PrismRingMode.MORSE && !device.enso)
        if (component.mode != PrismRingMode.MORSE) morseHint?.let { hint -> gtk_widget_hide(hint) }
    }
}

private data class CallbackData<W : PrismComponentWidgets<*>>(val wraith: WraithPrism, val widgets: W)

private fun COpaquePointer.use(task: (CallbackData<*>) -> Unit) = useWith<PrismComponentWidgets<*>>(task)
private inline fun <W : PrismComponentWidgets<*>> COpaquePointer.useWith(task: (CallbackData<W>) -> Unit) {
    asStableRef<CallbackData<W>>().get().let {
        if (!it.widgets.isReloading) task(it)
    }
}

private inline fun <W : PrismComponentWidgets<C>, C : PrismComponent> COpaquePointer.useUpdate(task: C.(W) -> Unit) {
    val (wraith, widgets) = asStableRef<CallbackData<W>>().get()
    if (!widgets.isReloading) {
        wraith.update(widgets.component) { task(widgets) }
    }
}

private inline fun COpaquePointer.useUpdateBasic(task: PrismComponent.(PrismComponentWidgets<*>) -> Unit) =
    useUpdate(task)

private fun GdkRGBA.toColor() = Color((255 * red).toInt(), (255 * green).toInt(), (255 * blue).toInt())

private val String.hintText: String
    get() = when {
        isBlank() -> "Enter either morse code (dots and dashes) or text"
        parseMorseOrTextToBytes().size > 120 -> "Maximum length exceeded"
        isMorseCode -> "Parsing as morse code"
        isValidMorseText -> "Parsing as text"
        else -> "Invalid characters detected: $invalidMorseChars"
    }
