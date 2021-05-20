package se.skoview

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import se.skoview.model.*
import se.skoview.plugins.tiDomainStorage

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

var myHost = "localhost"
var myPort = "8080"

fun Application.module() {
    println("In Application.module()")

    myHost = environment.config.propertyOrNull("ktor.deployment.host")?.getString() ?: myHost
    myPort = environment.config.propertyOrNull("ktor.deployment.port")?.getString() ?: myPort

    // There might be better ways, see https://ktor.io/docs/requests.html#route_parameters 
    println("I am $myHost:$myPort")

    loadExternalInfo()
    // mkTkInfoInfo()

    println("Number of ti-domains: ${tiDomainStorage.size}")

    install(ContentNegotiation) {
        json(
            kotlinx.serialization.json.Json {
                encodeDefaults = false // Removes null values from response (and all other defaults!)
                ignoreUnknownKeys = true
            }
        )
    }

    routing {
        get("/") {
            call.respondText("Hello, världen!")
        }
        get("/domains") {
            call.respond(tiDomainStorage)
        }
        get("/domains/{name}") {
            val name = call.parameters["name"]
            val domain: TiDomain? = TiDomain.NAME_MAPP[name]
            if (domain != null) {
                call.response.status(HttpStatusCode.OK) // Seems to be default
                call.respond(domain)
            } else call.response.status(HttpStatusCode.NotFound)
        }
    }
}
