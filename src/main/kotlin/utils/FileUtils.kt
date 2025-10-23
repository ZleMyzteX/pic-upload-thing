package er.codes.utils

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Utility class for handling file operations with async support for high concurrency
 */
object FileUtils {

    // Base upload directory - can be configured via environment variable
    private val BASE_UPLOAD_DIR = System.getenv("UPLOAD_DIR") ?: "uploads"

    // Supported MIME types for images
    private val SUPPORTED_IMAGE_TYPES = setOf(
        "image/jpeg", "image/jpg", "image/png", "image/gif",
        "image/webp", "image/heic", "image/heif", "image/bmp",
        "image/tiff", "image/svg+xml"
    )

    // Supported MIME types for videos
    private val SUPPORTED_VIDEO_TYPES = setOf(
        "video/mp4", "video/quicktime", "video/x-msvideo",
        "video/x-matroska", "video/webm", "video/mpeg",
        "video/3gpp", "video/x-flv"
    )

    // Maximum file size: 500MB per file (configurable)
    private const val MAX_FILE_SIZE = 500L * 1024 * 1024

    /**
     * Validates if the content type is supported for media uploads
     */
    fun isValidMediaType(contentType: String?): Boolean {
        if (contentType == null) return false
        return SUPPORTED_IMAGE_TYPES.contains(contentType) ||
               SUPPORTED_VIDEO_TYPES.contains(contentType)
    }

    /**
     * Validates file size is within acceptable limits
     */
    fun isValidFileSize(size: Long): Boolean {
        return size > 0 && size <= MAX_FILE_SIZE
    }

    /**
     * Creates a unique upload directory based on optional name and timestamp
     * Format: uploads/{uploadName or "unnamed"}/{timestamp}/
     */
    fun createUploadDirectory(uploadName: String?): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val safeName = sanitizeFileName(uploadName ?: "unnamed")
        val uploadDir = File(BASE_UPLOAD_DIR, "$safeName/$timestamp")

        if (!uploadDir.exists()) {
            uploadDir.mkdirs()
        }

        return uploadDir
    }

    /**
     * Sanitizes filename to prevent directory traversal and special characters
     */
    fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            .take(100) // Limit filename length
    }

    /**
     * Saves a multipart file item to disk asynchronously
     * Uses IO dispatcher for non-blocking file operations
     */
    suspend fun saveFile(
        partData: PartData.FileItem,
        targetDirectory: File
    ): Triple<String, Long, String> = withContext(Dispatchers.IO) {
        val originalFileName = partData.originalFileName ?: "unnamed_file"
        val sanitizedName = sanitizeFileName(originalFileName)
        val targetFile = File(targetDirectory, sanitizedName)

        var bytesCopied = 0L

        // Stream the file directly to disk to handle large files efficiently
        partData.streamProvider().use { input ->
            targetFile.outputStream().buffered().use { output ->
                val buffer = ByteArray(8192) // 8KB buffer for optimal performance
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    bytesCopied += bytesRead
                }
            }
        }

        Triple(targetFile.absolutePath, bytesCopied, sanitizedName)
    }

    /**
     * Zips all files in the upload directory recursively
     * Returns the zip file for download
     */
    suspend fun zipAllUploads(): File = withContext(Dispatchers.IO) {
        val uploadDir = File(BASE_UPLOAD_DIR)
        val zipFileName = "uploads_export_${System.currentTimeMillis()}.zip"
        val zipFile = File(uploadDir.parent ?: ".", zipFileName)

        ZipOutputStream(FileOutputStream(zipFile).buffered()).use { zipOut ->
            if (uploadDir.exists() && uploadDir.isDirectory) {
                zipDirectory(uploadDir, uploadDir, zipOut)
            }
        }

        zipFile
    }

    /**
     * Recursively adds files to zip archive
     */
    private fun zipDirectory(baseDir: File, currentDir: File, zipOut: ZipOutputStream) {
        currentDir.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> zipDirectory(baseDir, file, zipOut)
                file.isFile -> {
                    val relativePath = file.relativeTo(baseDir).path
                    val zipEntry = ZipEntry(relativePath)
                    zipOut.putNextEntry(zipEntry)

                    FileInputStream(file).buffered().use { input ->
                        input.copyTo(zipOut, bufferSize = 8192)
                    }

                    zipOut.closeEntry()
                }
            }
        }
    }

    /**
     * Gets file count and total size of all uploads
     */
    suspend fun getUploadStats(): Pair<Int, Long> = withContext(Dispatchers.IO) {
        val uploadDir = File(BASE_UPLOAD_DIR)
        var fileCount = 0
        var totalSize = 0L

        fun processDirectory(dir: File) {
            dir.listFiles()?.forEach { file ->
                when {
                    file.isDirectory -> processDirectory(file)
                    file.isFile -> {
                        fileCount++
                        totalSize += file.length()
                    }
                }
            }
        }

        if (uploadDir.exists()) {
            processDirectory(uploadDir)
        }

        Pair(fileCount, totalSize)
    }

    /**
     * Gets file extension from content type
     */
    fun getExtensionFromContentType(contentType: String?): String {
        return when (contentType) {
            "image/jpeg", "image/jpg" -> ".jpg"
            "image/png" -> ".png"
            "image/gif" -> ".gif"
            "image/webp" -> ".webp"
            "image/heic" -> ".heic"
            "image/heif" -> ".heif"
            "video/mp4" -> ".mp4"
            "video/quicktime" -> ".mov"
            "video/x-msvideo" -> ".avi"
            "video/x-matroska" -> ".mkv"
            "video/webm" -> ".webm"
            else -> ""
        }
    }
}


