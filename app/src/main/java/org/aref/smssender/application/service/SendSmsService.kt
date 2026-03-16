package org.aref.smssender.application.service

import org.aref.smssender.domain.model.SmsMessage
import org.aref.smssender.domain.model.SmsResult
import org.aref.smssender.domain.port.input.SendSmsUseCase
import org.aref.smssender.domain.port.output.SmsSenderPort

class SendSmsService(
    private val smsSenderPort: SmsSenderPort
) : SendSmsUseCase {

    override fun sendSms(smsMessage: SmsMessage): SmsResult {
        if (smsMessage.to.isBlank()) {
            return SmsResult.Failure(
                errorCode = 400,
                errorMessage = "El número de teléfono destino es requerido"
            )
        }

        if (smsMessage.message.isBlank()) {
            return SmsResult.Failure(
                errorCode = 400,
                errorMessage = "El mensaje es requerido"
            )
        }

        return try {
            smsSenderPort.send(smsMessage)
        } catch (e: Exception) {
            SmsResult.Failure(
                errorCode = 500,
                errorMessage = "Error al enviar SMS: ${e.message}"
            )
        }
    }
}

