package com.serebit.wraith.gtk

import com.serebit.wraith.core.*
import gtk3.*
import kotlinx.cinterop.*
import kotlin.system.exitProcess

@OptIn(ExperimentalUnsignedTypes::class)
fun CPointer<GtkApplication>.createWindowOrNull(addWidgets: Widget.() -> Unit): Widget? =
    if (gtk_application_get_active_window(this) == null) gtk_application_window_new(this)!!.apply {
        // unset focus on left click with mouse button
        connectSignal(
            "button-press-event",
            staticCFunction<Widget, CPointer<GdkEventButton>, Boolean> { it, event ->
                if (event.pointed.type == GDK_BUTTON_PRESS && event.pointed.button == 1u) {
                    gtk_window_set_focus(it.reinterpret(), null)
                    gtk_window_set_focus_visible(it.reinterpret(), 0)
                }
                false
            })

        val headerBar = gtk_header_bar_new()!!.apply {
            gtk_header_bar_set_show_close_button(reinterpret(), 1)
            gtk_header_bar_set_title(reinterpret(), "Wraith Master")
            gtk_header_bar_pack_start(reinterpret(), iconButton("dialog-information", null, staticCFunction { _, _ ->
                runAboutDialog()
            }))
        }

        gtk_window_set_titlebar(reinterpret(), headerBar)
        gtk_window_set_icon_name(reinterpret(), "wraith-master")
        gtk_window_set_default_icon_name("applications-games")
        addWidgets()

        gtk_window_set_position(reinterpret(), GtkWindowPosition.GTK_WIN_POS_CENTER)
        gtk_widget_show_all(this)
    } else null

fun runAboutDialog() {
    gtk_about_dialog_new()!!.apply {
        gtk_about_dialog_set_program_name(reinterpret(), "Wraith Master")
        gtk_about_dialog_set_logo_icon_name(reinterpret(), "wraith-master")
        gtk_about_dialog_set_version(reinterpret(), "Version 1.0.0")
        gtk_about_dialog_set_website(reinterpret(), "https://gitlab.com/serebit/wraith-master")
        gtk_about_dialog_set_website_label(reinterpret(), "Visit on GitLab")
        gtk_about_dialog_set_copyright(reinterpret(), "Copyright © 2020 Campbell Jones\nLicensed under the Apache License 2.0")
        gtk_dialog_run(reinterpret())
        gtk_widget_destroy(this)
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun runNoExtraWindowsDialog() {
    gtk_message_dialog_new(
        null, 0u, GtkMessageType.GTK_MESSAGE_INFO, GtkButtonsType.GTK_BUTTONS_OK, "%s",
        "Cannot open extra Wraith Master windows."
    )?.let {
        gtk_dialog_run(it.reinterpret())
        gtk_widget_destroy(it)
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun Widget.activate(prismPtr: COpaquePointer) {
    val box = gtk_box_new(GtkOrientation.GTK_ORIENTATION_VERTICAL, 0)!!.also { addChild(it) }
    val mainNotebook = gtk_notebook_new()!!.also { box.addChild(it) }

    val logoGrid = mainNotebook.newSettingsPage("Logo").newSettingsGrid()
    val fanGrid = mainNotebook.newSettingsPage("Fan").newSettingsGrid()
    val ringGrid = mainNotebook.newSettingsPage("Ring").newSettingsGrid()

    val wraith = prismPtr.asStableRef<WraithPrism>().get()
    val logoWidgets = LogoWidgets(wraith).apply { initialize(logoGrid) }
    val fanWidgets = FanWidgets(wraith).apply { initialize(fanGrid) }
    val ringWidgets = RingWidgets(wraith).apply { initialize(ringGrid) }

    val saveOptionBox = gtk_button_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL)?.apply {
        gtk_container_add(box.reinterpret(), this)
        gtk_container_set_border_width(reinterpret(), 10u)
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
            device.reset()
            widgets.forEach { it.reload() }
        })
        gtk_container_add(saveOptionBox?.reinterpret(), this)
    }

    gtk_button_new()?.apply {
        gtk_button_set_label(reinterpret(), "Save")
        gtk_style_context_add_class(gtk_widget_get_style_context(this), "suggested-action")
        connectSignalWithData("clicked", prismPtr, staticCFunction<Widget, COpaquePointer, Unit> { _, ptr ->
            ptr.asStableRef<WraithPrism>().get().save()
        })
        gtk_container_add(saveOptionBox?.reinterpret(), this)
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
            staticCFunction<CPointer<GtkApplication>, COpaquePointer, Unit> { it, ptr ->
                it.createWindowOrNull { activate(ptr) } ?: runNoExtraWindowsDialog()
            }
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
