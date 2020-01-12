package com.serebit.wraith.gtk

import com.serebit.wraith.core.*
import gtk3.*
import kotlinx.cinterop.*
import kotlin.math.roundToInt
import kotlin.properties.Delegates
import kotlin.system.exitProcess

val wraith by lazy { obtainWraithPrism() }

@UseExperimental(ExperimentalUnsignedTypes::class)
fun CPointer<GtkApplication>.activate() {
    val windowWidget = gtk_application_window_new(this)!!

    val window = windowWidget.reinterpret<GtkWindow>()
    gtk_window_set_title(window, "Wraith Master")
    gtk_window_set_default_size(window, 480, 200)
    gtk_window_set_default_icon_name("applications-games")
    gtk_window_set_icon_name(window, "wraith-master")

    val box = gtk_box_new(GtkOrientation.GTK_ORIENTATION_VERTICAL, 0)!!
    gtk_container_add(window.reinterpret(), box)

    val mainNotebook = gtk_notebook_new()!!
    gtk_container_add(box.reinterpret(), mainNotebook)

    val logoGrid = mainNotebook.newSettingsPage("Logo").newSettingsGrid()
    val fanGrid = mainNotebook.newSettingsPage("Fan").newSettingsGrid()
    val ringGrid = mainNotebook.newSettingsPage("Ring").newSettingsGrid()

    var position: Int by Delegates.notNull()
    fun CPointer<GtkWidget>?.gridLabel(text: String) = gtk_label_new(text)?.apply {
        gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_START)
        gtk_widget_set_hexpand(this, 1)
        gtk_grid_attach(this@gridLabel?.reinterpret(), this, 0, position++, 1, 1)
    }

    listOf(logoGrid, fanGrid, ringGrid).forEach {
        position = 0
        it.gridLabel("Mode")
        it.gridLabel("Color")
        it.gridLabel("Brightness")
        it.gridLabel("Speed")
    }
    ringGrid.gridLabel("Direction")

    position = 0
    memScoped {
        logoGrid.gridComboBox(0, wraith!!.logo.mode, LedMode.values(), staticCFunction<CPointer<GtkWidget>, Unit> {
            val text = gtk_combo_box_text_get_active_text(it.reinterpret())!!.toKString()
            wraith!!.update(wraith!!.logo) {
                mode = LedMode.valueOf(text.toUpperCase())
            }
        })
        logoGrid.gridColorButton(1, wraith!!.logo.color, staticCFunction<CPointer<GtkWidget>, Unit> {
            wraith!!.update(wraith!!.logo) {
                color = memScoped {
                    alloc<GdkRGBA>().apply { gtk_color_button_get_rgba(it.reinterpret(), ptr) }.toColor()
                }
            }
        })
        logoGrid.gridScale(2, wraith!!.logo.brightness, 3, staticCFunction<CPointer<GtkWidget>, Unit> {
            wraith!!.update(wraith!!.logo) {
                brightness = gtk_adjustment_get_value(it.reinterpret()).roundToInt().toUByte()
            }
        })
        logoGrid.gridScale(3, wraith!!.logo.speed, 5, staticCFunction<CPointer<GtkWidget>, Unit> {
            wraith!!.update(wraith!!.logo) {
                speed = gtk_adjustment_get_value(it.reinterpret()).roundToInt().toUByte()
            }
        })
        fanGrid.gridComboBox(0, wraith!!.fan.mode, LedMode.values(), staticCFunction<CPointer<GtkWidget>, Unit> {
            val text = gtk_combo_box_text_get_active_text(it.reinterpret())!!.toKString()
            wraith!!.update(wraith!!.fan) {
                mode = LedMode.valueOf(text.toUpperCase())
            }
        })
        fanGrid.gridColorButton(1, wraith!!.fan.color, staticCFunction<CPointer<GtkWidget>, Unit> {
            wraith!!.update(wraith!!.fan) {
                color = memScoped {
                    alloc<GdkRGBA>().apply { gtk_color_button_get_rgba(it.reinterpret(), ptr) }.toColor()
                }
            }
        })
        fanGrid.gridScale(2, wraith!!.fan.brightness, 3, staticCFunction<CPointer<GtkWidget>, Unit> {
            wraith!!.update(wraith!!.fan) {
                brightness = gtk_adjustment_get_value(it.reinterpret()).roundToInt().toUByte()
            }
        })
        fanGrid.gridScale(3, wraith!!.fan.speed, 5, staticCFunction<CPointer<GtkWidget>, Unit> {
            wraith!!.update(wraith!!.fan) {
                speed = gtk_adjustment_get_value(it.reinterpret()).roundToInt().toUByte()
            }
        })
        ringGrid.gridComboBox(0, wraith!!.ring.mode, RingMode.values(), staticCFunction<CPointer<GtkWidget>, Unit> {
            val text = gtk_combo_box_text_get_active_text(it.reinterpret())!!.toKString()
            wraith!!.update(wraith!!.ring) {
                mode = RingMode.valueOf(text.toUpperCase())
            }
        })
        ringGrid.gridColorButton(1, wraith!!.ring.color, staticCFunction<CPointer<GtkWidget>, Unit> {
            wraith!!.update(wraith!!.ring) {
                color = memScoped {
                    alloc<GdkRGBA>().apply { gtk_color_button_get_rgba(it.reinterpret(), ptr) }.toColor()
                }
            }
        })
        ringGrid.gridScale(2, wraith!!.ring.brightness, 3, staticCFunction<CPointer<GtkWidget>, Unit> {
            wraith!!.update(wraith!!.ring) {
                brightness = gtk_adjustment_get_value(it.reinterpret()).roundToInt().toUByte()
            }
        })
        ringGrid.gridScale(3, wraith!!.ring.speed, 5, staticCFunction<CPointer<GtkWidget>, Unit> {
            wraith!!.update(wraith!!.ring) {
                speed = gtk_adjustment_get_value(it.reinterpret()).roundToInt().toUByte()
            }
        })
        ringGrid.gridComboBox(
            4, wraith!!.ring.direction, RotationDirection.values(), staticCFunction<CPointer<GtkWidget>, Unit> {
                val text = gtk_combo_box_text_get_active_text(it.reinterpret())!!.toKString()
                wraith!!.update(wraith!!.ring) {
                    direction = RotationDirection.valueOf(text.toUpperCase())
                }
            })
    }

    val saveOptionBox = gtk_button_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL)?.apply {
        gtk_container_add(box.reinterpret(), this)
        gtk_container_set_border_width(reinterpret(), 12)
        gtk_button_box_set_layout(reinterpret(), GTK_BUTTONBOX_END)
        gtk_box_set_spacing(reinterpret(), 8)
        gtk_box_set_child_packing(box.reinterpret(), this, 0, 1, 0, GtkPackType.GTK_PACK_END)
    }

    gtk_button_new()?.apply {
        gtk_button_set_label(reinterpret(), "Reset")
        gSignalConnect(this, "clicked", staticCFunction<CPointer<GtkWidget>, Unit> { wraith!!.reset() })
        gtk_container_add(saveOptionBox?.reinterpret(), this)
    }

    gtk_button_new()?.apply {
        gtk_button_set_label(reinterpret(), "Save")
        gtk_style_context_add_class(gtk_widget_get_style_context(this), "suggested-action")
        gSignalConnect(this, "clicked", staticCFunction<CPointer<GtkWidget>, Unit> { wraith!!.save(); Unit })
        gtk_container_add(saveOptionBox?.reinterpret(), this)
    }

    gtk_widget_show_all(windowWidget)
}

@UseExperimental(ExperimentalUnsignedTypes::class)
fun main(args: Array<String>) {
    val app = gtk_application_new("com.serebit.wraith", G_APPLICATION_FLAGS_NONE)!!
    val status: Int

    if (wraith != null) {
        gSignalConnect(app, "activate", staticCFunction { it: CPointer<GtkApplication>, _: gpointer ->
            it.activate()
        })
    } else {
        gSignalConnect(app, "activate", staticCFunction { _: CPointer<GtkApplication>, _: gpointer ->
            val message = "Failed to find Wraith Prism.\nMake sure the internal USB 2.0 cable is connected."
            val dialog = gtk_message_dialog_new(
                null, 0u, GtkMessageType.GTK_MESSAGE_ERROR, GtkButtonsType.GTK_BUTTONS_OK, "%s", message
            )

            gtk_dialog_run(dialog?.reinterpret())
        })
    }

    status = memScoped { g_application_run(app.reinterpret(), args.size, args.map { it.cstr.ptr }.toCValues()) }
    wraith?.close()

    g_object_unref(app)
    if (status != 0) exitProcess(status)
}
