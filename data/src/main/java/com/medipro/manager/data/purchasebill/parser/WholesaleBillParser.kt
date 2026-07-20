package com.medipro.manager.data.purchasebill.parser

import com.medipro.manager.data.purchasebill.ParsedWholesaleBill

interface WholesaleBillParser {
    val name: String
    val supplierKeywords: List<String>
    fun canParse(supplierName: String?, ocrText: String): Boolean
    fun parse(ocrText: String): ParsedWholesaleBill
}
