package ca.voidstarzero.soliddemo.config

import ca.voidstarzero.soliddemo.dpop.DPoPUtils
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.authentication.WebAuthenticationDetails
import javax.servlet.http.HttpSessionEvent
import javax.servlet.http.HttpSessionListener

class KeyRemovalSessionListener(private val dPoPUtils: DPoPUtils) : HttpSessionListener
{
    override fun sessionDestroyed(se: HttpSessionEvent)
    {
        val sessionId = ((se.session.getAttribute("SPRING_SECURITY_CONTEXT") as SecurityContextImpl)
            .authentication.details as WebAuthenticationDetails).sessionId

        dPoPUtils.removeSessionKey(sessionId)
    }
}