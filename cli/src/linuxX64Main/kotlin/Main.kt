@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package com.serebit.wraith.cli

import com.serebit.wraith.core.*
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
            failAssertion(
                """
                |Option $name is expected to be a color, either represented by channel values separated by commas (such 
                |as (255, 128, 0) or a hex color (such as 03A9F4).
                """.trimMargin()
            )
        }
    override val description = "{ Color with format r,g,b or RRGGBB }"
}

enum class Components { LOGO, FAN, RING }

fun main(args: Array<String>) {
    val wraith: WraithPrism by lazy { obtainWraithPrism() ?: error("Failed to find Wraith Prism.") }

    try {
        val parser = ArgParser("wraith-master")

        val component by parser.argument(ArgType.Choice(Components.values().map { it.name.toLowerCase() }))

        val ringModes = RingMode.values().map { it.name.toLowerCase() }
        val ledModes = LedMode.values().map { it.name.toLowerCase() }
        val modes = ringModes.plus(ledModes).distinct()

        val mode by parser.option(
            ArgType.Choice(modes), shortName = "m",
            description = "(Modes ${modes - ledModes} are only supported by ring component)"
        )
        val color by parser.option(ColorArgType, shortName = "c")
        val brightness by parser.option(ArgType.Int, shortName = "b", description = "Value from 1 to 3")
        val speed by parser.option(ArgType.Int, shortName = "s", description = "Value from 1 to 5")

        parser.parse(args)

        brightness?.let { if (it !in 1..3) parser.printError("Brightness must be within the range of 1 to 3") }
        speed?.let { if (it !in 1..5) parser.printError("Speed must be within the range of 1 to 5") }

        val ledDevice = when (Components.valueOf(component.toUpperCase())) {
            Components.LOGO -> wraith.logo
            Components.FAN -> wraith.fan
            Components.RING -> wraith.ring
        }

        when (ledDevice) {
            is BasicLedDevice -> {
                if (mode !in LedMode.values().map { it.name.toLowerCase() })
                    parser.printError("Provided mode is not in valid modes for component $component.")
                mode?.let { wraith.update(ledDevice) { this.mode = LedMode.valueOf(it.toUpperCase()) } }
            }
            is Ring -> mode?.let { wraith.update(ledDevice) { this.mode = RingMode.valueOf(it.toUpperCase()) } }
        }

        color?.let { wraith.update(ledDevice) { this.color = it } }
        brightness?.let { wraith.update(ledDevice) { this.brightness = it.toUByte() } }
        speed?.let { wraith.update(ledDevice) { this.speed = it.toUByte() } }

        wraith.apply()
        wraith.save()
    } finally {
        wraith.close()
    }
}
