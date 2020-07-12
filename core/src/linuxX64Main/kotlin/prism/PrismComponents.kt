package com.serebit.wraith.core.prism

interface PrismComponent {
    val byteValues: List<Int>
    var savedByteValues: List<Int>

    val mode: PrismMode
    val modes: List<PrismMode>
    val channel: Int
    var color: Color
    var speed: Speed
    var brightness: Brightness
    var useRandomColor: Boolean

    fun assignValuesFromChannel(channelValues: ChannelValues)
}

interface BasicPrismComponent : PrismComponent {
    override var mode: BasicPrismMode
}

class BasicPrismComponentDelegate(initialValues: ChannelValues, override val channel: Int) : BasicPrismComponent {
    override lateinit var mode: BasicPrismMode
    override lateinit var speed: Speed
    override lateinit var brightness: Brightness
    override lateinit var color: Color
    override var useRandomColor = false

    override val modes: List<BasicPrismMode> = BasicPrismMode.values().toList()
    override var savedByteValues: List<Int> = emptyList()

    init {
        assignValuesFromChannel(initialValues)
        savedByteValues = byteValues
    }

    override fun assignValuesFromChannel(channelValues: ChannelValues) {
        mode = BasicPrismMode.values().first { it.mode == channelValues.mode }

        color = if (mode.colorSupport != ColorSupport.NONE) channelValues.color else Color.BLACK

        useRandomColor = mode.colorSupport == ColorSupport.ALL && channelValues.colorSource == 0x80

        speed = mode.speeds.indexOfOrNull(channelValues.speed)
            ?.let { Speed.values()[it] }
            ?: Speed.MEDIUM

        brightness = mode.brightnesses.indexOfOrNull(channelValues.brightness)
            ?.let { Brightness.values()[it] }
            ?: Brightness.MEDIUM
    }

    override val byteValues: List<Int>
        get() {
            val brightness = mode.brightnesses.elementAtOrNull(brightness.ordinal) ?: 0
            val speed = mode.speeds.elementAtOrNull(speed.ordinal) ?: 0x2C
            val colorSource = if (useRandomColor) 0x80 else 0x20
            return listOf(channel, speed, colorSource, mode.mode, 0xFF, brightness, color.r, color.g, color.b)
        }
}

class PrismLogoComponent(initial: ChannelValues) : BasicPrismComponent by BasicPrismComponentDelegate(initial, 5)

class PrismFanComponent(initial: ChannelValues) : BasicPrismComponent by BasicPrismComponentDelegate(initial, 6) {
    var mirageState: MirageState = MirageState.Off // no hardware getter, so it starts as off due to being unknown
}

class PrismRingComponent(initialValues: ChannelValues) : PrismComponent {
    override val modes: List<PrismRingMode> = PrismRingMode.values().toList()
    override lateinit var mode: PrismRingMode
    override val channel: Int get() = mode.channel
    override lateinit var color: Color
    override var useRandomColor = false
    override var speed = Speed.MEDIUM
    override var brightness = Brightness.MEDIUM
    lateinit var direction: RotationDirection
    override var savedByteValues: List<Int> = emptyList()
    var savedMorseBytes: List<Int>? = null

    init {
        assignValuesFromChannel(initialValues)
        savedByteValues = byteValues
    }

    override fun assignValuesFromChannel(channelValues: ChannelValues) {
        mode = PrismRingMode.values().first { it.channel == channelValues.channel }

        color = if (mode.colorSupport != ColorSupport.NONE) channelValues.color else Color.BLACK

        speed = mode.speeds.indexOfOrNull(channelValues.speed)
            ?.let { Speed.values()[it] }
            ?: Speed.MEDIUM

        brightness = mode.brightnesses.indexOfOrNull(channelValues.brightness)
            ?.let { Brightness.values()[it] }
            ?: Brightness.MEDIUM

        useRandomColor = mode.colorSupport == ColorSupport.ALL && (channelValues.colorSource and 0x80 != 0)

        direction = if (mode.supportsDirection) {
            RotationDirection.values()[channelValues.colorSource and 1]
        } else {
            RotationDirection.CLOCKWISE
        }
    }

