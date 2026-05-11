package org.lapka.sms.plain_data_encoder

@OptIn(ExperimentalUnsignedTypes::class)
class Ascii : ShortEncoder() {

    override fun encodeChar(char: Char): NotAlignedEncoder.Code {
        val lower = char.lowercaseChar()
        val value = if (lower in '\u0000'..'\u007f') char.code else 63
        return NotAlignedEncoder.Code(value, decodingShifting)
    }

    override fun decodeChar(code: Int): Char {
        return if (code in 0..127) code.toChar() else '?'
    }

    override val mode: Int get() = Mode.ASCII.ordinal
}
