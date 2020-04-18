package com.serebit.wraith.gtk

import com.serebit.wraith.core.DeviceResult
import com.serebit.wraith.core.initLibusb
import com.serebit.wraith.core.obtainWraithPrism
import com.serebit.wraith.core.prism.*
import com.serebit.wraith.core.programVersion
import gtk3.*
import kotlinx.cinterop.*
import libusb.libusb_exit
import kotlin.system.exitProcess

@OptIn(ExperimentalUnsignedTypes::class)
fun main(args: Array<String>) {
    val app = gtk_application_new("com.serebit.wraith", G_APPLICATION_FLAGS_NONE)!!
    val status: Int

    initLibusb()
    val result = obtainWraithPrism()

    when (result) {
        is DeviceResult.Success -> app.connectSignalWithData("activate", StableRef.create(result.prism).asCPointer(),
            staticCFunction<CPointer<GtkApplication>, COpaquePointer, Unit> { it, ptr ->
                it.createWindowOrNull { activate(ptr.asStableRef<WraithPrism>().getAndDispose()) }
                    ?: runNoExtraWindowsDialog()
            })

        is DeviceResult.Failure -> app.connectSignalWithData(
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
    g_object_unref(app)

    if (result is DeviceResult.Success) result.prism.close()
    libusb_exit(null)

    if (status != 0) exitProcess(status)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun CPointer<GtkApplication>.createWindowOrNull(addWidgets: Widget.() -> Unit): Widget? =
    if (gtk_application_get_active_window(this) == null) gtk_application_window_new(this)!!.apply {
        // unset focus on left click with mouse button
        connectSignalWithData("button-press-event", null,
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
            val aboutButton = iconButton("dialog-information", null, null, staticCFunction { _, _ ->
                runAboutDialog()
            })
            gtk_header_bar_pack_start(reinterpret(), aboutButton)
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
        gtk_about_dialog_set_version(reinterpret(), programVersion?.let { "Version $it" } ?: "Unknown Version")
        gtk_about_dialog_set_website(reinterpret(), "https://gitlab.com/serebit/wraith-master")
        gtk_about_dialog_set_website_label(reinterpret(), "Visit on GitLab")
        gtk_about_dialog_set_copyright(
            reinterpret(),
            "Copyright © 2020 Campbell Jones\nLicensed under the Apache License 2.0"
        )
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
fun Widget.activate(wraith: WraithPrism) {
    val box = gtk_box_new(GtkOrientation.GTK_ORIENTATION_VERTICAL, 0)!!.also { gtk_container_add(reinterpret(), it) }
    val mainNotebook = gtk_notebook_new()!!.apply {
        gtk_container_add(box.reinterpret(), this)
        addCss("notebook header.top tabs tab { padding-left: 20px; padding-right: 20px; margin-right: 4px; }")
    }

    val ringGrid = mainNotebook.newSettingsPage("Ring").newSettingsGrid()
    val fanGrid = mainNotebook.newSettingsPage("Fan").newSettingsGrid()
    val logoGrid = mainNotebook.newSettingsPage("Logo").newSettingsGrid()

    val ringWidgets = RingWidgets(wraith).apply { initialize(ringGrid) }
    val fanWidgets = FanWidgets(wraith).apply { initialize(fanGrid) }
    val logoWidgets = LogoWidgets(wraith).apply { initialize(logoGrid) }

    val saveOptionBox = gtk_button_box_new(GtkOrientation.GTK_ORIENTATION_HORIZONTAL)!!.apply {
        gtk_container_add(box.reinterpret(), this)
        gtk_container_set_border_width(reinterpret(), 10u)
        gtk_button_box_set_layout(reinterpret(), GTK_BUTTONBOX_END)
        gtk_box_set_spacing(reinterpret(), 8)
        gtk_box_set_child_packing(box.reinterpret(), this, 0, 1, 0u, GtkPackType.GTK_PACK_END)
    }

    data class CallbackData(
        val wraith: WraithPrism,
        val widgets: List<PrismComponentWidgets<*>>,
        val buttons: List<Widget>
    )

    val resetButton = gtk_button_new()!!.apply {
        gtk_button_set_label(reinterpret(), "Reset")
        setSensitive(false)
        gtk_container_add(saveOptionBox.reinterpret(), this)
    }

    val saveButton = gtk_button_new()!!.apply {
        gtk_button_set_label(reinterpret(), "Save")
        gtk_style_context_add_class(gtk_widget_get_style_context(this), "suggested-action")
        setSensitive(false)
        gtk_container_add(saveOptionBox.reinterpret(), this)
    }

    val saveOptionButtons = listOf(resetButton, saveButton)
    val callbackData = CallbackData(wraith, listOf(ringWidgets, fanWidgets, logoWidgets), saveOptionButtons)
    val callbackPtr = StableRef.create(callbackData).asCPointer()

    resetButton.connectSignalWithData("clicked", callbackPtr, staticCFunction<Widget, COpaquePointer, Unit> { _, ptr ->
        val (device, widgets, buttons) = ptr.asStableRef<CallbackData>().get()
        device.restore()
        device.apply()
        val channels = device.getChannels()
        device.components.forEachIndexed { i, it ->
            it.assignValuesFromChannel(device.getChannelValues(channels[i + 8]))
        }
        widgets.forEach { it.reload() }
        buttons.forEach { it.setSensitive(false) }
    })

    saveButton.connectSignalWithData("clicked", callbackPtr, staticCFunction<Widget, COpaquePointer, Unit> { _, ptr ->
        val (device, _, buttons) = ptr.asStableRef<CallbackData>().get()
        device.save()
        buttons.forEach { it.setSensitive(false) }
    })

    wraith.onApply = {
        saveOptionButtons.forEach { it.setSensitive(wraith.hasUnsavedChanges) }
    }

    connectSignalWithData("delete-event", callbackPtr,
        staticCFunction<Widget, CPointer<GdkEvent>, COpaquePointer, Boolean> { window, _, ptr ->
            var returnValue = false
            val (prism, widgets, _) = ptr.asStableRef<CallbackData>().get()
            if (prism.hasUnsavedChanges) {
                val dialog = gtk_message_dialog_new(
                    window.reinterpret(), 0u, GtkMessageType.GTK_MESSAGE_QUESTION, GtkButtonsType.GTK_BUTTONS_NONE,
                    "%s", "You have unsaved changes. Would you like to save them?"
                )!!
                gtk_dialog_add_buttons(
                    dialog.reinterpret(),
                    "Yes", GTK_RESPONSE_YES,
                    "No", GTK_RESPONSE_NO,
                    "Cancel", GTK_RESPONSE_CANCEL,
                    null
                )

                when (gtk_dialog_run(dialog.reinterpret())) {
                    GTK_RESPONSE_YES -> prism.save()
                    GTK_RESPONSE_NO -> {
                        prism.restore()
                        prism.apply(runCallback = false)
                    }
                    GTK_RESPONSE_CANCEL -> {
                        gtk_widget_destroy(dialog)
                        returnValue = true
                    }
                }

                gtk_widget_destroy(dialog)
                widgets.forEach { it.close() }
            }
            ptr.asStableRef<CallbackData>().dispose()
            returnValue
        })
}

private fun <T : Any> StableRef<T>.getAndDispose(): T = get().also { dispose() }
