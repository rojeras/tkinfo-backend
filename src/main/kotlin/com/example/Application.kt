package com.example

import com.example.model.*
import com.example.plugins.*
import io.ktor.application.*
import io.ktor.client.features.DefaultRequest.Feature.install
import io.ktor.features.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

suspend fun main() {
    takApiLoad()
    domdbLoad()
    bitbucketLoad()
    tpdbLoad()
    mkTkInfoInfo()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
}