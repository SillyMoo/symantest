package org.sillymoo.symantest

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.log4j.Logger
import org.sillymoo.symantest.RepoByLanguage.GithubResponse.Companion.githubResponse
import org.sillymoo.symantest.github.RepositorySearchResponse
import org.sillymoo.symantest.model.Repository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.StringWriter
import java.net.URI
import java.net.URISyntaxException
import java.time.Instant
import java.util.*
import java.util.regex.Pattern
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
            is GithubResponse.GithubError, is GithubResponse.GithubForbidden ->
                Response.status(Response.Status.BAD_GATEWAY)
            is GithubResponse.GithubSuccessWithNext ->
                Response
                        .ok()
                        .link(URI(uriInfo.requestUri.toString() + "?gitHubPage=" + repos.page), "next")
                        .entity(repos.repos)
            is GithubResponse.GithubSuccessNoNext ->
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
            loop@while (null != next) {
                LOGGER.info("Looping")
                val target = client.target(next)
                val builder = target.request()
                val repos = githubResponse(builder.get())
                when(repos) {
                    is GithubResponse.GithubForbidden ->
                            repos.handlePossibleRateLimitViolation()
                    is GithubResponse.GithubError ->
                          break@loop
                    is GithubResponse.HasRepos -> {
                        for (repo in repos.repos) {
                            val sw = StringWriter()
                            mapper.writeValue(sw, repo)
                            writer.write(sw.toString())
                        }
                        next = (repos as? GithubResponse.GithubSuccessWithNext)?.next
                    }

                }
            }
            writer.write("]")
            writer.flush()
        }
        return Response.ok(streamingOutput).build()
    }

    sealed class GithubResponse(val response: Response) {
        interface HasRepos {
            val response: Response
            val repos: List<Repository>
                get() {
                    val searchResponse = response.readEntity(RepositorySearchResponse::class.java)
                    return processSearchResponse(searchResponse)

                }

            /**
             * Processes a Github search response, producing a list of repositories in our API's
             * format.
             * @param searchResponse The Github search response
             * @return List of repositories
             */
            private fun processSearchResponse(searchResponse: RepositorySearchResponse): ArrayList<Repository> {
                val repositories = ArrayList<Repository>(searchResponse.items.size)
                searchResponse.items.mapTo(repositories) {
                    Repository(
                            it.id,
                            it.name,
                            it.url,
                            it.owner.login
                    )
                }
                return repositories
            }
        }
        companion object {
            fun githubResponse(response:Response):GithubResponse =
                    when(response.statusInfo) {
                        Response.Status.FORBIDDEN ->
                            GithubForbidden(response)

                        Response.Status.OK ->
                            if(response.hasLink("next")) {
                                GithubSuccessWithNext(response)
                            } else {
                                GithubSuccessNoNext(response)
                            }

                        else ->
                            GithubError(response)
                    }

        }
        class GithubForbidden(response: Response): GithubResponse(response) {
            /**
             * Checks for a rate limit violation, if one is found pause until the violation should be resolved
             * @return True if there was a rate limit violation, otherwise false.
             */
            fun handlePossibleRateLimitViolation(): Boolean {
                if ("0" != response.getHeaderString("X-RateLimit-Remaining")) {
                    return false
                }
                try {
                    val waitUntilStr = response.getHeaderString("X-RateLimit-Reset")
                    val waitUntil = java.lang.Long.parseLong(waitUntilStr)
                    val now = Instant.now().epochSecond
                    val waitFor = waitUntil - now + 1
                    Thread.sleep((if (waitFor < 0) 1 else waitFor) * 1000)
                } catch (e: InterruptedException) {
                    return false
                } catch (e: NumberFormatException) {
                    return false
                }

                return true
            }
        }
        class GithubError(response:Response) : GithubResponse(response)
        class GithubSuccessNoNext(response:Response) : GithubResponse(response), HasRepos
        class GithubSuccessWithNext(response:Response) : GithubResponse(response), HasRepos {
            private val pagePattern = Pattern.compile(".*page=(\\d+).*")
            val next: String
                get() = response.getLink("next").uri.toString()
            val page: Int
                get() {
                    val m = pagePattern.matcher(next)
                    return if (m.matches()) {
                        java.lang.Integer.parseInt(m.group(1))
                    } else {
                        throw IllegalArgumentException("Unexpected link found")
                    }
                }
        }
    }
}
