package se.skoview

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.DefaultRequest.Feature.install
import io.ktor.client.features.get
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import se.skoview.model.*
import se.skoview.plugins.tiDomainStorage

/*
suspend fun main() {
    println("In main()")
    takApiLoad()
    domdbLoad()
    bitbucketLoad()
    tpdbLoad()
    mkTkInfoInfo()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
    }.start(wait = true)
}
*/
fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    println("In Application.module()")

    loadExternalInfo()
    // mkTkInfoInfo()

    println("Number of ti-domains: ${tiDomainStorage.size}")

    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/") {
            call.respondText("Hello, världen!")
        }
        get("/domains") {
            call.respond(tiDomainStorage)
        }
    }
}

fun loadExternalInfo() {
    println("Entering loadExternalInfo()")

    runBlocking {
        val client: io.ktor.client.HttpClient = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 25_000
            }
        }

        val url1 = "http://api.ntjp.se/coop/api/v1/installedContracts"
        val url2 = "http://api.ntjp.se/dominfo/v1/servicedomains.json"

        val tak: Deferred<String> = async { client.get(url1) }
        val dd: Deferred<String> = async { client.get(url2) }

        println("TAK-api size: ${tak.await().length}")
        println("Domdb size: ${dd.await().length}")
        // takApiLoad()
        // domdbLoad()
        // bitbucketLoad()
        // tpdbLoad()
    }
    println("Exiting loadExternalInfo()")
}