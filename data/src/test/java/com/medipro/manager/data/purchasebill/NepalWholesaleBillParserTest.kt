package com.medipro.manager.data.purchasebill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NepalWholesaleBillParserTest {

    private val sampleOcr = """
        SURYA MEDICO PVT. LTD.
        Balkumari Road, Narayangarh, Chitwan
        INVOICE
        Invoice No.: HASR0007100
        Date: 2026/07/16
        M/S: 1206 BIMAL PHARMECY
        S.N. HS CODE ITEM DESCRIPTION PACK BATCH EXP. DATE QTY CC/RATE AMOUNT M.R.P.
        1 G-RON - 1 1x10 GRT-26018 2028/03 10 94.85 948.50 110.00
        2 G-RON MD TAB 1x10 GWT-25004 2027/08 10 103.45 1034.50 120.00
        3 3004 GRANDEM - TABS 1x10 MPB260543 2028/01 10 148.70 1487.00 107.76
        5 ELATURB - 10 TABS 1x10 ELTP26041 2027/07 10 181.10 1811.00 210.00
        - do - 2 FREE 0.00 0.00 210.00
        6 ACEWIL-TAB 1x10 AA25002 2027/06 20 51.72 1034.40 60.00
        Total: 9086.65
        Net Total: 9087.00
    """.trimIndent()

    @Test
    fun parse_suryaMedicoSample_extractsHeaderAndItems() {
        val result = NepalWholesaleBillParser.parse(sampleOcr)

        assertEquals("SURYA MEDICO PVT. LTD.", result.supplierName)
        assertEquals("HASR0007100", result.invoiceNumber)
        assertEquals("2026-07-16", result.invoiceDate)
        assertEquals(9087.0, result.netTotal ?: 0.0, 0.01)
        assertTrue(result.lines.size >= 5)

        val first = result.lines.first()
        assertEquals("G-RON - 1", first.description)
        assertEquals("GRT-26018", first.batchNumber)
        assertEquals(10, first.quantity)
        assertEquals(94.85, first.unitPrice, 0.01)
        assertEquals(110.0, first.mrp, 0.01)
    }

    @Test
    fun parsePaidLine_readsNumericColumnsFromTail() {
        val line = "1 G-RON - 1 1x10 GRT-26018 2028/03 10 94.85 948.50 110.00"
        val parsed = NepalWholesaleBillParser.parsePaidLine(line)

        assertNotNull(parsed)
        assertEquals(10, parsed!!.quantity)
        assertEquals(948.50, parsed.amount, 0.01)
    }

    @Test
    fun parseFreeLine_linksToParentItem() {
        val parent = NepalWholesaleBillParser.parsePaidLine(
            "5 ELATURB - 10 TABS 1x10 ELTP26041 2027/07 10 181.10 1811.00 210.00",
        )!!
        val free = NepalWholesaleBillParser.parseFreeLine("- do - 2 FREE 0.00 0.00 210.00", parent)

        assertNotNull(free)
        assertEquals(2, free!!.quantity)
        assertTrue(free.isFreeItem)
        assertEquals(parent.description, free.description)
    }
}
