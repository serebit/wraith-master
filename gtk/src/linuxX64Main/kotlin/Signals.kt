package com.serebit.wraith.gtk

import gtk3.GObject
import gtk3.g_signal_connect_data
import gtk3.gpointer
import kotlinx.cinterop.*

private fun CPointer<*>.connectToSignal(
    signal: String,
    staticCallback: CPointer<CFunction<*>>,
    callbackRef: StableRef<*>
) {
    g_signal_connect_data(
        reinterpret(),
        signal,
        staticCallback.reinterpret(),
        callbackRef.asCPointer(),
        destroy_data = staticCFunction { void: gpointer?, _ ->
            void?.asStableRef<Any>()?.dispose()
        },
        0u
    )
}

fun CPointer<*>.connectToSignal(signal: String, callback: () -> Unit) =
    connectToSignal(signal, staticNoArgGCallback.reinterpret(), StableRef.create(callback))

fun <P1 : CPointed> CPointer<*>.connectToSignal(signal: String, callback: (P1) -> Boolean) =
    connectToSignal(signal, staticSingleArgGCallback.reinterpret(), StableRef.create(callback))

fun <P1 : CPointed, P2 : CPointed> CPointer<*>.connectToSignal(signal: String, callback: (P1, P2) -> Unit) =
    connectToSignal(signal, staticDualArgGCallback.reinterpret(), StableRef.create(callback))

val staticNoArgGCallback = staticCFunction { _: CPointer<GObject>?, data: gpointer ->
    data.asStableRef<() -> Unit>().get().invoke()
}

val staticSingleArgGCallback = staticCFunction { _: CPointer<GObject>?, arg1: CPointed, data: gpointer ->
    data.asStableRef<(Any) -> Boolean>().get().invoke(arg1)
}

val staticDualArgGCallback = staticCFunction { _: CPointer<GObject>?, arg1: CPointed, arg2: CPointed, data: gpointer ->
    data.asStableRef<(Any, Any) -> Unit>().get().invoke(arg1, arg2)
}
