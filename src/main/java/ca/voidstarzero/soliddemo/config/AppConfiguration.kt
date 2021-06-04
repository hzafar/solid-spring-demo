package ca.voidstarzero.soliddemo.config

import ca.voidstarzero.soliddemo.dpop.DPoPAuthorizationCodeTokenRequestClient
import ca.voidstarzero.soliddemo.dpop.DPoPUtils
import com.nimbusds.jose.jwk.ECKey
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.web.client.RestTemplate
import javax.servlet.http.HttpSessionListener

@Configuration
class AppConfiguration
{
    @Bean
    fun restTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate = restTemplateBuilder.build()

    @Bean
    fun dpopUtils(sessionKeyMap: Map<String, ECKey>): DPoPUtils = DPoPUtils()

    @Bean
    fun dpopAuthorizationCodeTokenRequestClient(
        utils: DPoPUtils,
        sessionKeyMap: Map<String, ECKey>,
        restTemplate: RestTemplate
    ): OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> =
        DPoPAuthorizationCodeTokenRequestClient(utils, restTemplate)

    @Bean
    fun keyRemovalSessionListener(dPoPUtils: DPoPUtils): HttpSessionListener =
        KeyRemovalSessionListener(dPoPUtils)
}