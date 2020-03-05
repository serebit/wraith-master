package com.serebit.wraith.core

interface LedComponent {
    @OptIn(ExperimentalUnsignedTypes::class)
    val values: UByteArray
    val mode: Mode
    var color: Color
    var speed: Int
    var brightness: Int
    var useRandomColor: Boolean

    fun assignValuesFromChannel(channelValues: ChannelValues)
}

interface BasicLedComponent : LedComponent {
    @OptIn(ExperimentalUnsignedTypes::class)
    val channel: UByte
    override var mode: LedMode

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun assignValuesFromChannel(channelValues: ChannelValues) {
        mode = LedMode.values.first { it.mode == channelValues.mode }
        if (mode != LedMode.CYCLE) {
            color = channelValues.color
        }
        if (mode.colorSupport == ColorSupport.ALL) {
            useRandomColor = channelValues.colorSource == 0x80u.toUByte()
        }
        speed = mode.speeds.indexOfOrNull(channelValues.speed)?.plus(1) ?: 3
        brightness = mode.brightnesses.indexOfOrNull(channelValues.brightness)?.plus(1) ?: 2
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override val values: UByteArray
        get() {
            val brightness = mode.brightnesses.elementAtOrNull(brightness - 1) ?: 0u
            val speed = mode.speeds.elementAtOrNull(speed - 1) ?: 0x2Cu
            val colorSource: UByte = if (useRandomColor) 0x80u else 0x20u
            return ubyteArrayOf(channel, speed, colorSource, mode.mode, 0xFFu, brightness, *color.bytes)
        }
}

class LogoComponent(initialValues: ChannelValues) : BasicLedComponent {
    @OptIn(ExperimentalUnsignedTypes::class)
    override val channel: UByte = 0x05u
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
    @OptIn(ExperimentalUnsignedTypes::class)
    override val channel: UByte = 0x06u
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

@OptIn(ExperimentalUnsignedTypes::class)
enum class RotationDirection(val value: UByte) { CLOCKWISE(0u), COUNTERCLOCKWISE(1u); }

@OptIn(ExperimentalUnsignedTypes::class)
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
        color = channelValues.let { if (mode.colorSupport != ColorSupport.NONE) it.color else Color(0u, 0u, 0u) }
        speed = mode.speeds.indexOfOrNull(channelValues.speed)?.plus(1) ?: 3
        brightness = mode.brightnesses.indexOfOrNull(channelValues.brightness)?.plus(1) ?: 2
        direction = if (mode.supportsDirection) {
            RotationDirection.values()[channelValues.colorSource.toInt() and 1]
        } else {
            RotationDirection.CLOCKWISE
        }
        if (mode.colorSupport == ColorSupport.ALL) {
            useRandomColor = channelValues.colorSource and 0x80u == 0x80u.toUByte()
        }
    }

