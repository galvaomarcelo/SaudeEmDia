package com.geosapiens.saudeemdia.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Doctor(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("name")
    val name: String,
    @SerialName("specialty")
    val specialty: String? = null,
    @SerialName("crm")
    val crm: String? = null,
    @SerialName("crm_state")
    val crmState: String? = "DF",
    @SerialName("phone")
    val phone: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("institution")
    val institution: String? = null,
    @SerialName("notes")
    val notes: String? = null,
)
