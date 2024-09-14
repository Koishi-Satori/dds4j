package top.kkoishi.dds

import java.util.EnumSet
import java.util.function.IntSupplier
import kotlin.jvm.Throws

internal object InternalUtils {
    @JvmStatic
    inline fun <reified T> Int.bitsToSet(): Set<T> where T : Enum<T>, T : IntSupplier =
        bitsToSet(this, T::class.java)

    @JvmStatic
    fun <T> bitsToSet(bits: Int, tClass: Class<T>): Set<T> where T : Enum<T>, T : IntSupplier {
        val res = EnumSet.noneOf(tClass)
        val enums: Array<T> = tClass.enumConstants
        enums.forEach {
            if (bits and it.asInt != 0)
                res.add(it)
        }
        return res
    }

    @Throws(DDSInvalidException::class)
    fun verify(message: String, condition: () -> Boolean) {
        if (!condition())
            throw DDSInvalidException(message)
    }
    @Throws(DDSInvalidException::class)
    fun verifyNot(message: String, condition: () -> Boolean) {
        if (condition())
            throw DDSInvalidException(message)
    }
}