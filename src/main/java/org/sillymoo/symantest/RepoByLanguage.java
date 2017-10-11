package org.sillymoo.symantest;

import org.sillymoo.symantest.github.RepositorySearchResponse;
import org.sillymoo.symantest.github.model.GithubRepository;
import org.sillymoo.symantest.model.Repository;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("github/repos/language")
@Produces("application/json")
public class RepoByLanguage {

    private Client client;
    private static Pattern pagePattern = Pattern.compile(".*page=(\\d+).*");

    @Autowired
    public RepoByLanguage(Client client) {
        this.client = client;
    }

    private String githubUrlForLanguage(String language, Integer githubPage){
        StringBuilder uri= new StringBuilder("https://api.github.com/search/repositories?q=language:");
        uri.append(language);
        if(githubPage!=null){
            uri.append("&page=").append(githubPage);
        }
        return uri.toString();
    }

    @Path("{language}")
    @GET
    //TODO error handling when we hit rate limiting errors
    //TODO language validation
    public Response getLanguage(@PathParam("language") String language,
                                @QueryParam("gitHubPage") Integer gitHubPage,
                                @Context UriInfo uriInfo) throws URISyntaxException {
        WebTarget target = client.target(githubUrlForLanguage(language, gitHubPage));
        Invocation.Builder builder = target.request();
        ArrayList<Repository> repositories = new ArrayList<>();
        Response response = builder.get();
        RepositorySearchResponse searchResponse = response.readEntity(RepositorySearchResponse.class);
        ProcessSearchResponse(repositories, searchResponse);
        Optional<Integer> page = getPage(response);
        if(page.isPresent()) {
            return Response
                    .ok()
                    .link(new URI(uriInfo.getRequestUri()+"?gitHubPage="+page.get()), "next")
                    .entity(searchResponse)
                    .build();
        } else {
            return Response
                    .ok()
                    .entity(searchResponse)
                    .build();
        }

    }

    private static void ProcessSearchResponse(ArrayList<Repository> repositories,
                                              RepositorySearchResponse searchResponse) {
        for(GithubRepository repo: searchResponse.getItems()) {
            repositories.add(new Repository(
                    repo.getId(),
                    repo.getName(),
                    repo.getOwner().getLogin(),
                    repo.getUrl()
            ));
        }
    }

    private static Optional<Integer> getPage(Response response) {
        if(response.hasLink("next")) {
            String nextLink = response.getLink("next").getUri().toString();
            Matcher m = pagePattern.matcher(nextLink);
            if(m.matches()){
                return Optional.of(Integer.parseInt(m.group(1)));
            } else {
                return Optional.empty();
            }
        }else {
            return Optional.empty();
        }
    }
}