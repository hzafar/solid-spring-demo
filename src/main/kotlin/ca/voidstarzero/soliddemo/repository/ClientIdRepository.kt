package ca.voidstarzero.soliddemo.repository

import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class ClientRegistrationIdRepository
{
    private val clients: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    fun registered(providerUri: String): Boolean = clients.containsKey(providerUri)

    fun clientRegistrationId(providerUri: String): String? = clients[providerUri]

    fun addClientRegistrationId(registrationId: String, providerUri: String) {
        clients[providerUri] = registrationId
    }
}