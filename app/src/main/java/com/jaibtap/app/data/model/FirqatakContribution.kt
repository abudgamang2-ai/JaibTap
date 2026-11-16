package com.jaibtap.app.data.model

import java.io.Serializable

data class FirqatakContribution(
    val id: String,
    val firqatakCode: String,
    val participantName: String,
    val participantWallet: String,
    val amount: Double,
    val timestamp: Long,
    val status: String,
    val nfcTagUid: String? = null
) : Serializable
