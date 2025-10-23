package er.codes.routes

import er.codes.models.*
import er.codes.utils.FileUtils
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.time.Instant

fun Route.uploadRoutes() {
    post("/upload") {
        try {
            val multipart = call.receiveMultipart()
            var uploadName: String? = null
            val uploadedFiles = mutableListOf<UploadedFileInfo>()
            var totalSize = 0L
            var uploadDir: File? = null
            var errorResponse: ErrorResponse? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        if (part.name == "uploadName") {
                            uploadName = part.value
                        }
                    }

                    is PartData.FileItem -> {
                        if (errorResponse != null) {
                            part.dispose()
                            return@forEachPart
                        }

                        if (uploadDir == null) {
                            uploadDir = FileUtils.createUploadDirectory(uploadName)
                        }

                        val contentType = part.contentType?.toString()
                        if (!FileUtils.isValidMediaType(contentType)) {
                            errorResponse = ErrorResponse(
                                error = "Invalid file type",
                                details = "File ${part.originalFileName} has unsupported type: $contentType"
                            )
                            part.dispose()
                            return@forEachPart
                        }

                        val (savedPath, fileSize) = FileUtils.saveFile(part, uploadDir!!)

                        if (!FileUtils.isValidFileSize(fileSize)) {
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
                                uploadedAt = Instant.now().toString()
                            )
                        )
                    }

                    else -> {}
                }
                part.dispose()
            }

            if (errorResponse != null) {
                call.respond(HttpStatusCode.BadRequest, errorResponse!!)
                return@post
            }

            if (uploadedFiles.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(error = "No files uploaded", details = "Please provide at least one valid media file")
                )
                return@post
            }

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
                ErrorResponse(error = "Upload failed", details = e.message)
            )
        }
    }

    get("/export") {
        try {
            val (fileCount, totalSize) = FileUtils.getUploadStats()

            if (fileCount == 0) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(error = "No files to export", details = "The uploads directory is empty")
                )
                return@get
            }

            val zipFile = FileUtils.zipAllUploads()

            if (!zipFile.exists()) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(error = "Export failed", details = "Failed to create zip archive")
                )
                return@get
            }

            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName,
                    zipFile.name
                ).toString()
            )

            call.respondFile(zipFile)
            zipFile.delete()
        } catch (e: Exception) {
            call.application.log.error("Export failed", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(error = "Export failed", details = e.message)
            )
        }
    }

    get("/status") {
        try {
            val (fileCount, totalSize) = FileUtils.getUploadStats()
            call.respond(
                mapOf(
                    "success" to true,
                    "totalFiles" to fileCount,
                    "totalSizeBytes" to totalSize
                )
            )
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(error = "Failed to get status", details = e.message)
            )
        }
    }
}

