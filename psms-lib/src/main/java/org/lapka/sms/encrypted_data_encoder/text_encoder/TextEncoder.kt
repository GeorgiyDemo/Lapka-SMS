package org.lapka.sms.encrypted_data_encoder.text_encoder

import org.lapka.sms.InvalidDataException
import org.lapka.sms.encrypted_data_encoder.EncryptedDataEncoder
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import java.security.SecureRandom

class TextEncoder(
    nonSpacesSubEncoders: List<SubEncoder>,
    private val spacedSubEncoders: List<SubEncoder>
) : EncryptedDataEncoder {

    private val subEncoders: List<SubEncoder> = nonSpacesSubEncoders + spacedSubEncoders

    private constructor() : this(
        listOf(PunctuationSubEncoder()),
        listOf(DateTimeSubEncoder()) +
                WordsSubEncoderInstances.instances +
                WordsSubEncoderInstances9.instances +
                WordsSubEncoderInstances10.instances
    )

    companion object {
        @JvmStatic
        fun getInstance(): TextEncoder = instanceHolder

        private val instanceHolder: TextEncoder by lazy { TextEncoder() }
    }

    override fun hasFrontPadding(): Boolean = true

    override fun encode(data: ByteArray): String {
        var dataCopy = data
        var actualData = data
        val bytesList = mutableListOf<Byte>()
        while (true) {
            try {
                return actualEncode(actualData)
            } catch (_: RequirePadding) {
                if (bytesList.isEmpty()) {
                    dataCopy = actualData
                    val bytes = ByteArray(256) { it.toByte() }
                    val rng = SecureRandom()
                    for (i in bytes.size - 1 downTo 1) {
                        val j = rng.nextInt(i + 1)
                        val tmp = bytes[i]; bytes[i] = bytes[j]; bytes[j] = tmp
                    }
                    bytesList.addAll(bytes.toList())
                }
                actualData = byteArrayOf(bytesList.removeLast()) + dataCopy
            }
        }
    }

    private fun actualEncode(data: ByteArray): String {
        val targetSize = BigInteger.ONE.shl(data.size * 8)
        var currentSize = BigInteger.ONE
        var currentValue = BigInteger.fromByteArray(data, Sign.POSITIVE)
        val words = mutableListOf<EncodeResult>()

        while (currentSize < targetSize) {
            val subEncoderList = if (words.isNotEmpty() &&
                words.last().needSpaceBefore && words.last().needSpaceAfter
            ) subEncoders else spacedSubEncoders

            val subEncoder = subEncoderList[(currentValue % subEncoderList.size).intValue()]
            if (currentValue < BigInteger(subEncoderList.size)) {
                throw RequirePadding()
            }
            currentValue /= subEncoderList.size
            currentSize *= subEncoderList.size

            val result = subEncoder.encode(currentValue)
            words.add(result)
            if (result.size != BigInteger.ZERO) {
                currentValue /= result.size
                currentSize *= result.size
            }
        }

        var result = ""
        var needSpaceAfterPrevious = true
        for (word in words) {
            if (result.isNotEmpty() && word.needSpaceBefore && needSpaceAfterPrevious) {
                result += " "
            }
            result += word.word
            needSpaceAfterPrevious = word.needSpaceAfter
        }
        return result
    }

    override fun decode(str: String): ByteArray {
        var index = 0
        var needSpace = true
        var actualSize = BigInteger(0)
        val coefficients = mutableListOf<Pair<Int, Int>>()

        while (index < str.length) {
            val subEncoderList = if (needSpace) spacedSubEncoders else subEncoders
            var decodeResult: DecodeResult? = null
            for (i in subEncoderList.indices) {
                decodeResult = subEncoderList[i].decode(str, index)
                if (decodeResult != null) {
                    coefficients.add(i to subEncoderList.size)
                    coefficients.add(decodeResult.value to decodeResult.size)
                    needSpace = !decodeResult.needSpaceBefore || !decodeResult.needSpaceAfter
                    index = decodeResult.newPosition
                    actualSize += decodeResult.size + subEncoderList.size
                    break
                }
            }
            if (decodeResult == null) {
                throw InvalidDataException("'$str' at $index is not in valid Text scheme")
            }
        }

        val value = coefficients.reversed().fold(BigInteger(0)) { acc, pair ->
            acc * pair.second + pair.first
        }

        val resultBytes = value.toByteArray()
        return ByteArray(sizeToByteCount(actualSize)) + resultBytes
    }

    private fun sizeToByteCount(size: BigInteger): Int {
        var sizeCopy = size
        var bitIndex = 0
        var hasNonHighestBits = false
        while (sizeCopy != BigInteger(0)) {
            hasNonHighestBits = hasNonHighestBits || (sizeCopy and BigInteger(1)) == BigInteger(1)
            sizeCopy = sizeCopy.shr(1)
            bitIndex++
        }
        val bitCount = if (hasNonHighestBits) bitIndex + 1 else bitIndex
        return kotlin.math.ceil(bitCount.toDouble() / 8.0).toInt()
    }

    private class RequirePadding : Exception()
}
