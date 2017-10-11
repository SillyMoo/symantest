package org.sillymoo.symantest.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sillymoo.symantest.github.model.GithubRepository;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RepositorySearchResponse {
    @JsonProperty("total_count")
    private int totalCount;
    private List<GithubRepository> items;

    public RepositorySearchResponse() {
    }

    public RepositorySearchResponse(int totalCount, List<GithubRepository> items) {
        this.totalCount = totalCount;
        this.items = items;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public List<GithubRepository> getItems() {
        return items;
    }
}
