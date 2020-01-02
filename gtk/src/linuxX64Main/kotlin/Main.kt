@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package com.serebit.wraith.gtk

import com.serebit.wraith.core.Color
import com.serebit.wraith.core.device
import com.serebit.wraith.core.reset
import com.serebit.wraith.core.save
import gtk3.*
import kotlinx.cinterop.*
import kotlin.math.roundToInt
import kotlin.system.exitProcess

private typealias GtkCallbackFunction = CPointer<CFunction<(CPointer<GtkWidget>) -> Unit>>

fun <F : CFunction<*>> g_signal_connect(
    obj: CPointer<*>, actionName: String,
    action: CPointer<F>, data: gpointer? = null, connect_flags: GConnectFlags = 0u
) = g_signal_connect_data(
    obj.reinterpret(), actionName, action.reinterpret(),
    data = data, destroy_data = null, connect_flags = connect_flags
)

fun MemScope.GdkRGBA(color: Color) = alloc<GdkRGBA>().apply {
    red = color.r.toDouble() / 255
    green = color.g.toDouble() / 255
    blue = color.b.toDouble() / 255
    alpha = 1.0
}

fun GdkRGBA.toColor() = Color(red, green, blue)

fun CPointer<GtkApplication>.activate() {
    val windowWidget = gtk_application_window_new(this)!!

    val window = windowWidget.reinterpret<GtkWindow>()
    gtk_window_set_title(window, "Wraith Master")
    gtk_window_set_default_size(window, 480, 200)

    val box = gtk_box_new(GtkOrientation.GTK_ORIENTATION_VERTICAL, 0)
    gtk_container_add(window.reinterpret(), box)

    val settingsGrid = gtk_grid_new()?.apply {
        gtk_grid_set_row_homogeneous(reinterpret(), 1)
        gtk_container_add(box?.reinterpret(), this)
        gtk_grid_set_column_spacing(reinterpret(), 64)
        gtk_grid_set_row_spacing(reinterpret(), 8)
        gtk_container_set_border_width(reinterpret(), 32)
    }

    fun gridLabel(text: String, position: Int) = gtk_label_new(text)?.apply {
        gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_START)
        gtk_widget_set_hexpand(this, 1)
        gtk_grid_attach(settingsGrid?.reinterpret(), this, 0, position, 1, 1)
    }

    gridLabel("Logo Color", 0)
    gridLabel("Logo Brightness", 1)
    gridLabel("Fan Color", 2)
    gridLabel("Fan Brightness", 3)
    gridLabel("Ring Color", 4)
    gridLabel("Ring Brightness", 5)

    memScoped {
        fun gridColorButton(color: Color, position: Int, action: GtkCallbackFunction) =
            gtk_color_button_new()?.apply {
                gtk_color_button_set_use_alpha(reinterpret(), 0)
                gtk_color_button_set_rgba(reinterpret(), GdkRGBA(color).ptr)
                gtk_widget_set_size_request(this, 72, -1)
                g_signal_connect(this, "color-set", action)
                gtk_grid_attach(settingsGrid?.reinterpret(), this, 1, position, 1, 1)
            }

        fun gridScale(value: UByte, position: Int, action: GtkCallbackFunction) =
            gtk_adjustment_new(value.toDouble() / 51, 1.0, 5.0, 1.0, 0.0, 0.0)?.let { adjustment ->
                g_signal_connect(adjustment, "value-changed", action)
                gtk_scale_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL, adjustment)?.apply {
                    gtk_scale_set_digits(reinterpret(), 0)
                    gtk_scale_set_draw_value(reinterpret(), 0)
                    for (i in 1..5) {
                        gtk_scale_add_mark(reinterpret(), i.toDouble(), GtkPositionType.GTK_POS_BOTTOM, null)
                    }
                    gtk_grid_attach(settingsGrid?.reinterpret(), this, 1, position, 1, 1)
                }
            }

        gridColorButton(device.logo.color, 0, staticCFunction<CPointer<GtkWidget>, Unit> {
            device.logo.color = memScoped {
                alloc<GdkRGBA>().apply { gtk_color_button_get_rgba(it.reinterpret(), ptr) }.toColor()
            }
        })
        gridScale(device.logo.brightness, 1, staticCFunction<CPointer<GtkWidget>, Unit> {
            device.logo.brightness = (gtk_adjustment_get_value(it.reinterpret()).roundToInt() * 51).toUByte()
        })
        gridColorButton(device.fan.color, 2, staticCFunction<CPointer<GtkWidget>, Unit> {
            device.fan.color = memScoped {
                alloc<GdkRGBA>().apply { gtk_color_button_get_rgba(it.reinterpret(), ptr) }.toColor()
            }
        })
        gridScale(device.fan.brightness, 3, staticCFunction<CPointer<GtkWidget>, Unit> {
            device.fan.brightness = (gtk_adjustment_get_value(it.reinterpret()).roundToInt() * 51).toUByte()
        })
        gridColorButton(device.ring.color, 4, staticCFunction<CPointer<GtkWidget>, Unit> {
            device.ring.color = memScoped {
                alloc<GdkRGBA>().apply { gtk_color_button_get_rgba(it.reinterpret(), ptr) }.toColor()
            }
        })
        gridScale(device.ring.brightness, 5, staticCFunction<CPointer<GtkWidget>, Unit> {
            device.ring.brightness = (gtk_adjustment_get_value(it.reinterpret()).roundToInt() * 51).toUByte()
        })
    }

    val saveOptionBox = gtk_button_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL)?.apply {
        gtk_container_add(box?.reinterpret(), this)
        gtk_container_set_border_width(reinterpret(), 16)
        gtk_button_box_set_layout(reinterpret(), GTK_BUTTONBOX_END)
        gtk_box_set_child_packing(box?.reinterpret(), this, 0, 1, 0, GtkPackType.GTK_PACK_END)
    }

    gtk_button_new()?.apply {
        gtk_button_set_label(reinterpret(), "Reset")
        g_signal_connect(this, "clicked", staticCFunction<CPointer<GtkWidget>, Unit> { device.reset() })
        gtk_container_add(saveOptionBox?.reinterpret(), this)
    }

    gtk_button_new()?.apply {
        gtk_button_set_label(reinterpret(), "Save")
        gtk_style_context_add_class(gtk_widget_get_style_context(this), "suggested-action")
        g_signal_connect(this, "clicked", staticCFunction<CPointer<GtkWidget>, Unit> { device.save() })
        gtk_container_add(saveOptionBox?.reinterpret(), this)
    }

    gtk_widget_show_all(windowWidget)
}

fun main(args: Array<String>) {
    var app: CPointer<GtkApplication>? = null
    var status = 0
    try {
        app = gtk_application_new("com.serebit.wraith", G_APPLICATION_FLAGS_NONE)!!
        g_signal_connect(app, "activate", staticCFunction { it: CPointer<GtkApplication>, _: gpointer ->
            it.activate()
        })
        status = memScoped { g_application_run(app.reinterpret(), args.size, args.map { it.cstr.ptr }.toCValues()) }
    } finally {
        g_object_unref(app)
        device.close()
        if (status != 0) exitProcess(status)
    }
}
