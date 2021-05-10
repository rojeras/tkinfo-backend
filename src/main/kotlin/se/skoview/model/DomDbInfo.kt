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

@file:Suppress("SENSELESS_COMPARISON")

package se.skoview.model

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlin.collections.List

@Serializable
data class SearchResult(val total_count: Int, val incomplete_results: Boolean)

val DomdbDomainArr = mutableListOf<DomdbServiceDomain>()
val DomdbDomainMap = mutableMapOf<String, DomdbServiceDomain>()

suspend fun domdbLoad() {
    println("In domdbLoad()")
    val client: io.ktor.client.HttpClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 25_000
        }
    }

    client.get<List<DomdbServiceDomain>>("http://api.ntjp.se/dominfo/v1/servicedomains.json")
    println("Leaving domdbLoad()")
    client.close()
}

/*
suspend fun domdbLoad() {
    // fun load(callback: () -> Unit) {
    println("In domdbLoad()")

    // val url = "${getBaseUrl()}/http://qa.api.ntjp.se/dominfo/v1/servicedomains.json"
    val url = "${getBaseUrl()}/http://api.ntjp.se/dominfo/v1/servicedomains.json"

    // Older version which I try again to get it to create the actual parsed objects
    /*
    getAsync(url) { response ->
        println("Size of response is: ${response.length}")
        val json = Json { allowStructuredMapKeys = true }
        // val serviceDomains: List<ServiceDomain> = json.decodeFromString(ListSerializer(ServiceDomain.serializer()), response)

        val domDb: DomDb = json.decodeFromString(DomDb.serializer(), response)

        console.log(domDb)

        RivManager.domdbLoadingComplete()
    }
     */

    val client: io.ktor.client.HttpClient = HttpClient(Js) { install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 25_000
        }
    }

    val response = client.request<List<ServiceDomain>>("http://api.ntjp.se/dominfo/v1/servicedomains.json") {
        method = HttpMethod.Get
    }
    client.close()
}
*/

