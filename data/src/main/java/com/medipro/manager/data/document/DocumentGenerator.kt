package com.medipro.manager.data.document

/** Shared contract for PDF document generators (invoice, report, ledger, …). */
interface DocumentGenerator<T> {
    fun generate(input: T): java.io.File
}
