package com.medipro.manager.domain.model

/**
 * Domain errors for sale operations. Map [Result] failures via [SaleError.from].
 */
sealed interface SaleError {
    val userMessage: String

    data object PostedInvoiceCannotBeCancelled : SaleError {
        override val userMessage: String =
            "This invoice has already been posted and cannot be cancelled. " +
                "Please use Process Return instead."
    }

    data class Unknown(val cause: Throwable?) : SaleError {
        override val userMessage: String = cause?.message?.takeIf { it.isNotBlank() } ?: "Sale operation failed"
    }

    companion object {
        fun from(throwable: Throwable): SaleError = when (throwable) {
            is PostedSaleCancellationNotAllowedException -> PostedInvoiceCannotBeCancelled
            else -> Unknown(throwable)
        }
    }
}

class PostedSaleCancellationNotAllowedException(
    message: String = "Posted invoices cannot be cancelled. Use Sales Return.",
) : IllegalStateException(message)

fun Result<*>.toSaleError(): SaleError? = exceptionOrNull()?.let { SaleError.from(it) }
