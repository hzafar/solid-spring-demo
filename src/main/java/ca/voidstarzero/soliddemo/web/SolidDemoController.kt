package ca.voidstarzero.soliddemo.web

import ca.voidstarzero.soliddemo.dpop.DPoPUtils
import org.apache.commons.io.IOUtils
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.sparql.vocabulary.FOAF
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.client.RestTemplate

@Controller
class SolidDemoController(
    private val clientService: OAuth2AuthorizedClientService,
    private val dpopUtils: DPoPUtils,
    private val restTemplate: RestTemplate
)
{
    @GetMapping("/")
    fun index(
        authentication: Authentication,
        model: Model
    ): String
    {
        val authToken = authToken(authentication)
        val profileURI = authentication.name.removeSuffix("#me")

        val profileResponse = doGetRequest(authToken, profileURI)
        val profileGraph = ModelFactory.createDefaultModel()
            .read(
                IOUtils.toInputStream(profileResponse, "UTF-8"),
                profileURI,
                "TURTLE"
            )

        val name = profileGraph
            .getResource(authentication.name)
            .getProperty(FOAF.name).`object`

        model.addAttribute("name", name)
        return "index"
    }

    @PostMapping("/update")
    fun updateProfile(
        authentication: Authentication,
        @RequestParam("new_name") newName: String,
        model: Model
    ): String
    {
        val authToken = authToken(authentication)
        val profileSubject = authentication.name

        val profileURI = profileSubject.removeSuffix("#me")
        val profileResponse = doGetRequest(authToken, profileURI)
        val profileGraph = ModelFactory.createDefaultModel()
            .read(
                IOUtils.toInputStream(profileResponse, "UTF-8"),
                profileURI,
                "TURTLE"
            )

        val oldName = profileGraph
            .getResource(profileSubject)
            .getProperty(FOAF.name).`object`

        val query =
            """DELETE DATA {<${profileSubject}> <${FOAF.name}> "$oldName".};""" +
                    """INSERT DATA {<${profileSubject}> <${FOAF.name}> "$newName".};"""

        doSparqlUpdate(authToken, profileSubject.removeSuffix("#me"), query)

        return "update"
    }

    private fun authToken(authentication: Authentication)
            : String
    {
        val oAuthToken = authentication as OAuth2AuthenticationToken
        val client = clientService
            .loadAuthorizedClient<OAuth2AuthorizedClient>(
                oAuthToken.authorizedClientRegistrationId,
                oAuthToken.name
            )

        return client.accessToken.tokenValue
    }

    private fun authHeaders(
        authToken: String,
        method: String,
        requestURI: String
    ): HttpHeaders
    {
        val headers = HttpHeaders()
        headers.add("Authorization", "DPoP $authToken")
        headers.add("DPoP", dpopUtils.dpopJWT(method, requestURI))
        return headers
    }

    private fun doGetRequest(
        authToken: String,
        requestURI: String
    ): String
    {
        val headers = authHeaders(authToken, "GET", requestURI)
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

    private fun doSparqlUpdate(
        authToken: String,
        requestURI: String,
        query: String
    )
    {
        val headers = authHeaders(authToken, "PATCH", requestURI)
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