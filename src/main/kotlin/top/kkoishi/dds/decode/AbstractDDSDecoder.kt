package top.kkoishi.dds.decode

import top.kkoishi.dds.DDS
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport

abstract class AbstractDDSDecoder(protected var dds: DDS) : DDSDecoder {
    protected var curLine: Int = 0
    protected val maxLine: Int
    protected val lineWidth: Int
    protected val lineByteSize: Int
    protected val lineByteCache: ByteBuffer
    protected val linePixelsIntCache: IntBuffer
    protected var pos: Int = 0

    init {
        val header = dds.header
        maxLine = header.dwHeight
        lineWidth = header.dwWidth
        lineByteSize = header.ddspf.dwRGBBitCount / 8 * lineWidth
        lineByteCache = ByteBuffer.allocate(lineByteSize)
        lineByteCache.order(ByteOrder.nativeOrder())
        linePixelsIntCache = lineByteCache.asIntBuffer()
    }

    /**
     * Load the next line into the byte cache.
     */
    protected fun loadNextLine() {
        lineByteCache.rewind()
        lineByteCache.put(dds.bdata, pos, lineByteSize)
        lineByteCache.flip()
        linePixelsIntCache.rewind()
        pos += lineByteSize
    }

    override fun spliterator(): Spliterator<IntArray> = Spliterators.spliterator(
        iterator(),
        maxLine.toLong(),
        Spliterator.SORTED or Spliterator.SIZED or Spliterator.ORDERED or Spliterator.IMMUTABLE
    )

    override fun dataLineStream(): Stream<IntArray> = StreamSupport.stream(spliterator(), false)

    override fun dataLineIterator(): Iterator<IntArray> = DDSLineIterator()

    /**
     * Internal DDS Iterator.
     */
    private inner class DDSLineIterator : Iterator<IntArray> {
        override fun hasNext(): Boolean = curLine < maxLine

        override fun next(): IntArray = decodeDataLine()
    }
}
