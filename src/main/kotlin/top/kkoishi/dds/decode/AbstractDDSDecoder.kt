package top.kkoishi.dds.decode

import top.kkoishi.dds.DDSRef
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport

abstract class AbstractDDSDecoder(protected var dds: DDSRef) : DDSDecoder {
    /**
     * The Current **data line** the decoder has reached.
     *
     * @see decodeDataLine
     */
    protected var curLine: Int = 0

    /**
     * The max **data line**, equal to [top.kkoishi.dds.DDSHeader.dwHeight].
     *
     * @see decodeDataLine
     * @see top.kkoishi.dds.DDSHeader.dwHeight
     */
    protected val maxLine: Int

    /**
     * The **data line** width in int, actually is the size of every "row" array
     * in the data two-dimensional array, and equal to [top.kkoishi.dds.DDSHeader.dwWidth].
     *
     * @see decodeDataLine
     * @see top.kkoishi.dds.DDSHeader.dwWidth
     */
    protected val lineWidth: Int

    /**
     * The **data line** width in int, actually is the bits size of every "row" array
     * in the data two-dimensional array, and equal to
     * [top.kkoishi.dds.DDSHeader.dwWidth] * [top.kkoishi.dds.DDSPixelFormat.dwRGBBitCount].
     *
     * @see decodeDataLine
     * @see top.kkoishi.dds.DDSHeader.dwWidth
     * @see top.kkoishi.dds.DDSHeader.ddspf
     * @see top.kkoishi.dds.DDSPixelFormat.dwRGBBitCount
     */
    protected val lineByteSize: Int
    protected val lineByteCache: ByteBuffer
    protected val linePixelsIntCache: IntBuffer

    /**
     * The Current position (of [DDSRef.bdata]) the decoder has reached.
     *
     * @see top.kkoishi.dds.DDSRef.bdata
     * @see top.kkoishi.dds.DDSRef.bdata2
     */
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
     * Load the next data line into the byte cache.
     *
     * @see decodeDataLine
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
