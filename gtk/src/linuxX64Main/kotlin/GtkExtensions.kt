package com.serebit.wraith.gtk

import com.serebit.wraith.core.Color
import gtk3.*
import kotlinx.cinterop.*

private typealias GtkCallbackFunction = CPointer<CFunction<(CPointer<GtkWidget>) -> Unit>>

@UseExperimental(ExperimentalUnsignedTypes::class)
fun <F : CFunction<*>> gSignalConnect(
    obj: CPointer<*>, actionName: String,
    action: CPointer<F>, data: gpointer? = null, connect_flags: GConnectFlags = 0u
) = g_signal_connect_data(
    obj.reinterpret(), actionName, action.reinterpret(),
    data = data, destroy_data = null, connect_flags = connect_flags
)

@UseExperimental(ExperimentalUnsignedTypes::class)
fun MemScope.gdkRgba(color: Color) = alloc<GdkRGBA>().apply {
    red = color.r.toDouble() / 255
    green = color.g.toDouble() / 255
    blue = color.b.toDouble() / 255
    alpha = 1.0
}

fun GdkRGBA.toColor() = Color(red, green, blue)

@UseExperimental(ExperimentalUnsignedTypes::class)
fun CPointer<GtkWidget>.newSettingsPage(label: String): CPointer<GtkWidget> =
    gtk_box_new(GtkOrientation.GTK_ORIENTATION_VERTICAL, 0)!!.apply {
        gtk_container_set_border_width(reinterpret(), 24u)
        gtk_widget_set_vexpand(this, 1)
        gtk_notebook_append_page(this@newSettingsPage.reinterpret(), this, gtk_label_new(label))
    }

@UseExperimental(ExperimentalUnsignedTypes::class)
fun CPointer<GtkWidget>.newSettingsGrid(): CPointer<GtkWidget> = gtk_grid_new()!!.apply {
    gtk_grid_set_row_homogeneous(reinterpret(), 1)
    gtk_grid_set_column_spacing(reinterpret(), 64)
    gtk_grid_set_row_spacing(reinterpret(), 8)
    gtk_widget_set_valign(this, GtkAlign.GTK_ALIGN_START)
    gtk_container_add(this@newSettingsGrid.reinterpret(), this)
}

fun <E : Enum<*>> CPointer<GtkWidget>.gridComboBox(
    position: Int, default: E, elements: Array<E>, action: GtkCallbackFunction
) = gtk_combo_box_text_new()?.apply {
    elements.forEach {
        gtk_combo_box_text_append_text(reinterpret(), it.name.toLowerCase().capitalize())
    }
    gtk_combo_box_set_active(reinterpret(), elements.indexOf(default))
    gSignalConnect(this, "changed", action)
    gtk_grid_attach(this@gridComboBox.reinterpret(), this, 1, position, 1, 1)
}

fun CPointer<GtkWidget>.gridColorButton(position: Int, color: Color, action: GtkCallbackFunction) =
    gtk_color_button_new()?.apply {
        gtk_color_button_set_use_alpha(reinterpret(), 0)
        memScoped { gtk_color_button_set_rgba(reinterpret(), gdkRgba(color).ptr) }
        gtk_widget_set_size_request(this, 72, -1)
        gSignalConnect(this, "color-set", action)
        gtk_grid_attach(this@gridColorButton.reinterpret(), this, 1, position, 1, 1)
    }

@ExperimentalUnsignedTypes
fun CPointer<GtkWidget>.gridScale(position: Int, default: UByte, marks: Int, action: GtkCallbackFunction) =
    gtk_adjustment_new(default.toDouble(), 1.0, marks.toDouble(), 1.0, 0.0, 0.0)?.let { adjustment ->
        gSignalConnect(adjustment, "value-changed", action)
        gtk_scale_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, adjustment)?.apply {
            gtk_scale_set_digits(reinterpret(), 0)
            gtk_scale_set_draw_value(reinterpret(), 0)
            for (i in 1..marks) {
                gtk_scale_add_mark(reinterpret(), i.toDouble(), GtkPositionType.GTK_POS_BOTTOM, null)
            }
            gtk_grid_attach(this@gridScale.reinterpret(), this, 1, position, 1, 1)
        }
    }
