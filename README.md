# Media Upload Server - Production-Ready Ktor Backend

A high-performance Ktor backend server designed for handling media uploads from Android and iOS mobile clients. Supports concurrent uploads from 50-100 users with proper rate limiting, compression, and async file handling.

## Features

✅ **Multi-format Support**
- Images: JPG, JPEG, PNG, GIF, WebP, HEIC, HEIF, BMP, TIFF, SVG
- Videos: MP4, MOV, AVI, MKV, WebM, MPEG, 3GP, FLV

✅ **Production-Ready Performance**
- Handles 50-100 concurrent users
- Async file I/O with Kotlin coroutines
- Rate limiting (100 requests/minute per IP)
- Request compression (gzip/deflate)
- Streaming uploads (no memory overload)

✅ **Mobile-Friendly**
- CORS enabled for cross-origin requests
- Multipart form data support
- Optional upload naming ("Vacation Photos")
- Progress-trackable uploads
- Resumable downloads (partial content)

✅ **API Endpoints**
- `POST /upload` - Upload multiple media files
- `GET /export` - Download all uploads as ZIP
- `GET /status` - Get upload statistics
- `GET /health` - Health check endpoint

## Quick Start

### 1. Prerequisites
- JDK 17 or higher
- Gradle 8.x (included via wrapper)

### 2. Build and Run

```bash
# Build the project
./gradlew build

# Run the server
./gradlew run

# Server starts on http://localhost:8080
```

### 3. Test the Upload Endpoint

```bash
# Upload a single image
curl -X POST http://localhost:8080/upload \
  -F "uploadName=Test Upload" \
  -F "files=@/path/to/image.jpg"

# Upload multiple files
curl -X POST http://localhost:8080/upload \
  -F "uploadName=Vacation Photos" \
  -F "files=@photo1.jpg" \
  -F "files=@photo2.jpg" \
  -F "files=@video.mp4"

# Get upload statistics
curl http://localhost:8080/status

# Download all uploads as ZIP
curl -O http://localhost:8080/export
```

## Project Structure

```
src/main/kotlin/
├── Application.kt              # Main entry point and module configuration
├── HTTP.kt                     # HTTP plugins (CORS, compression, rate limiting)
├── Serialization.kt            # JSON serialization configuration
├── Monitoring.kt               # Logging, metrics, and error handling
├── Routing.kt                  # Route configuration
├── models/
│   └── UploadModels.kt        # Data models for requests/responses
├── routes/
│   └── UploadRoutes.kt        # Upload and export route handlers
└── utils/
    └── FileUtils.kt           # File operations and validation

src/main/resources/
├── application.yaml           # Server configuration
└── logback.xml               # Logging configuration

client-examples/
├── AndroidClient.kt          # Android/Kotlin client example
└── iOSClient.swift          # iOS/Swift client example
```

## API Documentation

### POST /upload

Upload one or multiple media files with optional naming.

**Request:**
- Content-Type: `multipart/form-data`
- Form Fields:
  - `uploadName` (optional): Custom name for the upload batch
  - `files` (required, multiple): Media files to upload

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Successfully uploaded 3 file(s)",
  "uploadedFiles": [
    {
      "originalName": "photo.jpg",
      "savedPath": "/uploads/vacation/20250423_143022/photo.jpg",
      "mimeType": "image/jpeg",
      "size": 2048576,
      "uploadedAt": "2025-04-23T14:30:22.123Z"
    }
  ],
  "totalFiles": 3,
  "totalSize": 5242880,
  "uploadPath": "/uploads/vacation/20250423_143022"
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "error": "Invalid file type",
  "details": "File video.avi has unsupported type: video/x-msvideo"
}
```

### GET /export

Download all uploaded files as a ZIP archive.

**Response:**
- Content-Type: `application/zip`
- Content-Disposition: `attachment; filename="uploads_export_1234567890.zip"`
- Binary ZIP file containing all uploads

### GET /status

Get current upload statistics.

**Response (200 OK):**
```json
{
  "success": true,
  "totalFiles": 42,
  "totalSizeMB": 350,
  "totalSizeBytes": 367001600
}
```

## Configuration

### application.yaml

```yaml
ktor:
  deployment:
    port: 8080                    # Server port
    connectionGroupSize: 4        # Netty connection threads
    workerGroupSize: 8            # Netty worker threads
    callGroupSize: 16             # Call processing threads
    maxContentLength: 524288000   # Max request size (500MB)
