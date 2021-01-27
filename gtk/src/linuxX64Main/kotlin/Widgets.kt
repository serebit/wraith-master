package com.serebit.wraith.gtk

import com.serebit.wraith.core.prism.*
import gtk3.*
import kotlinx.cinterop.*

typealias IconPressCallbackFunc = CFunction<(Widget, GtkEntryIconPosition, CPointer<GdkEvent>, COpaquePointer) -> Unit>
typealias StateSetCallbackFunc = CFunction<(Widget, Int, COpaquePointer) -> Boolean>

sealed class PrismComponentWidgets(val device: WraithPrism, modes: Array<out PrismMode>) {
    abstract val component: PrismComponent<*>
    protected val callbackPtr by lazy { StableRef.create(CallbackData(device, this)).asCPointer() }
    var isReloading = false
        private set

    private val modeBox: CPointer<GtkWidget> = comboBox(modes.map { it.name }) {
        connectToSignal("changed", callbackPtr, staticCFunction<Widget, COpaquePointer, Unit> { widget, ptr ->
            val (wraith, widgets) = ptr.asStableRef<CallbackData<PrismComponentWidgets>>().get()
            val component = widgets.component

            if (!widgets.isReloading) {
                val modeText = gtk_combo_box_text_get_active_text(widget.reinterpret())!!.toKString().toUpperCase()

                when (component) {
                    is PrismRingComponent -> {
                        component.mode = PrismRingMode.valueOf(modeText)
                        component.reloadValues()
                        wraith.assignChannels()
                    }
                    is PrismFanComponent -> {
                        component.mode = BasicPrismMode.valueOf(modeText)
                    }
                    is PrismLogoComponent -> {
                        component.mode = BasicPrismMode.valueOf(modeText)

                    }
                }

                component.submitValues()
                wraith.apply()
                widgets.reload()
            }
        })
    }

    private val colorButton = gtk_color_button_new()!!.apply {
        gtk_color_button_set_use_alpha(reinterpret(), 0)
        gtk_widget_set_size_request(this, 96, -1)
        connectToSignal<StandardCallbackFunc>("color-set", callbackPtr, staticCFunction { widget, ptr ->
            ptr.useCallbackPtr<PrismComponentWidgets> { wraith, widgets ->
                widgets.component.color = memScoped {
                    alloc<GdkRGBA>()
                        .also { gtk_color_button_get_rgba(widget.reinterpret(), it.ptr) }
                        .run { Color((255 * red).toInt(), (255 * green).toInt(), (255 * blue).toInt()) }
                }
                widgets.component.submitValues()
                wraith.apply()
            }
        })
    }

    private val randomizeColorCheckbox = gtk_check_button_new_with_label("Randomize?")!!.apply {
        connectToSignal<StandardCallbackFunc>("toggled", callbackPtr, staticCFunction { widget, ptr ->
            ptr.useCallbackPtr<PrismComponentWidgets> { wraith, widgets ->
                val isActive = gtk_toggle_button_get_active(widget.reinterpret())
                if (widgets.component.mode.colorSupport == ColorSupport.ALL) {
                    widgets.component.useRandomColor = isActive == 1
                    widgets.component.submitValues()
                    wraith.apply()
                }
                widgets.colorButton.setSensitive(isActive == 0)
            }
        })
    }

    private val brightnessScale = gridScale(Brightness.values().size, callbackPtr, staticCFunction { widget, ptr ->
        ptr.useCallbackPtr<PrismComponentWidgets> { wraith, widgets ->
            widgets.component.brightness = Brightness.values()[gtk_adjustment_get_value(widget.reinterpret()).toInt()]
            widgets.component.submitValues()
            wraith.apply()
        }
    })

    private val speedScale = gridScale(Speed.values().size, callbackPtr, staticCFunction { widget, ptr ->
        ptr.useCallbackPtr<PrismComponentWidgets> { wraith, widgets ->
            widgets.component.speed = Speed.values()[gtk_adjustment_get_value(widget.reinterpret()).toInt()]
            widgets.component.submitValues()
            wraith.apply()
        }
    })

    private val colorBox = gtk_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, 4)!!.apply {
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
            brightnessScale.setSensitive(component.mode.brightnesses.isNotEmpty())

            gtk_range_set_value(speedScale.reinterpret(), component.speed.ordinal.toDouble())
            speedScale.setSensitive(component.mode.speeds.isNotEmpty())
        }
        extraReload()

        isReloading = false
    }

    protected open fun extraReload() = Unit

    protected open fun attachExtraWidgets(grid: Widget) = Unit

    fun close() = callbackPtr.asStableRef<CallbackData<*>>().dispose()
}

class LogoWidgets(device: WraithPrism) : PrismComponentWidgets(device, BasicPrismMode.values()) {
    override val component = device.logo
}

class FanWidgets(device: WraithPrism) : PrismComponentWidgets(device, BasicPrismMode.values()) {
    override val component = device.fan

