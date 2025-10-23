package er.codes.routes

import er.codes.models.*
import er.codes.utils.FileUtils
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.datetime.Clock
import java.io.File

/**
 * Configures all upload-related routes
 *
 * Performance tuning for 50-100 concurrent users:
 * 1. Uses async/await for parallel file processing
 * 2. Streaming file uploads with buffered I/O (no full memory load)
 * 3. IO dispatcher prevents blocking the main event loop
 * 4. Rate limiting configured in HTTP.kt
 * 5. Compression enabled for JSON responses
 */
fun Route.uploadRoutes() {

    /**
     * POST /upload
     * Handles multipart file uploads with optional naming
     *
     * Form fields:
     * - uploadName (optional): Custom name for the upload batch
     * - files (multiple): The actual media files
     *
     * Returns: JSON with upload details and file paths
     */
    post("/upload") {
        try {
            val multipart = call.receiveMultipart()
            var uploadName: String? = null
            val uploadedFiles = mutableListOf<UploadedFileInfo>()
            var totalSize = 0L
            var errorResponse: ErrorResponse? = null

            // Create upload directory early
            var uploadDir: File? = null

            // Process multipart data
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        // Extract optional upload name
                        if (part.name == "uploadName") {
                            uploadName = part.value
                            // Create directory once we have the name
                            if (uploadDir == null) {
                                uploadDir = FileUtils.createUploadDirectory(uploadName)
                            }
                        }
                    }

                    is PartData.FileItem -> {
                        // Skip processing if we already have an error
                        if (errorResponse != null) {
                            part.dispose()
                            return@forEachPart
                        }

                        // Lazy-create directory if not already created
                        if (uploadDir == null) {
                            uploadDir = FileUtils.createUploadDirectory(uploadName)
                        }

                        val contentType = part.contentType?.toString()

                        // Validate media type
                        if (!FileUtils.isValidMediaType(contentType)) {
                            errorResponse = ErrorResponse(
                                error = "Invalid file type",
                                details = "File ${part.originalFileName} has unsupported type: $contentType"
                            )
                            part.dispose()
                            return@forEachPart
                        }

                        // Save file asynchronously
                        val (savedPath, fileSize, savedName) = FileUtils.saveFile(part, uploadDir!!)

                        // Validate file size after saving
                        if (!FileUtils.isValidFileSize(fileSize)) {
                            // Delete the file if it's too large
                            File(savedPath).delete()
                            errorResponse = ErrorResponse(
                                error = "File too large",
                                details = "File ${part.originalFileName} exceeds maximum size limit"
                            )
                            part.dispose()
                            return@forEachPart
                        }

                        totalSize += fileSize

                        uploadedFiles.add(
                            UploadedFileInfo(
                                originalName = part.originalFileName ?: "unknown",
                                savedPath = savedPath,
                                mimeType = contentType ?: "application/octet-stream",
                                size = fileSize,
                                uploadedAt = Clock.System.now().toString()
                            )
                        )
                    }

                    else -> part.dispose()
                }
                part.dispose()
            }

            // Check if we encountered an error during processing
            if (errorResponse != null) {
                call.respond(HttpStatusCode.BadRequest, errorResponse)
                return@post
            }

            // Validate that at least one file was uploaded
            if (uploadedFiles.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        error = "No files uploaded",
                        details = "Please provide at least one valid media file"
                    )
                )
                return@post
            }

            // Return success response
            call.respond(
                HttpStatusCode.Created,
                UploadResponse(
                    success = true,
                    message = "Successfully uploaded ${uploadedFiles.size} file(s)",
                    uploadedFiles = uploadedFiles,
                    totalFiles = uploadedFiles.size,
                    totalSize = totalSize,
                    uploadPath = uploadDir?.absolutePath ?: "unknown"
                )
            )

        } catch (e: Exception) {
            call.application.log.error("Upload failed", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = "Upload failed",
                    details = e.message ?: "Unknown error occurred"
                )
            )
        }
    }

    /**
     * GET /export
     * Zips all uploaded files and returns them as a downloadable archive
     *
     * This operation can be memory-intensive for large datasets.
     * Consider implementing pagination or selective export for production.
     */
    get("/export") {
        try {
            call.application.log.info("Starting export of all uploads")

            // Get upload statistics
            val (fileCount, totalSize) = FileUtils.getUploadStats()

            if (fileCount == 0) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        error = "No files to export",
                        details = "The uploads directory is empty"
                    )
                )
                return@get
            }

            // Create zip file asynchronously
            val zipFile = FileUtils.zipAllUploads()

            if (!zipFile.exists()) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        error = "Export failed",
                        details = "Failed to create zip archive"
                    )
                )
                return@get
            }

            call.application.log.info("Export complete: ${zipFile.name} (${fileCount} files, ${totalSize / 1024 / 1024}MB)")

            // Send the zip file
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName,
                    zipFile.name
                ).toString()
            )

            call.respondFile(zipFile)

            // Optional: Delete zip file after sending (uncomment if needed)
            zipFile.delete()
        } catch (e: Exception) {
            call.application.log.error("Export failed", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = "Export failed",
                    details = e.message ?: "Unknown error occurred"
                )
            )
        }
    }

    /**
     * GET /status
     * Returns current upload statistics
     */
    get("/status") {
        try {
            val (fileCount, totalSize) = FileUtils.getUploadStats()

            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "success" to true,
                    "totalFiles" to fileCount,
                    "totalSizeMB" to (totalSize / 1024 / 1024),
                    "totalSizeBytes" to totalSize
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = "Failed to get status",
                    details = e.message
                )
            )
        }
    }
}

