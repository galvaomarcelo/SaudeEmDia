package com.geosapiens.saudeemdia.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Prescription(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("patient_id")
    val patientId: Long? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String,
    @SerialName("prescription_date")
    val prescriptionDate: String? = null,
    @SerialName("doctor_name")
    val doctorName: String? = null,
    @SerialName("doctor_crm")
    val doctorCrm: String? = null,
    @SerialName("institution")
    val institution: String? = null,
    @SerialName("document_url")
    val documentUrl: String? = null,
    @SerialName("medications")
    val medications: JsonElement? = null,
)