    private val mirageLabels = listOf("Red", "Green", "Blue").map { gtk_label_new("$it (Hz)")!! }
    private val mirageToggle = gtk_switch_new()!!.apply {
        gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_START)
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER) // override for some themes, like adwaita

        connectToSignal<StateSetCallbackFunc>("state-set", callbackPtr, staticCFunction { _, state, ptr ->
            ptr.useCallbackPtr<FanWidgets> { _, widgets ->
                val component = widgets.component

                component.mirageState = if (state == 0) MirageState.Off else MirageState.DEFAULT

                val sensitive = component.mode != BasicPrismMode.OFF && component.mirageState != MirageState.Off
                (widgets.mirageFreqSpinners union widgets.mirageLabels).forEach { widget ->
                    widget.setSensitive(sensitive)
                }
            }
            false
        })
    }

    private val mirageFreqSpinners = List(3) {
        gtk_spin_button_new_with_range(45.0, 2000.0, 1.0)!!.apply {
            gtk_spin_button_set_update_policy(reinterpret(), GtkSpinButtonUpdatePolicy.GTK_UPDATE_IF_VALID)
            gtk_spin_button_set_numeric(reinterpret(), 1)
            gtk_spin_button_set_value(reinterpret(), 330.0)
            addCss("spinbutton button { padding: 2px; min-width: unset; }")
        }
    } // red, green, blue; in order

    private val mirageReload = iconButton("gtk-ok", "Apply", callbackPtr, staticCFunction { _, ptr ->
        ptr.useCallbackPtr<FanWidgets> { wraith, widgets ->
            val component: PrismFanComponent = widgets.component

            if (gtk_switch_get_active(widgets.mirageToggle.reinterpret()) == 1) {
                val (red, green, blue) = widgets.mirageFreqSpinners.map {
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
        gtk_switch_set_state(mirageToggle.reinterpret(), if (component.mirageState is MirageState.On) 1 else 0)
        val mirageAllowed = component.mode != BasicPrismMode.OFF && !device.enso
        mirageToggle.setSensitive(mirageAllowed)
        mirageReload.setSensitive(mirageAllowed)
        (mirageFreqSpinners + mirageLabels).forEach {
            it.setSensitive(mirageAllowed && component.mirageState is MirageState.On)
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
}

class RingWidgets(wraith: WraithPrism) : PrismComponentWidgets(wraith, PrismRingMode.values()) {
    override val component = wraith.ring

    private var morseHintLabel: Widget? = null
    private var morseHint: Widget? = null

    private val directionComboBox = comboBox(RotationDirection.values().map { it.name }) {
        connectToSignal("changed", callbackPtr, staticCFunction<Widget, COpaquePointer, Unit> { widget, ptr ->
            ptr.useCallbackPtr<RingWidgets> { wraith, widgets ->
                val text = gtk_combo_box_text_get_active_text(widget.reinterpret())!!.toKString()
                widgets.component.direction = RotationDirection.valueOf(text.toUpperCase())
                wraith.apply()
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

        connectToSignal<StandardCallbackFunc>("changed", callbackPtr, staticCFunction { widget, ptr ->
            ptr.useCallbackPtr<RingWidgets> { _, widgets ->
                widgets.changeCallback(widget)
            }
        })
        connectToSignal<IconPressCallbackFunc>("icon-press", callbackPtr, staticCFunction { widget, _, _, ptr ->
            ptr.useCallbackPtr<RingWidgets> { _, widgets ->
                when {
                    widgets.morseHint == null -> {
                        widgets.morseHintLabel = gtk_label_new(widget.text.hintText)!!
                        widgets.morseHint = gtk_popover_new(widget)!!.apply {
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
        connectToSignal("clicked", callbackPtr, staticCFunction<Widget, COpaquePointer, Unit> { widget, ptr ->
            ptr.useCallbackPtr<RingWidgets> { wraith, widgets ->
                gtk_entry_set_text(widgets.morseTextBox.reinterpret(), widgets.morseTextBox.text.trim())
                wraith.updateRingMorseText(widgets.morseTextBox.text)
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
        gtk_combo_box_set_active(directionComboBox.reinterpret(), device.ring.direction.ordinal)
        directionComboBox.setSensitive(device.ring.mode.supportsDirection && !device.enso)
        morseContainer.setSensitive(device.ring.mode == PrismRingMode.MORSE && !device.enso)
        if (device.ring.mode != PrismRingMode.MORSE) morseHint?.let { hint -> gtk_widget_hide(hint) }
    }
}

private inline fun <W : PrismComponentWidgets> COpaquePointer.useCallbackPtr(action: (WraithPrism, W) -> Unit) {
    val (wraith, widgets) = asStableRef<CallbackData<W>>().get()
    if (!widgets.isReloading) {
        action(wraith, widgets)
    }
}

private class CallbackData<W : PrismComponentWidgets>(val wraith: WraithPrism, val widgets: W) {
    operator fun component1() = wraith
    operator fun component2() = widgets
}

private val String.hintText: String
    get() = when {
        isBlank() -> "Enter either morse code (dots and dashes) or text"
        parseMorseOrTextToBytes().size > 120 -> "Maximum length exceeded"
        isMorseCode -> "Parsing as morse code"
        isValidMorseText -> "Parsing as text"
        else -> "Invalid characters detected: $invalidMorseChars"
    }
