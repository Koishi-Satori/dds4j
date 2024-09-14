package top.kkoishi.dds.decode

import java.util.function.Consumer
import java.util.stream.Stream

/**
 * DDS Decoder, used to decode **DDS.bdata**.
 */
interface DDSDecoder : Iterable<IntArray> {
    /**
     * Decode the next data line.
     */
    fun decodeDataLine(): IntArray

    fun dataLineIterator(): Iterator<IntArray>

    fun dataLineStream(): Stream<IntArray>

    override fun forEach(action: Consumer<in IntArray>) {
        dataLineStream().forEach(action)
    }

    override fun iterator(): Iterator<IntArray> = dataLineIterator()
}
