package com.stefanosiano.powerful_libraries.samasample

import androidx.room.Ignore
import com.stefanosiano.powerful_libraries.sama.generatedextensions.defaultContentEquals
import com.stefanosiano.powerful_libraries.sama.view.SamaListItem
import java.math.BigDecimal

internal class SampleLI(
    val uid: String,
    val bigNum: BigDecimal,
    val num: Double,
    val count: Int
) : SamaListItem() {
    companion object {
        const val aaa = ""
    }
}

internal abstract class S2 : SamaListItem()

internal class S3 : S2() {

    val id2: String = ""

    @Ignore val id: String = ""

    override fun contentEquals(other: SamaListItem): Boolean = defaultContentEquals(other)
}
internal open class S4 : SamaListItem()
internal open class S5(
    val asd: String
) : S4()
internal class S6 : S5("a")
