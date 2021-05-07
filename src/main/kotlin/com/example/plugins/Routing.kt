package com.example.plugins

import com.example.model.TiDomain
import com.example.model.tiDomainStorage
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Application.configureRouting() {
    // Starting point for a Ktor app:
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        get("/domains") {
            call.respond(tiDomainStorage)
            println("There are ${getNumberOfDomains()} domains.")
        }
    }
}

fun getNumberOfDomains(): Int {

    println("In getNumberOfDomains: ${tiDomainStorage.size}")
    return tiDomainStorage.size
}