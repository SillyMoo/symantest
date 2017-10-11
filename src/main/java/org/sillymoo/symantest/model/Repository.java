package org.sillymoo.symantest.model;

/**
 * API representation of a Github repository.
 */
public class Repository {
    private String id;
    private String name;
    private String url;
    private String owner;

    public Repository() {
    }

    /**
     * Main constructor
     * @param id Github ID of the repository
     * @param name Name of the repository
     * @param url URL of the repository
     * @param owner Login name of the repository owner
     */
    public Repository(String id, String name, String url, String owner) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.owner = owner;
    }

    /**
     * Get the Github ID of the repository
     * @return Github ID of the repository
     */
    public String getId() {
        return id;
    }

    /**
     * Get the name of the repository
     * @return name of the repository
     */
    public String getName() {
        return name;
    }

    /**
     * Get the URL of the repository
     * @return name of the repository
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get the login name of the repository owner
     * @return login name of the repository owner
     */
    public String getOwner() {
        return owner;
    }
}
