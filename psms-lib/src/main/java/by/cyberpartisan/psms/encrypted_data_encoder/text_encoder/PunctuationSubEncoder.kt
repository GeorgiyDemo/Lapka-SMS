package by.cyberpartisan.psms.encrypted_data_encoder.text_encoder

import com.ionspin.kotlin.bignum.integer.BigInteger

private const val chars = ",:().?!"

class PunctuationSubEncoder : SubEncoder {

    override fun encode(currentValue: BigInteger): EncodeResult {
        val index = (currentValue % chars.length).intValue()
        val word = chars[index].toString()
        return EncodeResult(BigInteger(chars.length), word, needSpaceBefore = false, needSpaceAfter = false)
    }

    override fun decode(str: String, index: Int): DecodeResult? {
        val charIndex = chars.indexOf(str[index])
        if (charIndex == -1) return null
        val newPosition = if (index + 1 < str.length && str[index + 1] == ' ') index + 2 else index + 1
        return DecodeResult(chars.length, charIndex, newPosition, needSpaceBefore = false, needSpaceAfter = false)
    }
}
