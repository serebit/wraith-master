package com.serebit.wraith.cli

import com.serebit.wraith.core.DeviceResult
import com.serebit.wraith.core.obtainWraithPrism
import com.serebit.wraith.core.prism.*
import com.serebit.wraith.core.programVersion
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlin.String as KString

fun main(args: Array<KString>) {
    when (args.singleOrNull()) {
        "-v", "--version" -> return println("Wraith Master, version $programVersion")
        "-f", "--firmwareversion" -> return modifyWraithPrism(false) {
            println("The connected Wraith Prism has firmware version ${requestFirmwareVersion()}")
        }
        "-R", "--resettodefault" -> return modifyWraithPrism(false) {
            resetToDefault()
        }
        "-e", "--toggleenso" -> return modifyWraithPrism(false) {
            enso = !enso
            if (enso) {
                println("Enabled enso mode.")
            } else {
                println("Disabled enso mode. Settings have been reset to factory defaults.")
            }
        }
        "--resetport" -> return modifyWraithPrism(false) {
            resetPort()
            println("Reset USB port.")
        }
    }

    val parser = ArgParser("wraith-master")

    val component by parser.argument(ArgType.Choice(listOf("logo", "fan", "ring")))

    val basicModes: List<KString> = BasicPrismMode.values().map { it.name.toLowerCase() }
    val ringModes: List<KString> = PrismRingMode.values().map { it.name.toLowerCase() }
    val modeNames = (basicModes union ringModes).toList()

    val mode by parser.option(
        ArgType.Choice(modeNames),
        shortName = "m",
        description = "(Modes $ringModes are only supported by ring component)"
    )

    val color by parser.option(
        ColorArgType,
        shortName = "c",
        description = "The color of the component's LEDs"
    )

    val brightness by parser.option(
        ArgType.Int,
        shortName = "b",
        description = "The brightness of the component's LEDs. Value can range from 1 to 3"
    )

    val speed by parser.option(
        ArgType.Int,
        shortName = "s",
        description = "The speed of the pattern for the component's LEDs. Value can range from 1 to 5"
    )

    val direction by parser.option(
        ArgType.Choice(listOf("clockwise", "counterclockwise")),
        shortName = "d",
        description = "The direction of the ring mode's rotation. Only supported by ring modes swirl and chase"
    )

    val randomColor by parser.option(
        ArgType.Boolean,
        shortName = "r",
        fullName = "randomcolor",
        description = "Enables or disables random color cycling for the component's LEDs"
    )

    val mirage by parser.option(
        MirageArgType,
        shortName = "M",
        description = "The fan LED mirage frequencies"
    )

    val morseText by parser.option(
        ArgType.String,
        shortName = "t",
        fullName = "morsetext",
        description = "Plaintext or morse code for the ring to use when the morse mode is selected"
    )

    val verbose by parser.option(
        ArgType.Boolean,
        shortName = "V",
        description = "Print changes made to device LEDs to the console"
    )

    @Suppress("UNUSED_VARIABLE")
    val version by parser.option(
        ArgType.Boolean,
        shortName = "v",
        description = "Print program version and exit. Ignored unless run as the only argument"
    )

    @Suppress("UNUSED_VARIABLE")
    val resetToDefault by parser.option(
        ArgType.Boolean,
        shortName = "R",
        fullName = "resettodefault",
        description = "Resets the lights to their default configuration. Ignored unless run as the only argument"
    )

    @Suppress("UNUSED_VARIABLE")
    val toggleEnso by parser.option(
        ArgType.Boolean,
        shortName = "e",
        fullName = "toggleenso",
        description = "Toggles Enso mode. Erases all settings when enabled. Ignored unless run as the only argument"
    )

    @Suppress("UNUSED_VARIABLE")
    val firmwareVersion by parser.option(
        ArgType.Boolean,
        shortName = "f",
        fullName = "firmwareversion",
        description = "Reports the firmware version of the connected cooler. Ignored unless run as the only argument"
    )

    @Suppress("UNUSED_VARIABLE")
    val resetPort by parser.option(
        ArgType.Boolean,
        fullName = "resetport",
        description = "Performs a USB port reset to reinitialize the connected cooler. Ignored unless run as the only argument"
    )

    parser.parse(args)

    if (verbose == true) print("Opening interface to device... ")
    when (val result: DeviceResult = obtainWraithPrism()) {
        is DeviceResult.Failure -> error(result.message)

        is DeviceResult.Success -> result.run {
            if (verbose == true) println("Done.")

            // parameter validation
            mode?.let { it ->
                val invalidRingMode = component.toLowerCase() == "ring"
                        && it.toUpperCase() !in PrismRingMode.values().map { it.name }
                val invalidLedMode = component.toLowerCase() in listOf("fan", "logo")
                        && it.toUpperCase() !in BasicPrismMode.values().map { it.name }
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

            val prismComponent = when (component) {
                "logo" -> prism.logo
                "fan" -> prism.fan
                "ring" -> prism.ring
                else -> throw IllegalStateException()
            }

            fun shortCircuit(message: KString): Nothing {
                if (verbose == true) println("Encountered error, short-circuiting")
                prism.finalize(prismComponent, verbose)
                error(message)
            }

            if (verbose == true) println("Modifying component $component")

            mode?.let {
                if (verbose == true) println("\tSetting mode to $it")
                when (prismComponent) {
                    is PrismFanComponent -> prismComponent.mode = BasicPrismMode.valueOf(it.toUpperCase())
                    is PrismLogoComponent -> prismComponent.mode = BasicPrismMode.valueOf(it.toUpperCase())
                    is PrismRingComponent -> prismComponent.mode = PrismRingMode.valueOf(it.toUpperCase())
                }
            }

            randomColor?.let {
                if (prismComponent.mode.colorSupport != ColorSupport.ALL)
                    shortCircuit("Currently selected mode does not support color randomization")
                if (verbose == true) println("\t${if (it) "Enabling" else "Disabling"} color randomization")
                prismComponent.useRandomColor = it
            }
            color?.let {
                if (prismComponent.mode.colorSupport == ColorSupport.NONE)
                    shortCircuit("Currently selected mode does not support setting a color")
                if (verbose == true) println("\tSetting color to ${it.r},${it.g},${it.b}")
                prismComponent.color = it
                prismComponent.useRandomColor = false
            }

            brightness?.let {
                if (prismComponent.mode.brightnesses.isEmpty())
                    shortCircuit("Currently selected mode does not support setting the brightness level")
                if (verbose == true) println("\tSetting brightness to level $it")
                prismComponent.brightness = Brightness.values()[it - 1]
            }
            speed?.let {
                if (prismComponent.mode.speeds.isEmpty())
                    shortCircuit("Currently selected mode does not support the speed setting")
                if (verbose == true) println("\tSetting speed to level $it")
                prismComponent.speed = Speed.values()[it - 1]
            }

            if (prismComponent is PrismRingComponent) {
                direction?.let {
                    if (!prismComponent.mode.supportsDirection)
                        shortCircuit("Currently selected mode does not support the rotation direction setting")
                    if (verbose == true) println("\tSetting rotation direction to $it")
                    prismComponent.direction = RotationDirection.valueOf(it.toUpperCase())
                }

                morseText?.let {
                    if (prismComponent.mode != PrismRingMode.MORSE)
                        shortCircuit("Can't set morse text on any modes other than morse")
                    if (verbose == true) println("\tSetting morse text to \"$it\"")
                    prism.updateRingMorseText(it)
                }
            } else if (prismComponent is PrismFanComponent) mirage?.let {
                if (prismComponent.mode == BasicPrismMode.OFF) {
                    shortCircuit("Currently selected mode does not support fan mirage")
                }
                prism.fan.mirageState = it
                prism.pushFanMirageState()

                if (verbose == true) {
                    if (it is MirageState.On) {
                        println("\tEnabling mirage, setting frequencies to ${it.redFreq},${it.greenFreq},${it.blueFreq}")
                    } else {
                        println("\tDisabling mirage")
                    }
                }
            }

            prism.finalize(prismComponent, verbose)
        }
    }
}

private object MirageArgType : ArgType<MirageState>(true) {
    private val commaSeparatedChannelPattern = "(\\d{2,4}),(\\d{2,4}),(\\d{2,4})".toRegex() // r,g,b

    override val description =
        """{ Value should be three frequencies in the format "r,g,b", each within the range 45-2000 (e.g. "330,330,330"), or one of the strings "on" or "off" }"""

    override fun convert(value: KString, name: KString): MirageState {
        commaSeparatedChannelPattern.matchEntire(value)
            ?.groupValues?.drop(1)
            ?.mapNotNull { it.toIntOrNull() }
            ?.filter { it in 45..2000 }
            ?.takeIf { it.size == 3 }
            ?.let { return MirageState.On(it[0], it[1], it[2]) }

        return when (value.toLowerCase()) {
            "on" -> MirageState.On(330, 330, 330)
            "off" -> MirageState.Off
            else -> error("""Option $name is expected to be either three comma-separated values, or one of the strings "on" or "off".""")
        }
    }
}

private object ColorArgType : ArgType<Color>(true) {
    private val commaSeparatedChannelPattern = "(\\d{1,3}),(\\d{1,3}),(\\d{1,3})".toRegex() // r,g,b
    private val hexColorPattern = "#?[\\da-fA-F]{6}".toRegex() // RRGGBB

    override val description = "{ Color with format r,g,b in decimal or RRGGBB in hex }"
    override fun convert(value: KString, name: KString): Color {
        commaSeparatedChannelPattern.matchEntire(value)
            ?.groupValues?.drop(1)
            ?.map { it.toInt() }
            ?.onEach { if (it !in 0..255) error("Color channel value exceeded maximum of 255") }
            ?.let { return Color(it[0], it[1], it[2]) }

        hexColorPattern.matchEntire(value)
            ?.value
            ?.removePrefix("#")
            ?.toIntOrNull(16)?.let {
                val r = it shr 16 and 0xFF
                val g = it shr 8 and 0xFF
                val b = it and 0xFF
                return Color(r, g, b)
            }

        error("""Option $name is expected to be a color, either represented by channel values separated by commas (such as 255,128,0) or a hex color (such as 03A9F4).""")
    }
}

private fun modifyWraithPrism(verbose: Boolean?, task: WraithPrism.() -> Unit) {
    if (verbose == true) print("Opening interface to device... ")
    when (val result: DeviceResult = obtainWraithPrism()) {
        is DeviceResult.Failure -> error(result.message)
        is DeviceResult.Success -> result.prism.run {
            if (verbose == true) println("Done.")
            task()
            if (enso) {
                println("Warning: Modifications will not apply while enso mode is on.")
            }
            if (verbose == true) print("Closing USB interface... ")
            close()
            if (verbose == true) println("Done.")
        }
    }
}

private fun WraithPrism.finalize(component: PrismComponent<*>, verbose: Boolean?) {
    if (verbose == true) print("Applying changes...")
    component.submitValues()
    assignChannels()
    apply()
    save()
    close()
    if (verbose == true) println("Done.")
}
