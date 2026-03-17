package org.aref.smssender.infrastructure.adapter.input.http

import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import org.aref.smssender.BuildConfig
import org.aref.smssender.domain.model.SmsResult
import org.aref.smssender.domain.port.input.SendSmsUseCase
import org.aref.smssender.infrastructure.adapter.input.http.dto.SendSmsRequest
import org.aref.smssender.infrastructure.adapter.input.http.dto.SendSmsResponse
import org.aref.smssender.infrastructure.config.AppConfig
import org.aref.smssender.infrastructure.log.SmsLog


class HttpServerAdapter(
    private val sendSmsUseCase: SendSmsUseCase,
    private val appConfig: AppConfig,
) : NanoHTTPD(BuildConfig.APP_PORT) {

    private val gson = Gson()

    override fun serve(session: IHTTPSession): Response {
        val clientIp = session.remoteIpAddress
        SmsLog.request(session.method.name, session.uri, clientIp)

        if (session.method != Method.POST) {
            SmsLog.warn("Método no permitido: ${session.method}")
            return jsonResponse(
                Response.Status.METHOD_NOT_ALLOWED,
                SendSmsResponse(
                    success = false,
                    message = "Método no permitido. Use POST.",
                    errorCode = 405
                )
            )
        }

        val uri = session.uri
        if (uri != "/api/sendsms") {
            SmsLog.warn("Ruta no encontrada: $uri")
            return jsonResponse(
                Response.Status.NOT_FOUND,
                SendSmsResponse(
                    success = false,
                    message = "Ruta no encontrada. Use /send-sms o /api/sendsms",
                    errorCode = 404
                )
            )
        }

        // Validar API Key
        val apiKey = session.headers["x-api-key"]
        if (apiKey.isNullOrBlank() || apiKey != appConfig.apiKey) {
            SmsLog.error("API Key inválida desde $clientIp")
            return jsonResponse(
                Response.Status.UNAUTHORIZED,
                SendSmsResponse(
                    success = false,
                    message = "API Key inválida o no proporcionada",
                    errorCode = 401
                )
            )
        }

        // Parsear body JSON
        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            val body = files["postData"] ?: ""

            if (body.isBlank()) {
                SmsLog.warn("Body vacío desde $clientIp")
                return jsonResponse(
                    Response.Status.BAD_REQUEST,
                    SendSmsResponse(
                        success = false,
                        message = "Body JSON vacío",
                        errorCode = 400
                    )
                )
            }

            val request = gson.fromJson(body, SendSmsRequest::class.java)
            val smsMessage = request.toDomain()
            SmsLog.info("Enviando SMS a ${smsMessage.to} (sim configurada en servidor)")

            // Ejecutar caso de uso
            when (val result = sendSmsUseCase.sendSms(smsMessage)) {
                is SmsResult.Success -> {
                    SmsLog.sms(smsMessage.to, "OK", result.simUsed)
                    jsonResponse(
                        Response.Status.OK,
                        SendSmsResponse(
                            success = true,
                            message = "SMS enviado exitosamente",
                            messageId = result.messageId,
                            simUsed = result.simUsed
                        )
                    )
                }
                is SmsResult.Failure -> {
                    SmsLog.sms(smsMessage.to, "FAIL: ${result.errorMessage}", null)
                    val status = when (result.errorCode) {
                        400 -> Response.Status.BAD_REQUEST
                        else -> Response.Status.INTERNAL_ERROR
                    }
                    jsonResponse(
                        status,
                        SendSmsResponse(
                            success = false,
                            message = result.errorMessage,
                            errorCode = result.errorCode
                        )
                    )
                }
            }
        } catch (e: Exception) {
            SmsLog.error("Excepción: ${e.message}")
            jsonResponse(
                Response.Status.INTERNAL_ERROR,
                SendSmsResponse(
                    success = false,
                    message = "Error procesando la solicitud: ${e.message}",
                    errorCode = 500
                )
            )
        }
    }

    private fun jsonResponse(status: Response.Status, body: SendSmsResponse): Response {
        val json = gson.toJson(body)
        return newFixedLengthResponse(status, "application/json", json)
    }
}

