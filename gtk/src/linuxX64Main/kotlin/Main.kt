package com.serebit.wraith.gtk

import com.serebit.wraith.core.DeviceResult
import com.serebit.wraith.core.TransferError
import com.serebit.wraith.core.obtainWraithPrism
import com.serebit.wraith.core.prism.WraithPrism
import com.serebit.wraith.core.prism.hasUnsavedChanges
import com.serebit.wraith.core.prism.resetToDefault
import gtk3.*
import kotlinx.cinterop.*
import libusb.libusb_exit
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val app = gtk_application_new("com.serebit.wraith", G_APPLICATION_FLAGS_NONE)!!
    val status: Int

    val result = obtainWraithPrism()

    when (result) {
        is DeviceResult.Success -> {
            val prismPtr = StableRef.create(result.prism).asCPointer()

            app.connectToSignal("activate", prismPtr,
                staticCFunction<CPointer<GtkApplication>, COpaquePointer, Unit> { it, ptr ->
                    it.createWindowOrNull { activate(ptr.asStableRef<WraithPrism>().get()) }
                        ?: runNoExtraWindowsDialog()
                })

            app.connectToSignal("shutdown", prismPtr,
                staticCFunction<CPointer<GApplication>, COpaquePointer, Unit> { _, ptr ->
                    ptr.asStableRef<WraithPrism>().dispose()
                })
        }

        is DeviceResult.Failure -> app.connectToSignal(
            "activate", StableRef.create(result.message).asCPointer(),
            staticCFunction<CPointer<GtkApplication>, COpaquePointer, Unit> { _, ptr ->
                val dialog = gtk_message_dialog_new(
                    null, 0u, GtkMessageType.GTK_MESSAGE_ERROR, GtkButtonsType.GTK_BUTTONS_OK,
                    "%s", ptr.asStableRef<String>().get()
                )!!

                gtk_dialog_run(dialog.reinterpret())

                gtk_widget_destroy(dialog)
                ptr.asStableRef<String>().dispose()
            })
    }

    status = try {
        memScoped { g_application_run(app.reinterpret(), args.size, args.map { it.cstr.ptr }.toCValues()) }
    } catch (err: TransferError) {
        err.printStackTrace()
        1
    }

    g_object_unref(app)

    if (result is DeviceResult.Success) result.prism.close()
    libusb_exit(null)

    if (status != 0) exitProcess(status)
}

fun CPointer<GtkApplication>.createWindowOrNull(addWidgets: Widget.() -> Unit): Widget? =
    if (gtk_application_get_active_window(this) == null) gtk_application_window_new(this)!!.apply {
        clearFocusOnClickOrEsc()

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
        val widgets: List<PrismComponentWidgets>,
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

    gtk_window_get_titlebar(reinterpret())!!.apply {
        val firmwareVersion = wraith.requestFirmwareVersion()
        gtk_header_bar_set_subtitle(reinterpret(), "Wraith Prism, firmware $firmwareVersion")

        val resetToDefaultButton = gtk_menu_item_new_with_label("Reset to Default")!!.apply {
            connectToSignal("activate", callbackPtr, staticCFunction<Widget, COpaquePointer, Unit> { _, ptr ->
                val (device, widgets, _) = ptr.asStableRef<CallbackData>().get()
                device.resetToDefault()

                widgets.filterIsInstance<FanWidgets>().forEach { it.setMirageEnabled(330, 330, 330) }
                widgets.forEach { it.reload() }
            })
            gtk_widget_show(this)
        }

        val toggleEnsoModeButton = gtk_menu_item_new_with_label("Toggle Enso Mode")!!.apply {
            connectToSignal("activate", callbackPtr, staticCFunction<Widget, COpaquePointer, Unit> { _, ptr ->
                val (device, widgets, _) = ptr.asStableRef<CallbackData>().get()
                device.apply {
                    enso = !enso
                    if (!enso) {
                        device.resetToDefault()
                        save()
                    }
                }
                widgets.forEach { it.reload() }
            })
            gtk_widget_show(this)
        }

        val dropdownMenuButton = gtk_menu_button_new()!!.apply {
            gtk_menu_button_set_popup(reinterpret(), gtk_menu_new()!!.apply {
                gtk_menu_attach(reinterpret(), resetToDefaultButton, 0, 1, 0, 1)
                gtk_menu_attach(reinterpret(), toggleEnsoModeButton, 0, 1, 1, 2)
            })
        }

        gtk_header_bar_pack_start(reinterpret(), dropdownMenuButton)
    }

    resetButton.connectToSignal("clicked", callbackPtr, staticCFunction<Widget, COpaquePointer, Unit> { _, ptr ->
        val (device, widgets, buttons) = ptr.asStableRef<CallbackData>().get()
        device.restore()
        device.apply()
        device.components.forEach { it.reloadValues() }
        widgets.forEach { it.reload() }
        buttons.forEach { it.setSensitive(false) }
    })

    saveButton.connectToSignal("clicked", callbackPtr, staticCFunction<Widget, COpaquePointer, Unit> { _, ptr ->
        val (device, _, buttons) = ptr.asStableRef<CallbackData>().get()
        device.save()
        buttons.forEach { it.setSensitive(false) }
    })

    wraith.onApply = {
        saveOptionButtons.forEach { it.setSensitive(wraith.hasUnsavedChanges) }
    }

    connectToSignal("delete-event", callbackPtr,
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
                    GTK_RESPONSE_CANCEL -> returnValue = true
                }

                gtk_widget_destroy(dialog)
            }

            if (!returnValue) {
                widgets.forEach { it.close() }
                ptr.asStableRef<CallbackData>().dispose()
            }

            returnValue
        })
}
