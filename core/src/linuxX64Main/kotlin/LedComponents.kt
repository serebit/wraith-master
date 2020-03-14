package com.serebit.wraith.core

interface LedComponent {
    val byteValues: List<Int>
    val mode: Mode
    var color: Color
    var speed: Int
    var brightness: Int
    var useRandomColor: Boolean

    fun assignValuesFromChannel(channelValues: ChannelValues)
}

interface BasicLedComponent : LedComponent {
    val channel: Int
    override var mode: LedMode

    override fun assignValuesFromChannel(channelValues: ChannelValues) {
        mode = LedMode.values.first { it.mode == channelValues.mode }
        if (mode != LedMode.CYCLE) color = channelValues.color
        if (mode.colorSupport == ColorSupport.ALL) useRandomColor = channelValues.colorSource == 0x80
        speed = mode.speeds.indexOfOrNull(channelValues.speed)?.plus(1) ?: 3
        brightness = mode.brightnesses.indexOfOrNull(channelValues.brightness)?.plus(1) ?: 2
    }

    override val byteValues: List<Int>
        get() {
            val brightness = mode.brightnesses.elementAtOrNull(brightness - 1) ?: 0
            val speed = mode.speeds.elementAtOrNull(speed - 1) ?: 0x2C
            val colorSource = if (useRandomColor) 0x80 else 0x20
            return listOf(channel, speed, colorSource, mode.mode, 0xFF, brightness, color.r, color.g, color.b)
        }
}

class LogoComponent(initialValues: ChannelValues) : BasicLedComponent {
    override val channel = 5
    override lateinit var mode: LedMode
    override var color = Color(0, 0, 0)
    override var useRandomColor = false
    override var speed = 0
    override var brightness = 0

    init {
        assignValuesFromChannel(initialValues)
    }
}

class FanComponent(initialValues: ChannelValues) : BasicLedComponent {
    override val channel = 6
    override lateinit var mode: LedMode
    override var color = Color(0, 0, 0)
    override var useRandomColor = false
    override var speed = 0
    override var brightness = 0
    var mirage = false

    init {
        assignValuesFromChannel(initialValues)
    }
}

enum class RotationDirection(val value: Int) { CLOCKWISE(0), COUNTERCLOCKWISE(1); }

class RingComponent(initialValues: ChannelValues) : LedComponent {
    override lateinit var mode: RingMode
    override lateinit var color: Color
    override var useRandomColor = false
    override var speed = 0
    override var brightness = 0
    lateinit var direction: RotationDirection

    init {
        assignValuesFromChannel(initialValues)
    }

    override fun assignValuesFromChannel(channelValues: ChannelValues) {
        mode = RingMode.values.first { it.channel == channelValues.channel }
        color = channelValues.let { if (mode.colorSupport != ColorSupport.NONE) it.color else Color(0, 0, 0) }
        speed = mode.speeds.indexOfOrNull(channelValues.speed)?.plus(1) ?: 3
        brightness = mode.brightnesses.indexOfOrNull(channelValues.brightness)?.plus(1) ?: 2
        direction = if (mode.supportsDirection) {
            RotationDirection.values()[channelValues.colorSource and 1]
        } else {
            RotationDirection.CLOCKWISE
        }
        if (mode.colorSupport == ColorSupport.ALL) {
            useRandomColor = channelValues.colorSource and 0x80 != 0
        }
    }

    override val byteValues: List<Int>
        get() {
            val brightness = mode.brightnesses.elementAtOrNull(brightness - 1) ?: 0x99
            val speed = if (mode != RingMode.MORSE) {
                mode.speeds.elementAtOrNull(speed - 1) ?: 0xFF
            } else 0x6B
            val colorSource = when {
                mode.colorSupport == ColorSupport.ALL && useRandomColor ->
                    0x80.let { if (mode.supportsDirection) it + direction.value else it }
                mode.supportsDirection -> direction.value
                else -> mode.colorSource
            }
            return listOf(mode.channel, speed, colorSource, mode.mode, 0xFF, brightness, color.r, color.g, color.b)
        }
}

