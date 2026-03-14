package org.lapka.sms.encrypted_data_encoder.text_encoder

object WordsSubEncoderInstancesEn {
    val instances: List<WordsSubEncoder> by lazy {
        val words = loadWordsEn("words_base_en.txt")
        words.groupBy { it.length }
            .toSortedMap()
            .values
            .map { WordsSubEncoder(it) }
    }
}

object WordsSubEncoderInstancesEn9 {
    val instances: List<WordsSubEncoder> by lazy {
        listOf(WordsSubEncoder(loadWordsEn("words_9_en.txt")))
    }
}

object WordsSubEncoderInstancesEn10 {
    val instances: List<WordsSubEncoder> by lazy {
        listOf(WordsSubEncoder(loadWordsEn("words_10_en.txt")))
    }
}

private fun loadWordsEn(filename: String): List<String> {
    val stream = WordsSubEncoderInstancesEn::class.java.classLoader!!.getResourceAsStream(filename)
        ?: throw IllegalStateException("Resource not found: $filename")
    return stream.bufferedReader().useLines { it.toList() }
}
