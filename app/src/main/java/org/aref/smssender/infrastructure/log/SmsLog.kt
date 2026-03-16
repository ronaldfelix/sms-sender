package org.aref.smssender.infrastructure.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.aref.smssender.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmsLog {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun info(message: String) {
        append("INFO", message)
    }

    fun error(message: String) {
        append("ERROR", message)
    }

    fun warn(message: String) {
        append("WARN", message)
    }

    fun request(method: String, uri: String, ip: String?) {
        append("REQ", "$method $uri from ${ip ?: "unknown"}")
    }

    fun sms(to: String, status: String, sim: String?) {
        val simInfo = if (sim != null) " [$sim]" else ""
        append("SMS", "-> $to $status$simInfo")
    }

    fun clear() {
        _logs.value = emptyList()
    }

    private fun append(tag: String, message: String) {
        val timestamp = timeFormat.format(Date())
        val line = "[$timestamp][$tag] $message"
        val current = _logs.value.toMutableList()
        current.add(line)
        if (current.size > BuildConfig.MAX_LOG_LINES) {
            _logs.value = current.takeLast(BuildConfig.MAX_LOG_LINES)
        } else {
            _logs.value = current
        }
    }
}

