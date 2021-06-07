package ca.voidstarzero.soliddemo.dpop

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class DPoPUtils
{
    private val sessionKeyMap: ConcurrentHashMap<String, ECKey> = ConcurrentHashMap()

    fun sessionKey(sessionId: String): ECKey? = sessionKeyMap[sessionId]

    fun saveSessionKey(sessionId: String, key: ECKey)
    {
        sessionKeyMap[sessionId] = key
    }

    fun removeSessionKey(sessionId: String)
    {
        sessionKeyMap.remove(sessionId)
    }

    fun dpopJWT(method: String, targetURI: String, sessionKey: ECKey): String =
        signedJWT(header(sessionKey), payload(method, targetURI), sessionKey)

    private fun signedJWT(header: JWSHeader, payload: JWTClaimsSet, sessionKey: ECKey): String
    {
        val signedJWT = SignedJWT(header, payload)
        signedJWT.sign(ECDSASigner(sessionKey.toECPrivateKey()))
        return signedJWT.serialize()
    }

    private fun payload(method: String, targetURI: String): JWTClaimsSet =
        JWTClaimsSet.Builder()
            .jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(Instant.now()))
            .claim("htm", method)
            .claim("htu", targetURI)
            .build()

    private fun header(sessionKey: ECKey): JWSHeader =
        JWSHeader.Builder(JWSAlgorithm.ES256)
            .type(JOSEObjectType("dpop+jwt"))
            .jwk(sessionKey.toPublicJWK())
            .build();
}