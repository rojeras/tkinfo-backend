/**
 * Copyright (C) 2021 Lars Erik Röjerås
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
package se.skoview.model

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.get
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.serialization.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import se.skoview.myHost
import se.skoview.myPort
import se.skoview.plugins.tiDomainStorage

/**
 * Populate TkView objects from Bitbucket, DOMDB-api, TAK-api and TPDB-api
 *
 * Algorithm:
 *  1. Go through all repos in bitbucket
 *  2. Filter and keep those that exists in DOMDB
 *  3. Populate the TiDomains:
 *      a. Based on content in DOMDB
 *      b. By iterating through all relevant information in bitbucket
 *  4. Populate TiContracts during b. above
 *
 */

// val tiDomainStorage = mutableListOf<TiDomain>()

fun mkTkInfoInfo(client: io.ktor.client.HttpClient) {
    println("In mkTkInfoInfo()")

    /*
     val client: io.ktor.client.HttpClient = HttpClient(CIO) {
         install(JsonFeature) {
             serializer = KotlinxSerializer()
         }
         install(HttpTimeout) {
             requestTimeoutMillis = 25_000
         }
     }
     */

    // Loop through all domains in bitbucket
    for (bbDomainEntry in BbDomain.mapp) {

        val bbDomain = bbDomainEntry.value

        // FOR TESTING PURPOSES
        if (bbDomain.name.startsWith("riv.clinicalprocess.activity"))
            mkTkInfoDomain(client, bbDomain)
    }

    client.close()

    println("All TiDomains, there are ${tiDomainStorage.size}")
}

fun mkTkInfoDomain(client: io.ktor.client.HttpClient, bbDomain: BbDomain) {

    val bbDomainName = bbDomain.name
    val domainName = bbDomainName.removePrefix("riv.").replace(".", ":")

    // Does this domain exist in domdb?
    if (DomdbDomainMap[domainName] == null) {
        // println("No visible version for ${bbDomain.name}")
        return
    }

    // We will never get here if ddDomain is not set
    val ddDomain: DomdbServiceDomain = DomdbDomainMap[domainName]!!

    // val visibleTags = domainMeta.domainVersions.map { it.tag }

    val tiDomainType: TiDomainTypeEnum? = when (ddDomain.domainType.name) {
        "Extern" -> TiDomainTypeEnum.EXTERNAL
        "Nationell tjänstedomän" -> TiDomainTypeEnum.NATIONAL
        "Applikationsspecifik tjänstedomän" -> TiDomainTypeEnum.APPLICATION_SPECIFIC
        else -> null
    }

    val bbBaseUrl = "https://bitbucket.org/rivta-domains"

    val issuesUrl: TiHref? =
        if (bbDomain.links?.issues != null) // TiHref(bbDomain.links.issues.href)
            TiHref("$bbBaseUrl/$bbDomainName/issues")
        else null

    val sourceUrl: TiHref = TiHref("$bbBaseUrl/$bbDomainName/src")
    /*
            if (bbDomain.links?.source != null) TiHref(bbDomain.links.source.href)
            else null
         */

    val hippoUrl: TiHref? =
        if (!mkHippoDomainUrl(domainName).isNullOrBlank()) TiHref(mkHippoDomainUrl(domainName))
        else null

    val releaseNotesUrl: TiHref? =
        if (ddDomain.infoPageUrl != null) TiHref(ddDomain.infoPageUrl)
        else null

    val tiLinks = TiLinks(
        self = TiHref("http://$myHost:$myPort/domains/$domainName"),
        issues = issuesUrl,
        source = sourceUrl,
        hippo = hippoUrl,
        `release-notes` = releaseNotesUrl,
    )

    // Time to collect information about tags/versions
    val versions = mkTiDomainVersions(client, bbDomain)

    /*
 
         val tkvOwner =
             if (!domainMeta.owner.isNullOrBlank())
                 String(domainMeta.owner, TkvSourceEnum.COMMON_META_DATA)
             else null
 
         val tkvReleaseNotesUrl =
             if (!domainMeta.releaseNotesUrl.isNullOrEmpty())
                 String(
                     domainMeta.releaseNotesUrl,
                     TkvSourceEnum.COMMON_META_DATA
                 )
             else null
 
         // Information from bitbucket
 
         val domainBaseUrl = "https://bitbucket.org/rivta-domains/${domainName.replace(":", ".")}"
         val domainApiBaseUrl =
             "https://api.bitbucket.org/2.0/repositories/rivta-domains/${domainName.replace(":", ".")}"
         val domainApiSrcBaseUrl = "$domainApiBaseUrl/src"
 
         // https://bitbucket.org/rivta-domains/riv.clinicalprocess.activity.actions/issues
         val tkvIssuesUrl = if (bbDomain.value.has_issues)
             String(
                 "$domainBaseUrl/issues",
                 TkvSourceEnum.CALCULATED
             )
         else null
 
         // Go through all versions and gather information
         for (version in domainMeta.domainVersionMetas) {
             val tkvDomVerTag = version.tag
             val tkvDomVerName = version.name
             val tagSourceUrl = "$domainApiSrcBaseUrl/$tkvDomVerTag/"
             // val response = getSync(tagUrl)
             println(tagSourceUrl)
         }
 
 /*
 To get the source of a certain tag
 1. Fetch the tag: https://api.bitbucket.org/2.0/repositories/rivta-domains/riv.clinicalprocess.logistics.logistics/refs/tags/3.0.7_RC1
 2. Read the hash = target.hash
 3. Get source: https://api.bitbucket.org/2.0/repositories/rivta-domains/riv.clinicalprocess.logistics.logistics/src/$hash/ -- VERY IMPORTANT that this URL ends with a slash
  */
 */
    val tiDomain =
        TiDomain(
            name = domainName,
            swedishShortName = ddDomain.swedishShort,
            swedishLongName = ddDomain.swedishLong,
            domainType = tiDomainType,
            description = ddDomain.description,
            owner = ddDomain.owner,
            links = tiLinks,
            bbiUid = bbDomain.uuid,
            versions = versions
        )
    tiDomainStorage.add(tiDomain)
}

