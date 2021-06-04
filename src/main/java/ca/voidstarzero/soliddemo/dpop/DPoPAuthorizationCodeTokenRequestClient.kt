package ca.voidstarzero.soliddemo.dpop

import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.request.RequestContextHolder
import java.util.*

class DPoPAuthorizationCodeTokenRequestClient(
    private val dpopUtils: DPoPUtils,
    private val restTemplate: RestTemplate
) : OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
{
    private val mapResponseType: ParameterizedTypeReference<HashMap<String, String>> =
        object : ParameterizedTypeReference<HashMap<String, String>>()
        {}
    private val defaultAuthorizationCodeTokenResponseClient = DefaultAuthorizationCodeTokenResponseClient()
    override fun getTokenResponse(authorizationCodeGrantRequest: OAuth2AuthorizationCodeGrantRequest): OAuth2AccessTokenResponse
    {
        val oidcResponse = defaultAuthorizationCodeTokenResponseClient.getTokenResponse(authorizationCodeGrantRequest)
        val codeVerifier =
            authorizationCodeGrantRequest.authorizationExchange.authorizationRequest
                .getAttribute<String>("code_verifier")
        val code =
            authorizationCodeGrantRequest.authorizationExchange.authorizationResponse
                .code
        val tokenURI = authorizationCodeGrantRequest.clientRegistration.providerDetails.tokenUri
        val redirectURI = authorizationCodeGrantRequest.clientRegistration.redirectUri
        val clientId = authorizationCodeGrantRequest.clientRegistration.clientId

        val sessionId = RequestContextHolder.currentRequestAttributes().sessionId
        val sessionKey = ECKeyGenerator(Curve.P_256)
            .algorithm(Algorithm("EC"))
            .keyUse(KeyUse.SIGNATURE)
            .keyID(UUID.randomUUID().toString())
            .generate()

        dpopUtils.saveSessionKey(sessionId, sessionKey)

        val jwt = dpopUtils.dpopJWT("POST", tokenURI, sessionKey)

        val response = dpopResponse(codeVerifier, code, redirectURI, clientId, tokenURI, jwt)
        return OAuth2AccessTokenResponse.withToken(response["access_token"])
            .tokenType(OAuth2AccessToken.TokenType.BEARER)
            .refreshToken(response["refresh_token"])
            .additionalParameters(oidcResponse.additionalParameters)
            .expiresIn(response["expires_in"]?.toLong() ?: 0)
            .build()
    }

    private fun dpopResponse(
        codeVerifier: String,
        code: String,
        redirectURI: String,
        clientId: String,
        tokenURI: String,
        jwt: String
    ): Map<String, String>
    {
        val headers = HttpHeaders()
        headers.add("DPoP", jwt)
        headers.add("Content-Type", "application/x-www-form-urlencoded")

        val params = LinkedMultiValueMap<String, String>()
        params.add("grant_type", "authorization_code")
        params.add("code_verifier", codeVerifier)
        params.add("code", code)
        params.add("redirect_uri", redirectURI)
        params.add("client_id", clientId)

        val httpEntity = HttpEntity<LinkedMultiValueMap<String, String>>(params, headers)

        return restTemplate.exchange(
            tokenURI,
            HttpMethod.POST,
            httpEntity,
            mapResponseType
        ).body ?: emptyMap()
    }
}