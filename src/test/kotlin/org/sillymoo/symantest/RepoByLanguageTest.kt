package org.sillymoo.symantest

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.sillymoo.symantest.github.*
import org.springframework.util.Assert
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import javax.ws.rs.core.Link
import javax.ws.rs.core.Response
import org.mockito.Mockito.`when` as on



class RepoByLanguageTest {
    @Test
    fun testProcessSearchResponse() {
        val searchResponse = RepositorySearchResponse(
                1234,
                Arrays.asList(
                        GithubRepository(
                                "12",
                                Owner("fred"),
                                "repo 1",
                                "http://someplace"
                        ),
                        GithubRepository(
                                "34",
                                Owner("bob"),
                                "repo 2",
                                "http://someotherplace"
                        )
                )
        )
        val response = mock(Response::class.java)
        on(response.readEntity(RepositorySearchResponse::class.java)).thenReturn(searchResponse)
        on(response.statusInfo).thenReturn(Response.Status.OK)
        val repos = githubResponse(response)
        Assert.isInstanceOf(GithubSuccessNoNext::class.java, repos)

        for ((repo1, repo2) in searchResponse.items.zip((repos as GithubSuccessNoNext).toRepoList())) {
            assertEquals(repo1.id, repo2.id)
            assertEquals(repo1.name, repo2.name)
            assertEquals(repo1.owner.login, repo2.owner)
            assertEquals(repo1.url, repo2.url)
        }
    }

    @Test
    @Throws(URISyntaxException::class)
    fun testGetPage() {
        val response = mock(Response::class.java)
        val link = mock(Link::class.java)
        on(response.hasLink("next")).thenReturn(false)
        on(response.statusInfo).thenReturn(Response.Status.OK)
        Assert.isInstanceOf(GithubSuccessNoNext::class.java, githubResponse(response))

        on(response.hasLink("next")).thenReturn(true)
        on(response.getLink("next")).thenReturn(link)
        on(link.uri).thenReturn(URI("http://somegiberish.com"))
        Assert.isInstanceOf(GithubSuccessWithNext::class.java, githubResponse(response))
        var hasExcepted = false
        try {
            (githubResponse(response) as GithubSuccessWithNext).page
        } catch (e: IllegalArgumentException) {
            hasExcepted = true
        }

        Assert.isTrue(hasExcepted, "Expected an exception")

        on(response.hasLink("next")).thenReturn(true)
        on(response.getLink("next")).thenReturn(link)
        on(link.uri).thenReturn(URI("http://somegiberish.com?page=12345"))
        val ghResponse = githubResponse(response)
        Assert.isInstanceOf(GithubSuccessWithNext::class.java, ghResponse)
        assertEquals(12345, (ghResponse as GithubSuccessWithNext).page.toLong())
    }

}