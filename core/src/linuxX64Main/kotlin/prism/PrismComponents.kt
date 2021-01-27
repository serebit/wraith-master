package com.serebit.wraith.core.prism

import com.serebit.wraith.core.UsbInterface

interface PrismComponent<M : PrismMode> {
    var mode: M
    val byteValues: List<UByte>
    var savedByteValues: List<UByte>

    val channel: UByte
    var color: Color
    var speed: Speed
    var brightness: Brightness
    var useRandomColor: Boolean

    fun reloadValues()
    fun submitValues()
}

private fun PrismComponent<*>.assignCommonValuesFromChannel(mode: PrismMode, channelValues: ChannelValues) {
    color = if (mode.colorSupport != ColorSupport.NONE) channelValues.color else Color.BLACK
    useRandomColor = mode.colorSupport == ColorSupport.ALL && (channelValues.colorSource and 0x80u != 0.toUByte())

    speed = mode.speeds.indexOfOrNull(channelValues.speed)
        ?.let { Speed.values()[it] }
        ?: Speed.MEDIUM

    brightness = mode.brightnesses.indexOfOrNull(channelValues.brightness)
        ?.let { Brightness.values()[it] }
        ?: Brightness.MEDIUM
}

class BasicPrismComponentDelegate internal constructor(private val usb: UsbInterface, override val channel: UByte) :
    PrismComponent<BasicPrismMode> {
    override lateinit var mode: BasicPrismMode
    override lateinit var speed: Speed
    override lateinit var brightness: Brightness
    override lateinit var color: Color
    override var useRandomColor = false
    override var savedByteValues: List<UByte> = emptyList()

    init {
        reloadValues()
        savedByteValues = byteValues
    }

    override fun reloadValues() {
        val channelValues = usb.fetchChannelValues(channel)
        mode = BasicPrismMode.values().first { it.mode == channelValues.mode }
        assignCommonValuesFromChannel(mode, channelValues)
    }

    override fun submitValues() = usb.submitChannelValues(byteValues)

    override val byteValues: List<UByte>
        get() {
            val brightness: UByte = mode.brightnesses.elementAtOrNull(brightness.ordinal) ?: 0u
            val speed: UByte = mode.speeds.elementAtOrNull(speed.ordinal) ?: 0x2Cu
            val colorSource: UByte = if (useRandomColor) 0x80u else 0x20u
            return listOf(channel, speed, colorSource, mode.mode, 0xFFu, brightness, color.r, color.g, color.b)
        }
}

class PrismLogoComponent internal constructor(usb: UsbInterface, channel: UByte) :
    PrismComponent<BasicPrismMode> by BasicPrismComponentDelegate(usb, channel)

class PrismFanComponent internal constructor(usb: UsbInterface, channel: UByte) :
    PrismComponent<BasicPrismMode> by BasicPrismComponentDelegate(usb, channel) {
    var mirageState: MirageState = MirageState.Off // no hardware getter, so it starts as off due to being unknown
}

class PrismRingComponent internal constructor(private val usb: UsbInterface, channel: UByte) :
    PrismComponent<PrismRingMode> {
    override lateinit var mode: PrismRingMode
    override val channel: UByte get() = mode.channel
    override lateinit var color: Color
    override var useRandomColor = false
    override var speed = Speed.MEDIUM
    override var brightness = Brightness.MEDIUM
    lateinit var direction: RotationDirection
    override var savedByteValues: List<UByte> = emptyList()
    var savedMorseBytes: List<UByte>? = null

    init {
        mode = PrismRingMode.values().find { it.channel == channel } ?: run {
            println("Received invalid ring channel byte $channel. Falling back to rainbow mode")
            PrismRingMode.RAINBOW
        }
        reloadValues()
        savedByteValues = byteValues
    }

    override fun reloadValues() {
        val channelValues = usb.fetchChannelValues(mode.channel)
        mode = PrismRingMode.values().first { it.channel == channelValues.channel }
        direction = if (mode.supportsDirection) {
            RotationDirection.values()[channelValues.colorSource.toInt() and 1]
        } else {
            RotationDirection.CLOCKWISE
        }
        assignCommonValuesFromChannel(mode, channelValues)
    }

    override fun submitValues() = usb.submitChannelValues(byteValues)

    override val byteValues: List<UByte>
        get() {
            val brightness: UByte = mode.brightnesses.elementAtOrNull(brightness.ordinal) ?: 0x99u
            val speed: UByte = if (mode != PrismRingMode.MORSE) {
                mode.speeds.elementAtOrNull(speed.ordinal) ?: 0xFFu
            } else 0x6Bu
            val colorSource: UByte = when {
                mode.colorSupport == ColorSupport.ALL && useRandomColor ->
                    if (mode.supportsDirection) {
                        (0x80 + direction.ordinal).toUByte()
                    } else {
                        0.toUByte()
                    }
                mode.supportsDirection -> direction.ordinal.toUByte()
                else -> mode.colorSource
            }
            return listOf(mode.channel, speed, colorSource, mode.mode, 0xFFu, brightness, color.r, color.g, color.b)
        }
}

