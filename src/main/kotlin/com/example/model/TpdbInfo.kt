/**
 * Copyright (C) 2020 Lars Erik Röjerås
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.example.model

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val tpdbDomainMap by lazy { mutableMapOf<String, TpdbServiceDomain>() }
val tpdbContractMap: MutableMap<Pair<String, Int>, TpdbServiceContract> = mutableMapOf()

suspend fun tpdbLoad() {
    println("In tpdbLoad()")

    val client: io.ktor.client.HttpClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 25_000
        }
    }

    val urlDomains = "https://integrationer.tjansteplattform.se/tpdb/tpdbapi.php/api/v1/domains"
    client.get<List<TpdbServiceDomain>>(urlDomains)


    val urlContracts = "https://integrationer.tjansteplattform.se/tpdb/tpdbapi.php/api/v1/contracts"
    client.get<List<TpdbServiceContract>>(urlContracts)

    println("Leaving tpdbLoad()")
    client.close()
}

/*
fun tpdbLoad() {
    // fun load(callback: () -> Unit) {
    println("In tpdbload()")

    val url = "${getBaseUrl()}/https://integrationer.tjansteplattform.se/tpdb/tpdbapi.php/api/v1/domains"

    // Older version which I try again to get it to create the actual parsed objects
    getAsync(url) { response ->
        println("Size of domains are: ${response.length}")
        val json = Json { allowStructuredMapKeys = true }
        // val serviceDomains: List<TpdbServiceDomain> = json.decodeFromString(ListSerializer(TpdbServiceDomain.serializer()), response)
        val tpdbServiceDomainDto: TpdbServiceDomainDto =
            json.decodeFromString(TpdbServiceDomainDto.serializer(), response)
        console.log(tpdbServiceDomainDto)

        RivManager.refresh()
    }

    val contractsUrl = "${getBaseUrl()}/https://integrationer.tjansteplattform.se/tpdb/tpdbapi.php/api/v1/contracts"

    getAsync(contractsUrl) { response ->
        println("Size of contracts are: ${response.length}")
        val json = Json { allowStructuredMapKeys = true }
        // val serviceContracts: List<TpdbServiceContract> = json.decodeFromString(ListSerializer(TpdbServiceContract.serializer()), response)
        val tpdbServiceContractsDto: TpdbServiceContractDto =
            json.decodeFromString(TpdbServiceContractDto.serializer(), response)
        console.log(tpdbServiceContractsDto)

        RivManager.refresh()
    }
}
 */

@Serializable
data class TpdbServiceContractDto(
    val answer: List<TpdbServiceContract>,
    val lastChangeTime: String
) {
    init {
        lastUpdateTime = lastChangeTime
    }

    companion object {
        lateinit var lastUpdateTime: String
    }
}

@Serializable
data class TpdbServiceContract(
    val id: Int,
    val serviceDomainId: Int,
    val name: String,
    val namespace: String,
    val major: Int,
    val synonym: String? = null
) {
    init {
        tpdbContractMap[Pair(name, major)] = this
    }
}

@Serializable
data class TpdbServiceDomainDto(
    val answer: List<TpdbServiceDomain>,
    val lastChangeTime: String
) {
    init {
        lastUpdateTime = lastChangeTime
    }

    companion object {
        lateinit var lastUpdateTime: String
    }
}

@Serializable
data class TpdbServiceDomain(
    val id: Int,
    val domainName: String,
    val synonym: String? = null
) {
    init {
        tpdbDomainMap[domainName] = this
    }
}

fun mkHippoDomainUrl(domainName: String): String {
    val domain: TpdbServiceDomain? = tpdbDomainMap[domainName]
    if (domain == null) return ""

    if (!takInstalledDomain(domainName)) return ""

    // return "https://integrationer.tjansteplattform.se/hippo/?filter=d${domain.id}"
    return "https://integrationer.tjansteplattform.se/hippo/#/hippo/filter=d${domain.id}"
}

fun mkHippoContractUrl(contractName: String, major: Int): String {
    val contract: TpdbServiceContract? = tpdbContractMap[Pair(contractName, major)]
    if (contract == null) {
        println("Contract $contractName v$major is not used according to TPDB")
        return ""
    }

    // Handle namespace error in TPDB
    val checkedNamespace =
        if (contract.namespace.equals("urn:riv:cliniralprocess:logistics:logistics:GetCareContactsResponder:3")) "urn:riv:clinicalprocess:logistics:logistics:GetCareContactsResponder:3"
        else contract.namespace

    // A contract may have been installed earlier, but removed today. Check in TakApi if currently installed.
    if (!takInstalledContractNamespace.contains(checkedNamespace)) {
        println("Contract $contractName v$major is not installed today according to TakApi. Namespace=${contract.namespace}")
        return ""
    }

    // return "https://integrationer.tjansteplattform.se/hippo/?filter=C${contract.id}"
    return "https://integrationer.tjansteplattform.se/hippo/#/hippo/filter=C${contract.id}"
}