private fun List<Int>.indexOfOrNull(value: Int) = indexOf(value).let { if (it == -1) null else it }

enum class ColorSupport { NONE, SPECIFIC, ALL }

sealed class Mode constructor(
    val name: String,
    val mode: Int,
    val speeds: List<Int>,
    val brightnesses: List<Int>,
    val colorSupport: ColorSupport
) {
    abstract val index: Int
}

sealed class LedMode(
    name: String,
    mode: Int,
    speeds: List<Int> = emptyList(),
    brightnesses: List<Int> = listOf(0x4C, 0x99, 0xFF),
    colorSupport: ColorSupport = ColorSupport.NONE
) : Mode(name, mode, speeds, brightnesses, colorSupport) {
    override val index get() = values.indexOf(this)

    object OFF : LedMode("OFF", 0, brightnesses = emptyList())
    object STATIC : LedMode("STATIC", 1, colorSupport = ColorSupport.SPECIFIC)
    object CYCLE : LedMode("CYCLE", 2, listOf(0x96, 0x8C, 0x80, 0x6E, 0x68), listOf(0x10, 0x40, 0x7F))
    object BREATHE : LedMode("BREATHE", 3, listOf(0x3C, 0x37, 0x31, 0x2C, 0x26), colorSupport = ColorSupport.ALL)

    companion object {
        val values = listOf(OFF, STATIC, CYCLE, BREATHE)
        operator fun get(name: String) = values.first { it.name == name }
    }
}

sealed class RingMode(
    name: String,
    val channel: Int, mode: Int,
    speeds: List<Int> = emptyList(),
    brightnesses: List<Int> = listOf(0x4C, 0x99, 0xFF),
    colorSupport: ColorSupport = ColorSupport.NONE,
    val supportsDirection: Boolean = false,
    val colorSource: Int = 0x20
) : Mode(name, mode, speeds, brightnesses, colorSupport) {
    override val index get() = values.indexOf(this)

    object OFF : RingMode("OFF", 0xFE, 0, emptyList(), emptyList())
    object STATIC : RingMode("STATIC", 0, 0xFF, colorSupport = ColorSupport.SPECIFIC)
    object RAINBOW : RingMode("RAINBOW", 7, 5, listOf(0x72, 0x68, 0x64, 0x62, 0x61), colorSource = 0)
    object SWIRL : RingMode(
        "SWIRL", 0xA, 0x4A, listOf(0x77, 0x74, 0x6E, 0x6B, 0x67),
        colorSupport = ColorSupport.ALL, supportsDirection = true
    )

    object CHASE : RingMode(
        "CHASE", 9, 0xC3, listOf(0x77, 0x74, 0x6E, 0x6B, 0x67),
        colorSupport = ColorSupport.ALL, supportsDirection = true
    )

    object BOUNCE : RingMode("BOUNCE", 8, 0xFF, listOf(0x77, 0x74, 0x6E, 0x6B, 0x67), colorSource = 0x80)
    object MORSE : RingMode("MORSE", 0xB, 5, colorSupport = ColorSupport.ALL, colorSource = 0)
    object CYCLE : RingMode("CYCLE", 2, 0xFF, listOf(0x96, 0x8C, 0x80, 0x6E, 0x68), listOf(0x10, 0x40, 0x7F))
    object BREATHE : RingMode("BREATHE", 1, 0xFF, listOf(0x3C, 0x37, 0x31, 0x2C, 0x26), colorSupport = ColorSupport.ALL)

    companion object {
        val values = listOf(OFF, STATIC, RAINBOW, SWIRL, CHASE, BOUNCE, MORSE, CYCLE, BREATHE)
        operator fun get(name: String) = values.first { it.name == name }
    }
}

data class Color(val r: Int, val g: Int, val b: Int) {
    constructor(r: Double, g: Double, b: Double) : this((255 * r).toInt(), (255 * g).toInt(), (255 * b).toInt())
}

val Mode.supportsBrightness get() = brightnesses.isNotEmpty()
val Mode.supportsSpeed get() = speeds.isNotEmpty()
