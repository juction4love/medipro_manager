package com.medipro.manager.core.common

/**
 * Monetary amounts stored as paisa (1 NPR = 100 paisa) to avoid floating-point errors.
 */
@JvmInline
value class Money(val paisa: Long) {
    val rupees: Double get() = paisa / 100.0

    operator fun plus(other: Money): Money = Money(paisa + other.paisa)
    operator fun minus(other: Money): Money = Money(paisa - other.paisa)
    operator fun times(multiplier: Int): Money = Money(paisa * multiplier)

    companion object {
        fun fromRupees(amount: Double): Money = Money(kotlin.math.round(amount * 100).toLong())
        fun zero(): Money = Money(0)
    }
}

fun Money.formatNpr(): String = FormatUtils.formatCurrency(rupees)
