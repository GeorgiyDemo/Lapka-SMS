package com.moez.QKSMS.extensions

import org.junit.Test

class StringExtensionsTest {

    @Test
    fun `removeAccents strips acute accents`() {
        assert("cafe".removeAccents() == "cafe")
        assert("caf\u00e9".removeAccents() == "cafe")
    }

    @Test
    fun `removeAccents strips grave accents`() {
        assert("\u00e8".removeAccents() == "e")
        assert("\u00e0".removeAccents() == "a")
    }

    @Test
    fun `removeAccents strips circumflex`() {
        assert("\u00ea".removeAccents() == "e")
        assert("\u00f4".removeAccents() == "o")
    }

    @Test
    fun `removeAccents strips dieresis`() {
        assert("\u00fc".removeAccents() == "u")
        assert("\u00f6".removeAccents() == "o")
        assert("\u00e4".removeAccents() == "a")
    }

    @Test
    fun `removeAccents strips tilde`() {
        assert("\u00f1".removeAccents() == "n")
    }

    @Test
    fun `removeAccents handles mixed text`() {
        val input = "R\u00e9sum\u00e9 na\u00efve caf\u00e9"
        val expected = "Resume naive cafe"
        assert(input.removeAccents() == expected) { "Got: ${input.removeAccents()}" }
    }

    @Test
    fun `removeAccents preserves plain ASCII`() {
        val input = "Hello World 123!@#"
        assert(input.removeAccents() == input)
    }

    @Test
    fun `removeAccents handles empty string`() {
        assert("".removeAccents() == "")
    }

    @Test
    fun `removeAccents handles all accented characters`() {
        val input = "\u00c0\u00c1\u00c2\u00c3\u00c4\u00c5"
        val expected = "AAAAAA"
        assert(input.removeAccents() == expected) { "Got: ${input.removeAccents()}" }
    }

    @Test
    fun `removeAccents works on CharSequence`() {
        val cs: CharSequence = StringBuilder("caf\u00e9")
        assert(cs.removeAccents() == "cafe")
    }
}
