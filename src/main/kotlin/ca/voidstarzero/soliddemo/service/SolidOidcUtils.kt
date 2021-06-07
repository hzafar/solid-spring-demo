package ca.voidstarzero.soliddemo.service

import ca.voidstarzero.soliddemo.dpop.DPoPUtils
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class SolidOidcUtils(
    private val dpopUtils: DPoPUtils,
    private val restTemplate: RestTemplate
)
{
    fun authHeaders(
        authToken: String,
        sessionId: String,
        method: String,
        requestURI: String
    ): HttpHeaders
    {
        val headers = HttpHeaders()
        headers.add("Authorization", "DPoP $authToken")
        dpopUtils.sessionKey(sessionId)?.let { key ->
            headers.add("DPoP", dpopUtils.dpopJWT(method, requestURI, key))
        }

        return headers
    }

    fun doGetRequest(
        authToken: String,
        sessionId: String,
        requestURI: String
    ): String
    {
        val headers = authHeaders(authToken, sessionId, "GET", requestURI)
        val httpEntity = HttpEntity<String>(headers)
        return try
        {
            val response = restTemplate.exchange(
                requestURI, HttpMethod.GET, httpEntity, String::class.java
            )
            response.body!!
        } catch (e: Exception)
        {
            println(e)
            ""
        }
    }

    fun doSparqlUpdate(
        authToken: String,
        sessionId: String,
        requestURI: String,
        query: String
    )
    {
        val headers = authHeaders(authToken, sessionId, "PATCH", requestURI)
        headers.add("Content-Type", "application/sparql-update")
        val httpEntity = HttpEntity<String>(query, headers)

        restTemplate.exchange(
            requestURI,
            HttpMethod.PATCH,
            httpEntity,
            String::class.java
        )
    }
}