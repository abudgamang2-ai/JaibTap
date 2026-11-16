package com.jaibtap.app.data.model

data class NfcTagInfo(
    val tagUid: String,
    val tagType: String,
    val isKnown: Boolean,
    val ownerUser: User? = null
)
