package org.sillymoo.symantest.github

import java.time.Instant
import java.util.regex.Pattern
import javax.ws.rs.core.Response

sealed class GithubResponse(val response: Response) {
    interface HasRepos {
        val response: Response
        val repos: RepositorySearchResponse
            get() = response.readEntity(RepositorySearchResponse::class.java)
    }
}
fun githubResponse(response: Response):GithubResponse =
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
class GithubError(response: Response) : GithubResponse(response)
class GithubSuccessNoNext(response: Response) : GithubResponse(response), GithubResponse.HasRepos
class GithubSuccessWithNext(response: Response) : GithubResponse(response), GithubResponse.HasRepos {
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