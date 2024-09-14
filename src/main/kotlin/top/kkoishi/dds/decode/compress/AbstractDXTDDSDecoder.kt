package top.kkoishi.dds.decode.compress

import top.kkoishi.dds.DDS
import top.kkoishi.dds.decode.DDSDecoder
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * DDS Decoder, used to decode DXT compressed [top.kkoishi.dds.DDS.bdata].
 *
 * DXT Compress reference: [MSDN-Block Compress(DX10)](https://learn.microsoft.com/windows/win32/direct3d10/d3d10-graphics-programming-guide-resources-block-compression)
 *
 * The decoder can read the pixels data as **data lines**.
 *
 * @see decodeDataLine
 */
abstract class AbstractDXTDDSDecoder(protected var dds: DDS, protected val blockByteSize: Int) : DDSDecoder {
    /**
     * The Current **data line** the decoder has reached.
     *
     * @see decodeDataLine
     */
    protected var curLine: Int = 0

    /**
     * The current data block the decoder has reached.
     *
     * @see decodeDataLine
     */
    protected var curBlock: Int = 0

    /**
     * The max **data line**, equal to [top.kkoishi.dds.DDSHeader.dwHeight].
     *
     * @see decodeDataLine
     * @see top.kkoishi.dds.DDSHeader.dwHeight
     */
    protected val maxLine: Int

    /**
     * The max **data block**, equal to ([maxLine]+3)/4.
     */
    protected val maxBlock: Int

    /**
     * The **data line** width in int, actually is the size of every "row" array
     * in the data two-dimensional array, and equal to [top.kkoishi.dds.DDSHeader.dwWidth].
     *
     * @see decodeDataLine
     * @see top.kkoishi.dds.DDSHeader.dwWidth
     */
    protected val lineWidth: Int

    protected val blockWidth: Int
    protected val blockBytes: Int
    protected val blocksCache: IntArray
    protected val blocksLineCache: ByteArray

    /**
     * The Current position (of [DDS.bdata]) the decoder has reached.
     *
     * @see top.kkoishi.dds.DDS.bdata
     * @see top.kkoishi.dds.DDS.bdata2
     */
    protected var pos: Int = 0

    init {
        val header = dds.header
        maxLine = header.dwHeight
        lineWidth = header.dwWidth
        maxBlock = (maxLine + 3) / 4
        blockWidth = (lineWidth + 3) / 4
        blockBytes = blockByteSize * blockWidth
        blocksCache = IntArray(16 * blockWidth)
        blocksLineCache = ByteArray(blockBytes)
    }

    /**
     * Load the next line of data blocks into the byte cache.
     *
     * @see decodeDataLine
     */
    fun loadNextBlocksLine() {
        System.arraycopy(dds.bdata, pos, blocksLineCache, 0, blockBytes)
        pos += blockBytes
        curBlock++
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
