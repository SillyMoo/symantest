package org.sillymoo.symantest.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class RepositorySearchResponse ( @JsonProperty("total_count") val totalCount: Int,
                                      @JsonProperty("items") val items: List<GithubRepository>)
