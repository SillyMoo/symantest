package org.sillymoo.symantest.github.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Owner {
    private String login;

    public Owner() {

    }

    public Owner(String login) {
        this.login = login;
    }

    public String getLogin() {
        return login;
    }
}
