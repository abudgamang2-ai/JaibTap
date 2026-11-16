package com.jaibtap.app.data.model

import java.io.Serializable

data class User(
    val id: String,
    val name: String,
    val phone: String,
    val walletNumber: String,
    var balance: Double,
    val firqatakCode: String?,
    val pin: String,
    val password: String,
    var withdrawalLimit: Double = 5000.0,
    var authCode: String = "9988",
    val nfcTagUid: String? = null
) : Serializable
