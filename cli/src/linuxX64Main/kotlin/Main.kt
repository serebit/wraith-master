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

fun WraithPrism.finalize(component: LedComponent, verbose: Boolean?) {
    if (verbose == true) println("Applying changes")
    setChannelValues(component)
    assignChannels()
    apply()
    if (verbose == true) print("Closing USB interface... ")
    close()
    if (verbose == true) println("Done.")
}

@OptIn(ExperimentalUnsignedTypes::class)
fun main(args: Array<KString>) {
    val parser = ArgParser("wraith-master")

    val component by parser.argument(ArgType.Choice(listOf("logo", "fan", "ring")))

    val allModes = (RingMode.values + LedMode.values)
    val modeNames = allModes.map { it.name.toLowerCase() }.distinct()

    val mode by parser.option(
        ArgType.Choice(modeNames), shortName = "m",
        description = "(Modes ${modeNames - LedMode.values.map { it.name.toLowerCase() }} are only supported by ring component)"
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
    val verbose by parser.option(ArgType.Boolean, description = "Print changes made to device LEDs to the console")

    parser.parse(args)

    mode?.let { it ->
        val invalidRingMode = component.toLowerCase() == "ring"
                && it.toUpperCase() !in RingMode.values.map { it.name }
        val invalidLedMode = component.toLowerCase() in listOf("fan", "logo")
                && it.toUpperCase() !in LedMode.values.map { it.name }
        if (invalidRingMode || invalidLedMode) {
            error("Provided mode $it is not in valid modes for component $component.")
        }
    }
    brightness?.let { if (it !in 1..3) error("Brightness must be within the range of 1 to 3") }
    speed?.let { if (it !in 1..5) error("Speed must be within the range of 1 to 5") }
    morseText?.let {
        if (!it.isMorseCode && !it.isValidMorseText)
            error("Invalid chars in morse-text argument: ${it.invalidMorseChars}")
    }
    mirage?.let {
        if (component != "fan") error("Only the fan component supports the mirage setting")
    }
    randomColor?.let {
        if (color != null) error("Cannot set color randomness along with a specific color")
    }

    if (verbose == true) print("Opening interface to device... ")
    when (val result: WraithPrismResult = obtainWraithPrism()) {
        is WraithPrismResult.Failure -> error(result.message)

        is WraithPrismResult.Success -> {
            if (verbose == true) println("Done.")
            val wraith = result.device

            val ledComponent = when (component) {
                "logo" -> wraith.logo
                "fan" -> wraith.fan
                "ring" -> wraith.ring
                else -> throw IllegalStateException()
            }

            fun shortCircuit(message: KString): Nothing {
                if (verbose == true) println("Encountered error, short-circuiting")
                wraith.finalize(ledComponent, verbose)
                error(message)
            }

            if (verbose == true) println("Modifying component $component")

            mode?.let {
                if (verbose == true) println("  Setting mode to $it")
                when (ledComponent) {
                    is BasicLedComponent -> ledComponent.mode = LedMode[it.toUpperCase()]
                    is RingComponent -> ledComponent.mode = RingMode[it.toUpperCase()]
                }
            }

            randomColor?.let {
                if (ledComponent.mode.colorSupport != ColorSupport.ALL)
                    shortCircuit("Currently selected mode does not support color randomization")
                if (verbose == true) println("  ${if (it) "Enabling" else "Disabling"} color randomization")
                ledComponent.useRandomColor = it
            }
            color?.let {
                if (ledComponent.mode.colorSupport == ColorSupport.NONE)
                    shortCircuit("Currently selected mode does not support setting a color")
                if (verbose == true) println("  Setting color to ${it.r},${it.g},${it.b}")
                ledComponent.color = it
                ledComponent.useRandomColor = false
            }

            brightness?.let {
                if (!ledComponent.mode.supportsBrightness)
                    shortCircuit("Currently selected mode does not support setting the brightness level")
                if (verbose == true) println("  Setting brightness to level $it")
                ledComponent.brightness = it
            }
            speed?.let {
                if (!ledComponent.mode.supportsSpeed)
                    shortCircuit("Currently selected mode does not support the speed setting")
                if (verbose == true) println("  Setting speed to level $it")
                ledComponent.speed = it
            }

            if (ledComponent is RingComponent) {
                direction?.let {
                    if (!ledComponent.mode.supportsDirection)
                        shortCircuit("Currently selected mode does not support the rotation direction setting")
                    if (verbose == true) println("  Setting rotation direction to $it")
                    ledComponent.direction = RotationDirection.valueOf(it.toUpperCase())
                }

                morseText?.let {
                    if (ledComponent.mode != RingMode.MORSE)
                        shortCircuit("Can't se")
                    if (verbose == true) println("  Setting morse text to \"$it\"")
                    wraith.updateRingMorseText(it)
                }
            } else if (ledComponent is FanComponent) mirage?.let {
                if (ledComponent.mode == LedMode.OFF)
                    shortCircuit("Currently selected mode does not support fan mirage")
                if (verbose == true) println("  ${if (it == "on") "Enabling" else "Disabling"} mirage")
                ledComponent.mirage = it == "on"
                wraith.updateFanMirage()
            }

            wraith.finalize(ledComponent, verbose)
        }
    }
}
