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
package com.example.model

import kotlinx.serialization.Serializable

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

val tiDomainStorage = mutableListOf<TiDomain>()

fun mkTkInfoInfo() {
    println("In mkTkInfoInfo()")

    for (bbDomainEntry in BbDomain.mapp) {

        val bbDomain = bbDomainEntry.value

        val domainName = bbDomain.name.removePrefix("riv.").replace(".", ":")

        // Does this domain exist in domdb?
        if (DomdbDomainMap[domainName] == null) {
            println("No visible version for ${bbDomain.name}")
            continue
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

        val issuesUrl: String? =
            if (bbDomain.links?.issues != null) bbDomain.links.issues.href
            else null

        val sourceUrl: String? =
            if (bbDomain.links?.source != null) bbDomain.links.source.href
            else null

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
                issuesUrl = issuesUrl,
                sourceUrl = sourceUrl,
                releaseNotesUrl = ddDomain.infoPageUrl,
                bbiUid = bbDomain.uuid,
                // todo: Empty hippoUrl should be changed to null
                hippoUrl = mkHippoDomainUrl(domainName),
                // versions = null
            )
        tiDomainStorage.add(tiDomain)
    }
    println("All TiDomains, there are ${tiDomainStorage.size}")
    for (dom in tiDomainStorage) {
        println(dom)
    }
}

enum class TiDomainTypeEnum {
    NATIONAL,
    EXTERNAL,
    APPLICATION_SPECIFIC,
    UNKNOWN
}

enum class TiSourceEnum {
    DOMDB,
    BITBUCKET,
    DOMAIN_META_DATA,
    COMMON_META_DATA,
    TPDB,
    CALCULATED
}

@Serializable
data class TiDomain(
    val id: Int = idIx++,
    val name: String,
    val swedishShortName: String? = null,
    val swedishLongName: String? = null,
    val domainType: TiDomainTypeEnum? = null,
    val description: String,
    val owner: String?,
    val issuesUrl: String?,
    val sourceUrl: String?,
    val releaseNotesUrl: String?,
    val bbiUid: String = "",
    val hippoUrl: String?,
    // val versions: List<DomainVersion>?
) {

    init {
        mapp[name] = this
    }

    companion object {
        var idIx = 0
        val mapp: MutableMap<String, TiDomain> = mutableMapOf()
    }
}

@Serializable
data class DomainVersion(
    val name: String,
    val tag: String,
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
    val partOfDomainVersions: List<DomainVersion>
)

@Serializable
data class Review(
    val area: String,
    val result: String,
    val reportUrl: String
)
