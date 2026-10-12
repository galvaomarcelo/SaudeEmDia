package com.geosapiens.saudeemdia.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ExamResult(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("patient_id")
    val patientId: Long? = null,
    @SerialName("exam_date")
    val examDate: String,
    @SerialName("exam_name")
    val examName: String,
    @SerialName("specialty")
    val specialty: String? = null,
    @SerialName("exam_type")
    val examType: String? = null,
    @SerialName("exam_request_id")
    val examRequestId: Long? = null,
    @SerialName("examiner_doctor_id")
    val examinerDoctorId: Long? = null,
    @SerialName("institution")
    val institution: String? = null,
    @SerialName("result")
    val result: String? = null,
    @SerialName("conclusion")
    val conclusion: String? = null,
    @SerialName("summary")
    val summary: String? = null,
    @SerialName("exam_cover_url")
    val examCoverUrl: String? = null,
    @SerialName("document_url")
    val documentUrl: String? = null,
    @SerialName("repository_hyperlink")
    val repositoryHyperlink: String? = null,
    @SerialName("tags")
    val tags: JsonElement? = null,
    @SerialName("exam_code")
    val examCode: String? = null,
    @SerialName("requesting_doctor")
    val requestingDoctor: String? = null,
    @SerialName("examiner")
    val examiner: String? = null,
    @SerialName("amount")
    val amount: Int? = 1,
    @SerialName("patient_code")
    val patientCode: String? = null
)
