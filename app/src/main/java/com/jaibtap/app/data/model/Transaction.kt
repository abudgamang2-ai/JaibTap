package com.jaibtap.app.data.model

import java.io.Serializable

enum class TransactionType {
    TRANSFER_SUBSCRIBER,
    WITHDRAWAL,
    DEPOSIT,
    POS_PAYMENT,
    FIRQATAK
}

enum class TransactionStatus {
    SUCCESS,
    FAILED
}

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val senderWallet: String,
    val receiverWallet: String,
    val receiverName: String,
    val timestamp: Long,
    val status: TransactionStatus,
    val notes: String? = null,
    val failureReason: String? = null,
    val nfcUsed: Boolean = false
) : Serializable
