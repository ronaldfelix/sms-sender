package org.aref.smssender.domain.model

data class SmsMessage(
    val to: String,
    val message: String,
    val dataCoding: Int? = null,
    val status: Boolean? = null,
    val simSlot: Int? = null
)