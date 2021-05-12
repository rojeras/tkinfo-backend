package se.skoview.model

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

fun loadExternalInfo() {
    println("Entering loadExternalInfo()")

    runBlocking {
        val client: HttpClient = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 25_000
            }
        }

        val takUrl = "http://api.ntjp.se/coop/api/v1/installedContracts"
        val domUrl = "http://api.ntjp.se/dominfo/v1/servicedomains.json"
        val bitUrl = "https://api.bitbucket.org/2.0/repositories/rivta-domains/"
        val tpdbDomainsUrl = "https://integrationer.tjansteplattform.se/tpdb/tpdbapi.php/api/v1/domains"
        val tpdbContractsUrl = "https://integrationer.tjansteplattform.se/tpdb/tpdbapi.php/api/v1/contracts"

        val takList: Deferred<List<InstalledContracts>> = async { client.get(takUrl) }
        val domdbList: Deferred<List<DomdbServiceDomain>> = async { client.get(domUrl) }
        val tpdbDomains: Deferred<List<TpdbServiceDomain>> = async { client.get(tpdbDomainsUrl) }
        val tpdbContractList: Deferred<List<TpdbServiceContract>> = async { client.get(tpdbContractsUrl) }

        var bbDomainPagination: BbDomainPagination

        var url: String? = bitUrl
        val bitPages: Deferred<Int> = async {
            var bitFetches = 0
            while (url != null) {
                bbDomainPagination = client.get(url!!)
                bitFetches += 1
                url = bbDomainPagination.next
            }
            return@async bitFetches
        }

        println("TAK-api: number of contracts: ${takList.await().size}")
        println("TPDB: number of domains: ${tpdbDomains.await().size}")
        println("TPDB: number of contracts: ${tpdbContractList.await().size}")
        println("BitBukcet: ${bitPages.await()} pages, number of domains: ${BbDomain.mapp.size}")
        println("Domdb: number of domains: ${domdbList.await().size}")
    }
    println("Exiting loadExternalInfo()")
}
