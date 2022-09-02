package com.stefanosiano.powerful_libraries.sama_sample

import androidx.room.Ignore
import com.stefanosiano.powerful_libraries.sama.generatedextensions.defaultContentEquals
import com.stefanosiano.powerful_libraries.sama.view.SamaListItem
import java.math.BigDecimal

internal class SampleLI(
    val uid: String,
    val destinazioneUid: String,
    val destinatario: String,
    val descr: String,
    val indirizzo: String,
    val citta: String,
    val provincia: String,
    val nazione: String,
    val cap: String,
    val tags: BigDecimal,
    val state: Double,
    val img: Int
) : SamaListItem() {
    companion object {
        val aaa = ""
    }
}

internal abstract class S2: SamaListItem()

internal class S3 : S2() {

    val id2 : String = ""
    @Ignore val id : String = ""

    override fun contentEquals(other: SamaListItem): Boolean {
        return defaultContentEquals(other)
    }
}
internal open class S4 : S2()
internal open class S5(
    val asd: String
) : S4()
internal class S6 : S5("a")
