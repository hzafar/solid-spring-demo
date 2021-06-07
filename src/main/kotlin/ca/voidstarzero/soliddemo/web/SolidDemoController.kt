package ca.voidstarzero.soliddemo.web

import ca.voidstarzero.soliddemo.service.GraphUtils
import ca.voidstarzero.soliddemo.service.SolidOidcUtils
import org.apache.jena.sparql.vocabulary.FOAF
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class SolidDemoController(
    private val clientService: OAuth2AuthorizedClientService,
    private val solidOidcUtils: SolidOidcUtils,
    private val graphUtils: GraphUtils
)
{
    @GetMapping("/")
    fun index(
        authentication: Authentication,
        model: Model
    ): String
    {
        val authToken = authToken(authentication)
        val profileURI = authentication.name.removeSuffix("#me") // FIXME lol
        val sessionId = ((authentication as OAuth2AuthenticationToken)
            .details as WebAuthenticationDetails).sessionId

        val profileResponse = solidOidcUtils.doGetRequest(authToken, sessionId, profileURI)
        val profileGraph = graphUtils.graph(profileResponse, profileURI)

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
        val sessionId = ((authentication as OAuth2AuthenticationToken)
            .details as WebAuthenticationDetails).sessionId

        val profileURI = profileSubject.removeSuffix("#me")
        val profileResponse = solidOidcUtils.doGetRequest(authToken, sessionId, profileURI)
        val profileGraph = graphUtils.graph(profileResponse, profileURI)

        val oldName = profileGraph
            .getResource(profileSubject)
            .getProperty(FOAF.name).`object`

        val query =
            """DELETE DATA {<${profileSubject}> <${FOAF.name}> "$oldName".};""" +
                    """INSERT DATA {<${profileSubject}> <${FOAF.name}> "$newName".};"""

        solidOidcUtils.doSparqlUpdate(authToken, sessionId, profileSubject.removeSuffix("#me"), query)

        return "update"
    }

    @PostMapping("/get-resource")
    fun getProtectedResource(
        authentication: Authentication,
        @RequestParam("resource_uri") resourceUri: String,
        model: Model
    ): String
    {
        val authToken = authToken(authentication)
        val sessionId = ((authentication as OAuth2AuthenticationToken)
            .details as WebAuthenticationDetails).sessionId
        val resourceResponse = solidOidcUtils.doGetRequest(authToken, sessionId, resourceUri)

        model.addAttribute("ttlFileContents", resourceResponse)
        return "resource"
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
}