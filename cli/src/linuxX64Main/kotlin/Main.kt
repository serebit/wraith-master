@file:UseExperimental(ExperimentalUnsignedTypes::class)

package com.serebit.wraith.cli

import com.serebit.wraith.core.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.failAssertion
import kotlin.String as KString

object ColorArgType : ArgType<Color>(true) {
    private val commaSeparatedChannelPattern = "(\\d{1,3}),(\\d{1,3}),(\\d{1,3})".toRegex() // r,g,b
    private val hexColorPattern = "#?[\\da-fA-F]{6}".toRegex() // RRGGBB

    override val description = "{ Color with format r,g,b or RRGGBB }"
    override val conversion: (value: KString, name: KString) -> Color = fun(value: KString, name: KString): Color {
        commaSeparatedChannelPattern.matchEntire(value)
            ?.groupValues
            ?.drop(1)
            ?.map { it.toUByteOrNull() ?: failAssertion("Color channel value exceeded maximum of 255") }
            ?.let { return Color(it.component1(), it.component2(), it.component3()) }

        hexColorPattern.matchEntire(value)
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
                |as 255,128,0) or a hex color (such as 03A9F4).
                """.trimMargin()
        )
    }
}

private enum class Components { LOGO, FAN, RING }

fun main(args: Array<KString>) {
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
        val direction by parser.option(
            ArgType.Choice(listOf("clockwise", "counterclockwise")),
            shortName = "d", description = "Only supported by ring modes swirl and chase"
        )

        parser.parse(args)

        brightness?.let { if (it !in 1..3) parser.printError("Brightness must be within the range of 1 to 3") }
        speed?.let { if (it !in 1..5) parser.printError("Speed must be within the range of 1 to 5") }

        val ledComponent = when (Components.valueOf(component.toUpperCase())) {
            Components.LOGO -> wraith.logo
            Components.FAN -> wraith.fan
            Components.RING -> wraith.ring
        }

        when (ledComponent) {
            is BasicLedComponent -> {
                if (mode !in LedMode.values().map { it.name.toLowerCase() })
                    parser.printError("Provided mode is not in valid modes for component $component.")
                mode?.let { wraith.update(ledComponent) { this.mode = LedMode.valueOf(it.toUpperCase()) } }
            }
            is RingComponent -> {
                mode?.let { wraith.update(ledComponent) { this.mode = RingMode.valueOf(it.toUpperCase()) } }
                direction?.let {
                    wraith.update(ledComponent) { this.direction = RotationDirection.valueOf(it.toUpperCase()) }
                }
            }
        }

        color?.let { wraith.update(ledComponent) { this.color = it } }
        brightness?.let { wraith.update(ledComponent) { this.brightness = it.toUByte() } }
        speed?.let { wraith.update(ledComponent) { this.speed = it.toUByte() } }

        wraith.apply()
        wraith.save()
    } finally {
        wraith.close()
    }
}
