package com.serebit.wraith.gtk

import com.serebit.wraith.core.prism.*
import gtk3.*
import kotlinx.cinterop.*

sealed class PrismComponentWidgets<C : PrismComponent>(protected val device: WraithPrism, val component: C) {
    protected val ptr by lazy { StableRef.create(CallbackData(device, this)).asCPointer() }
    var isReloading = false
        private set

    private val modeBox = comboBox(component.modes.map { it.name }) {
        connectToSignal("changed", ptr, staticCFunction<Widget, COpaquePointer, Unit> { widget, ptr ->
            ptr.useWith<PrismComponentWidgets<*>> { wraith -> wraith.updateMode(this, widget) }
        })
    }

    private val colorButton = colorButton(ptr, onColorChange = staticCFunction { widget, ptr ->
        ptr.useUpdateBasic { color = widget.getRgbaAsColor() }
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

            if (component.mode.colorSupport != ColorSupport.NONE) {
                colorBox.setSensitive(true)

                memScoped { gtk_color_button_set_rgba(colorButton.reinterpret(), gdkRgba(component.color).ptr) }

                val useRandomColor = component.mode.colorSupport == ColorSupport.ALL && component.useRandomColor
                gtk_toggle_button_set_active(randomizeColorCheckbox.reinterpret(), useRandomColor.toByte().toInt())

                randomizeColorCheckbox.setSensitive(component.mode.colorSupport == ColorSupport.ALL)
                colorButton.setSensitive(!useRandomColor)
            } else {
                colorBox.setSensitive(false)

                gtk_toggle_button_set_active(randomizeColorCheckbox.reinterpret(), 0)
                memScoped { gtk_color_button_set_rgba(colorButton.reinterpret(), gdkRgba(Color.BLACK).ptr) }
            }

            gtk_range_set_value(brightnessScale.reinterpret(), component.brightness.ordinal.toDouble())
            brightnessScale.setSensitive(component.mode.supportsBrightness)

            gtk_range_set_value(speedScale.reinterpret(), component.speed.ordinal.toDouble())
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
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER) // override for some themes, like adwaita

        connectToSignal<StateSetCallbackFunc>("state-set", ptr, staticCFunction { _, state, ptr ->
            ptr.use {
                component.mirageState = if (state == 0) MirageState.Off else MirageState.DEFAULT

                val sensitive = component.mode != BasicPrismMode.OFF && component.mirageState != MirageState.Off
                (mirageFreqSpinners union mirageLabels).forEach { widget ->
                    widget.setSensitive(sensitive)
                }
            }
            false
        })
    }

    private val mirageFreqSpinners = List(3) { frequencySpinButton() } // red, green, blue; in order

    private val mirageReload = iconButton("gtk-ok", "Apply", ptr, staticCFunction { _, ptr ->
        ptr.use { wraith ->
            if (gtk_switch_get_active(mirageToggle.reinterpret()) == 1) {
                val (red, green, blue) = mirageFreqSpinners.map {
                    gtk_spin_button_get_value_as_int(it.reinterpret())
                }

                component.mirageState = MirageState.On(red, green, blue)
            } else {
                component.mirageState = MirageState.Off
            }

            wraith.pushFanMirageState()
        }
    })

    private val mirageGrid = gtk_grid_new()!!.apply {
        gtk_grid_set_column_spacing(reinterpret(), 4u)
        gtk_grid_set_row_spacing(reinterpret(), 4u)
        gtk_grid_attach(reinterpret(), mirageToggle, 0, 0, 1, 1)
        gtk_grid_attach(reinterpret(), mirageReload, 2, 0, 1, 1)
        mirageFreqSpinners.forEachIndexed { i, it -> gtk_grid_attach(reinterpret(), it, i, 1, 1, 1) }
        mirageLabels.forEachIndexed { i, it -> gtk_grid_attach(reinterpret(), it, i, 2, 1, 1) }
    }

