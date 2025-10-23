package er.codes

import er.codes.routes.uploadRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Configures all application routes
 * Modular routing structure for maintainability
 */
fun Application.configureRouting() {
    routing {
        // Serve index.html at root path
        get("/") {
            call.respondRedirect("/static/index.html")
        }

        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
        }

        // Upload and export routes
        uploadRoutes()

        // Static resources (CSS, JS, images, and index.html)
        staticResources("/static", "static")
    }
}
