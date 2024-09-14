package top.kkoishi.dds

import top.kkoishi.dds.InternalUtils.verify
import top.kkoishi.dds.decode.compress.DXGIBCUtils.DXGI_FORMAT_BC1_DXT1_BLOCK_SIZE_BYTES
import top.kkoishi.dds.decode.compress.DXGIBCUtils.DXGI_FORMAT_BC2_DXT3_BLOCK_SIZE_BYTES
import java.io.DataInputStream
import java.lang.Integer.reverseBytes
import java.util.EnumSet

class DDSRef : DDSReadable {
    companion object {
        /**
         * DDS Magic Number: 'D', 'D', 'S', ' '.
         */
        @JvmStatic
        val DDS_MAGIC_NUMBER = reverseBytes(0x44445320)

        @JvmStatic
        val BAD_DW_FOUR_CC_STR = "${Char(0)}${Char(0)}${Char(0)}${Char(0)}"
    }

    /**
     * The magic number of a DDS file.
     */
    var dwMagic = 0
        private set(value) {
            field = value
        }

    /**
     * The DDS file header.
     */
    private lateinit var _header: DDSHeader
    var header: DDSHeader
        get() = _header
        private set(value) {
            _header = value
        }

    /**
     * The pixels data of a DDS file.
     */
    private lateinit var _bdata: ByteArray
    var bdata: ByteArray
        get() = _bdata
        private set(value) {
            _bdata = value
        }

    /**
     * unused.
     */
    private lateinit var _bdata2: ByteArray
    var bdata2: ByteArray
        get() = _bdata2
        private set(value) {
            _bdata2 = value
        }

    override fun validate() {
        verify("Invalid DDS: Magic Number is not 'DDS '") {
            dwMagic == DDS_MAGIC_NUMBER
        }
        header.validate()
    }

    override fun read(ins: DataInputStream) {
        dwMagic = reverseBytes(ins.readInt())
        header = DDSHeader()
        header.read(ins)
        bdata = if (header.dwFlags.contains(DDSHeader.DWFlags.DDSD_LINEARSIZE))
            ByteArray(calcCompressedDataSize())
        else
            ByteArray(calcDataSize())
        ins.readFully(bdata)
        validate()
    }

    private fun calcDataSize(): Int {
        val numPixels = header.dwWidth * header.dwHeight
        val dwFlags = header.ddspf.dwFlags
        if (dwFlags.contains(DDSPixelFormat.DWFlags.DDPF_FOURCC))
            return calcCompressedDataSize()
        var bytesPerPixel = 0
        if (dwFlags.contains(DDSPixelFormat.DWFlags.DDPF_ALPHA))
            bytesPerPixel = 1
        val validRGBBits = EnumSet.of(
            DDSPixelFormat.DWFlags.DDPF_LUMINANCE,
            DDSPixelFormat.DWFlags.DDPF_YUV,
            DDSPixelFormat.DWFlags.DDPF_RGB
        )
        validRGBBits.retainAll(dwFlags)
        if (validRGBBits.isNotEmpty())
            bytesPerPixel = header.ddspf.dwRGBBitCount / 8
        return numPixels * bytesPerPixel
    }

    private fun calcCompressedDataSize(): Int {
        // turns out the pitchOrLinearSize field is actually unreliable,
        // so we have to calculate out the size ourselves
        val pixelFormat = header.ddspf
        val dwFlags = pixelFormat.dwFlags
        if (dwFlags.contains(DDSPixelFormat.DWFlags.DDPF_FOURCC)) {
            val blockSize = calcBlockSize(String(pixelFormat.dwFourCC))
            // blocks are 4x4, so determine the dimension in blocks
            val width = header.dwWidth
            val height = header.dwHeight
            // from integer division floors, we must round up to the nearest multiple of 4
            val blockWidth = (width + 3) / 4
            val blockHeight = (height + 3) / 4
            return blockWidth * blockHeight * blockSize
        }
        // for now fall back on what they give us.
        // TODO: handle the odd corner cases
        return header.dwPitchOrLinearSize
    }

    /**
     * Calc the BC/DXT compressed block size.
     *
     * The block size of BC1/DXT1 is eight.
     *
     * And the block size of BC2 and BC3 is 16.
     *
     * @param dwFourCC [dwFourCC]
     * @return the compressed block size.
     * @see top.kkoishi.dds.decode.compress.DXGIBCUtils.DXGI_FORMAT_BC1_DXT1_BLOCK_SIZE_BYTES
     * @see top.kkoishi.dds.decode.compress.DXGIBCUtils.DXGI_FORMAT_BC2_DXT3_BLOCK_SIZE_BYTES
     * @see top.kkoishi.dds.decode.compress.DXGIBCUtils.DXGI_FORMAT_BC3_DXT5_BLOCK_SIZE_BYTES
     */
    private fun calcBlockSize(dwFourCC: String): Int = when (dwFourCC) {
        BAD_DW_FOUR_CC_STR -> throw IllegalArgumentException(
            "The provided DDS file has DDPF_FOURCC flag set but no dwFourCC"
        )

        "DXT1" -> DXGI_FORMAT_BC1_DXT1_BLOCK_SIZE_BYTES
        else -> DXGI_FORMAT_BC2_DXT3_BLOCK_SIZE_BYTES
    }

    override fun toString(): String {
        return "DDS(dwMagic=$dwMagic, header=$header, bdata_size=${bdata.size})"
    }
}
