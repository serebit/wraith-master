@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package com.serebit.wraith.cli

import com.serebit.wraith.core.Color
import com.serebit.wraith.core.device
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.failAssertion

object ColorArgType : ArgType<Color>(true) {
    override val conversion: (value: kotlin.String, name: kotlin.String) -> Color =
        fun(value: kotlin.String, name: kotlin.String): Color {
            "(\\d{1,3}),(\\d{1,3}),(\\d{1,3})".toRegex()
                .matchEntire(value)
                ?.groupValues
                ?.let { channels ->
                    val (r, g, b) = channels.drop(1).map { it.toUByteOrNull() ?: failAssertion("Failed assertion") }
                    return Color(r.toUByte(), g.toUByte(), b.toUByte())
                }
            "#?[\\da-fA-F]{6}".toRegex()
                .matchEntire(value)
                ?.value
                ?.removePrefix("#")
                ?.toIntOrNull(16)?.let {
                    val r = it shr 16 and 0xFF
                    val g = it shr 8 and 0xFF
                    val b = it and 0xFF
                    return Color(r.toUByte(), g.toUByte(), b.toUByte())
                }
            failAssertion("""
                |Option $name is expected to be a color, either represented by channel values separated by commas (such 
                |as (255, 128, 0) or a hex color (such as 03A9F4).
            """.trimMargin())
        }
    override val description = "{ Color with format r,g,b or RRGGBB }"
}

fun main(args: Array<String>) {
    val parser = ArgParser("wraith-master")
    val component by parser.argument(ArgType.Choice(listOf("logo", "fan", "ring")))
    val color by parser.option(ColorArgType, shortName = "c", description = "The hex color to apply to the component")
    val brightness by parser.option(
        ArgType.Int,
        shortName = "b",
        description = "The component's brightness from 1 to 5"
    )
    parser.parse(args)
    brightness?.let {
        if (it !in 1..5) parser.printError("Brightness must be within the range of 1 to 5")
        when (component) {
            "logo" -> device.logo.brightness = (it * 51).toUByte()
            "fan" -> device.fan.brightness = (it * 51).toUByte()
            "ring" -> device.ring.brightness = (it * 51).toUByte()
        }
    }
    color?.let {
        when (component) {
            "logo" -> device.logo.color = it
            "fan" -> device.fan.color = it
            "ring" -> device.ring.color = it
        }
    }

    device.close()
}
