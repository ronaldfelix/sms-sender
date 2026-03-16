package org.aref.smssender.domain.port.input

import org.aref.smssender.domain.model.SmsMessage
import org.aref.smssender.domain.model.SmsResult

interface SendSmsUseCase {
    fun sendSms(smsMessage: SmsMessage): SmsResult
}

