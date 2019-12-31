@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package com.serebit.wraith.gtk

import com.serebit.wraith.*
import gtk3.*
import kotlinx.cinterop.*
import kotlin.system.exitProcess

// Note that all callback parameters must be primitive types or nullable C pointers.
fun <F : CFunction<*>> g_signal_connect(
    obj: CPointer<*>, actionName: String,
    action: CPointer<F>, data: gpointer? = null, connect_flags: GConnectFlags = 0u
) {
    g_signal_connect_data(
        obj.reinterpret(), actionName, action.reinterpret(),
        data = data, destroy_data = null, connect_flags = connect_flags
    )
}

val GdkRGBA.r get() = (red * 256 - 1).toInt().toUByte()
val GdkRGBA.g get() = (green * 256 - 1).toInt().toUByte()
val GdkRGBA.b get() = (blue * 256 - 1).toInt().toUByte()

fun CPointer<GtkApplication>.activate() {
    val windowWidget = gtk_application_window_new(this)!!

    val window = windowWidget.reinterpret<GtkWindow>()
    gtk_window_set_title(window, "Wraith Master")
    gtk_window_set_default_size(window, 480, 200)

    val box = gtk_box_new(GtkOrientation.GTK_ORIENTATION_VERTICAL, 0)
    gtk_container_add(window.reinterpret(), box)

    val settingsGrid = gtk_grid_new()
    gtk_container_add(box?.reinterpret(), settingsGrid)
    gtk_grid_set_column_spacing(settingsGrid?.reinterpret(), 64)
    gtk_grid_set_row_spacing(settingsGrid?.reinterpret(), 8)
    gtk_container_set_border_width(settingsGrid?.reinterpret(), 32)

    val logoLabel = gtk_label_new("Logo Color")
    gtk_widget_set_halign(logoLabel, GtkAlign.GTK_ALIGN_START)
    gtk_widget_set_hexpand(logoLabel, 1)
    gtk_grid_attach(settingsGrid?.reinterpret(), logoLabel, 0, 0, 1, 1)

    val logoColorChooser = gtk_color_button_new()!!
    gtk_color_button_set_use_alpha(logoColorChooser.reinterpret(), 0)
    gtk_color_button_set_title(logoColorChooser.reinterpret(), "Logo Color")
    g_signal_connect(logoColorChooser, "color-set", staticCFunction<CPointer<GtkWidget>, Unit> {
        val color = memScoped { alloc<GdkRGBA>().apply { gtk_color_button_get_rgba(it.reinterpret(), ptr) } }
        device.setChannel(0x06u, 0xFFu, 0x20u, 0x01u, 0xFFu, color.r, color.g, color.b)
    })
    gtk_grid_attach(settingsGrid?.reinterpret(), logoColorChooser, 1, 0, 1, 1)

    val fanLabel = gtk_label_new("Fan Color")
    gtk_widget_set_halign(fanLabel, GtkAlign.GTK_ALIGN_START)
    gtk_widget_set_hexpand(fanLabel, 1)
    gtk_grid_attach(settingsGrid?.reinterpret(), fanLabel, 0, 1, 1, 1)

    val fanColorChooser = gtk_color_button_new()!!
    gtk_color_button_set_use_alpha(fanColorChooser.reinterpret(), 0)
    gtk_color_button_set_title(fanColorChooser.reinterpret(), "Fan Color")
    g_signal_connect(fanColorChooser, "color-set", staticCFunction<CPointer<GtkWidget>, Unit> {
        val color = memScoped { alloc<GdkRGBA>().apply { gtk_color_button_get_rgba(it.reinterpret(), ptr) } }
        device.setChannel(0x05u, 0xFFu, 0x20u, 0x01u, 0xFFu, color.r, color.g, color.b)
    })
    gtk_grid_attach(settingsGrid?.reinterpret(), fanColorChooser, 1, 1, 1, 1)

    val ringLabel = gtk_label_new("Ring Color")
    gtk_widget_set_halign(ringLabel, GtkAlign.GTK_ALIGN_START)
    gtk_widget_set_hexpand(ringLabel, 1)
    gtk_grid_attach(settingsGrid?.reinterpret(), ringLabel, 0, 2, 1, 1)

    val ringColorChooser = gtk_color_button_new()!!
    gtk_color_button_set_use_alpha(ringColorChooser.reinterpret(), 0)
    gtk_color_button_set_title(ringColorChooser.reinterpret(), "Ring Color")
    g_signal_connect(ringColorChooser, "color-set", staticCFunction<CPointer<GtkWidget>, Unit> {
        // enable ring LEDs
        device.sendBytes(0x51u, 0xa0u, 0x01u, 0u, 0u, 0x03u, 0u, 0u, 0x05u, 0x06u)

        val color = memScoped { alloc<GdkRGBA>().apply { gtk_color_button_get_rgba(it.reinterpret(), ptr) } }
        device.setChannel(0x00u, 0xFFu, 0x20u, 0xFFu, 0xFFu, color.r, color.g, color.b)
    })
    gtk_grid_attach(settingsGrid?.reinterpret(), ringColorChooser, 1, 2, 1, 1)

    val saveOptionBox = gtk_button_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL)
    gtk_container_add(box?.reinterpret(), saveOptionBox)
    gtk_container_set_border_width(saveOptionBox?.reinterpret(), 16)
    gtk_button_box_set_layout(saveOptionBox?.reinterpret(), GTK_BUTTONBOX_END)
    gtk_box_set_child_packing(box?.reinterpret(), saveOptionBox, 0, 1, 0, GtkPackType.GTK_PACK_END)

    val resetOption = gtk_button_new()!!
    gtk_button_set_label(resetOption.reinterpret(), "Reset")
    g_signal_connect(resetOption, "clicked", staticCFunction<CPointer<GtkWidget>, Unit> { device.reset() })
    gtk_container_add(saveOptionBox?.reinterpret(), resetOption)

    val saveOption = gtk_button_new()!!
    gtk_button_set_label(saveOption.reinterpret(), "Save")
    gtk_style_context_add_class(gtk_widget_get_style_context(saveOption), "suggested-action")
    g_signal_connect(saveOption, "clicked", staticCFunction<CPointer<GtkWidget>, Unit> { device.save() })
    gtk_container_add(saveOptionBox?.reinterpret(), saveOption)

    gtk_widget_show_all(windowWidget)
}

fun main(args: Array<String>) {
    val app = gtk_application_new("com.serebit.wraith", G_APPLICATION_FLAGS_NONE)!!
    g_signal_connect(app, "activate", staticCFunction { it: CPointer<GtkApplication>, _: gpointer -> it.activate() })
    val status = memScoped {
        g_application_run(app.reinterpret(), args.size, args.map { it.cstr.ptr }.toCValues())
    }
    g_object_unref(app)

    device.close()

    if (status != 0) exitProcess(status)
}
