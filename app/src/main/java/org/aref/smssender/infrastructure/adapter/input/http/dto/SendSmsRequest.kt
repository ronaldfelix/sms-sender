package org.aref.smssender.infrastructure.adapter.input.http.dto

import com.google.gson.annotations.SerializedName
import org.aref.smssender.domain.model.SmsMessage

data class SendSmsRequest(
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("to") val to: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data_coding") val dataCoding: Int? = null,
    @SerializedName("status") val status: Boolean? = null,
    @SerializedName("sim_slot") val simSlot: Int? = null
) {
    fun toDomain(): SmsMessage {
        val destination = phone ?: to ?: ""
        val msg = message ?: ""
        return SmsMessage(
            to = destination,
            message = msg,
            dataCoding = dataCoding,
            status = status,
            simSlot = simSlot
        )
    }
}

