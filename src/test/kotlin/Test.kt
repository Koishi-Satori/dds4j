import ar.com.hjg.pngj.ImageInfo
import ar.com.hjg.pngj.ImageLineHelper
import ar.com.hjg.pngj.ImageLineInt
import ar.com.hjg.pngj.PngWriter
import org.junit.jupiter.api.Test
import top.kkoishi.dds.DDS
import top.kkoishi.dds.decode.DDSDecoder
import top.kkoishi.dds.decode.RGBADDSDecoder
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

class Test {
    @Test
    fun testDDSRead() {
        val dds = DDS()
        val ins = Path("./test.dds").inputStream().buffered()
        dds.read(ins)
        println(dds)
        println(ins.available())
        ins.close()
        println(dds.header.ddspf.isDX10HeaderPresent())
    }

    @Test
    fun testDDSConvert() {
        val dds = DDS()
        val ins = Path("./test.dds").inputStream().buffered()
        dds.read(ins)
        ins.close()
        // convert test, this DDS file should use RGBADDSDecoder
        val header = dds.header
        val decoder: DDSDecoder = RGBADDSDecoder(dds)
        val imageInfo = ImageInfo(header.dwWidth, header.dwHeight, 8, true)
        val oos = Path("./out_png/rgba_dds.png").outputStream().buffered()
        val writer = PngWriter(oos, imageInfo)
        val line = ImageLineInt(imageInfo)
        decoder.forEach {
            ImageLineHelper.setPixelsRGBA8(line, it)
            writer.writeRow(line)
        }
        writer.end()
        oos.close()
    }
}