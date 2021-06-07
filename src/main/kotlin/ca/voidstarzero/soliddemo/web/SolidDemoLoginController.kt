package ca.voidstarzero.soliddemo.web

import ca.voidstarzero.soliddemo.repository.ClientRegistrationIdRepository
import ca.voidstarzero.soliddemo.repository.SolidOidcClientRegistrationRepository
import ca.voidstarzero.soliddemo.service.GraphUtils
import ca.voidstarzero.soliddemo.service.SolidOidcUtils
import org.apache.jena.rdf.model.impl.PropertyImpl
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.util.*

@Controller
class SolidDemoLoginController(
    private val clientRegistrationRepository: SolidOidcClientRegistrationRepository,
    private val clientRegistrationIdRepository: ClientRegistrationIdRepository,
    private val solidOidcUtils: SolidOidcUtils,
    private val graphUtils: GraphUtils,
    private val restTemplate: RestTemplate
)
{
    private val solidOidcIssuerProperty = PropertyImpl("http://www.w3.org/ns/solid/terms#oidcIssuer")

    @GetMapping("/solid-login")
    fun solidLoginHome(): String = "solid-login"

    @PostMapping("/solid-login/solid-oidc")
    fun solidOidcLogin(
        @RequestParam("uri") suppliedUri: URI
    ): String
    {
        val providerUri =
            if (suppliedUri.fragment.isNotBlank())
                getOidcProvider(suppliedUri)
            else suppliedUri.toString()

        if (clientRegistrationIdRepository.registered(providerUri))
        {
            val registrationId = clientRegistrationIdRepository.clientRegistrationId(providerUri)
            return "redirect:/oauth2/authorization/$registrationId"
        }

        val registrationId = UUID.randomUUID().toString()
        clientRegistrationRepository.createRegistration(registrationId, providerUri)
        clientRegistrationIdRepository.addClientRegistrationId(registrationId, providerUri)
        return "redirect:/oauth2/authorization/$registrationId"
    }

    private fun getOidcProvider(profileUri: URI): String
    {
        val profileTTL = restTemplate.getForObject(profileUri, String::class.java) ?: ""

        val profileBaseUri = "${profileUri.scheme}:${profileUri.schemeSpecificPart}"
        val profileGraph = graphUtils.graph(profileTTL, profileBaseUri)

        val providerBaseUri = "${profileUri.scheme}://${profileUri.host}"
        val providerUri: String =
            if (profileGraph.getResource(profileUri.toString()).hasProperty(solidOidcIssuerProperty))
                profileGraph.getResource(profileUri.toString()).getProperty(solidOidcIssuerProperty).`object`.toString()
            else providerBaseUri

        return providerUri
    }
}