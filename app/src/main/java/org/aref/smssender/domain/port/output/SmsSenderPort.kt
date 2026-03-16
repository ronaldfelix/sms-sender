package org.aref.smssender.domain.port.output

import org.aref.smssender.domain.model.SmsMessage
import org.aref.smssender.domain.model.SmsResult

interface SmsSenderPort {
    fun send(smsMessage: SmsMessage): SmsResult
}