private fun <T> List<T>.indexOfOrNull(value: T) = indexOf(value).let { if (it == -1) null else it }

interface PrismMode {
    val name: String
    val ordinal: Int
    val mode: UByte
    val speeds: List<UByte>
    val brightnesses: List<UByte>
    val colorSupport: ColorSupport
}

enum class BasicPrismMode(
    override val mode: UByte,
    override val speeds: List<UByte> = emptyList(),
    override val brightnesses: List<UByte> = listOf(0x4Cu, 0x99u, 0xFFu),
    override val colorSupport: ColorSupport = ColorSupport.NONE
) : PrismMode {
    OFF(0u, brightnesses = emptyList()),
    STATIC(1u, colorSupport = ColorSupport.SPECIFIC),
    CYCLE(2u, listOf(0x96u, 0x8Cu, 0x80u, 0x6Eu, 0x68u), listOf(0x10u, 0x40u, 0x7Fu)),
    BREATHE(3u, listOf(0x3Cu, 0x37u, 0x31u, 0x2Cu, 0x26u), colorSupport = ColorSupport.ALL)
}

enum class PrismRingMode(
    val channel: UByte, override val mode: UByte,
    override val speeds: List<UByte> = emptyList(),
    override val colorSupport: ColorSupport = ColorSupport.NONE,
    override val brightnesses: List<UByte> = listOf(0x4Cu, 0x99u, 0xFFu),
    val supportsDirection: Boolean = false,
    val colorSource: UByte = 0x20u
) : PrismMode {
    OFF(0xFEu, 0u, brightnesses = emptyList()),
    STATIC(0u, 0xFFu, colorSupport = ColorSupport.SPECIFIC),
    RAINBOW(7u, 5u, listOf(0x72u, 0x68u, 0x64u, 0x62u, 0x61u), colorSource = 0u),
    SWIRL(0xAu, 0x4Au, listOf(0x77u, 0x74u, 0x6Eu, 0x6Bu, 0x67u), ColorSupport.ALL, supportsDirection = true),
    CHASE(9u, 0xC3u, listOf(0x77u, 0x74u, 0x6Eu, 0x6Bu, 0x67u), ColorSupport.ALL, supportsDirection = true),
    BOUNCE(8u, 0xFFu, listOf(0x77u, 0x74u, 0x6Eu, 0x6Bu, 0x67u), colorSource = 0x80u),
    MORSE(0xBu, 5u, colorSupport = ColorSupport.ALL, brightnesses = emptyList(), colorSource = 0u),
    CYCLE(2u, 0xFFu, listOf(0x96u, 0x8Cu, 0x80u, 0x6Eu, 0x68u), brightnesses = listOf(0x10u, 0x40u, 0x7Fu)),
    BREATHE(1u, 0xFFu, listOf(0x3Cu, 0x37u, 0x31u, 0x2Cu, 0x26u), ColorSupport.ALL)
}

class Color(val r: UByte, val g: UByte, val b: UByte) {
    constructor(r: Int, g: Int, b: Int) : this(r.toUByte(), g.toUByte(), b.toUByte())

    companion object {
        val BLACK = Color(0u, 0u, 0u)
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

private fun UsbInterface.fetchChannelValues(channel: UByte): ChannelValues =
    ChannelValues(sendBytes(0x52u, 0x2Cu, 1u, 0u, channel))

private fun UsbInterface.submitChannelValues(byteValues: List<UByte>) {
    sendBytes(0x51u, 0x2Cu, 1u, 0u, *byteValues.toUByteArray(), 0u, 0u, 0u, filler = 0xFFu)
}
