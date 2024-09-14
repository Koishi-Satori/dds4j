package top.kkoishi.dds.decode.compress

import top.kkoishi.dds.DDSRef
import top.kkoishi.dds.decode.compress.DXGIBCUtils.DXGI_FORMAT_BC1_DXT1_BLOCK_SIZE_BYTES

/**
 * The decoder for BC1/DXT1 compressed dds pixels data.
 *
 * [Block Compression (Direct3D 10)](https://learn.microsoft.com/en-us/windows/win32/direct3d10/d3d10-graphics-programming-guide-resources-block-compression)
 */
class DXT1DDSDecoder(dds: DDSRef) : AbstractDXTDDSDecoder(dds, DXGI_FORMAT_BC1_DXT1_BLOCK_SIZE_BYTES) {
    override fun decodeDataLine(): IntArray {
        if (curLine >= maxLine)
            throw NoSuchElementException("No more line or data blocks.")
        val subLine = curLine % 4
        if (subLine == 0) {
            loadNextBlocksLine()
            for (index in 0 until blockWidth)
                DXGIBCUtils.decompressBC1DXT1DataBlock(blocksLineCache, index * blockByteSize, blocksCache, index * 16)
        }
        val line = IntArray(lineWidth)
        //  decoded blocks are stored in 4x4 blocks, like [r0, r1, r2, r3] * 4.
        //  so to get the nth row from each block, we skip (row * 4) entries,
        //  read 4 entries, skip 16 entries, until we've read enough.
        var p = subLine * 4
        for (i in 0 until blockWidth) {
            System.arraycopy(blocksCache, p, line, i * 4, 4)
            p += 16
        }
        curLine++
        return line
    }
}
