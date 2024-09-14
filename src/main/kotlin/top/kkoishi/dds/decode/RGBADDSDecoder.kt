package top.kkoishi.dds.top.kkoishi.dds.decode

import top.kkoishi.dds.DDS
import top.kkoishi.dds.decode.AbstractDDSDecoder
import java.lang.Integer.numberOfTrailingZeros

class RGBADDSDecoder(dds: DDS) : AbstractDDSDecoder(dds) {
    private val rBitMask: Int
    private val gBitMask: Int
    private val bBitMask: Int
    private val aBitMask: Int
    private val rBitShift: Int
    private val gBitShift: Int
    private val bBitShift: Int
    private val aBitShift: Int

    private val skipShifting: Boolean

    init {
        val pixelFormat = dds.header.ddspf
        rBitMask = pixelFormat.dwRBitMask
        gBitMask = pixelFormat.dwGBitMask
        bBitMask = pixelFormat.dwBBitMask
        aBitMask = pixelFormat.dwABitMask
        // if DDS is already in RGBA format,
        // we can skip further processing.
        // -0x1000000 = 0xFF000000
        skipShifting = (aBitMask == -0x1000000)
                && (rBitMask == 0x00FF0000)
                && (gBitMask == 0x0000FF00)
                && (bBitMask == 0x000000FF)
        rBitShift = numberOfTrailingZeros(rBitMask)
        gBitShift = numberOfTrailingZeros(gBitMask)
        bBitShift = numberOfTrailingZeros(bBitMask)
        aBitShift = numberOfTrailingZeros(aBitMask)
    }

    override fun decodeDataLine(): IntArray {
        if (curLine >= maxLine)
            throw NoSuchElementException("No more data line.")
        loadNextLine()
        val line = IntArray(lineWidth)
        if (skipShifting)
            linePixelsIntCache.get(line)
        else {
            var pixel: Int
            for (index in 0 until lineWidth) {
                pixel = linePixelsIntCache.get()
                // calc the pixel RGBA.
                // aBitShift = trailing zeros of dwABitMask.
                // a = (pixel & dwABitMask) >> aBitShift << 24.
                line[index] = (((pixel and aBitMask) shr aBitShift and 0xFF) shl 24) or
                        (((pixel and rBitMask) shr rBitShift and 0xFF) shl 16) or
                        (((pixel and gBitMask) shr gBitShift and 0xFF) shl 8) or
                        ((pixel and bBitMask) shr bBitShift and 0xFF)
            }
        }
        curLine++
        return line
    }
}