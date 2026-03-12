package by.cyberpartisan.psms.plain_data_encoder

@OptIn(ExperimentalUnsignedTypes::class)
class ShortCp1251Cyrillic : ShortEncoder() {

    override fun encodeChar(char: Char): NotAlignedEncoder.Code {
        val value = when {
            char in ' '..'~' -> char.lowercaseChar().code
            char in '\u0430'..'\u044f' -> char.code - 1072
            char == '\u0451' -> 127
            char in '\u0410'..'\u0418' -> char.code - 1040 + 65
            char == '\u0401' -> 74
            char in '\u041a'..'\u0429' -> char.code - 1050 + 75
            char in '\u042a'..'\u042c' || char == '\u0419' -> char.code - 1040
            char == '\u042d' -> 38
            char == '\u042e' -> 94
            char == '\u042f' -> 126
            else -> 63
        }
        return NotAlignedEncoder.Code(value, decodingShifting)
    }

    override fun decodeChar(code: Int): Char {
        return when {
            code in 0 until 32 -> (code + 1072).toChar()
            code == 127 -> '\u0451'
            code in 65..73 -> (code - 65 + 1040).toChar()
            code == 74 -> '\u0401'
            code in 75..90 -> (code - 75 + 1050).toChar()
            code == 38 -> '\u042d'
            code == 94 -> '\u042e'
            code == 126 -> '\u042f'
            code in 0..126 -> code.toChar()
            else -> '?'
        }
    }

    override val mode: Int get() = Mode.SHORT_CP1251_PREFER_CYRILLIC.ordinal
}
