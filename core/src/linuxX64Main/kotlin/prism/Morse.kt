package com.serebit.wraith.core.prism

val String.isMorseCode get() = trim().split(' ').all { it in charMorseRepresentation.values }
val String.isValidMorseText get() = toUpperCase().all { it in charMorseRepresentation }
val String.invalidMorseChars get() = filter { it.toUpperCase() !in charMorseRepresentation }.toList().distinct()

private fun String.fromTextToMorse() = trim().mapNotNull { charMorseRepresentation[it.toUpperCase()] }.joinToString(" ")

private val charToBitsMap = mapOf('.' to "10", '-' to "01", ' ' to "00")
private fun String.fromMorseToBits() = trim().map { charToBitsMap[it] ?: error("Invalid character") }

fun String.parseMorseOrTextToBytes(): List<UByte> = (if (isMorseCode) this else fromTextToMorse())
    .fromMorseToBits()
    .joinToString("", postfix = "0011")
    .chunked(8)
    .map { it.reversed().toUByte(2) }

private val charMorseRepresentation = mapOf(
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.", 'G' to "--.", 'H' to "....",
    'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.",
    'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
    'Y' to "-.--", 'Z' to "--..",

    '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
    '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",

    'ä' to ".-.-", 'á' to ".--.-", 'å' to ".--.-", 'é' to "..-..", 'ñ' to "--.--", 'ö' to "---.", 'ü' to "..--",

    '&' to ".-...", '\'' to ".----.", '@' to ".--.-.", ')' to "-.--.-", '(' to "-.--.", ':' to "---...",
    ',' to "--..--", '=' to "-...-", '!' to "-.-.--", '.' to ".-.-.-", '-' to "-....-", '+' to ".-.-.",
    '\"' to ".-..-.", '?' to "..--..", '/' to "-..-.", ' ' to ' '
)
