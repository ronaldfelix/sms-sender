package org.aref.smssender.infrastructure.adapter.output.sms

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import org.aref.smssender.domain.model.SimInfo
import org.aref.smssender.domain.model.SmsMessage
import org.aref.smssender.domain.model.SmsResult
import org.aref.smssender.domain.port.output.SmsSenderPort
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.aref.smssender.BuildConfig

class AndroidSmsSenderAdapter(
    private val context: Context,
    private val defaultSimSlot: Int = BuildConfig.DEFAULT_SIM_SLOT
) : SmsSenderPort {

    /**
     * Obtiene la lista de SIMs activas en el dispositivo.
     * Retorna lista de pares (slotIndex empezando en 1, displayName)
     */
    fun getActiveSimCards(): List<SimInfo> {
        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            @Suppress("MissingPermission")
            val activeList: List<SubscriptionInfo> = subscriptionManager.activeSubscriptionInfoList ?: emptyList()
            activeList.mapIndexed { index, info ->
                SimInfo(
                    slot = index + 1,
                    displayName = info.displayName?.toString() ?: "SIM ${index + 1}",
                    carrierName = info.carrierName?.toString() ?: "Desconocido",
                    number = info.number ?: "",
                    subscriptionId = info.subscriptionId
                )
            }
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun send(smsMessage: SmsMessage): SmsResult {
        val messageId = UUID.randomUUID().toString()

        return try {
            val requestedSlot = smsMessage.simSlot ?: defaultSimSlot
            val resolvedSim = resolveSmsManager(requestedSlot)

            val parts = resolvedSim.smsManager.divideMessage(smsMessage.message)

            val result = if (parts.size == 1) {
                sendSingleMessage(resolvedSim.smsManager, smsMessage.to, smsMessage.message, messageId)
            } else {
                sendMultipartMessage(resolvedSim.smsManager, smsMessage.to, parts, messageId)
            }

            when (result) {
                is SmsResult.Success -> result.copy(simUsed = resolvedSim.simLabel)
                is SmsResult.Failure -> result
            }
        } catch (e: SecurityException) {
            SmsResult.Failure(
                errorCode = 403,
                errorMessage = "Permiso SEND_SMS no concedido: ${e.message}"
            )
        } catch (e: Exception) {
            SmsResult.Failure(
                errorCode = 500,
                errorMessage = "Error enviando SMS: ${e.message}"
            )
        }
    }

    private data class ResolvedSim(
        val smsManager: SmsManager,
        val simLabel: String
    )

    /**
     * Resuelve qué SmsManager usar según el slot solicitado
     */
    private fun resolveSmsManager(slot: Int): ResolvedSim {
        if (slot <= 0) {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            return ResolvedSim(smsManager, "SIM Default")
        }

        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            @Suppress("MissingPermission")
            val activeList: List<SubscriptionInfo> = subscriptionManager.activeSubscriptionInfoList ?: emptyList()

            if (activeList.isEmpty()) {
                throw IllegalStateException("No hay SIMs activas en el dispositivo")
            }

            val index = slot - 1
            if (index >= activeList.size) {
                throw IllegalStateException("SIM $slot no disponible. Solo hay ${activeList.size} SIM(s) activa(s)")
            }

            val subscriptionInfo = activeList[index]
            val subscriptionId = subscriptionInfo.subscriptionId
            val simLabel = "SIM $slot (${subscriptionInfo.displayName ?: subscriptionInfo.carrierName ?: "Desconocido"})"

            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java).createForSubscriptionId(subscriptionId)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            }

            ResolvedSim(smsManager, simLabel)
        } catch (e: SecurityException) {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            ResolvedSim(smsManager, "SIM Default (sin permiso para elegir)")
        }
    }

    private fun sendSingleMessage(
        smsManager: SmsManager,
        to: String,
        message: String,
        messageId: String
    ): SmsResult {
        val latch = CountDownLatch(1)
        val resultCode = AtomicInteger(Activity.RESULT_CANCELED)

        val action = "${BuildConfig.SMS_SENT_ACTION}.$messageId"
        val sentIntent = PendingIntent.getBroadcast(
            context, 0, Intent(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                resultCode.set(this.resultCode)
                latch.countDown()
                try { context.unregisterReceiver(this) } catch (_: Exception) {}
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(action), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(action))
        }

        smsManager.sendTextMessage(to, null, message, sentIntent, null)

        val completed = latch.await(BuildConfig.SMS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!completed) {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
            return SmsResult.Success(messageId = messageId)
        }

        return when (resultCode.get()) {
            Activity.RESULT_OK -> SmsResult.Success(messageId = messageId)
            else -> SmsResult.Success(messageId = messageId)
        }
    }

    private fun sendMultipartMessage(
        smsManager: SmsManager,
        to: String,
        parts: ArrayList<String>,
        messageId: String
    ): SmsResult {
        val latch = CountDownLatch(parts.size)
        val sentIntents = ArrayList<PendingIntent>()
        val action = "${BuildConfig.SMS_SENT_ACTION}.$messageId"

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                latch.countDown()
                if (latch.count == 0L) {
                    try { context.unregisterReceiver(this) } catch (_: Exception) {}
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(action), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(action))
        }

        for (i in parts.indices) {
            sentIntents.add(PendingIntent.getBroadcast(
                context, i, Intent(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            ))
        }

        smsManager.sendMultipartTextMessage(to, null, parts, sentIntents, null)

        val completed = latch.await(BuildConfig.SMS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!completed) {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }

        return SmsResult.Success(messageId = messageId)
    }
}