```

### Environment Variables

```bash
# Upload directory (default: ./uploads)
export UPLOAD_DIR=/var/uploads

# Server port (overrides application.yaml)
export PORT=8080
```

## Performance Tuning

### For 50-100 Concurrent Users

The server is pre-configured for optimal performance:

1. **Async I/O**: All file operations use `Dispatchers.IO` to prevent blocking
2. **Streaming Uploads**: Files stream directly to disk (8KB buffer)
3. **Rate Limiting**: 100 requests/minute per IP prevents abuse
4. **Compression**: Automatic gzip/deflate for JSON responses
5. **Thread Pools**: Netty configured with optimal thread counts

### Scaling Beyond 100 Users

For higher concurrency:

```yaml
# application.yaml
ktor:
  deployment:
    connectionGroupSize: 8      # 2x CPU cores
    workerGroupSize: 16         # 4x CPU cores
    callGroupSize: 32           # 8x CPU cores
```

Increase rate limits in `HTTP.kt`:
```kotlin
rateLimiter(limit = 200, refillPeriod = 60.seconds)
```

## Mobile Client Examples

### Android (Kotlin + Ktor Client)

See `client-examples/AndroidClient.kt` for complete implementation.

```kotlin
val uploadClient = MediaUploadClient("http://your-server.com:8080")

uploadClient.uploadMedia(
    files = selectedUris,
    uploadName = "Vacation Photos",
    context = context,
    onProgress = { progress -> 
        // Update UI: 0.0 to 1.0
    }
).onSuccess { response ->
    // Handle success
}
```

### iOS (Swift + URLSession)

See `client-examples/iOSClient.swift` for complete implementation.

```swift
let uploadClient = MediaUploadClient(baseURL: "http://your-server.com:8080")

uploadClient.uploadMedia(
    fileURLs: selectedFiles,
    uploadName: "Vacation Photos",
    progressHandler: { progress in
        // Update UI: 0.0 to 1.0
    },
    completion: { result in
        // Handle result
    }
)
```

## File Storage

Files are organized as:
```
uploads/
├── vacation-photos/
│   └── 20250423_143022/
│       ├── image1.jpg
│       ├── image2.png
│       └── video.mp4
└── unnamed/
    └── 20250423_150130/
        └── file.jpg
```

## Security Considerations

### Production Checklist

- [ ] **CORS**: Replace `anyHost()` with specific domains in `HTTP.kt`
- [ ] **File Validation**: Validate file signatures (magic bytes), not just MIME types
- [ ] **Storage Limits**: Implement disk quota monitoring
- [ ] **Authentication**: Add user authentication (JWT, OAuth)
- [ ] **HTTPS**: Deploy behind reverse proxy with SSL (nginx, Caddy)
- [ ] **Rate Limiting**: Tune per your expected traffic
- [ ] **Monitoring**: Set up external monitoring (Prometheus, Grafana)

### Example CORS Production Config

```kotlin
install(CORS) {
    allowHost("your-app.com")
    allowHost("api.your-app.com", schemes = listOf("https"))
    allowCredentials = true
}
```

## Testing

```bash
# Run tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

## Deployment

### Docker

```dockerfile
FROM gradle:8-jdk17 AS build
COPY . /app
WORKDIR /app
RUN gradle shadowJar

FROM openjdk:17-slim
COPY --from=build /app/build/libs/*-all.jar /app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app.jar"]
```

### Build Commands

```bash
# Create fat JAR
./gradlew shadowJar

# Run fat JAR
java -jar build/libs/pic-upload-thing-all.jar
```

## Troubleshooting

### Issue: "Connection refused" from mobile

**Solution:** Use correct IP address:
- Android emulator: `http://10.0.2.2:8080`
- iOS simulator: `http://localhost:8080`
- Real device: `http://192.168.1.x:8080` (your computer's local IP)

### Issue: "File too large" error

**Solution:** Files are limited to 500MB. Increase in `FileUtils.kt`:
```kotlin
private const val MAX_FILE_SIZE = 1000L * 1024 * 1024 // 1GB
```

### Issue: Rate limit exceeded

**Solution:** Increase rate limit in `HTTP.kt` or implement per-user limits.

## License

MIT License - Feel free to use in your projects!

## Support

For issues and questions, please open an issue on GitHub.

