package top.kkoishi.dds.decode

import java.util.function.Consumer
import java.util.stream.Stream

/**
 * DDS Decoder, used to decode [top.kkoishi.dds.DDS.bdata].
 *
 * The decoder can read the pixels data as **data lines**.
 *
 * @see decodeDataLine
 */
interface DDSDecoder : Iterable<IntArray> {
    /**
     * Decode the next data line.
     *
     * The **data line** means a row in the pixels data two-dimensional array.
     *
     * For the DXT-Compressed [top.kkoishi.dds.DDS.bdata], this will decode the next line of data blocks.
     *
     * @return next line.
     */
    fun decodeDataLine(): IntArray

    fun dataLineIterator(): Iterator<IntArray>

    fun dataLineStream(): Stream<IntArray>

    override fun forEach(action: Consumer<in IntArray>) {
        dataLineStream().forEach(action)
    }

    override fun iterator(): Iterator<IntArray> = dataLineIterator()
}