    override val byteValues: List<Int>
        get() {
            val brightness = mode.brightnesses.elementAtOrNull(brightness.ordinal) ?: 0x99
            val speed = if (mode != PrismRingMode.MORSE) {
                mode.speeds.elementAtOrNull(speed.ordinal) ?: 0xFF
            } else 0x6B
            val colorSource = when {
                mode.colorSupport == ColorSupport.ALL && useRandomColor ->
                    0x80.let { if (mode.supportsDirection) it + direction.ordinal else it }
                mode.supportsDirection -> direction.ordinal
                else -> mode.colorSource
            }
            return listOf(mode.channel, speed, colorSource, mode.mode, 0xFF, brightness, color.r, color.g, color.b)
        }
}

private fun List<Int>.indexOfOrNull(value: Int) = indexOf(value).let { if (it == -1) null else it }

interface PrismMode {
    val name: String
    val ordinal: Int
    val mode: Int
    val speeds: List<Int>
    val brightnesses: List<Int>
    val colorSupport: ColorSupport
}

enum class BasicPrismMode(
    override val mode: Int,
    override val speeds: List<Int> = emptyList(),
    override val brightnesses: List<Int> = listOf(0x4C, 0x99, 0xFF),
    override val colorSupport: ColorSupport = ColorSupport.NONE
) : PrismMode {
    OFF(0, brightnesses = emptyList()),
    STATIC(1, colorSupport = ColorSupport.SPECIFIC),
    CYCLE(2, listOf(0x96, 0x8C, 0x80, 0x6E, 0x68), listOf(0x10, 0x40, 0x7F)),
    BREATHE(3, listOf(0x3C, 0x37, 0x31, 0x2C, 0x26), colorSupport = ColorSupport.ALL)
}

enum class PrismRingMode(
    val channel: Int, override val mode: Int,
    override val speeds: List<Int> = emptyList(),
    override val brightnesses: List<Int> = listOf(0x4C, 0x99, 0xFF),
    override val colorSupport: ColorSupport = ColorSupport.NONE,
    val supportsDirection: Boolean = false,
    val colorSource: Int = 0x20
) : PrismMode {
    OFF(0xFE, 0, brightnesses = emptyList()),
    STATIC(0, 0xFF, colorSupport = ColorSupport.SPECIFIC),
    RAINBOW(7, 5, listOf(0x72, 0x68, 0x64, 0x62, 0x61), colorSource = 0),
    SWIRL(0xA, 0x4A, listOf(0x77, 0x74, 0x6E, 0x6B, 0x67), colorSupport = ColorSupport.ALL, supportsDirection = true),
    CHASE(9, 0xC3, listOf(0x77, 0x74, 0x6E, 0x6B, 0x67), colorSupport = ColorSupport.ALL, supportsDirection = true),
    BOUNCE(8, 0xFF, listOf(0x77, 0x74, 0x6E, 0x6B, 0x67), colorSource = 0x80),
    MORSE(0xB, 5, brightnesses = emptyList(), colorSupport = ColorSupport.ALL, colorSource = 0),
    CYCLE(2, 0xFF, listOf(0x96, 0x8C, 0x80, 0x6E, 0x68), listOf(0x10, 0x40, 0x7F)),
    BREATHE(1, 0xFF, listOf(0x3C, 0x37, 0x31, 0x2C, 0x26), colorSupport = ColorSupport.ALL)
}

data class Color(val r: Int, val g: Int, val b: Int) {
    companion object {
        val BLACK = Color(0, 0, 0)
    }
}

sealed class MirageState {
    class On(val redFreq: Int, val greenFreq: Int, val blueFreq: Int) : MirageState()
    object Off : MirageState()

    companion object {
        val DEFAULT = On(330, 330, 330)
    }
}

enum class ColorSupport { NONE, SPECIFIC, ALL }
enum class Speed { SLOWEST, SLOW, MEDIUM, FAST, FASTEST }
enum class Brightness { LOW, MEDIUM, HIGH }
enum class RotationDirection { CLOCKWISE, COUNTERCLOCKWISE }

val PrismMode.supportsBrightness get() = brightnesses.isNotEmpty()
val PrismMode.supportsSpeed get() = speeds.isNotEmpty()
