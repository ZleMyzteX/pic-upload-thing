package er.codes.models

import kotlinx.serialization.Serializable

/**
 * Request model for file uploads with optional naming
 */
@Serializable
data class UploadRequest(
    val uploadName: String? = null // Optional user-provided name like "Vacation Photos"
)

/**
 * Response model for successful file uploads
 */
@Serializable
data class UploadResponse(
    val success: Boolean,
    val message: String,
    val uploadedFiles: List<UploadedFileInfo>,
    val totalFiles: Int,
    val totalSize: Long,
    val uploadPath: String
)

/**
 * Detailed information about each uploaded file
 */
@Serializable
data class UploadedFileInfo(
    val originalName: String,
    val savedPath: String,
    val mimeType: String,
    val size: Long,
    val uploadedAt: String
)

/**
 * Error response model
 */
@Serializable
data class ErrorResponse(
    val success: Boolean = false,
    val error: String,
    val details: String? = null
)

/**
 * Export/zip response metadata
 */
@Serializable
data class ExportResponse(
    val success: Boolean,
    val fileName: String,
    val fileCount: Int,
    val totalSize: Long
)

