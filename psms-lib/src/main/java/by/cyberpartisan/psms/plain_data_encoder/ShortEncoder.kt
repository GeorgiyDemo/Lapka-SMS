package by.cyberpartisan.psms.plain_data_encoder

@OptIn(ExperimentalUnsignedTypes::class)
abstract class ShortEncoder : NotAlignedEncoder() {

    var stringBuilder: StringBuilder? = null
    override val decodingShifting: Int = 7

    abstract fun decodeChar(code: Int): Char

    override fun beforeDecode() {
        stringBuilder = StringBuilder()
    }

    override fun processDecodingValue(value: Int) {
        stringBuilder!!.append(decodeChar(value))
    }

    override fun getDecodedString(): String = stringBuilder.toString()
}
