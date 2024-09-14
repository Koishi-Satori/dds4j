package top.kkoishi.dds

import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.jvm.Throws

interface DDSReadable {
    @Throws(DDSInvalidException::class)
    fun validate()

    @Throws(IOException::class)
    fun read(ins: DataInputStream)

    @Throws(IOException::class)
    fun read(ins: InputStream) = read(DataInputStream(ins))
}