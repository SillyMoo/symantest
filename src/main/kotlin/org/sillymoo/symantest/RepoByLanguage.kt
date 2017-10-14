package org.sillymoo.symantest

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.log4j.Logger
import org.sillymoo.symantest.github.*
import org.sillymoo.symantest.model.Repository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.StringWriter
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import javax.ws.rs.*
import javax.ws.rs.client.Client
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput
import javax.ws.rs.core.UriInfo

@Path("github/repos/language")
@Produces("application/json")
@Component
class RepoByLanguage

@Autowired
constructor(private val client: Client) {
    private val LOGGER = Logger.getLogger(RepoByLanguage::class.java.name)

    private fun githubUrlForLanguage(language: String, githubPage: Int? = null): String {
        val uri = StringBuilder("https://api.github.com/search/repositories?q=language:")
        uri.append(language)
        if (null != githubPage) {
            uri.append("&page=").append(githubPage)
        }
        return uri.toString()
    }

    @Path("{language}/paged")
    @GET
    @Throws(URISyntaxException::class)
    fun getLanguage(@PathParam("language") language: String,
                    @QueryParam("gitHubPage") gitHubPage: Int?,
                    @Context uriInfo: UriInfo): Response {
        val target = client.target(githubUrlForLanguage(language, gitHubPage))
        val builder = target.request()
        val repos = githubResponse(builder.get())
        return when(repos) {
            is GithubError, is GithubForbidden ->
                Response.status(Response.Status.BAD_GATEWAY)
            is GithubSuccessWithNext ->
                Response
                        .ok()
                        .link(URI(uriInfo.requestUri.toString() + "?gitHubPage=" + repos.page), "next")
                        .entity(repos.repos)
            is GithubSuccessNoNext ->
                Response
                        .ok()
                        .entity(repos.repos)
        }.build()
    }

    @Path("{language}")
    @GET
    fun getLanguageAllRepos(@PathParam("language") language: String): Response {
        val streamingOutput = StreamingOutput{ output ->
            var next: String? = githubUrlForLanguage(language)
            val writer = BufferedWriter(OutputStreamWriter(output))
            writer.write("[\n")
            val mapper = ObjectMapper()
            while (null != next) {
                LOGGER.info("Looping")
                val target = client.target(next)
                val builder = target.request()
                val repos = githubResponse(builder.get())
                when(repos) {
                    is GithubForbidden ->
                            repos.handlePossibleRateLimitViolation()
                    is GithubError ->
                          next = null
                    is GithubResponse.HasRepos -> {
                        for (repo in repos.toRepoList()) {
                            val sw = StringWriter()
                            mapper.writeValue(sw, repo)
                            writer.write(sw.toString())
                        }
                        next = (repos as? GithubSuccessWithNext)?.next
                    }

                }
            }
            writer.write("]")
            writer.flush()
        }
        return Response.ok(streamingOutput).build()
    }
}

fun GithubResponse.HasRepos.toRepoList():List<Repository> {
    val repositories = ArrayList<Repository>(repos.items.size)
    repos.items.mapTo(repositories) {
        Repository(
                it.id,
                it.name,
                it.url,
                it.owner.login
        )
    }
    return repositories
}
