package com.serebit.wraith.gtk

import com.serebit.wraith.core.*
import gtk3.*
import kotlinx.cinterop.*
import kotlin.system.exitProcess

@OptIn(ExperimentalUnsignedTypes::class)
fun CPointer<GtkApplication>.activate(wraithPtr: COpaquePointer) {
    val wraith = wraithPtr.asStableRef<WraithPrism>().get()
    val activeWindow = gtk_application_get_active_window(this)
    if (activeWindow == null) {
        val window = gtk_application_window_new(this)!!

        val logoWidgets = LogoWidgets(wraith).apply { fullReload() }
        val fanWidgets = FanWidgets(wraith).apply { fullReload() }
        val ringWidgets = RingWidgets(wraith).apply { fullReload() }

        // unset focus on left click with mouse button
        window.connectSignal(
            "button-press-event",
            staticCFunction<Widget, CPointer<GdkEventButton>, Unit> { it, event ->
                if (event.pointed.type == GDK_BUTTON_PRESS && event.pointed.button == 1u) {
                    gtk_window_set_focus(it.reinterpret(), null)
                    gtk_window_set_focus_visible(it.reinterpret(), 0)
                }
            })

        gtk_window_set_title(window.reinterpret(), "Wraith Master")
        gtk_window_set_default_icon_name("applications-games")
        gtk_window_set_icon_name(window.reinterpret(), "wraith-master")

        val box = gtk_box_new(GtkOrientation.GTK_ORIENTATION_VERTICAL, 0)!!.also { window.addChild(it) }
        val mainNotebook = gtk_notebook_new()!!.also { box.addChild(it) }

        val logoGrid = mainNotebook.newSettingsPage("Logo").newSettingsGrid()
        val fanGrid = mainNotebook.newSettingsPage("Fan").newSettingsGrid()
        val ringGrid = mainNotebook.newSettingsPage("Ring").newSettingsGrid()

        listOf(logoGrid, fanGrid, ringGrid).forEach { it.newGridLabels("Mode", "Color", "Brightness", "Speed") }
        fanGrid.newGridLabel(4, "Mirage")
        ringGrid.newGridLabel(4, "Rotation Direction")
        ringGrid.newGridLabel(5, "Morse Text")

        logoWidgets.attachToGrid(logoGrid)
        fanWidgets.attachToGrid(fanGrid)
        ringWidgets.attachToGrid(ringGrid)

        val saveOptionBox = gtk_button_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL)?.apply {
            gtk_container_add(box.reinterpret(), this)
            gtk_container_set_border_width(reinterpret(), 12u)
            gtk_button_box_set_layout(reinterpret(), GTK_BUTTONBOX_END)
            gtk_box_set_spacing(reinterpret(), 8)
            gtk_box_set_child_packing(box.reinterpret(), this, 0, 1, 0u, GtkPackType.GTK_PACK_END)
        }

        gtk_button_new()?.apply {
            gtk_button_set_label(reinterpret(), "Reset")
            val data = StableRef.create(wraith to listOf(logoWidgets, fanWidgets, ringWidgets)).asCPointer()
            connectSignalWithData("clicked", data, staticCFunction<Widget, COpaquePointer, Unit> { _, ptr ->
                val ref = ptr.asStableRef<Pair<WraithPrism, List<ComponentWidgets<*>>>>()
                val (device, widgets) = ref.get()
                val (logo, fan, ring) = widgets
                device.reset()
                logo.fullReload(); fan.fullReload(); ring.fullReload()
                gtk_combo_box_set_active(logo.modeBox.reinterpret(), logo.component.mode.index)
                gtk_combo_box_set_active(logo.modeBox.reinterpret(), fan.component.mode.index)
                ref.dispose()
            })
            gtk_container_add(saveOptionBox?.reinterpret(), this)
        }

        gtk_button_new()?.apply {
            gtk_button_set_label(reinterpret(), "Save")
            gtk_style_context_add_class(gtk_widget_get_style_context(this), "suggested-action")
            connectSignalWithData("clicked", wraithPtr, staticCFunction<Widget, COpaquePointer, Unit> { _, ptr ->
                ptr.asStableRef<WraithPrism>().get().save()
            })
            gtk_container_add(saveOptionBox?.reinterpret(), this)
        }

        gtk_widget_show_all(window)
    } else {
        gtk_message_dialog_new(
            activeWindow, 0u, GtkMessageType.GTK_MESSAGE_INFO, GtkButtonsType.GTK_BUTTONS_OK, "%s",
            "Cannot open extra Wraith Master windows."
        )?.let {
            gtk_dialog_run(it.reinterpret())
            gtk_widget_destroy(it)
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun main(args: Array<String>) {
    val app = gtk_application_new("com.serebit.wraith", G_APPLICATION_FLAGS_NONE)!!
    val status: Int
    val result = obtainWraithPrism()

    when (result) {
        is WraithPrismResult.Success -> app.connectSignalWithData(
            "activate", StableRef.create(result.device).asCPointer(),
            staticCFunction<CPointer<GtkApplication>, COpaquePointer, Unit> { it, ptr -> it.activate(ptr) }
        )

        is WraithPrismResult.Failure -> app.connectSignalWithData(
            "activate", StableRef.create(result.message).asCPointer(),
            staticCFunction<CPointer<GtkApplication>, COpaquePointer, Unit> { _, ptr ->
                val dialog = gtk_message_dialog_new(
                    null, 0u, GtkMessageType.GTK_MESSAGE_ERROR, GtkButtonsType.GTK_BUTTONS_OK,
                    "%s", ptr.asStableRef<String>().get()
                )

                gtk_dialog_run(dialog?.reinterpret())
            })
    }

    status = memScoped { g_application_run(app.reinterpret(), args.size, args.map { it.cstr.ptr }.toCValues()) }
    if (result is WraithPrismResult.Success) result.device.close()

    g_object_unref(app)
    if (status != 0) exitProcess(status)
}
