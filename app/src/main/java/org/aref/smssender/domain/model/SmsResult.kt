package org.aref.smssender.domain.model

sealed class SmsResult {
    data class Success(val messageId: String, val simUsed: String? = null) : SmsResult()
    data class Failure(val errorCode: Int, val errorMessage: String) : SmsResult()
}