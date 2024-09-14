package top.kkoishi.dds.decode

import ar.com.hjg.pngj.ImageInfo
import ar.com.hjg.pngj.ImageLineHelper
import ar.com.hjg.pngj.ImageLineInt
import ar.com.hjg.pngj.PngWriter
import top.kkoishi.dds.DDSPixelFormat
import top.kkoishi.dds.DDSRef
import top.kkoishi.dds.decode.compress.DXT1DDSDecoder
import top.kkoishi.dds.decode.compress.DXT3DSSDecoder
import top.kkoishi.dds.decode.compress.DXT5DDSDecoder
import java.io.OutputStream
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * DDS Decoder, used to decode [top.kkoishi.dds.DDSRef.bdata].
 *
 * The decoder can read the pixels data as **data lines**.
 *
 * @see decodeDataLine
 */
interface DDSDecoder : Iterable<IntArray> {
    companion object {
        @JvmStatic
        fun DDSRef.decoder() = createDecoder(this)

        @JvmStatic
        fun DDSRef.decodeToPng(oos: OutputStream, autoFlush: Boolean = false, autoClose: Boolean = false) {
            val header = this.header
            val decoder: DDSDecoder = this.decoder()
            val imageInfo = ImageInfo(header.dwWidth, header.dwHeight, 8, true)
            val writer = PngWriter(oos, imageInfo)
            val line = ImageLineInt(imageInfo)
            decoder.forEach {
                ImageLineHelper.setPixelsRGBA8(line, it)
                writer.writeRow(line)
            }
            writer.end()
            if (autoFlush)
                oos.flush()
            if (autoClose)
                oos.close()
        }

        @JvmStatic
        fun createDecoder(dds: DDSRef): DDSDecoder {
            val pixelFormat = dds.header.ddspf
            val dwFlags = pixelFormat.dwFlags
            when {
                dwFlags.contains(DDSPixelFormat.DWFlags.DDPF_FOURCC) -> {
                    // compressed texture, use BC/DXT decoder
                    return when (String(pixelFormat.dwFourCC)) {
                        DDSRef.BAD_DW_FOUR_CC_STR ->
                            throw IllegalArgumentException("The provided DDS file has DDPF_FOURCC flag set but no dwFourCC")

                        "DXT1" -> DXT1DDSDecoder(dds)
                        "DXT3" -> DXT3DSSDecoder(dds)
                        "DXT5" -> DXT5DDSDecoder(dds)
                        else ->
                            throw IllegalArgumentException(
                                "No valid decoder for the dwFourCC of the provided DDS file, provided: ${
                                    String(
                                        pixelFormat.dwFourCC
                                    )
                                }."
                            )
                    }
                }

                dwFlags.contains(DDSPixelFormat.DWFlags.DDPF_RGB) -> {
                    return if (dwFlags.contains(DDSPixelFormat.DWFlags.DDPF_ALPHAPIXELS))
                        RGBADDSDecoder(dds)
                    else
                        RGBDDSDecoder(dds)
                }

                else -> throw IllegalArgumentException("The provided DDS file has invalid ddspf.dwFlags: $dwFlags")
            }
        }
    }

    /**
     * Decode the next data line.
     *
     * The **data line** means a row in the pixels data two-dimensional array.
     *
     * For the DXT-Compressed [top.kkoishi.dds.DDSRef.bdata], this will decode the next line of data blocks.
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
