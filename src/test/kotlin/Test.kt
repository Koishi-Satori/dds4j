import org.junit.jupiter.api.Test
import top.kkoishi.dds.DDSRef
import top.kkoishi.dds.decode.DDSDecoder.Companion.decodeToPng
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

class Test {
    @Test
    fun testDDSRead() {
        val dds = DDSRef()
        val ins = Path("./test.dds").inputStream().buffered()
        dds.read(ins)
        println(dds)
        println(ins.available())
        ins.close()
        println(dds.header.ddspf.isDX10HeaderPresent())
    }

    @Test
    fun testDDSConvert() {
        val dds = DDSRef()
        val ins = Path("./test.dds").inputStream().buffered()
        dds.read(ins)
        ins.close()
        val oos = Path("./out_png/rgba_dds.png").outputStream().buffered()
        dds.decodeToPng(oos, autoFlush = true, autoClose = true)
    }

    @Test
    fun testBC1DXT1DDSConvert() {
        val dds = DDSRef()
        val ins = Path("./test_dxt1.dds").inputStream().buffered()
        dds.read(ins)
        ins.close()
        val oos = Path("./out_png/rgba_dxt1_dds.png").outputStream().buffered()
        dds.decodeToPng(oos, autoFlush = true, autoClose = true)
    }

    @Test
    fun testBC2DXT3DDSConvert() {
        val dds = DDSRef()
        val ins = Path("./test_dxt3.dds").inputStream().buffered()
        dds.read(ins)
        ins.close()
        val oos = Path("./out_png/rgba_dxt3_dds.png").outputStream().buffered()
        dds.decodeToPng(oos, autoFlush = true, autoClose = true)
    }

    @Test
    fun testBC3DXT5DDSConvert() {
        val dds = DDSRef()
        val ins = Path("./test_dxt5.dds").inputStream().buffered()
        dds.read(ins)
        ins.close()
        val oos = Path("./out_png/rgba_dxt5_dds.png").outputStream().buffered()
        dds.decodeToPng(oos, autoFlush = true, autoClose = true)
    }
}