    override val values: UByteArray
        get() {
            val brightness = mode.brightnesses.elementAtOrNull(brightness - 1) ?: 0x99u
            val speed = if (mode != RingMode.MORSE) {
                mode.speeds.elementAtOrNull(speed - 1) ?: 0xFFu
            } else 0x6Bu
            val colorSource = when {
                mode.colorSupport == ColorSupport.ALL && useRandomColor ->
                    0x80u.let { if (mode.supportsDirection) it + direction.value else it }.toUByte()
                mode.supportsDirection -> direction.value
                else -> mode.colorSource
            }
            return ubyteArrayOf(mode.channel, speed, colorSource, mode.mode, 0xFFu, brightness, *color.bytes)
        }
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun List<UByte>.indexOfOrNull(value: UByte) = indexOf(value).let { if (it == -1) null else it }

enum class ColorSupport { NONE, SPECIFIC, ALL }

@OptIn(ExperimentalUnsignedTypes::class)
sealed class Mode constructor(
    val name: String,
    val mode: UByte,
    val speeds: List<UByte>,
    val brightnesses: List<UByte>,
    val colorSupport: ColorSupport
)

@OptIn(ExperimentalUnsignedTypes::class)
sealed class LedMode(
    name: String,
    mode: UByte,
    speeds: List<UByte> = emptyList(),
    brightnesses: List<UByte> = listOf(0x4Cu, 0x99u, 0xFFu),
    colorSupport: ColorSupport = ColorSupport.NONE
) : Mode(name, mode, speeds, brightnesses, colorSupport) {
    object OFF : LedMode("OFF", 0u, brightnesses = emptyList())
    object STATIC : LedMode("STATIC", 1u, colorSupport = ColorSupport.SPECIFIC)
    object CYCLE : LedMode("CYCLE", 2u, listOf(0x96u, 0x8Cu, 0x80u, 0x6Eu, 0x68u), listOf(0x10u, 0x40u, 0x7Fu))
    object BREATHE : LedMode("BREATHE", 3u, listOf(0x3Cu, 0x37u, 0x31u, 0x2Cu, 0x26u), colorSupport = ColorSupport.ALL)

    companion object {
        val values = listOf(OFF, STATIC, CYCLE, BREATHE)
        operator fun get(name: String) = values.first { it.name == name }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
sealed class RingMode(
    name: String,
    val channel: UByte, mode: UByte,
    speeds: List<UByte> = emptyList(),
    brightnesses: List<UByte> = listOf(0x4Cu, 0x99u, 0xFFu),
    colorSupport: ColorSupport = ColorSupport.NONE,
    val supportsDirection: Boolean = false,
    val colorSource: UByte = 0x20u
) : Mode(name, mode, speeds, brightnesses, colorSupport) {
    object OFF : RingMode("OFF", 0xFEu, 0u, emptyList(), emptyList())
    object STATIC : RingMode("STATIC", 0u, 0xFFu, colorSupport = ColorSupport.SPECIFIC)
    object RAINBOW : RingMode("RAINBOW", 7u, 5u, listOf(0x72u, 0x68u, 0x64u, 0x62u, 0x61u), colorSource = 0u)
    object SWIRL : RingMode(
        "SWIRL", 0xAu, 0x4Au, listOf(0x77u, 0x74u, 0x6Eu, 0x6Bu, 0x67u),
        colorSupport = ColorSupport.ALL, supportsDirection = true
    )

    object CHASE : RingMode(
        "CHASE", 9u, 0xC3u, listOf(0x77u, 0x74u, 0x6Eu, 0x6Bu, 0x67u),
        colorSupport = ColorSupport.ALL, supportsDirection = true
    )

    object BOUNCE : RingMode("BOUNCE", 8u, 0xFFu, listOf(0x77u, 0x74u, 0x6Eu, 0x6Bu, 0x67u), colorSource = 0x80u)
    object MORSE : RingMode("MORSE", 0xBu, 0x05u, colorSupport = ColorSupport.ALL, colorSource = 0u)
    object CYCLE : RingMode("CYCLE", 2u, 0xFFu, listOf(0x96u, 0x8Cu, 0x80u, 0x6Eu, 0x68u), listOf(0x10u, 0x40u, 0x7Fu))
    object BREATHE : RingMode(
        "BREATHE", 1u, 0xFFu, listOf(0x3Cu, 0x37u, 0x31u, 0x2Cu, 0x26u),
        colorSupport = ColorSupport.ALL
    )

    companion object {
        val values = listOf(OFF, STATIC, RAINBOW, SWIRL, CHASE, BOUNCE, MORSE, CYCLE, BREATHE)
        operator fun get(name: String) = values.first { it.name == name }
    }
}

class Color(val r: Int, val g: Int, val b: Int) {
    @OptIn(ExperimentalUnsignedTypes::class)
    constructor(r: UByte, g: UByte, b: UByte) : this(r.toInt(), g.toInt(), b.toInt())

    constructor(r: Double, g: Double, b: Double) : this((255 * r).toInt(), (255 * g).toInt(), (255 * b).toInt())
}

@OptIn(ExperimentalUnsignedTypes::class)
val Color.bytes
    get() = ubyteArrayOf(r.toUByte(), g.toUByte(), b.toUByte())

val Mode.supportsBrightness get() = brightnesses.isNotEmpty()
val Mode.supportsSpeed get() = speeds.isNotEmpty()
