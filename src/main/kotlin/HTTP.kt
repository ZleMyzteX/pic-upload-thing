package er.codes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.seconds

/**
 * Configures HTTP-related plugins for optimal performance and mobile client support
 *
 * Performance optimizations for 50-100 concurrent users:
 * 1. Rate limiting prevents server overload
 * 2. Compression reduces bandwidth usage for JSON responses
 * 3. Partial content enables resume functionality for large files
 * 4. CORS configured for mobile app clients
 */
fun Application.configureHTTP() {

    // CORS configuration for mobile clients (Android & iOS)
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader("uploadName") // Custom header for upload naming

        // For production, replace with specific domains:
        // allowHost("your-mobile-app.com")
        // allowHost("192.168.1.0/24") // Local network testing
        anyHost() // Development only - restrict in production!

        allowCredentials = true
    }

    // Compression for JSON responses (saves bandwidth)
    install(Compression) {
        gzip {
            priority = 1.0
            matchContentType(
                ContentType.Application.Json,
                ContentType.Text.Plain,
                ContentType.Text.Html
            )
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // Only compress files > 1KB
        }
    }

    // Partial content support for resumable downloads
    install(PartialContent) {
        maxRangeCount = 10
    }

    // Auto HEAD response support
    install(AutoHeadResponse)

    // Caching headers for static content
    install(CachingHeaders) {
        options { _, outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Text.Html -> io.ktor.http.content.CachingOptions(
                    CacheControl.MaxAge(maxAgeSeconds = 3600)
                )
                ContentType.Application.Json -> io.ktor.http.content.CachingOptions(
                    CacheControl.NoCache(null)
                )
                else -> null
            }
        }
    }

    // Rate limiting to handle concurrent users and prevent abuse
    // Allows 100 requests per user per minute
    install(RateLimit) {
        register(RateLimitName("upload")) {
            rateLimiter(limit = 100, refillPeriod = 60.seconds)
            requestKey { call ->
                // Rate limit by IP address
                call.request.local.remoteHost
            }
        }

        register(RateLimitName("export")) {
            rateLimiter(limit = 10, refillPeriod = 60.seconds)
            requestKey { call ->
                call.request.local.remoteHost
            }
        }
    }
}
