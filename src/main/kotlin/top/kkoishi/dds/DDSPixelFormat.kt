package top.kkoishi.dds

import top.kkoishi.dds.InternalUtils.bitsToSet
import top.kkoishi.dds.InternalUtils.verify
import top.kkoishi.dds.InternalUtils.verifyNot
import java.io.DataInputStream
import java.io.IOException
import java.lang.Integer.reverseBytes
import java.util.function.IntSupplier
import kotlin.jvm.Throws

class DDSPixelFormat : DDSReadable {
    companion object {
        const val DW_SIZE = 32
        const val DW_FOUR_CC_SIZE = 4

        /**
         * DX10 value for dwFourCC as bytes ('D' 'X' '1' '0')
         */
        val DXGI_DX10_DW_FOUR_CC = byteArrayOf(0x44, 0x58, 0x31, 0x30) // 'D', 'X', '1', '0'
    }

    var dwSize: Int = 0
        private set(value) {
            field = value
        }
    private lateinit var _dwFlags: Set<DWFlags>
    var dwFlags: Set<DWFlags>
        get() = _dwFlags
        private set(value) {
            _dwFlags = value
        }
    private lateinit var _dwFourCC: ByteArray
    var dwFourCC: ByteArray
        get() = _dwFourCC
        private set(value) {
            _dwFourCC = value
        }
    var dwRGBBitCount: Int = 0
        private set(value) {
            field = value
        }
    var dwRBitMask: Int = 0
        private set(value) {
            field = value
        }
    var dwGBitMask: Int = 0
        private set(value) {
            field = value
        }
    var dwBBitMask: Int = 0
        private set(value) {
            field = value
        }
    var dwABitMask: Int = 0
        private set(value) {
            field = value
        }

    @Throws(DDSInvalidException::class)
    override fun validate() {
        verify("Invalid DDSPixelFormat: dwSize should be $DW_SIZE, but got $dwSize") {
            dwSize == DW_SIZE
        }
        verifyNot("Invalid DDSPixelFormat: dwFlags cannot be empty") {
            dwFlags.isEmpty()
        }
    }

    @Throws(IOException::class)
    override fun read(ins: DataInputStream) {
        dwSize = reverseBytes(ins.readInt())
        dwFlags = reverseBytes(ins.readInt()).bitsToSet()
        dwFourCC = ByteArray(DW_FOUR_CC_SIZE)
        ins.readFully(dwFourCC)
        dwRGBBitCount = reverseBytes(ins.readInt())
        dwRBitMask = reverseBytes(ins.readInt())
        dwGBitMask = reverseBytes(ins.readInt())
        dwBBitMask = reverseBytes(ins.readInt())
        dwABitMask = reverseBytes(ins.readInt())
        validate()
    }

    fun isDX10HeaderPresent(): Boolean {
        return dwFlags.contains(DWFlags.DDPF_FOURCC) && dwFourCC.contentEquals(DXGI_DX10_DW_FOUR_CC)
    }

    override fun toString(): String {
        return "DDSPixelFormat(dwSize=$dwSize, dwRGBBitCount=$dwRGBBitCount, dwRBitMask=$dwRBitMask, dwGBitMask=$dwGBitMask, dwBBitMask=$dwBBitMask, dwABitMask=$dwABitMask, dwFlags=$dwFlags, dwFourCC=${dwFourCC.contentToString()})"
    }

    enum class DWFlags(val bits: Int) : IntSupplier {
        DDPF_ALPHAPIXELS(0x1),
        DDPF_ALPHA(0x2),
        DDPF_FOURCC(0x4),
        DDPF_RGB(0x40),
        DDPF_YUV(0x200),
        DDPF_LUMINANCE(0x20000);

        override fun getAsInt(): Int = bits
    }
}