    override fun extraReload() {
        mirageToggle.setSensitive(component.mode != BasicPrismMode.OFF && !device.enso)
        mirageReload.setSensitive(component.mode != BasicPrismMode.OFF && !device.enso)
        (mirageFreqSpinners + mirageLabels).forEach {
            it.setSensitive(component.mode != BasicPrismMode.OFF && component.mirageState is MirageState.On && !device.enso)
        }
    }

    override val extraLabels = listOf("Mirage")
    override fun attachExtraWidgets(grid: Widget) {
        gtk_grid_attach(grid.reinterpret(), mirageGrid, 1, baseWidgets.lastIndex + 1, 1, 2)
    }

    fun setMirageEnabled(redFreq: Int, greenFreq: Int, blueFreq: Int) {
        gtk_switch_set_state(mirageToggle.reinterpret(), 1)
        mirageFreqSpinners.zip(listOf(redFreq, greenFreq, blueFreq)).forEach {
            gtk_spin_button_set_value(it.first.reinterpret(), it.second.toDouble())
        }
    }

    private inline fun COpaquePointer.use(task: FanWidgets.(WraithPrism) -> Unit) = useWith(task)
}

@OptIn(ExperimentalUnsignedTypes::class)
class RingWidgets(prism: WraithPrism) : PrismComponentWidgets<PrismRingComponent>(prism, prism.ring) {
    private var morseHintLabel: Widget? = null
    private var morseHint: Widget? = null

    private val directionComboBox = comboBox(RotationDirection.values().map { it.name }) {
        connectToSignal("changed", ptr, staticCFunction<Widget, COpaquePointer, Unit> { widget, ptr ->
            val text = gtk_combo_box_text_get_active_text(widget.reinterpret())!!.toKString()
            ptr.useUpdate<RingWidgets, PrismRingComponent> {
                direction = RotationDirection.valueOf(text.toUpperCase())
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
        gtk_widget_set_size_request(this, 192, -1)

        connectToSignal<StandardCallbackFunc>("changed", ptr, staticCFunction { widget, ptr ->
            ptr.use { changeCallback(widget) }
        })
        connectToSignal<IconPressCallbackFunc>("icon-press", ptr, staticCFunction { widget, _, _, ptr ->
            ptr.use {
                when {
                    morseHint == null -> {
                        morseHintLabel = gtk_label_new(widget.text.hintText)!!
                        morseHint = gtk_popover_new(widget)!!.apply {
                            gtk_popover_set_modal(reinterpret(), 0)
                            gtk_container_set_border_width(reinterpret(), 8u)
                            gtk_container_add(reinterpret(), morseHintLabel)
                            gtk_widget_show_all(this)
                        }
                    }
                    gtk_widget_is_visible(morseHint) == 1 -> gtk_widget_hide(morseHint)
                    else -> gtk_widget_show(morseHint)
                }
            }
        })
    }

    private val morseReloadButton = gtk_button_new_from_icon_name("gtk-ok", GtkIconSize.GTK_ICON_SIZE_BUTTON)!!.apply {
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
        connectToSignal("clicked", ptr, staticCFunction<Widget, COpaquePointer, Unit> { widget, ptr ->
            ptr.use { wraith ->
                gtk_entry_set_text(morseTextBox.reinterpret(), morseTextBox.text.trim())
                wraith.updateRingMorseText(morseTextBox.text)
                widget.setSensitive(false)
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

    private inline fun COpaquePointer.use(task: RingWidgets.(WraithPrism) -> Unit) = useWith(task)
}

private data class CallbackData<W : PrismComponentWidgets<*>>(val wraith: WraithPrism, val widgets: W)

private inline fun <W : PrismComponentWidgets<*>> COpaquePointer.useWith(task: W.(WraithPrism) -> Unit) {
    asStableRef<CallbackData<W>>().get().let {
        if (!it.widgets.isReloading) it.widgets.task(it.wraith)
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

private val String.hintText: String
    get() = when {
        isBlank() -> "Enter either morse code (dots and dashes) or text"
        parseMorseOrTextToBytes().size > 120 -> "Maximum length exceeded"
        isMorseCode -> "Parsing as morse code"
        isValidMorseText -> "Parsing as text"
        else -> "Invalid characters detected: $invalidMorseChars"
    }
