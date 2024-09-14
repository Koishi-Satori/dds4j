package top.kkoishi.dds.decode.compress

internal object DXGIBCUtils {
    /**
     * The block size bytes of BC1.
     */
    const val DXGI_FORMAT_BC1_DXT1_BLOCK_SIZE_BYTES = 8

    /**
     * The block size bytes of BC2.
     */
    const val DXGI_FORMAT_BC2_DXT3_BLOCK_SIZE_BYTES = 16

    /**
     * The block size bytes of BC3.
     */
    const val DXGI_FORMAT_BC3_DXT5_BLOCK_SIZE_BYTES = 16

    /**
     * The block size bytes of BC4 and BC4_UNORM.
     */
    const val DXGI_FORMAT_BC4_UNORM_BLOCK_SIZE_BYTES = 8

    /**
     * RGB565 bit mask of the red component.
     *
     * RGB 565 reference: [MSDN-Working with 16-bit rgb](https://learn.microsoft.com/windows/win32/directshow/working-with-16-bit-rgb)
     */
    const val RGB_565_RED_MASK = 0xf800

    /**
     * RGB565 bit mask of the green component.
     *
     * RGB 565 reference: [MSDN-Working with 16-bit rgb](https://learn.microsoft.com/windows/win32/directshow/working-with-16-bit-rgb)
     */
    const val RGB_565_GREEN_MASK = 0x7E0

    /**
     * RGB565 bit mask of the blue component.
     *
     * RGB 565 reference: [MSDN-Working with 16-bit rgb](https://learn.microsoft.com/windows/win32/directshow/working-with-16-bit-rgb)
     */
    const val RGB_565_BLUE_MASK = 0x1F

    /**
     * Decompress the BC1/DTX1 data block.
     *
     * A BC1/DXT1 block is eight bytes long, and the decompressed one is a 4*4 block.
     *
     * RGB 565 reference:
     * [MSDN-Working with 16-bit rgb](https://learn.microsoft.com/windows/win32/directshow/working-with-16-bit-rgb)
     *
     * @param dataBlock the data block to be decompressed.
     * @param offset the starting offset of the data block.
     * @param dest the destination int array.
     * @param destOffset the starting offset of the dest array.
     */
    @JvmStatic
    fun decompressBC1DXT1DataBlock(dataBlock: ByteArray, offset: Int, dest: IntArray, destOffset: Int) {
        var index = offset
        // get the endpoint color.
        val color1 = dataBlock.getShort(index)
        index += 2
        val color2 = dataBlock.getShort(index)
        index += 2
        // extract the RGB component colors from a pixel using the bit masks.
        var r1 = color1 and RGB_565_RED_MASK shr 11
        var g1 = color1 and RGB_565_GREEN_MASK shr 5
        var b1 = color1 and RGB_565_BLUE_MASK
        var r2 = color2 and RGB_565_RED_MASK shr 11
        var g2 = color2 and RGB_565_GREEN_MASK shr 5
        var b2 = color2 and RGB_565_BLUE_MASK
        // the red and blue channels are 5-bits and the green channel is 6 bits.
        // to convert these values to 8-bit components (for 24-bit or 32-bit RGB),
        // we must rescale them to 8-bit.
        r1 = (r1 * 0x100 / 0x20) and 0xFF
        g1 = (g1 * 0x100 / 0x40) and 0xFF
        b1 = (b1 * 0x100 / 0x20) and 0xFF
        r2 = (r2 * 0x100 / 0x20) and 0xFF
        g2 = (g2 * 0x100 / 0x40) and 0xFF
        b2 = (b2 * 0x100 / 0x20) and 0xFF
        // then we construct the ARGB colors.
        // since this is a 4*4 block, the array size is 4.
        // and we've processed the color components before,
        // so there is no need to mask them again.
        val colors = IntArray(4)
        colors[0] = -0x1000000 or (r1 shl 16) or (g1 shl 8) or b1
        colors[1] = -0x1000000 or (r2 shl 16) or (g2 shl 8) or b2
        if (color1 < color2) {
            // the components of colors[2] are all (component_1 + component_2)/2
            // and the components of colors[3] are all transparent,
            // so we set it to zero.
            colors[2] = -0x1000000 or (((r1 + r2) / 2) shl 16) or (((g1 + g2) / 2) shl 8) or ((b1 + b2) / 2)
            colors[3] = 0
        } else {
            // for this case, the components of colors[2] are all (2 * component_1 + component_2)/3,
            // and the components of colors[3] are all (component_1 + 2 * component_2)/3.
            colors[2] = -0x1000000 or (((2 * r1 + r2) / 3) shl 16) or (((2 * g1 + g2) / 3) shl 8) or ((2 * b1 + b2) / 3)
            colors[3] = -0x1000000 or (((r1 + 2 * r2) / 3) shl 16) or (((g1 + 2 * g2) / 3) shl 8) or ((b1 + 2 * b2) / 3)
        }
        for (row in 0..3) {
            val rowByte = dataBlock[index].toInt()
            index++
            // pixels are laid out with MSBit on the left, so the top left pixel is
            // the highest 2 bits in the first byte.
            for (col in 3 downTo 0)
                dest[destOffset + col + row * 4] = colors[rowByte shr 2 * col and 0x03]
        }
    }

