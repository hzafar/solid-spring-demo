package ca.voidstarzero.soliddemo.config

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest

@EnableWebSecurity
@Configuration
class SecurityConfiguration(
    val tokenResponseClient: OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
) : WebSecurityConfigurerAdapter() {

    override fun configure(web: WebSecurity?) {
        http.authorizeRequests()
            .antMatchers("/solid-login", "/solid-login/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .oauth2Login().loginPage("/solid-login")
            .and()
            .oauth2Client()
    }

    override fun configure(http: HttpSecurity) {
        http.oauth2Login().tokenEndpoint()
            .accessTokenResponseClient(tokenResponseClient)

        http.logout()
            .logoutUrl("/logout")
            .deleteCookies("JSESSIONID")
            .clearAuthentication(true)
            .invalidateHttpSession(true)
    }
}