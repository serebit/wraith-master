package com.serebit.wraith.gtk

import com.serebit.wraith.core.*
import gtk3.*
import kotlinx.cinterop.*
import kotlin.properties.Delegates
import kotlin.system.exitProcess

private val wraithOrNull by lazy { obtainWraithPrism() }
val wraith get() = wraithOrNull!!

@UseExperimental(ExperimentalUnsignedTypes::class)
fun CPointer<GtkApplication>.activate() {
    val windowWidget = gtk_application_window_new(this)!!

    val window = windowWidget.reinterpret<GtkWindow>()
    gtk_window_set_title(window, "Wraith Master")
    gtk_window_set_default_size(window, 480, -1)
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

    gridComboBox(wraith.logo.mode, LedMode.values(), true, staticCFunction<CPointer<GtkWidget>, Unit> {
        wraith.updateMode(wraith.logo, it)
        logoColorButton.setSensitive(wraith.logo.mode.supportsColor)
        logoBrightnessScale.setSensitive(wraith.logo.mode.supportsBrightness)
        logoSpeedScale.setSensitive(wraith.logo.mode.supportsSpeed)
    }).also { logoGrid.gridAttachRight(it, 0) }
    logoGrid.gridAttachRight(logoColorButton, 1)
    logoGrid.gridAttachRight(logoBrightnessScale, 2)
    logoGrid.gridAttachRight(logoSpeedScale, 3)

    gridComboBox(wraith.fan.mode, LedMode.values(), true, staticCFunction<CPointer<GtkWidget>, Unit> {
        wraith.updateMode(wraith.logo, it)
        fanColorButton.setSensitive(wraith.fan.mode.supportsColor)
        fanBrightnessScale.setSensitive(wraith.fan.mode.supportsBrightness)
        fanSpeedScale.setSensitive(wraith.fan.mode.supportsSpeed)
    }).also { fanGrid.gridAttachRight(it, 0) }
    fanGrid.gridAttachRight(fanColorButton, 1)
    fanGrid.gridAttachRight(fanBrightnessScale, 2)
    fanGrid.gridAttachRight(fanSpeedScale, 3)

    gridComboBox(wraith.ring.mode, RingMode.values(), true, staticCFunction<CPointer<GtkWidget>, Unit> {
        val text = gtk_combo_box_text_get_active_text(it.reinterpret())!!.toKString()
        wraith.update(wraith.ring) {
            mode = RingMode.valueOf(text.toUpperCase())
        }
        ringColorButton.setSensitive(wraith.ring.mode.supportsColor)
        ringBrightnessScale.setSensitive(wraith.ring.mode.supportsBrightness)
        ringSpeedScale.setSensitive(wraith.ring.mode.supportsSpeed)
        ringDirectionComboBox.setSensitive(wraith.ring.mode.supportsDirection)
    }).also { ringGrid.gridAttachRight(it, 0) }
    ringGrid.gridAttachRight(ringColorButton, 1)
    ringGrid.gridAttachRight(ringBrightnessScale, 2)
    ringGrid.gridAttachRight(ringSpeedScale, 3)
    ringGrid.gridAttachRight(ringDirectionComboBox, 4)

    val saveOptionBox = gtk_button_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL)?.apply {
        gtk_container_add(box.reinterpret(), this)
        gtk_container_set_border_width(reinterpret(), 12u)
        gtk_button_box_set_layout(reinterpret(), GTK_BUTTONBOX_END)
        gtk_box_set_spacing(reinterpret(), 8)
        gtk_box_set_child_packing(box.reinterpret(), this, 0, 1, 0u, GtkPackType.GTK_PACK_END)
    }

    gtk_button_new()?.apply {
        gtk_button_set_label(reinterpret(), "Reset")
        gSignalConnect(this, "clicked", staticCFunction<CPointer<GtkWidget>, Unit> { wraith.reset() })
        gtk_container_add(saveOptionBox?.reinterpret(), this)
    }

    gtk_button_new()?.apply {
        gtk_button_set_label(reinterpret(), "Save")
        gtk_style_context_add_class(gtk_widget_get_style_context(this), "suggested-action")
        gSignalConnect(this, "clicked", staticCFunction<CPointer<GtkWidget>, Unit> { wraith.save(); Unit })
        gtk_container_add(saveOptionBox?.reinterpret(), this)
    }

    gtk_widget_show_all(windowWidget)
}

@UseExperimental(ExperimentalUnsignedTypes::class)
fun main(args: Array<String>) {
    val app = gtk_application_new("com.serebit.wraith", G_APPLICATION_FLAGS_NONE)!!
    val status: Int

    if (wraithOrNull != null) {
        gSignalConnect(app, "activate", staticCFunction { it: CPointer<GtkApplication> -> it.activate() })
    } else {
        gSignalConnect(app, "activate", staticCFunction { _: CPointer<GtkApplication> ->
            val message = "Failed to find Wraith Prism.\nMake sure the internal USB 2.0 cable is connected."
            val dialog = gtk_message_dialog_new(
                null, 0u, GtkMessageType.GTK_MESSAGE_ERROR, GtkButtonsType.GTK_BUTTONS_OK, "%s", message
            )

            gtk_dialog_run(dialog?.reinterpret())
        })
    }

    status = memScoped { g_application_run(app.reinterpret(), args.size, args.map { it.cstr.ptr }.toCValues()) }
    wraithOrNull?.close()

    g_object_unref(app)
    if (status != 0) exitProcess(status)
}
