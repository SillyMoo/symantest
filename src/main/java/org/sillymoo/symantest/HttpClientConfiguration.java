package org.sillymoo.symantest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

@Configuration
public class HttpClientConfiguration {
    @Bean
    public Client client(){
        return ClientBuilder.newClient();
    }
}
