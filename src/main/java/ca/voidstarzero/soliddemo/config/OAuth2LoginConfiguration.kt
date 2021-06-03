package ca.voidstarzero.soliddemo.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames

// Temporary workaround for parsing issue when Spring tries to auto-configure
// an OAuth Client for Solid. (See https://github.com/solid/oidc-op/issues/33.)
@Configuration
class OAuth2LoginConfiguration(
    val config: SolidClientConfigurationProperties
) {

    @Configuration
    @ConfigurationProperties(prefix = "spring.security.oauth2.client.registration.solid")
    data class SolidClientConfigurationProperties(
        var clientName: String? = null,
        var clientId: String? = null,
        var clientSecret: String? = null,
        var clientAuthenticationMethod: String? = null,
        var redirectUri: String? = null,
        var scope: String? = null,
        var authorizationGrantType: String? = null
    )

    @Bean
    fun clientRegistrationRepository(): ClientRegistrationRepository {
        val registration =
            ClientRegistration.withRegistrationId("solid")
                .clientName(config.clientName)
                .clientId(config.clientId)
                .clientSecret(config.clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod(config.clientAuthenticationMethod))
                .redirectUri(config.redirectUri)
                .scope(config.scope?.split(","))
                .authorizationGrantType(AuthorizationGrantType(config.authorizationGrantType))
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .providerConfigurationMetadata(metadata())
                .authorizationUri(metadata()["authorization_endpoint"] as String)
                .tokenUri(metadata()["token_endpoint"] as String)
                .jwkSetUri(metadata()["jwks_uri"] as String)
                .userInfoUri(metadata()["userinfo_endpoint"] as String)
                .build()

        return InMemoryClientRegistrationRepository(
            listOf(registration)
        )
    }

    private fun metadata(): Map<String, Any> =
        mapOf(
            "issuer" to "https://solidcommunity.net" as Any,
            "jwks_uri" to "https://solidcommunity.net/jwks" as Any,
            "response_types_supported" to listOf(
                "code",
                "code token",
                "code id_token",
                "id_token code",
                "id_token",
                "id_token token",
                "code id_token token",
                "none"
            ) as Any,
            "token_types_supported" to listOf("legacyPop", "dpop") as Any,
            "response_modes_supported" to listOf("query", "fragment") as Any,
            "grant_types_supported" to listOf(
                "authorization_code",
                "implicit",
                "refresh_token",
                "client_credentials"
            ) as Any,
            "subject_types_supported" to listOf("public") as Any,
            "id_token_signing_alg_values_supported" to listOf("RS256") as Any,
            "token_endpoint_auth_methods_supported" to listOf("client_secret_basic") as Any,
            "token_endpoint_auth_signing_alg_values_supported" to listOf("RS256") as Any,
            "display_values_supported" to listOf<String>() as Any,
            "claim_types_supported" to listOf("normal") as Any,
            "claims_supported" to listOf<String>() as Any,
            "claims_parameter_supported" to false as Any,
            "request_parameter_supported" to true as Any,
            "request_uri_parameter_supported" to false as Any,
            "require_request_uri_registration" to false as Any,
            "check_session_iframe" to "https://solidcommunity.net/session" as Any,
            "end_session_endpoint" to "https://solidcommunity.net/logout" as Any,
            "authorization_endpoint" to "https://solidcommunity.net/authorize" as Any,
            "token_endpoint" to "https://solidcommunity.net/token" as Any,
            "userinfo_endpoint" to "https://solidcommunity.net/userinfo" as Any,
            "registration_endpoint" to "https://solidcommunity.net/register" as Any
        )
}