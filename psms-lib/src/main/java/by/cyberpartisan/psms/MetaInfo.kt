package by.cyberpartisan.psms

class MetaInfo(
    val mode: Int,
    val version: Int,
    val isChannel: Boolean
) {
    fun toByte(): Byte = (mode or (version shl 4) or (if (isChannel) 1 shl 7 else 0)).toByte()

    companion object {
        fun parse(metaInfoByte: Byte): MetaInfo {
            val b = metaInfoByte.toInt()
            val mode = b and 0x0F
            val version = (b and 0x70) shr 4
            val isChannel = (b and 0x80) != 0
            return MetaInfo(mode, version, isChannel)
        }
    }
}
