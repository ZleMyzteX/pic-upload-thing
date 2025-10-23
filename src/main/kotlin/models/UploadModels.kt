package er.codes.models

data class UploadResponse(
    val success: Boolean,
    val message: String,
    val uploadedFiles: List<UploadedFileInfo>,
    val totalFiles: Int,
    val totalSize: Long,
    val uploadPath: String
)

data class UploadedFileInfo(
    val originalName: String,
    val savedPath: String,
    val mimeType: String,
    val size: Long,
    val uploadedAt: String
)

data class ErrorResponse(
    val success: Boolean = false,
    val error: String,
    val details: String? = null
)


