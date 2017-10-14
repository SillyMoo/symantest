package org.sillymoo.symantest

import org.glassfish.jersey.server.ResourceConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import javax.ws.rs.ApplicationPath

import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder

@Configuration
class ProductionConfiguration {
    @Bean
    fun client(): Client {
        return ClientBuilder.newClient()
    }
}

@Component
@ApplicationPath("/v1")
class JerseyConfig : ResourceConfig() {
    init {
        registerEndpoints()
    }

    private final fun registerEndpoints() {
        register(RepoByLanguage::class.java)
    }
}