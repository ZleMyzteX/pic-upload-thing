package er.codes

import er.codes.models.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.slf4j.event.*

/**
 * Configures monitoring, logging, and error handling
 * Essential for production debugging and observability
 */
fun Application.configureMonitoring() {

    // Detailed call logging for debugging and monitoring
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val path = call.request.path()
            val duration = call.processingTimeMillis()

            "$httpMethod $path - Status: $status - Duration: ${duration}ms - Agent: $userAgent"
        }
    }

    // Global exception handling and status pages
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = "Internal server error",
                    details = if (call.application.developmentMode) cause.message else null
                )
            )
        }

        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                ErrorResponse(
                    error = "Not found",
                    details = "The requested resource was not found"
                )
            )
        }

        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(
                status,
                ErrorResponse(
                    error = "Unauthorized",
                    details = "Authentication required"
                )
            )
        }
    }
}
