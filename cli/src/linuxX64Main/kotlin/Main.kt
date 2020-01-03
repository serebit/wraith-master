@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package com.serebit.wraith.cli

import com.serebit.wraith.core.*
import kotlinx.cli.*

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

class BasicLedSubcommand(name: String, private val ledDevice: WraithPrism.BasicLedDevice) : Subcommand(name) {
    val mode by option(ArgType.Choice(listOf("off", "static", "breathe")))
    val color by option(ColorArgType, shortName = "c")
    val brightness by option(ArgType.Int, shortName = "b", description = "Value from 1 to 5")
    val speed by option(ArgType.Int, shortName = "s", description = "Value from 1 to 5")

    @UseExperimental(ExperimentalCli::class)
    override fun execute() {
        mode?.let { ledDevice.mode = LedMode.valueOf(it.toUpperCase()) }
        color?.let { ledDevice.color = it }
        brightness?.let {
            if (it !in 1..5) printError("Brightness must be within the range of 1 to 5")
            ledDevice.brightness = (it * 51).toUByte()
        }
        speed?.let {
            if (it !in 1..5) printError("Speed must be within the range of 1 to 5")
            ledDevice.speed = ubyteArrayOf(0x3Cu, 0x34u, 0x2cu, 0x20u, 0x18u)[it - 1]
        }
    }
}

val logo = BasicLedSubcommand("logo", device.logo)
val fan = BasicLedSubcommand("fan", device.fan)

@UseExperimental(ExperimentalCli::class)
val ring = object : Subcommand("ring") {
    val mode by option(ArgType.Choice(listOf("off", "static", "breathe", "swirl")))
    val color by option(ColorArgType, shortName = "c")
    val brightness by option(ArgType.Int, shortName = "b", description = "Value from 1 to 5")

    override fun execute() {
        mode?.let { device.ring.mode = RingMode.valueOf(it.toUpperCase()) }
        color?.let { device.ring.color = it }
        brightness?.let {
            if (it !in 1..5) printError("Brightness must be within the range of 1 to 5")
            device.ring.brightness = (it * 51).toUByte()
        }
    }
}

fun main(args: Array<String>) {
    try {
        val parser = ArgParser("wraith-master")

        parser.subcommands(logo, fan, ring)
        parser.parse(args)

        device.assignChannels()

        device.apply()
        device.save()
    } finally {
        device.close()
    }
}
