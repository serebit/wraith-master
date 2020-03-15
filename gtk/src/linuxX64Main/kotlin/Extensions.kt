package com.serebit.wraith.gtk

import com.serebit.wraith.core.*
import gtk3.*
import kotlinx.cinterop.*
import kotlin.math.roundToInt

typealias Widget = CPointer<GtkWidget>
typealias CallbackCFunction = CPointer<CFunction<(Widget, COpaquePointer) -> Unit>>
private typealias CallbackSpinCFunction = CPointer<CFunction<(Widget, GtkScrollType, COpaquePointer) -> Unit>>

@OptIn(ExperimentalUnsignedTypes::class)
fun <F : CFunction<*>> CPointer<*>.connectSignalWithData(signal: String, data: COpaquePointer?, action: CPointer<F>) =
    g_signal_connect_data(reinterpret(), signal, action.reinterpret(), data, null, 0u)

fun <F : CFunction<*>> CPointer<*>.connectSignal(signal: String, action: CPointer<F>) =
    connectSignalWithData(signal, null, action)

@OptIn(ExperimentalUnsignedTypes::class)
fun MemScope.gdkRgba(color: Color) = alloc<GdkRGBA>().apply {
    red = color.r.toDouble() / 255
    green = color.g.toDouble() / 255
    blue = color.b.toDouble() / 255
    alpha = 1.0
}

val Widget.text get() = gtk_entry_get_text(reinterpret())!!.toKString()

fun Widget.align() {
    gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_END)
    gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun Widget.newSettingsPage(label: String) = gtk_box_new(GtkOrientation.GTK_ORIENTATION_VERTICAL, 0)!!.apply {
    gtk_container_set_border_width(reinterpret(), 16u)
    addCss("box { padding: 0 8px; }")
    gtk_widget_set_vexpand(this, 1)
    gtk_notebook_append_page(this@newSettingsPage.reinterpret(), this, gtk_label_new(label))
}

@OptIn(ExperimentalUnsignedTypes::class)
fun Widget.newSettingsGrid(): Widget = gtk_grid_new()!!.apply {
    gtk_grid_set_row_homogeneous(reinterpret(), 1)
    gtk_grid_set_column_spacing(reinterpret(), 64u)
    gtk_grid_set_row_spacing(reinterpret(), 8u)
    gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_START)
    gtk_container_add(this@newSettingsGrid.reinterpret(), this)
}

fun Widget.newGridLabels(vararg text: String) = text.forEachIndexed { i, it -> newGridLabel(i, it) }
fun Widget.newGridLabel(position: Int, text: String) = gtk_label_new(text)!!.apply {
    gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_START)
    gtk_widget_set_hexpand(this, 1)
    gtk_widget_set_size_request(this, -1, 36)
    gtk_grid_attach(this@newGridLabel.reinterpret(), this, 0, position, 1, 1)
}

fun Widget.addChild(widget: Widget) = gtk_container_add(reinterpret(), widget)

fun Widget.gridAttachRight(widget: Widget, position: Int) {
    widget.align()
    gtk_grid_attach(reinterpret(), widget, 1, position, 1, 1)
}

fun Widget.setSensitive(boolean: Boolean) = gtk_widget_set_sensitive(this, boolean.toByte().toInt())

fun Widget.addCss(css: String) = gtk_widget_get_style_context(this)!!.apply {
    val provider = gtk_css_provider_new()!!
    memScoped {
        val err = allocPointerTo<GError>().ptr
        gtk_css_provider_load_from_data(provider, css, css.length.toLong(), err)
        err.pointed.pointed?.message?.toKString()?.let { println("CSS Error: $it") }
    }
    gtk_style_context_add_provider(this, provider.reinterpret(), GTK_STYLE_PROVIDER_PRIORITY_USER)
}

