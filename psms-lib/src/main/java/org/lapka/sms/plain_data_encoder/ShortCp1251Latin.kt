package org.lapka.sms.plain_data_encoder

@OptIn(ExperimentalUnsignedTypes::class)
class ShortCp1251Latin : ShortEncoder() {

    override fun encodeChar(char: Char): NotAlignedEncoder.Code {
        val lower = char.lowercaseChar()
        val value = when {
            lower in ' '..'~' -> char.code
            lower in '\u0430'..'\u044f' -> char.lowercaseChar().code - 1072
            lower == '\u0451' -> 127
            else -> 63
        }
        return NotAlignedEncoder.Code(value, decodingShifting)
    }

    override fun decodeChar(code: Int): Char {
        return when {
            code in 32..126 -> code.toChar()
            code in 0..32 -> (code + 1072).toChar()
            code == 127 -> '\u0451'
            else -> '?'
        }
    }

    override val mode: Int get() = Mode.SHORT_CP1251_PREFER_LATIN.ordinal
}
