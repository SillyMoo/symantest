package org.sillymoo.symantest;

import org.junit.Test;
import org.sillymoo.symantest.github.RepositorySearchResponse;
import org.sillymoo.symantest.github.GithubRepository;
import org.sillymoo.symantest.github.Owner;
import org.sillymoo.symantest.model.Repository;
import org.springframework.util.Assert;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RepoByLanguageTest {

    @Test
    public void testProcessSearchResponse() {
        RepositorySearchResponse searchResponse = new RepositorySearchResponse(
                1234,
                Arrays.asList(
                        new GithubRepository(
                                "12",
                                new Owner("fred"),
                                "repo 1",
                                "http://someplace"
                        ),
                        new GithubRepository(
                                "34",
                                new Owner("bob"),
                                "repo 2",
                                "http://someotherplace"
                        )
                )
        );
        Response response = mock(Response.class);
        when(response.readEntity(RepositorySearchResponse.class)).thenReturn(searchResponse);
        when(response.getStatusInfo()).thenReturn(Response.Status.OK);
        RepoByLanguage.GithubResponse repos= RepoByLanguage.GithubResponse.Companion.githubResponse(response);
        Assert.isInstanceOf(RepoByLanguage.GithubResponse.GithubSuccessNoNext.class, repos);
        int i=0;
        for(GithubRepository repo1: searchResponse.getItems()) {
            Repository repo2= ((RepoByLanguage.GithubResponse.GithubSuccessNoNext)repos).getRepos().get(i++);
            assertEquals(repo1.getId(), repo2.getId());
            assertEquals(repo1.getName(), repo2.getName());
            assertEquals(repo1.getOwner().getLogin(), repo2.getOwner());
            assertEquals(repo1.getUrl(), repo2.getUrl());
        }
    }

    @Test
    public void testGetPage() throws URISyntaxException {
        Response response = mock(Response.class);
        Link link = mock(Link.class);
        when(response.hasLink("next")).thenReturn(false);
        when(response.getStatusInfo()).thenReturn(Response.Status.OK);
        Assert.isInstanceOf(RepoByLanguage.GithubResponse.GithubSuccessNoNext.class, RepoByLanguage.GithubResponse.Companion.githubResponse(response));

        when(response.hasLink("next")).thenReturn(true);
        when(response.getLink("next")).thenReturn(link);
        when(link.getUri()).thenReturn(new URI("http://somegiberish.com"));
        Assert.isInstanceOf(RepoByLanguage.GithubResponse.GithubSuccessWithNext.class, RepoByLanguage.GithubResponse.Companion.githubResponse(response));
        boolean hasExcepted=false;
        try {
            ((RepoByLanguage.GithubResponse.GithubSuccessWithNext)RepoByLanguage.GithubResponse.Companion.githubResponse(response)).getPage();
        } catch (IllegalArgumentException e) {
            hasExcepted=true;
        }
        Assert.isTrue(hasExcepted, "Expected an exception");

        when(response.hasLink("next")).thenReturn(true);
        when(response.getLink("next")).thenReturn(link);
        when(link.getUri()).thenReturn(new URI("http://somegiberish.com?page=12345"));
        RepoByLanguage.GithubResponse ghResponse = RepoByLanguage.GithubResponse.Companion.githubResponse(response);
        Assert.isInstanceOf(RepoByLanguage.GithubResponse.GithubSuccessWithNext.class, ghResponse);
        assertEquals(12345,((RepoByLanguage.GithubResponse.GithubSuccessWithNext)ghResponse).getPage());
    }

}