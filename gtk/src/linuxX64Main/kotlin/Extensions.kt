package com.serebit.wraith.gtk

import com.serebit.wraith.core.prism.*
import gtk3.*
import kotlinx.cinterop.*

typealias Widget = CPointer<GtkWidget>
typealias StandardCallbackFunc = CFunction<(Widget, COpaquePointer) -> Unit>

fun <F : CFunction<*>> CPointer<*>.connectToSignal(signal: String, data: COpaquePointer?, action: CPointer<F>) =
    g_signal_connect_data(reinterpret(), signal, action.reinterpret(), data, null, 0u)

fun MemScope.gdkRgba(color: Color) = alloc<GdkRGBA>().apply {
    red = color.r.toDouble() / 255
    green = color.g.toDouble() / 255
    blue = color.b.toDouble() / 255
    alpha = 1.0
}

val Widget.text get() = gtk_entry_get_text(reinterpret())!!.toKString()

fun Widget.newSettingsPage(label: String) = gtk_box_new(GtkOrientation.GTK_ORIENTATION_VERTICAL, 0)!!.apply {
    gtk_container_set_border_width(reinterpret(), 16u)
    addCss("box { padding: 0 10px; }")
    gtk_widget_set_vexpand(this, 1)
    gtk_notebook_append_page(this@newSettingsPage.reinterpret(), this, gtk_label_new(label))
}

fun Widget.newSettingsGrid(): Widget = gtk_grid_new()!!.apply {
    gtk_grid_set_column_spacing(reinterpret(), 64u)
    gtk_grid_set_row_spacing(reinterpret(), 10u)
    gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_START)
    gtk_container_add(this@newSettingsGrid.reinterpret(), this)
}

fun Widget.newGridLabel(position: Int, text: String) = gtk_label_new(text)!!.apply {
    gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_START)
    gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_START)
    gtk_widget_set_hexpand(this, 1)
    gtk_widget_set_size_request(this, -1, 36)
    gtk_grid_attach(this@newGridLabel.reinterpret(), this, 0, position, 1, 1)
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

inline fun comboBox(elements: List<String>, init: Widget.() -> Unit = {}) =
    gtk_combo_box_text_new()!!.apply {
        elements.forEach { gtk_combo_box_text_append_text(reinterpret(), it.toLowerCase().capitalize()) }
        gtk_widget_set_size_request(this, 96, -1)
        gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_END)
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
        init()
    }

fun iconButton(iconName: String, text: String?, ptr: COpaquePointer?, onClick: CPointer<StandardCallbackFunc>) =
    gtk_button_new_from_icon_name(iconName, GtkIconSize.GTK_ICON_SIZE_BUTTON)!!.apply {
        text?.let { gtk_button_set_label(reinterpret(), it) }
        gtk_button_set_always_show_image(reinterpret(), 1)
        gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
        connectToSignal("clicked", ptr, onClick)
    }

fun gridScale(marks: Int, data: COpaquePointer, action: CPointer<StandardCallbackFunc>) =
    gtk_adjustment_new(0.0, 0.0, marks.toDouble() - 1, 1.0, 0.0, 0.0)!!.let { adjustment ->
        gtk_scale_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, adjustment)!!.apply {
            gtk_scale_set_digits(reinterpret(), 0)
            gtk_scale_set_draw_value(reinterpret(), 0)
            gtk_widget_set_size_request(this, 96, -1)
            gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_END)
            gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_CENTER)
            for (i in 0 until marks) {
                gtk_scale_add_mark(reinterpret(), i.toDouble(), GtkPositionType.GTK_POS_BOTTOM, null)
            }
        }.also { adjustment.connectToSignal("value-changed", data, action) }
    }

fun Widget.clearFocusOnClickOrEsc() {
    connectToSignal("button-press-event", null,
        staticCFunction<Widget, CPointer<GdkEventButton>, Boolean> { it, event ->
            if (event.pointed.type == GDK_BUTTON_PRESS && event.pointed.button == 1u) {
                gtk_window_set_focus(it.reinterpret(), null)
                gtk_window_set_focus_visible(it.reinterpret(), 0)
            }
            false
        })

    connectToSignal("key-press-event", null,
        staticCFunction<Widget, CPointer<GdkEventKey>, Boolean> { it, event ->
            if (event.pointed.type == GDK_KEY_PRESS && event.pointed.keyval == 0xFF1Bu) {
                gtk_window_set_focus(it.reinterpret(), null)
                gtk_window_set_focus_visible(it.reinterpret(), 0)
            }
            false
        })
}
