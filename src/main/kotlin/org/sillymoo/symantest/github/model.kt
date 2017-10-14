package org.sillymoo.symantest.github

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubRepository(
        @JsonProperty("id") val id: String,
        @JsonProperty("owner") val owner: Owner,
        @JsonProperty("name") val name: String,
        @JsonProperty("url") val url: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Owner (@JsonProperty("login") val login: String)

