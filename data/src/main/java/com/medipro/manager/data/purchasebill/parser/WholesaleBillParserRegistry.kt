package com.medipro.manager.data.purchasebill.parser

import com.medipro.manager.data.purchasebill.NepalWholesaleBillParser
import com.medipro.manager.data.purchasebill.ParsedWholesaleBill

/** Surya Medico and similar column-layout invoices. */
object SuryaMedicoBillParser : WholesaleBillParser {
    override val name: String = "SuryaMedico"
    override val supplierKeywords: List<String> = listOf("SURYA", "MEDICO")

    override fun canParse(supplierName: String?, ocrText: String): Boolean {
        val haystack = "${supplierName.orEmpty()} $ocrText".uppercase()
        return supplierKeywords.all { haystack.contains(it) }
    }

    override fun parse(ocrText: String): ParsedWholesaleBill = NepalWholesaleBillParser.parse(ocrText)
}

object AsianPharmaBillParser : WholesaleBillParser {
    override val name: String = "AsianPharma"
    override val supplierKeywords: List<String> = listOf("ASIAN", "PHARMA")

    override fun canParse(supplierName: String?, ocrText: String): Boolean {
        val haystack = "${supplierName.orEmpty()} $ocrText".uppercase()
        return supplierKeywords.all { haystack.contains(it) }
    }

    override fun parse(ocrText: String): ParsedWholesaleBill = NepalWholesaleBillParser.parse(ocrText)
}

object OmDistributorsBillParser : WholesaleBillParser {
    override val name: String = "OmDistributors"
    override val supplierKeywords: List<String> = listOf("OM", "DISTRIBUT")

    override fun canParse(supplierName: String?, ocrText: String): Boolean {
        val haystack = "${supplierName.orEmpty()} $ocrText".uppercase()
        return haystack.contains("OM") && haystack.contains("DISTRIBUT")
    }

    override fun parse(ocrText: String): ParsedWholesaleBill = NepalWholesaleBillParser.parse(ocrText)
}

object LifeCareBillParser : WholesaleBillParser {
    override val name: String = "LifeCare"
    override val supplierKeywords: List<String> = listOf("LIFE", "CARE")

    override fun canParse(supplierName: String?, ocrText: String): Boolean {
        val haystack = "${supplierName.orEmpty()} $ocrText".uppercase()
        return supplierKeywords.all { haystack.contains(it) }
    }

    override fun parse(ocrText: String): ParsedWholesaleBill = NepalWholesaleBillParser.parse(ocrText)
}

object GenericWholesaleBillParser : WholesaleBillParser {
    override val name: String = "Generic"
    override val supplierKeywords: List<String> = emptyList()

    override fun canParse(supplierName: String?, ocrText: String): Boolean = true

    override fun parse(ocrText: String): ParsedWholesaleBill = NepalWholesaleBillParser.parse(ocrText)
}

object WholesaleBillParserRegistry {

    private val specialized = listOf(
        SuryaMedicoBillParser,
        AsianPharmaBillParser,
        OmDistributorsBillParser,
        LifeCareBillParser,
    )

    fun resolve(supplierName: String?, ocrText: String): WholesaleBillParser {
        return specialized.firstOrNull { it.canParse(supplierName, ocrText) } ?: GenericWholesaleBillParser
    }

    fun parse(ocrText: String): Pair<ParsedWholesaleBill, String> {
        val supplierGuess = NepalWholesaleBillParser.peekSupplierName(ocrText)
        val parser = resolve(supplierGuess, ocrText)
        return parser.parse(ocrText) to parser.name
    }
}
