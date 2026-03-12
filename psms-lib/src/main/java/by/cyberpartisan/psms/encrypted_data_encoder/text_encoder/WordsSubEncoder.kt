package by.cyberpartisan.psms.encrypted_data_encoder.text_encoder

import by.cyberpartisan.psms.isDigit
import by.cyberpartisan.psms.isLetter
import com.ionspin.kotlin.bignum.integer.BigInteger

class WordsSubEncoder(private val words: List<String>) : SubEncoder {

    private val wordsToIndexMap: Map<String, Int> =
        words.mapIndexed { index, word -> word to index }.toMap()

    override fun encode(currentValue: BigInteger): EncodeResult {
        val index = (currentValue % words.size).intValue()
        return EncodeResult(BigInteger(words.size), words[index])
    }

    override fun decode(str: String, index: Int): DecodeResult? {
        var lastIndex = index + 1
        while (lastIndex <= str.length) {
            if (lastIndex < str.length && (isLetter(str[lastIndex]) || isDigit(str[lastIndex]))) {
                lastIndex++
                continue
            }
            if (lastIndex + 1 < str.length && str[lastIndex] != ' ' && str[lastIndex + 1] != ' ') {
                return null
            }
            val word = str.substring(index, lastIndex)
            val value = wordsToIndexMap[word]
            if (value != null) {
                val newPosition = if (lastIndex < str.length && str[lastIndex] == ' ') lastIndex + 1 else lastIndex
                return DecodeResult(words.size, value, newPosition)
            }
            lastIndex++
        }
        return null
    }
}