@Serializable
data class DomDb(
    val answer: List<DomdbServiceDomain>,
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
data class DomdbServiceDomain(
    val name: String,
    val description: String = "",
    val swedishLong: String = "",
    val swedishShort: String = "",
    val owner: String? = null,
    val hidden: Boolean,
    val domainType: DomdbDomainType,
    val issueTrackerUrl: String? = null,
    val sourceCodeUrl: String? = null,
    val infoPageUrl: String? = null,
    val interactions: Array<DomdbInteraction>? = null, //  = arrayOf<Interaction>(),
    val serviceContracts: List<DomdbContract>? = null, //  = listOf<Contract>(),
    val versions: Array<DomdbVersion>? = null,
) {
    var domainTypeString: String? = null // Used for filtering in tabulator

    init {
        if (
            interactions != null &&
            serviceContracts != null &&
            versions != null
        ) {
            DomdbDomainMap[this.name] = this
            DomdbDomainArr.add(this)
            domainTypeString = domainType.name
        } else {
            if (!this.name.isNullOrBlank()) println("${this.name} is incomplete and removed")
        }
    }
}

@Serializable
data class DomdbDomainType(
    val name: String
) {
    val type: DomdbDomainTypeEnum
        get() = when (name) {
            "Nationell tjänstedomän" -> DomdbDomainTypeEnum.NATIONAL
            "Applikationsspecifik tjänstedomän" -> DomdbDomainTypeEnum.APPLICATION_SPECIFIC
            "Extern tjänstedomän" -> DomdbDomainTypeEnum.EXTERNAL
            else -> DomdbDomainTypeEnum.UNKNOWN
        }
}

@Serializable
data class DomdbInteraction(
    var name: String,
    val namespace: String,
    val rivtaProfile: String,
    val major: Int,
    val minor: Int = 0,
    val responderContract: DomdbContract,
    val initiatorContract: DomdbContract? = null,
    val interactionDescriptions: Array<DomdbInteractionDescription> = arrayOf<DomdbInteractionDescription>()
) {
    init {
        name = if (name == "GetLaboratoryOrderOutcomenteraction") "GetLaboratoryOrderOutcomeInteraction"
        else name
    }
}

@Serializable
data class DomdbVersion(
    val name: String,
    val sourceControlPath: String = "",
    val documentsFolder: String = "",
    val interactionsFolder: String = "",
    var zipUrl: String = "",
    val hidden: Boolean,
    val descriptionDocuments: List<DomdbDescriptionDocument> = listOf<DomdbDescriptionDocument>(),
    val interactionDescriptions: Array<DomdbInteractionDescription> = arrayOf<DomdbInteractionDescription>(),
    val reviews: List<DomdbReview> = listOf<DomdbReview>()
) {
    init {
        if (zipUrl.startsWith("http://rivta.se"))
            zipUrl = zipUrl.replace("http://rivta.se", "https://rivta.se")
    }
}

@Serializable
data class DomdbContract(
    val name: String,
    val major: Int,
    var minor: Int = 0,
    val namespace: String
) {
    init {
        listBucket.add(this)
    }

    companion object {
        val listBucket: MutableSet<DomdbContract> = mutableSetOf<DomdbContract>()
    }
}

@Serializable
data class DomdbDescriptionDocument(
    val fileName: String,
    val lastChangedDate: String? = null,
    val documentType: String
) {
    val type: RivDocumentTypeEnum
        get() = when (documentType) {
            "TKB" -> RivDocumentTypeEnum.TKB
            "AB" -> RivDocumentTypeEnum.AB
            "IS" -> RivDocumentTypeEnum.IS
            else -> RivDocumentTypeEnum.UNKNOWN
        }
}

@Serializable
data class DomdbInteractionDescription(
    val description: String = "",
    val lastChangedDate: String? = null,
    val folderName: String = "",
    val wsdlFileName: String
) {
    // Parse the wsdl file name to create contractName, major and minor
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is DomdbInteractionDescription) return false
        return description == other.description &&
            // Use string representation of dates to compare, otherwise always unequal
            lastChangedDate.toString() == other.lastChangedDate.toString() &&
            folderName == other.folderName &&
            wsdlFileName == other.wsdlFileName
    }
}

@Serializable
data class DomdbReview(
    val reviewProtocol: DomdbReviewProtocol,
    val reviewOutcome: DomdbReviewOutcome,
    var reportUrl: String = ""
) {
    init {
        if (reportUrl.startsWith("http://rivta.se"))
            reportUrl = reportUrl.replace("http://rivta.se", "https://rivta.se")
    }
}

@Serializable
data class DomdbReviewProtocol(
    val name: String,
    val code: String
)

@Serializable
data class DomdbReviewOutcome(
    val name: String,
    val symbol: String
)

enum class DomdbDomainTypeEnum(val displayName: String) {
    NATIONAL("Nationell"),
    APPLICATION_SPECIFIC("Applikationsspecifik"),
    EXTERNAL("Extern"),
    UNKNOWN("Okänd")
}

enum class RivDocumentTypeEnum {
    TKB,
    AB,
    IS,
    UNKNOWN
}

fun DomdbServiceDomain.getDescription(): String {
    return this.description
}

/**
 * Return the three parts of a TK identifier in a Triple
 *
 * @return Triple(ContractName; String, MajorVersion: Int, MinorVersion: Int)
 *
 */
fun DomdbInteractionDescription.wsdlContract(): Triple<String, Int, Int> {
    val aList = wsdlFileName.split("_")
    val nameInteraction = aList[0]
    val name = nameInteraction.removeSuffix("Interaction")
    val version = aList[1]
    val versionList = version.split(".")
    val major = versionList[0]
    val minor = versionList[1]
    return Triple(name, major.toInt(), minor.toInt())
}

fun DomdbVersion.getDocumentsAndChangeDate(): List<DomdbDescriptionDocument> {
    return this.descriptionDocuments.toList()
}
