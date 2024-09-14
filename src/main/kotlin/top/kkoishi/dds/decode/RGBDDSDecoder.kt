package top.kkoishi.dds.decode

import top.kkoishi.dds.DDS
import java.lang.Integer.numberOfTrailingZeros

class RGBDDSDecoder(dds: DDS) : AbstractDDSDecoder(dds) {
    private val rBitMask: Int
    private val gBitMask: Int
    private val bBitMask: Int
    private val rBitShift: Int
    private val gBitShift: Int
    private val bBitShift: Int

    /**
     * @see top.kkoishi.dds.DDSPixelFormat.dwRGBBitCount
     */
    private val rgbBitCount: Int

    /**
     * if DDS is already in RGB format, we can skip further processing.
     */
    private val skipShifting: Boolean

    init {
        val pixelFormat = dds.header.ddspf
        rBitMask = pixelFormat.dwRBitMask
        gBitMask = pixelFormat.dwGBitMask
        bBitMask = pixelFormat.dwBBitMask
        rgbBitCount = pixelFormat.dwRGBBitCount
        // if DDS is already in RGBS format,
        // we can skip further processing.
        skipShifting = (rBitMask == 0x00FF0000)
                && (gBitMask == 0x0000FF00)
                && (bBitMask == 0x000000FF)
        rBitShift = numberOfTrailingZeros(rBitMask)
        gBitShift = numberOfTrailingZeros(gBitMask)
        bBitShift = numberOfTrailingZeros(bBitMask)
    }

    override fun decodeDataLine(): IntArray {
        if (curLine >= maxLine)
            throw NoSuchElementException("No more data line.")
        loadNextLine()
        val line = IntArray(lineWidth)
        if (rgbBitCount != 24)
            throw UnsupportedOperationException("Unsupported dwRGBBitCount: $rgbBitCount")
        val pixel = ByteArray(3)
        if (skipShifting) {
            for (index in 0 until lineWidth)
                line[index] = read3BytesPixelAsInt(pixel)
        } else {
            var pixelInt: Int
            for (index in 0 until lineWidth) {
                pixelInt = read3BytesPixelAsInt(pixel)
                line[index] = (((pixelInt and rBitMask) shr rBitShift and 0xFF) shl 16) or
                        (((pixelInt and gBitMask) shr gBitShift and 0xFF) shl 8) or
                        ((pixelInt and bBitMask) shr bBitShift and 0xFF)
            }
        }
        return line
    }

    private fun read3BytesPixelAsInt(pixel: ByteArray): Int {
        lineByteCache.get(pixel)
        // pixel[2] = r, pixel[1] = g, pixel[0] = b
        return -0x1000000 or (((pixel[2].toInt() and 0xFF) shl 16) or
                ((pixel[1].toInt() and 0xFF) shl 8) or
                (pixel[2].toInt() and 0xFF))
    }
}
