package com.serebit.wraith.gtk

import gtk3.g_signal_connect_data
import gtk3.gpointer
import kotlinx.cinterop.*

fun <F : CFunction<*>> CPointer<*>.connectToSignal(signal: String, data: COpaquePointer?, action: CPointer<F>) =
    g_signal_connect_data(reinterpret(), signal, action.reinterpret(), data, null, 0u)

inline fun CPointer<*>.connectToSignal(signal: String, crossinline callback: () -> Unit) = g_signal_connect_data(
    reinterpret(),
    signal,
    staticNoArgGCallback.reinterpret(),
    StableRef.create { callback() }.asCPointer(),
    null,
    0u
)

val staticNoArgGCallback = staticCFunction { _: Widget?, data: gpointer? ->
    data?.asStableRef<() -> Unit>()?.get()?.invoke()
    Unit
}
