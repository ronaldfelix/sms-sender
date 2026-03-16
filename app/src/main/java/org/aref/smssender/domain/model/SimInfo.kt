package org.aref.smssender.domain.model

data class SimInfo(
        val slot: Int,
        val displayName: String,
        val carrierName: String,
        val number: String,
        val subscriptionId: Int
)

