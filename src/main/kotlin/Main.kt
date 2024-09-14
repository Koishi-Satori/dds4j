package top.kkoishi.dds

import top.kkoishi.dds.top.kkoishi.dds.DDS
import top.kkoishi.dds.top.kkoishi.dds.DDSHeader
import kotlin.io.path.Path
import kotlin.io.path.inputStream

fun main() {
    val dds = DDS()
    val ins = Path("./test.dds").inputStream().buffered()
    dds.read(ins)
    println(dds)
    println(ins.available())
    ins.close()
    println(dds.header.ddspf.isDX10HeaderPresent())
}
