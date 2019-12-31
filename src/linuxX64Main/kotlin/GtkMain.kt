@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package com.serebit.wraith

import gtk3.*
import kotlinx.cinterop.*

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

fun activate(app: CPointer<GtkApplication>?, user_data: gpointer?) {
    val provider = gtk_css_provider_new()

    val error = memScoped {
        val error = allocPointerTo<GError>()
        gtk_css_provider_load_from_path(provider, "style.css", error.ptr)
        error
    }

    val windowWidget = gtk_application_window_new(app)!!

    val window = windowWidget.reinterpret<GtkWindow>()
    gtk_window_set_title(window, "Wraith Master")

    val box = gtk_box_new(GtkOrientation.GTK_ORIENTATION_VERTICAL, 0)
    gtk_container_add(window.reinterpret(), box)

    val settings = gtk_grid_new()
    gtk_container_add(box?.reinterpret(), settings)
    gtk_grid_set_column_spacing(settings?.reinterpret(), 64)
    gtk_grid_set_row_spacing(settings?.reinterpret(), 8)

    val style = gtk_widget_get_style_context(settings)
    gtk_style_context_add_class(style, "main-grid")
    gtk_style_context_add_provider(style, provider?.reinterpret(), GTK_STYLE_PROVIDER_PRIORITY_USER)

    val logoLabel = gtk_label_new("Logo Color")
    gtk_grid_attach(settings?.reinterpret(), logoLabel, 0, 0, 1, 1)
    gtk_widget_set_hexpand(logoLabel, 1)
    val logoColorChooser = gtk_color_button_new()!!
    gtk_color_button_set_use_alpha(logoColorChooser.reinterpret(), 0)
    gtk_color_button_set_title(logoColorChooser.reinterpret(), "Logo Color")
    gtk_grid_attach(settings?.reinterpret(), logoColorChooser, 1, 0, 1, 1)

    gtk_grid_attach(settings?.reinterpret(), gtk_label_new("Fan Color"), 0, 1, 1, 1)
    val fanColorChooser = gtk_color_button_new()!!
    gtk_color_button_set_use_alpha(fanColorChooser.reinterpret(), 0)
    gtk_color_button_set_title(fanColorChooser.reinterpret(), "Fan Color")
    gtk_grid_attach(settings?.reinterpret(), fanColorChooser, 1, 1, 1, 1)

    val bottomToolbar = gtk_toolbar_new()
    gtk_container_add(box?.reinterpret(), bottomToolbar)
    val saveOption = gtk_tool_button_new(null, "Save")
    gtk_toolbar_insert(bottomToolbar?.reinterpret(), saveOption, 0)

    g_signal_connect(logoColorChooser, "color-set", staticCFunction { button: CPointer<GtkWidget>? ->
        val color = memScoped { alloc<GdkRGBA>().apply { gtk_color_button_get_rgba(button?.reinterpret(), ptr) } }
        device.setChannel(0x06u, 0x3Cu, 0x20u, 0x01u, 0xFFu, color.r, color.g, color.b)
    })

    g_signal_connect(fanColorChooser, "color-set", staticCFunction { button: CPointer<GtkWidget>? ->
        val color = memScoped { alloc<GdkRGBA>().apply { gtk_color_button_get_rgba(button?.reinterpret(), ptr) } }
        device.setChannel(0x05u, 0x3Cu, 0x20u, 0x01u, 0xFFu, color.r, color.g, color.b)
    })

    gtk_widget_show_all(windowWidget)
}

fun gtkMain(args: Array<String>): Int {
    val app = gtk_application_new("org.gtk.example", G_APPLICATION_FLAGS_NONE)!!
    g_signal_connect(app, "activate", staticCFunction(::activate))
    val status = memScoped {
        g_application_run(app.reinterpret(), args.size, args.map { it.cstr.ptr }.toCValues())
    }
    g_object_unref(app)
    return status
}
