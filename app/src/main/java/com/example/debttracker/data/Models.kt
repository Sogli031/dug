package com.example.debttracker.data

const val EUR_TO_RSD = 117

data class Payment(val date: String, val amount: Int)

data class PaymentResult(
    val amount: Int,
    val date: String,
    val debtorName: String,
    val remainingDebt: Int,
    val wasRemoved: Boolean
)

enum class Currency(val label: String, val rateToRsd: Int) {
    RSD("RSD", 1),
    EUR("EUR", EUR_TO_RSD)
}

data class Debtor(
    var name: String,
    var initialDebt: Int,
    var totalDebt: Int,
    val payments: MutableList<Payment> = mutableListOf()
)

fun convertToRsd(amount: Int, currency: Currency): Int {
    return amount * currency.rateToRsd
}
