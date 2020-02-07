package com.serebit.wraith.gtk

import com.serebit.wraith.core.*
import gtk3.*
import kotlinx.cinterop.*
import kotlin.system.exitProcess

val result = obtainWraithPrism()
val wraith: WraithPrism get() = (result as? WraithPrismResult.Success)!!.device

inline val logo get() = wraith.logo
inline val fan get() = wraith.fan
inline val ring get() = wraith.ring

@UseExperimental(ExperimentalUnsignedTypes::class)
fun CPointer<GtkApplication>.activate() {
    if (gtk_application_get_active_window(this) == null) {
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

        fun CPointer<GtkWidget>?.gridLabel(position: Int, text: String) = gtk_label_new(text)?.apply {
            gtk_widget_set_halign(this, GtkAlign.GTK_ALIGN_START)
            gtk_widget_set_hexpand(this, 1)
            gtk_grid_attach(this@gridLabel?.reinterpret(), this, 0, position, 1, 1)
        }

        listOf(logoGrid, fanGrid, ringGrid).forEach {
            it.gridLabel(0, "Mode")
            it.gridLabel(1, "Color")
            it.gridLabel(2, "Brightness")
            it.gridLabel(3, "Speed")
        }
        fanGrid.gridLabel(4, "Mirage")
        ringGrid.gridLabel(4, "Direction")

        gridComboBox(logo.mode, LedMode.values(), true, staticCFunction<CPointer<GtkWidget>, Unit> {
            wraith.updateMode(logo, it)
            val color = if (logo.mode.supportsColor) logo.color else Color(0, 0, 0)
            memScoped { gtk_color_button_set_rgba(logoColorButton.reinterpret(), gdkRgba(color).ptr) }
            logoColorButton.setSensitive(logo.mode.supportsColor)
            logoBrightnessScale.setSensitive(logo.mode.supportsBrightness)
            logoSpeedScale.setSensitive(logo.mode.supportsSpeed)
        }).also { logoGrid.gridAttachRight(it, 0) }
        logoGrid.gridAttachRight(logoColorButton, 1)
        logoGrid.gridAttachRight(logoBrightnessScale, 2)
        logoGrid.gridAttachRight(logoSpeedScale, 3)

        gridComboBox(fan.mode, LedMode.values(), true, staticCFunction<CPointer<GtkWidget>, Unit> {
            wraith.updateMode(fan, it)
            val color = if (fan.mode.supportsColor) fan.color else Color(0, 0, 0)
            memScoped { gtk_color_button_set_rgba(fanColorButton.reinterpret(), gdkRgba(color).ptr) }
            fanColorButton.setSensitive(fan.mode.supportsColor)
            fanBrightnessScale.setSensitive(fan.mode.supportsBrightness)
            fanSpeedScale.setSensitive(fan.mode.supportsSpeed)
            fanMirageToggle.setSensitive(fan.mode != LedMode.OFF)
        }).also { fanGrid.gridAttachRight(it, 0) }
        fanGrid.gridAttachRight(fanColorButton, 1)
        fanGrid.gridAttachRight(fanBrightnessScale, 2)
        fanGrid.gridAttachRight(fanSpeedScale, 3)
        fanGrid.gridAttachRight(fanMirageToggle, 4)

        gridComboBox(ring.mode, RingMode.values(), true, staticCFunction<CPointer<GtkWidget>, Unit> {
            val text = gtk_combo_box_text_get_active_text(it.reinterpret())!!.toKString()
            val mode = RingMode.valueOf(text.toUpperCase())

            ring.assignValuesFromChannel(wraith.getChannelValues(mode.channel))
            wraith.update(ring) { this.mode = mode }
            memScoped { gtk_color_button_set_rgba(ringColorButton.reinterpret(), gdkRgba(ring.color).ptr) }
            gtk_range_set_value(ringBrightnessScale.reinterpret(), ring.brightness.toDouble())
            gtk_range_set_value(ringSpeedScale.reinterpret(), ring.speed.toDouble())
            gtk_combo_box_set_active(ringDirectionComboBox.reinterpret(), ring.direction.value.toInt())

            ringColorButton.setSensitive(ring.mode.supportsColor)
            ringBrightnessScale.setSensitive(ring.mode.supportsBrightness)
            ringSpeedScale.setSensitive(ring.mode.supportsSpeed)
            ringDirectionComboBox.setSensitive(ring.mode.supportsDirection)
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
            connectSignal("clicked", staticCFunction<CPointer<GtkWidget>, Unit> { wraith.reset() })
            gtk_container_add(saveOptionBox?.reinterpret(), this)
        }

        gtk_button_new()?.apply {
            gtk_button_set_label(reinterpret(), "Save")
            gtk_style_context_add_class(gtk_widget_get_style_context(this), "suggested-action")
            connectSignal("clicked", staticCFunction<CPointer<GtkWidget>, Unit> { wraith.save(); Unit })
            gtk_container_add(saveOptionBox?.reinterpret(), this)
        }

        gtk_widget_show_all(windowWidget)
    }
}

@UseExperimental(ExperimentalUnsignedTypes::class)
fun main(args: Array<String>) {
    val app = gtk_application_new("com.serebit.wraith", G_APPLICATION_FLAGS_NONE)!!
    val status: Int

    app.connectSignal("activate", when (result) {
        is WraithPrismResult.Success -> staticCFunction<CPointer<GtkApplication>, Unit> { it.activate() }
        is WraithPrismResult.Failure -> staticCFunction<CPointer<GtkApplication>, Unit> {
            val dialog = gtk_message_dialog_new(
                null, 0u, GtkMessageType.GTK_MESSAGE_ERROR, GtkButtonsType.GTK_BUTTONS_OK, "%s", result.message
            )

            gtk_dialog_run(dialog?.reinterpret())
        }
    })

    status = memScoped { g_application_run(app.reinterpret(), args.size, args.map { it.cstr.ptr }.toCValues()) }
    if (result is WraithPrismResult.Success) wraith.close()

    g_object_unref(app)
    if (status != 0) exitProcess(status)
}
