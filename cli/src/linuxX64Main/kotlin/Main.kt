package com.serebit.wraith.cli

import com.serebit.wraith.core.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlin.String as KString

@OptIn(ExperimentalUnsignedTypes::class)
object ColorArgType : ArgType<Color>(true) {
    private val commaSeparatedChannelPattern = "(\\d{1,3}),(\\d{1,3}),(\\d{1,3})".toRegex() // r,g,b
    private val hexColorPattern = "#?[\\da-fA-F]{6}".toRegex() // RRGGBB

    override val description = "{ Color with format r,g,b or RRGGBB }"
    override fun convert(value: KString, name: KString): Color {
        commaSeparatedChannelPattern.matchEntire(value)
            ?.groupValues
            ?.drop(1)
            ?.map { it.toUByteOrNull() ?: error("Color channel value exceeded maximum of 255") }
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

        error(
            """
                |Option $name is expected to be a color, either represented by channel values separated by commas (such 
                |as 255,128,0) or a hex color (such as 03A9F4).
                """.trimMargin()
        )
    }
}

private enum class Components { LOGO, FAN, RING }

@OptIn(ExperimentalUnsignedTypes::class)
fun main(args: Array<KString>) {
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
    val randomColor by parser.option(ArgType.Boolean, fullName = "random-color")
    val mirage by parser.option(ArgType.Choice(listOf("on", "off")), description = "Enable or disable fan mirage")
    val morseText by parser.option(
        ArgType.String, "morse-text",
        description = "Plaintext or morse code to apply to the morse code mode"
    )

    parser.parse(args)

    brightness?.let { if (it !in 1..3) parser.printError("Brightness must be within the range of 1 to 3") }
    speed?.let { if (it !in 1..5) parser.printError("Speed must be within the range of 1 to 5") }
    mode?.let { it ->
        val invalidRingMode = component.toLowerCase() == "ring"
                && it.toUpperCase() !in RingMode.values().map { it.name }
        val invalidLedMode = component.toLowerCase() in listOf("fan", "logo")
                && it.toUpperCase() !in LedMode.values().map { it.name }
        if (invalidRingMode || invalidLedMode) {
            parser.printError("Provided mode $it is not in valid modes for component $component.")
        }
    }
    morseText?.let {
        if (!it.isMorseCode && !it.isValidMorseText)
            error("Invalid chars in morse-text argument: ${it.invalidMorseChars}")
    }

    when (val result: WraithPrismResult = obtainWraithPrism()) {
        is WraithPrismResult.Failure -> parser.printError(result.message)

        is WraithPrismResult.Success -> {
            val wraith = result.device

            val ledComponent = when (Components.valueOf(component.toUpperCase())) {
                Components.LOGO -> wraith.logo
                Components.FAN -> wraith.fan
                Components.RING -> wraith.ring
            }

            mode?.toUpperCase()?.let {
                when (ledComponent) {
                    is BasicLedComponent -> wraith.update(ledComponent) { this.mode = LedMode.valueOf(it) }
                    is RingComponent -> wraith.update(ledComponent) { this.mode = RingMode.valueOf(it) }
                }
            }

            if (randomColor != null) {
                randomColor?.let { wraith.update(ledComponent) { this.useRandomColor = it } }
            } else if (color != null) {
                color?.let {
                    wraith.update(ledComponent) {
                        this.color = it
                        this.useRandomColor = false
                    }
                }
            }
            brightness?.let { wraith.update(ledComponent) { this.brightness = it } }
            speed?.let { wraith.update(ledComponent) { this.speed = it } }

            if (ledComponent is RingComponent) {
                direction?.toUpperCase()?.let {
                    wraith.update(ledComponent) { this.direction = RotationDirection.valueOf(it) }
                }

                morseText?.let { wraith.updateRingMorseText(it) }
            }
            if (ledComponent is FanComponent) mirage?.let {
                wraith.fan.mirage = it == "on"
                wraith.updateFanMirage()
            }

            wraith.apply()
            wraith.save()

            wraith.close()
        }
    }
}
