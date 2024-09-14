package top.kkoishi.dds.decode.compress

internal object DXGIBCUtils {
    const val DXGI_FORMAT_BC1_DXT1_BLOCK_SIZE_BYTES = 8
    const val DXGI_FORMAT_BC2_DXT3_BLOCK_SIZE_BYTES = 16
    const val DXGI_FORMAT_BC3_DXT5_BLOCK_SIZE_BYTES = 16
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
     * A BC1/DXT1 block is eight bytes long, and the decompressed one is 4*4 pixels.
     *
     * RGB 565 reference: [MSDN-Working with 16-bit rgb](https://learn.microsoft.com/windows/win32/directshow/working-with-16-bit-rgb)
     *
     * @param dataBlock the data block to be decompressed.
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

    private fun ByteArray.getShort(offset: Int): Int =
        ((this[offset + 1].toInt() shl 8) and 0x0000FF00) or (this[offset].toInt() and 0x000000FF)
}
