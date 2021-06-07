package ca.voidstarzero.soliddemo.repository

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames
import org.springframework.stereotype.Repository
import org.springframework.util.Assert
import org.springframework.web.client.RestTemplate
import java.util.concurrent.ConcurrentHashMap

@Repository
class SolidOidcClientRegistrationRepository(
    @Value("\${base-uri}") private val baseUri: String,
    private val restTemplate: RestTemplate
) : ClientRegistrationRepository
{
    private val registrations: ConcurrentHashMap<String, ClientRegistration> = ConcurrentHashMap()
    private val metadataEndpoint = "/.well-known/openid-configuration"
    private val redirectUriBase = "$baseUri/login/oauth2/code"

    private val mapResponseType: ParameterizedTypeReference<HashMap<String, Any>> =
        object : ParameterizedTypeReference<HashMap<String, Any>>()
        {}

    override fun findByRegistrationId(registrationId: String): ClientRegistration?
    {
        Assert.hasText(registrationId, "registrationId cannot be empty")
        return registrations[registrationId]
    }

    fun createRegistration(registrationId: String, providerUri: String)
    {
        val providerMetadata = providerMetadata(metadataUri(providerUri))
        val redirectUri = "$redirectUriBase/${registrationId}"
        val clientRegistrationInfo = registerClient(
            providerUri,
            redirectUri,
            providerMetadata["registration_endpoint"] as String
        )

        val clientRegistration = ClientRegistration.withRegistrationId(registrationId)
            .clientName(providerUri)
            .clientId(clientRegistrationInfo["client_id"] as String)
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .redirectUri(redirectUri)
            .scope("user", "openid", "offline_access")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .userNameAttributeName(IdTokenClaimNames.SUB)
            .providerConfigurationMetadata(providerMetadata)
            .authorizationUri(providerMetadata["authorization_endpoint"] as String)
            .tokenUri(providerMetadata["token_endpoint"] as String)
            .jwkSetUri(providerMetadata["jwks_uri"] as String)
            .userInfoUri(providerMetadata["userinfo_endpoint"] as String)
            .build()

        registrations[registrationId] = clientRegistration
    }

    private fun providerMetadata(providerMetadataUri: String): Map<String, Any>
    {
        val requestEntity = RequestEntity.get(providerMetadataUri).accept(MediaType.APPLICATION_JSON).build()
        val metadataResponse = restTemplate.exchange(requestEntity, mapResponseType).body!!

        // Temporary workaround for string instead of array in response
        val tokenEndpointAuthMethodsSupported = metadataResponse["token_endpoint_auth_methods_supported"] as? String
        tokenEndpointAuthMethodsSupported?.let {
            metadataResponse["token_endpoint_auth_methods_supported"] = listOf(it)
        }

        return metadataResponse
    }

    private fun registerClient(
        providerUri: String,
        redirectUri: String,
        registrationUri: String
    ): Map<String, Any>
    {
        val headers = HttpHeaders()
        headers.add("Content-Type", "application/json")

        val requestBodyJson = """{
            "application_type": "web",
            "redirect_uris":
                ["$redirectUri"],
            "client_name": "Spring Solid Demo Client for ($providerUri)",
            "token_endpoint_auth_method": "none"
        }""".trimMargin()

        val httpEntity = HttpEntity<String>(requestBodyJson, headers)

        val responseEntity = restTemplate.exchange(
            registrationUri,
            HttpMethod.POST,
            httpEntity,
            mapResponseType
        )

        return responseEntity.body ?: emptyMap()
    }

    private fun metadataUri(providerUri: String): String =
        providerUri.removeSuffix("/").plus(metadataEndpoint)
}