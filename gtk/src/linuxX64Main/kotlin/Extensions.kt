package com.serebit.wraith.gtk

import com.serebit.wraith.core.*
import gtk3.*
import kotlinx.cinterop.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

typealias Widget = CPointer<GtkWidget>
typealias CmpCallbackFunction = CPointer<CFunction<(Widget, COpaquePointer) -> Unit>>

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

@OptIn(ExperimentalUnsignedTypes::class)
fun Widget.newSettingsPage(label: String): Widget =
    gtk_box_new(GtkOrientation.GTK_ORIENTATION_VERTICAL, 0)!!.apply {
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

fun Widget.gridAttachRight(widget: Widget, position: Int) =
    gtk_grid_attach(reinterpret(), widget, 1, position, 1, 1)

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

fun gridComboBox(default: String, elements: List<String>, sensitive: Boolean) =
    gtk_combo_box_text_new()!!.apply {
        elements.forEach {
            gtk_combo_box_text_append_text(reinterpret(), it.toLowerCase().capitalize())
        }
        gtk_combo_box_set_active(reinterpret(), elements.indexOf(default))
        setSensitive(sensitive)
        gtk_widget_set_size_request(this, 96, -1)
        gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_END)
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
    }

fun gridColorButton(color: Color, sensitive: Boolean) = gtk_color_button_new()!!.apply {
    gtk_color_button_set_use_alpha(reinterpret(), 0)
    memScoped { gtk_color_button_set_rgba(reinterpret(), gdkRgba(color).ptr) }
    gtk_widget_set_size_request(this, 96, -1)
    gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_END)
    gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
    setSensitive(sensitive)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun gridScale(default: Int, marks: Int, sensitive: Boolean, data: COpaquePointer, action: CmpCallbackFunction) =
    gtk_adjustment_new(default.toDouble(), 1.0, marks.toDouble(), 1.0, 0.0, 0.0)!!.let { adjustment ->
        adjustment.connectSignalWithData("value-changed", data, action)
        gtk_scale_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, adjustment)!!.apply {
            gtk_scale_set_digits(reinterpret(), 0)
            gtk_scale_set_draw_value(reinterpret(), 0)
            gtk_widget_set_size_request(this, 96, -1)
            gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_END)
            gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
            for (i in 1..marks) {
                gtk_scale_add_mark(reinterpret(), i.toDouble(), GtkPositionType.GTK_POS_BOTTOM, null)
            }
            setSensitive(sensitive)
        }
    }

fun WraithPrism.updateColor(component: LedComponent, colorButton: Widget) = update(component) {
    color = memScoped {
        alloc<GdkRGBA>()
            .apply { gtk_color_button_get_rgba(colorButton.reinterpret(), ptr) }
            .run { Color(red, green, blue) }
    }
}

fun WraithPrism.updateRandomize(component: LedComponent, checkButton: Widget, colorButton: Widget) {
    val isActive = gtk_toggle_button_get_active(checkButton.reinterpret())
    if (component.mode.colorSupport == ColorSupport.ALL) update(component) { useRandomColor = isActive == 1 }
    gtk_widget_set_sensitive(colorButton, (isActive - 1).absoluteValue)
}

fun WraithPrism.updateSpeed(component: LedComponent, adjustment: Widget) = update(component) {
    speed = gtk_adjustment_get_value(adjustment.reinterpret()).roundToInt()
}

fun WraithPrism.updateBrightness(component: LedComponent, adjustment: Widget) = update(component) {
    brightness = gtk_adjustment_get_value(adjustment.reinterpret()).roundToInt()
}

fun WraithPrism.updateMode(component: LedComponent, comboBox: Widget) = update(component) {
    val text = gtk_combo_box_text_get_active_text(comboBox.reinterpret())!!.toKString()
    when (this) {
        is BasicLedComponent -> mode = LedMode[text.toUpperCase()]
        is RingComponent -> mode = RingMode[text.toUpperCase()]
    }
}

val LedComponent.colorOrBlack get() = if (mode.colorSupport != ColorSupport.NONE) color else Color(0, 0, 0)
