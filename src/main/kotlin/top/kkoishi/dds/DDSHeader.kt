package top.kkoishi.dds

import top.kkoishi.dds.InternalUtils.bitsToSet
import top.kkoishi.dds.InternalUtils.verify
import java.io.DataInputStream
import java.io.IOException
import java.lang.Integer.reverseBytes
import java.util.*
import java.util.function.IntSupplier
import kotlin.jvm.Throws

@Suppress("RedundantSetter")
class DDSHeader : DDSReadable {
    companion object {
        const val DW_SIZE = 124
        const val DW_RESERVED_1_SIZE = 11

        @JvmStatic
        val DW_RESERVED_1 = IntArray(DW_RESERVED_1_SIZE)
    }

    /**
     * The size of the header, it should be 124.
     */
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
    var dwHeight: Int = 0
        private set(value) {
            field = value
        }
    var dwWidth: Int = 0
        private set(value) {
            field = value
        }
    var dwPitchOrLinearSize: Int = 0
        private set(value) {
            field = value
        }
    var dwDepth: Int = 0
        private set(value) {
            field = value
        }
    var dwMipMapCount: Int = 0
        private set(value) {
            field = value
        }
    private lateinit var _dwReserved1: IntArray
    var dwReserved1: IntArray
        get() = _dwReserved1
        private set(value) {
            _dwReserved1 = value
        }
    private lateinit var _ddspf: DDSPixelFormat
    var ddspf: DDSPixelFormat
        get() = _ddspf
        private set(value) {
            _ddspf = value
        }
    private lateinit var _dwCaps: Set<DWCaps>
    var dwCaps: Set<DWCaps>
        get() = _dwCaps
        private set(value) {
            _dwCaps = value
        }
    private lateinit var _dwCaps2: Set<DWCaps2>
    var dwCaps2: Set<DWCaps2>
        get() = _dwCaps2
        private set(value) {
            _dwCaps2 = value
        }
    private lateinit var _dwCaps3: Set<DWCaps2>
    var dwCaps3: Set<DWCaps2>
        get() = _dwCaps3
        private set(value) {
            _dwCaps3 = value
        }
    private lateinit var _dwCaps4: Set<DWCaps2>
    var dwCaps4: Set<DWCaps2>
        get() = _dwCaps4
        private set(value) {
            _dwCaps4 = value
        }
    var dwReserved2: Int = 0
        private set(value) {
            field = value
        }

    @Throws(DDSInvalidException::class)
    override fun validate() {
        verify("Invalid DDSPixelFormat: dwSize should be $DW_SIZE, but got $dwSize") {
            dwSize == DW_SIZE
        }
        verify("IInvalid DDSHeader: dwFlags missing required flags") {
            dwFlags.containsAll(DWFlags.REQUIRED)
        }
        ddspf.validate()
        verify("Invalid DDSHeader: dwCaps missing required caps") {
            dwCaps.containsAll(DWCaps.REQUIRED)
        }
        verify("Invalid DDSHeader: dwCaps3 is not empty") {
            dwCaps3.isEmpty()
        }
        verify("Invalid DDSHeader: dwCaps4 is not empty") {
            dwCaps3.isEmpty()
        }
        verify("Invalid DDSHeader, dwReserved2 not zero") {
            dwReserved2 == 0
        }
    }

    @Throws(IOException::class)
    override fun read(ins: DataInputStream) {
        dwSize = reverseBytes(ins.readInt())
        dwFlags = reverseBytes(ins.readInt()).bitsToSet()
        dwHeight = reverseBytes(ins.readInt())
        dwWidth = reverseBytes(ins.readInt())
        dwPitchOrLinearSize = reverseBytes(ins.readInt())
        dwDepth = reverseBytes(ins.readInt())
        dwMipMapCount = reverseBytes(ins.readInt())
        dwReserved1 = IntArray(DW_RESERVED_1_SIZE)
        for (index in 0 until DW_RESERVED_1_SIZE)
            dwReserved1[index] = reverseBytes(ins.readInt())
        ddspf = DDSPixelFormat()
        ddspf.read(ins)
        dwCaps = reverseBytes(ins.readInt()).bitsToSet()
        dwCaps2 = reverseBytes(ins.readInt()).bitsToSet()
        dwCaps3 = reverseBytes(ins.readInt()).bitsToSet()
        dwCaps4 = reverseBytes(ins.readInt()).bitsToSet()
        dwReserved2 = reverseBytes(ins.readInt())
        validate()
    }

    override fun toString(): String {
        return "DDSHeader(dwSize=$dwSize, dwHeight=$dwHeight, dwWidth=$dwWidth, dwPitchOrLinearSize=$dwPitchOrLinearSize, dwDepth=$dwDepth, dwMipMapCount=$dwMipMapCount, dwReserved2=$dwReserved2, dwFlags=$dwFlags, dwReserved1=${dwReserved1.contentToString()}, ddspf=$ddspf, dwCaps=$dwCaps, dwCaps2=$dwCaps2, dwCaps3=$dwCaps3, dwCaps4=$dwCaps4)"
    }

    enum class DWFlags(val bits: Int) : IntSupplier {
        DDSD_CAPS(0x1),
        DDSD_HEIGHT(0x2),
        DDSD_WIDTH(0x4),
        DDSD_PITCH(0x8),
        DDSD_PIXELFORMAT(0x1000),
        DDSD_MIPMAPCOUNT(0x20000),
        DDSD_LINEARSIZE(0x80000),
        DDSD_DEPTH(0x800000);

        override fun getAsInt(): Int = bits

        companion object {
            @JvmStatic
            val REQUIRED: EnumSet<DWFlags> = EnumSet.of(DDSD_CAPS, DDSD_HEIGHT, DDSD_WIDTH, DDSD_PIXELFORMAT)
        }
    }

    enum class DWCaps(val bits: Int) : IntSupplier {
        DDSCAPS_COMPLEX(0x8),
        DDSCAPS_MIPMAP(0x400000),
        DDSCAPS_TEXTURE(0x1000);

        override fun getAsInt(): Int = bits

        companion object {
            @JvmStatic
            val REQUIRED: EnumSet<DWCaps> = EnumSet.of(DDSCAPS_TEXTURE)
        }
    }

    enum class DWCaps2(val bits: Int) : IntSupplier {
        DDSCAPS2_CUBEMAP(0x200),
        DDSCAPS2_CUBEMAP_POSITIVEX(0x400),
        DDSCAPS2_CUBEMAP_NEGATIVEX(0x800),
        DDSCAPS2_CUBEMAP_POSITIVEY(0x1000),
        DDSCAPS2_CUBEMAP_NEGATIVEY(0x2000),
        DDSCAPS2_CUBEMAP_POSITIVEZ(0x4000),
        DDSCAPS2_VOLUME(0x200000);

        override fun getAsInt(): Int = bits
    }

    enum class DWCaps3(val bits: Int) : IntSupplier {
        ;

        override fun getAsInt(): Int = bits
    }

    enum class DWCaps4(val bits: Int) : IntSupplier {
        ;

        override fun getAsInt(): Int = bits
    }
}