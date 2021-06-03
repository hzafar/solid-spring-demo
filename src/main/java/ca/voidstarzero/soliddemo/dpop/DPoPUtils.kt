package ca.voidstarzero.soliddemo.dpop

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.time.Instant
import java.util.*

class DPoPUtils(private val key: ECKey)
{
    fun dpopJWT(method: String, targetURI: String): String =
        signedJWT(header(), payload(method, targetURI))

    private fun signedJWT(header: JWSHeader, payload: JWTClaimsSet): String {
        val signedJWT = SignedJWT(header, payload)
        signedJWT.sign(ECDSASigner(key.toECPrivateKey()))
        return signedJWT.serialize()
    }

    private fun payload(method: String, targetURI: String): JWTClaimsSet =
        JWTClaimsSet.Builder()
            .jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(Instant.now()))
            .claim("htm", method)
            .claim("htu", targetURI)
            .build()

    private fun header(): JWSHeader =
        JWSHeader.Builder(JWSAlgorithm.ES256)
            .type(JOSEObjectType("dpop+jwt"))
            .jwk(key.toPublicJWK())
            .build();
}