    /**
     * Decompress the BC2/DTX3 data block.
     *
     * The BC2 format stores colors with the same number of bits and data layout as the BC1 format;
     * however, BC2 requires an additional 64-bits of memory to store the alpha data.
     * And the BC2 alpha section located after the BC1 data sections.
     *
     * RGB 565 reference:
     * [MSDN-Working with 16-bit rgb](https://learn.microsoft.com/windows/win32/directshow/working-with-16-bit-rgb)
     *
     * @param dataBlock the data block to be decompressed.
     * @param offset the starting offset of the data block.
     * @param dest the destination int array.
     * @param destOffset the starting offset of the dest array.
     */
    fun decompressBC2DXT3DataBlock(dataBlock: ByteArray, offset: Int, dest: IntArray, destOffset: Int) {
        // the BC1 data section is before the BC2 alpha data section, we decode it first.
        decompressBC1DXT1DataBlock(dataBlock, offset + 8, dest, destOffset)
        // BC2 alpha block uses 4 bit literal alpha values stored pixel-by-pixel
        for (row in 0..3) {
            val rowShort = dataBlock.getShort(offset + row * 2)
            for (col in 3 downTo 0) {
                // set the alpha data, this is a wizardry.
                dest[destOffset + col + row * 4] = dest[destOffset + col + row * 4] and
                        0x00FFFFFF or (rowShort shr 4 * col and 0xF) shl 28
            }
        }
    }