fun comboBox(default: String, elements: List<String>, data: COpaquePointer, action: CallbackCFunction) =
    gtk_combo_box_text_new()!!.apply {
        elements.forEach { gtk_combo_box_text_append_text(reinterpret(), it.toLowerCase().capitalize()) }
        gtk_combo_box_set_active(reinterpret(), elements.indexOf(default))
        gtk_widget_set_size_request(this, 96, -1)
        align()
        connectSignalWithData("changed", data, action)
    }

fun colorButton(color: Color, ptr: COpaquePointer, onColorChange: CallbackCFunction) = gtk_color_button_new()!!.apply {
    gtk_color_button_set_use_alpha(reinterpret(), 0)
    memScoped { gtk_color_button_set_rgba(reinterpret(), gdkRgba(color).ptr) }
    gtk_widget_set_size_request(this, 96, -1)
    connectSignalWithData("color-set", ptr, onColorChange)
}

fun gridScale(default: Int, marks: Int, data: COpaquePointer, action: CallbackCFunction) =
    gtk_adjustment_new(default.toDouble(), 1.0, marks.toDouble(), 1.0, 0.0, 0.0)!!.let { adjustment ->
        adjustment.connectSignalWithData("value-changed", data, action)
        gtk_scale_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, adjustment)!!.apply {
            gtk_scale_set_digits(reinterpret(), 0)
            gtk_scale_set_draw_value(reinterpret(), 0)
            gtk_widget_set_size_request(this, 96, -1)
            align()
            for (i in 1..marks) gtk_scale_add_mark(reinterpret(), i.toDouble(), GtkPositionType.GTK_POS_BOTTOM, null)
        }
    }

fun frequencySpinButton(data: COpaquePointer, action: CallbackSpinCFunction) =
    gtk_spin_button_new_with_range(50.0, 2000.0, 50.0)!!.apply {
        gtk_spin_button_set_update_policy(reinterpret(), GtkSpinButtonUpdatePolicy.GTK_UPDATE_IF_VALID)
        gtk_spin_button_set_snap_to_ticks(reinterpret(), 1)
        gtk_spin_button_set_numeric(reinterpret(), 1)
        gtk_spin_button_set_value(reinterpret(), 300.0)
        connectSignalWithData("change-value", data, action)
        gtk_adjustment_set_step_increment(gtk_spin_button_get_adjustment(reinterpret())!!.reinterpret(), 50.0)
    }

fun WraithPrism.updateColor(component: LedComponent, colorButton: Widget) = update(component) {
    color = memScoped {
        alloc<GdkRGBA>().apply { gtk_color_button_get_rgba(colorButton.reinterpret(), ptr) }
            .run { Color((255 * red).toInt(), (255 * green).toInt(), (255 * blue).toInt()) }
    }
}

fun WraithPrism.updateRandomize(component: LedComponent, checkButton: Widget, colorButton: Widget) {
    val isActive = gtk_toggle_button_get_active(checkButton.reinterpret())
    if (component.mode.colorSupport == ColorSupport.ALL) update(component) { useRandomColor = isActive == 1 }
    colorButton.setSensitive(isActive == 0)
}

fun WraithPrism.updateSpeed(component: LedComponent, adjustment: Widget) = update(component) {
    speed = gtk_adjustment_get_value(adjustment.reinterpret()).roundToInt()
}

fun WraithPrism.updateBrightness(component: LedComponent, adjustment: Widget) = update(component) {
    brightness = gtk_adjustment_get_value(adjustment.reinterpret()).roundToInt()
}

fun WraithPrism.updateMode(widgets: ComponentWidgets<*>, comboBox: Widget) = update(widgets.component) {
    val text = gtk_combo_box_text_get_active_text(comboBox.reinterpret())!!.toKString()
    when (this) {
        is BasicLedComponent -> mode = LedMode[text.toUpperCase()]
        is RingComponent -> {
            mode = RingMode[text.toUpperCase()]
            assignValuesFromChannel(getChannelValues(mode.channel))
        }
    }
    widgets.fullReload()
}

val LedComponent.colorOrBlack get() = if (mode.colorSupport != ColorSupport.NONE) color else Color(0, 0, 0)
