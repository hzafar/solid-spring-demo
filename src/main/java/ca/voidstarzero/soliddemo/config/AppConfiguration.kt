package ca.voidstarzero.soliddemo.config

import ca.voidstarzero.soliddemo.dpop.DPoPAuthorizationCodeTokenRequestClient
import ca.voidstarzero.soliddemo.dpop.DPoPUtils
import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.web.client.RestTemplate
import java.util.*

@Configuration
class AppConfiguration
{
    @Bean
    fun restTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate = restTemplateBuilder.build()

    @Bean
    fun key(): ECKey =
        ECKeyGenerator(Curve.P_256)
            .algorithm(Algorithm("EC"))
            .keyUse(KeyUse.SIGNATURE)
            .keyID(UUID.randomUUID().toString())
            .generate()

    @Bean
    fun dpopUtils(key: ECKey): DPoPUtils = DPoPUtils(key)

    @Bean
    fun dpopAuthorizationCodeTokenRequestClient(utils: DPoPUtils, restTemplate: RestTemplate)
            : OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> =
        DPoPAuthorizationCodeTokenRequestClient(utils, restTemplate)


}