    /**
     * Decompress the BC3/DTX5 data block.
     *
     * The BC3 format is based on BC1 and BC2,
     * it handles alpha by storing two reference values and interpolating between them
     * (similarly to how BC1 stores RGB color).
     *
     * The algorithm works on 4×4 blocks of texels.
     * Instead of storing 16 alpha values, the algorithm stores 2 reference alphas (alpha_0 and alpha_1)
     * and 16 3-bit color indexes (alpha a through p).
     *
     * The example given by the
     * [reference](https://learn.microsoft.com/en-us/windows/win32/direct3d10/d3d10-graphics-programming-guide-resources-block-compression)
     * ```c++
     * if( alpha_0 > alpha_1 )
     * {
     *   // 6 interpolated alpha values.
     *   alpha_2 = 6/7*alpha_0 + 1/7*alpha_1; // bit code 010
     *   alpha_3 = 5/7*alpha_0 + 2/7*alpha_1; // bit code 011
     *   alpha_4 = 4/7*alpha_0 + 3/7*alpha_1; // bit code 100
     *   alpha_5 = 3/7*alpha_0 + 4/7*alpha_1; // bit code 101
     *   alpha_6 = 2/7*alpha_0 + 5/7*alpha_1; // bit code 110
     *   alpha_7 = 1/7*alpha_0 + 6/7*alpha_1; // bit code 111
     * }
     * else
     * {
     *   // 4 interpolated alpha values.
     *   alpha_2 = 4/5*alpha_0 + 1/5*alpha_1; // bit code 010
     *   alpha_3 = 3/5*alpha_0 + 2/5*alpha_1; // bit code 011
     *   alpha_4 = 2/5*alpha_0 + 3/5*alpha_1; // bit code 100
     *   alpha_5 = 1/5*alpha_0 + 4/5*alpha_1; // bit code 101
     *   alpha_6 = 0;                         // bit code 110
     *   alpha_7 = 255;                       // bit code 111
     * }
     * ```
     *
     * RGB 565 reference:
     * [MSDN-Working with 16-bit rgb](https://learn.microsoft.com/windows/win32/directshow/working-with-16-bit-rgb)
     *
     * @param dataBlock the data block to be decompressed.
     * @param offset the starting offset of the data block.
     * @param dest the destination int array.
     * @param destOffset the starting offset of the dest array.
     */
    fun decompressBC3DXT5DataBlock(dataBlock: ByteArray, offset: Int, dest: IntArray, destOffset: Int) {
        // the BC1 data section is before the BC2 alpha data section, we decode it first.
        decompressBC1DXT1DataBlock(dataBlock, offset + 8, dest, destOffset)
        // The BC3 format uses the alpha indexes(a–p) to look up the original colors
        // from a lookup table that contains 8 values.
        // The first two values—alpha_0 and alpha_1—are the minimum and maximum values;
        // the other six intermediate values are calculated using linear interpolation.
        //
        // The algorithm determines the number of interpolated alpha values by examining
        // the two reference alpha values.
        // If alpha_0 is greater than alpha_1, then BC3 interpolates 6 alpha values;
        // otherwise, it interpolates 4. When BC3 interpolates only 4 alpha values,
        // it sets two additional alpha values(0 for fully transparent and 255 for fully opaque).
        // BC3 compresses the alpha values in the 4×4 texel area by storing the bit code
        // corresponding to the interpolated alpha values which most closely matches
        // the original alpha for a given texel.
        val alpha = IntArray(8)
        var index = offset
        val alpha0 = dataBlock[index++].toInt() and 0xFF
        val alpha1 = dataBlock[index++].toInt() and 0xFF
        alpha[0] = alpha0
        alpha[1] = alpha1
        if (alpha0 > alpha1) {
            // 6 interpolated alpha values
            alpha[2] = alpha0 * 6 / 7 + alpha1 / 7 // bit code 010
            alpha[3] = alpha0 * 5 / 7 + alpha1 * 2 / 7 // bit code 011
            alpha[4] = alpha0 * 4 / 7 + alpha1 * 3 / 7 // bit code 100
            alpha[5] = alpha0 * 3 / 7 + alpha1 * 4 / 7 // bit code 101
            alpha[6] = alpha0 * 2 / 7 + alpha1 * 5 / 7 // bit code 110
            alpha[7] = alpha0 / 7 + alpha1 * 6 / 7 // bit code 111
        } else {
            // 4 interpolated alpha values
            alpha[2] = alpha0 * 4 / 5 + alpha1 / 5 // bit code 010
            alpha[3] = alpha0 * 3 / 5 + alpha1 * 2 / 5 // bit code 011
            alpha[4] = alpha0 * 2 / 5 + alpha1 * 3 / 5 // bit code 100
            alpha[5] = alpha0 / 5 + alpha1 * 4 / 5 // bit code 101
            alpha[6] = 0 // bit code 110
            alpha[7] = 255 // bit code 111
        }
        // convert the alpha section, to be more specific,
        // this is the reference alphas (alpha_0 and alpha_1)
        val bits = convertBC3AlphaSection(dataBlock, index)
        for (j in 0..15) {
            // put the alpha data into the dest, we can get the alpha bits
            // using alpha indexes and the two reference alphas.
            dest[destOffset + j] = dest[destOffset + j] and 0x00FFFFFF or alpha[bits[j]] shl 24 and -0x1000000
        }
    }

    private fun convertBC3AlphaSection(data: ByteArray, offset: Int): IntArray {
        // this is similarly to how BC1 stores RGB color.
        val alphaRow1 = (data[offset + 2].toInt() shl 16) and 0xFF0000 or
                (data[offset + 1].toInt() shl 8) and 0x00FF00 or
                data[offset].toInt() and 0x0000FF
        val alphaRow2 = (data[offset + 5].toInt() shl 16) and 0xFF0000 or
                (data[offset + 4].toInt() shl 8) and 0x00FF00 or
                data[offset + 3].toInt() and 0x0000FF
        val bits = IntArray(16)
        for (index in 7 downTo 0) {
            bits[index] = alphaRow1 shr 3 * index and 0x07
            bits[index + 8] = alphaRow2 shr 3 * index and 0x07
        }
        return bits
    }

    private fun ByteArray.getShort(offset: Int): Int =
        ((this[offset + 1].toInt() shl 8) and 0x0000FF00) or (this[offset].toInt() and 0x000000FF)
}
