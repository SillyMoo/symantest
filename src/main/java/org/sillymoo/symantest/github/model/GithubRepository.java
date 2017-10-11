package org.sillymoo.symantest.github.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubRepository {
    private String id;
    private Owner owner;
    private String name;
    private String url;

    public GithubRepository(){

    }

    public GithubRepository(String id, Owner owner, String name, String url) {
        this.id = id;
        this.owner = owner;
        this.name = name;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public Owner getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }
}
