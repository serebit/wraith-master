package com.serebit.wraith.gtk

import com.serebit.wraith.core.programVersion
import gtk3.*
import kotlinx.cinterop.reinterpret

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
        clearFocusOnClickOrEsc()
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
