package er.codes.utils

import io.ktor.http.content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileUtils {
    private val BASE_UPLOAD_DIR = System.getenv("UPLOAD_DIR") ?: "uploads"
    private const val MAX_FILE_SIZE = 500L * 1024 * 1024

    private val SUPPORTED_IMAGE_TYPES = setOf(
        "image/jpeg", "image/jpg", "image/png", "image/gif",
        "image/webp", "image/heic", "image/heif", "image/bmp",
        "image/tiff", "image/svg+xml"
    )

    private val SUPPORTED_VIDEO_TYPES = setOf(
        "video/mp4", "video/quicktime", "video/x-msvideo",
        "video/x-matroska", "video/webm", "video/mpeg",
        "video/3gpp", "video/x-flv"
    )

    fun isValidMediaType(contentType: String?): Boolean {
        if (contentType == null) return false
        return contentType in SUPPORTED_IMAGE_TYPES || contentType in SUPPORTED_VIDEO_TYPES
    }

    fun isValidFileSize(size: Long): Boolean = size > 0 && size <= MAX_FILE_SIZE

    fun createUploadDirectory(uploadName: String?): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val safeName = sanitizeFileName(uploadName ?: "unnamed")
        val uploadDir = File(BASE_UPLOAD_DIR, "$safeName/$timestamp")
        uploadDir.mkdirs()
        return uploadDir
    }

    private fun sanitizeFileName(fileName: String): String {
        return fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_").take(100)
    }

    @Suppress("DEPRECATION")
    suspend fun saveFile(partData: PartData.FileItem, targetDirectory: File): Pair<String, Long> =
        withContext(Dispatchers.IO) {
            val originalFileName = partData.originalFileName ?: "unnamed_file"
            val sanitizedName = sanitizeFileName(originalFileName)
            val targetFile = File(targetDirectory, sanitizedName)
            var bytesCopied = 0L

            partData.streamProvider().use { input ->
                targetFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesCopied += bytesRead
                    }
                }
            }

            Pair(targetFile.absolutePath, bytesCopied)
        }

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

    private fun zipDirectory(baseDir: File, currentDir: File, zipOut: ZipOutputStream) {
        currentDir.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> zipDirectory(baseDir, file, zipOut)
                file.isFile -> {
                    val relativePath = file.relativeTo(baseDir).path
                    zipOut.putNextEntry(ZipEntry(relativePath))
                    FileInputStream(file).buffered().use { it.copyTo(zipOut, 8192) }
                    zipOut.closeEntry()
                }
            }
        }
    }

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
}


