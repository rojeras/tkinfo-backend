package com.example.model

import kotlinx.serialization.Serializable

/*
fun domainMetaLoad() {
    // fun load(callback: () -> Unit) {
    println("In domainMetaLoad()")

    val json = Json { allowStructuredMapKeys = true }

    val urlMetaInfo = "all.domainmeta.json"
    val response = getSync(urlMetaInfo)
    // json.decodeFromString(DomMetaInfo.serializer(), response)
    json.decodeFromString(ListSerializer(DomainMeta.serializer()), response)

    println("DomainMeta.mapp:")
    console.log(DomainMeta.mapp)

    // RivManager.domdbLoadingComplete()
}
*/

fun convertDomDb2MetaDomain(domainDomdb: DomdbServiceDomain): DomainMeta {
    val versions: List<DomdbVersion> =
        if (domainDomdb.versions == null)
            listOf()
        else
            domainDomdb.versions
                .filter { !it.hidden }
                .filter { !it.name.contains("RC") }
                .filter { !it.name.contains("trunk") }
                .sortedBy { it.name }
                .reversed()

    val domainVersionList = mutableListOf<DomainVersionMeta>()

    var S_review: String = ""
    var I_review: String = ""
    var T_review: String = ""

    for (version in versions) {
        // Pick up tag which might be different from version from the end of sourceControlPath
        val tag: String = version.sourceControlPath.substringAfterLast("/")

        for (review in version.reviews) {
            when (review.reviewProtocol.code) {
                "aor-s" -> S_review = review.reviewOutcome.symbol
                "aor-i" -> I_review = review.reviewOutcome.symbol
                "aor-t" -> T_review = review.reviewOutcome.symbol
            }
        }

        domainVersionList.add(
            DomainVersionMeta(
                name = version.name,
                tag = tag,
                S_review = S_review,
                I_review = I_review,
                T_review = T_review,
            )
        )
    }

    val domainVersionMetaListToAdd: List<DomainVersionMeta>? =
        if (domainVersionList.size > 0) domainVersionList
        else null

    return DomainMeta(
        name = domainDomdb.name,
        description = domainDomdb.description,
        swedishLong = domainDomdb.swedishLong,
        swedishShort = domainDomdb.swedishShort,
        domainType = domainDomdb.domainType.name,
        owner = domainDomdb.owner,
        releaseNotesUrl = domainDomdb.infoPageUrl,
        domainVersionMetas = domainVersionMetaListToAdd
    )
}

@Serializable
data class DomainMeta(
    val name: String,
    val description: String,
    val swedishLong: String,
    val swedishShort: String,
    val owner: String? = null,
    val domainType: String,
    val releaseNotesUrl: String?,
    val domainVersionMetas: List<DomainVersionMeta>?
) {
    init {
        mapp[name] = this
    }
    companion object {
        val mapp: MutableMap<String, DomainMeta> = mutableMapOf()
    }
}

@Serializable
data class DomainVersionMeta(
    val tag: String,
    val name: String,
    val S_review: String?,
    val I_review: String?,
    val T_review: String?
)
