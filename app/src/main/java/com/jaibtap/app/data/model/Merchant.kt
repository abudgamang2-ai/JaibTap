package com.jaibtap.app.data.model

import java.io.Serializable

data class Merchant(
    val id: String,
    val name: String,
    val posNumber: String,
    var balance: Double
) : Serializable
