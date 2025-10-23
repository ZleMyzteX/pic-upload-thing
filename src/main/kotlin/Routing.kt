package er.codes

import er.codes.routes.uploadRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondRedirect("/static/index.html")
        }

        get("/health") {
            call.respond(mapOf("status" to "healthy"))
        }

        uploadRoutes()
        staticResources("/static", "static")
    }
}
