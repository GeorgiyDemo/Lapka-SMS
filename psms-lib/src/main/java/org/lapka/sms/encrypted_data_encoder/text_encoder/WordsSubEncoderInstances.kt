package org.lapka.sms.encrypted_data_encoder.text_encoder

object WordsSubEncoderInstances {
    val instances: List<WordsSubEncoder> by lazy {
        val words = loadWords("words_base.txt")
        words.groupBy { it.length }
            .toSortedMap()
            .values
            .map { WordsSubEncoder(it) }
    }
}

object WordsSubEncoderInstances9 {
    val instances: List<WordsSubEncoder> by lazy {
        listOf(WordsSubEncoder(loadWords("words_9.txt")))
    }
}

object WordsSubEncoderInstances10 {
    val instances: List<WordsSubEncoder> by lazy {
        listOf(WordsSubEncoder(loadWords("words_10.txt")))
    }
}

private fun loadWords(filename: String): List<String> {
    val stream = WordsSubEncoderInstances::class.java.classLoader!!.getResourceAsStream(filename)
        ?: throw IllegalStateException("Resource not found: $filename")
    return stream.bufferedReader().useLines { it.toList() }
}
