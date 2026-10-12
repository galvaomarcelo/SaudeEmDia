package com.geosapiens.saudeemdia.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExamRequest(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("patient_id")
    val patientId: Long? = null,
    @SerialName("request_date")
    val requestDate: String,
    @SerialName("exam_name")
    val examName: String,
    @SerialName("speciality")
    val speciality: String? = null,
    @SerialName("exam_type")
    val examType: String? = null,
    @SerialName("requesting_doctor")
    val requestingDoctor: String? = null,
    @SerialName("institution")
    val institution: String? = null,
    @SerialName("summary")
    val summary: String? = null,
    @SerialName("priority")
    val priority: String? = null,
    @SerialName("status")
    val status: String? = "pendente",
    @SerialName("document_url")
    val documentUrl: String? = null,
)
