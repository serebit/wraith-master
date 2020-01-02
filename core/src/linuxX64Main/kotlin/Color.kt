@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.serebit.wraith.core

private fun Double.toUByte() = toInt().toUByte()

class Color(val r: UByte, val g: UByte, val b: UByte)

@Suppress("FunctionName")
fun Color(r: Double, g: Double, b: Double) = Color((255 * r).toUByte(), (255 * g).toUByte(), (255 * b).toUByte())