/**
 * Find and return all tags which match a version entry in DomDB
 */
fun mkTiDomainVersions(client: io.ktor.client.HttpClient, bbDomain: BbDomain): List<TiDomainVersion>? {
    println("In mkTiDomainVersions()")
    val bbDomainName = bbDomain.name
    val domainName = bbDomainName.removePrefix("riv.").replace(".", ":")

    println("domanName: $domainName")
    if (domainName == "supportprocess:logistics:scheduling") println("domdbDomain: ${DomdbDomainMap[domainName]}")
    if (!DomdbDomainMap.containsKey(domainName)) {
        println("  DomDb does not contain this domain")
        return null
    } else {
        if (DomdbDomainMap[domainName]?.versions == null) {
            println("  The domain does not contain any versions i DomDb")
            return null
        }
    }

/*
    1. Fetch all tags for the current domain
        https://api.bitbucket.org/2.0/repositories/rivta-domains/riv.eservicesupply.eoffering/refs/tags
    2. Match with domdb versions (tag and name through sourceControlPath
    3. Get the hash of the commit of the tag/version of interest
    4. Fetch all source files through:
        https://api.bitbucket.org/2.0/repositories/rivta-domains/riv.clinicalprocess.logistics.logistics/src/d5b1153f8f82303ff93d5a727c2e065fbe7103aa/
 */

    /*
    println("A")
    for (ddVersion in DomdbDomainMap[domainName]?.versions!!) {
        val tag = ddVersion.sourceControlPath.substringAfterLast("/")
        println("$domainName : name = ${ddVersion.name}, tag = $tag")
    }
     */

    if (bbDomain.links?.tags == null) return null

    // if (bbDomain.links == null) return null
    // else if (bbDomain.links.tags == null) return null

    val bbTagsUrl: String = bbDomain.links.tags.href

    var url: String? = bbTagsUrl
    var bbTagPagination: BbTagPagination

    var page = 0

    val tagList: MutableList<BbTag> = mutableListOf()

    runBlocking {
        while (url != null) {
            page += 1
            bbTagPagination = client.request(url!!)
            bbTagPagination.values?.let { tagList.addAll(it) }
            url = bbTagPagination.next
        }
    }

    // We now have a list of tags from bitbucket
    println("  Number of tags: ${tagList.size}")

    // Filter the list to only include tags that correspond to a version in DomDB
    if (DomdbDomainMap[domainName]?.versions == null) {
        println("No versions found of domain $domainName in DomDb")
    }

    val ddVersionList: Array<DomdbVersion>? = DomdbDomainMap[domainName]!!.versions
    if (ddVersionList != null) {
        for (ddVersion in ddVersionList) {
            if (ddVersion.name == "trunk") continue
            println("Version from DomDB: tagName=${ddVersion.tagName}, name=${ddVersion.name}")
            val bbTagList = tagList.filter { it.name == ddVersion.tagName }
                if (bbTagList.isNullOrEmpty()) {
                    // println("Filtering resulted in empty list when ddVersion.tagName = ${ddVersion.tagName}")
                    continue
                }
                val bbTag =  bbTagList[0]

            println("   bbTag <--> ddVersion : ${bbTag.name} <--> ${ddVersion.name}")
        }
    }

    return null
}

enum class TiDomainTypeEnum {
    NATIONAL,
    EXTERNAL,
    APPLICATION_SPECIFIC,
    UNKNOWN
}

@Serializable
data class TiDomain(
    val name: String,
    val swedishShortName: String? = null,
    val swedishLongName: String? = null,
    val domainType: TiDomainTypeEnum? = null,
    val description: String,
    val owner: String?,
    val bbiUid: String = "",
    val links: TiLinks,
    val versions: List<TiDomainVersion>? = null
) {

    init {
        NAME_MAPP[name] = this
    }

    companion object {
        val NAME_MAPP: MutableMap<String, TiDomain> = mutableMapOf()
    }
}

@Serializable
data class TiLinks(
    val self: TiHref,
    val source: TiHref? = null,
    val issues: TiHref? = null,
    val versions: TiHref? = null,
    val html: TiHref? = null,
    val hippo: TiHref? = null,
    val `release-notes`: TiHref? = null
)

@Serializable
data class TiHref(val href: String)

@Serializable
data class TiDomainVersion(
    val name: String, // Is the version name, ex 1.0
    val tag: String, // Is the corresponding tag, ought to be the same as name but might be different (see domdb version end part of sourceControlPath (EOFFERING_1_0_0)
    val tkbUrl: String,
    val abUrl: String,
    val zipUrl: String,
    val contracts: List<TiContract>,
    val reviews: List<Review>,
    val parent: TiDomain
)

@Serializable
data class TiContract(
    val name: String,
    val domain: TiDomain,
    val description: String,
    val versions: List<TiContractVersion>,
) {
    init {
        mapp[name] = this
    }

    companion object {
        val mapp: MutableMap<String, TiContract> = mutableMapOf()
    }
}

@Serializable
data class TiContractVersion(
    val major: Int,
    val minor: Int,
    val parent: TiContract,
    val hippoUrl: String?,
    val partOfTiDomainVersions: List<TiDomainVersion>
)

@Serializable
data class Review(
    val area: String,
    val result: String,
    val reportUrl: String
)
