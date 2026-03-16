package org.aref.smssender.infrastructure.adapter.input.http.dto

import com.google.gson.annotations.SerializedName

data class SendSmsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("messageId") val messageId: String? = null,
    @SerializedName("errorCode") val errorCode: Int? = null,
    @SerializedName("simUsed") val simUsed: String? = null
)