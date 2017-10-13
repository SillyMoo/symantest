package org.sillymoo.symantest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.sillymoo.symantest.github.RepositorySearchResponse;
import org.sillymoo.symantest.github.model.GithubRepository;
import org.sillymoo.symantest.model.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("github/repos/language")
@Produces("application/json")
@Component
public class RepoByLanguage {

    private final Client client;
    private static final Pattern pagePattern = Pattern.compile(".*page=(\\d+).*");
    private final Logger LOGGER = Logger.getLogger(RepoByLanguage.class.getName());

    @Autowired
    public RepoByLanguage(Client client) {
        this.client = client;
    }

    private String githubUrlForLanguage(String language) {
        return githubUrlForLanguage(language, null);
    }

    private String githubUrlForLanguage(String language, Integer githubPage){
        StringBuilder uri= new StringBuilder("https://api.github.com/search/repositories?q=language:");
        uri.append(language);
        if(null != githubPage){
            uri.append("&page=").append(githubPage);
        }
        return uri.toString();
    }

    @Path("{language}/paged")
    @GET
    //TODO language validation
    public Response getLanguage(@PathParam("language") String language,
                                @QueryParam("gitHubPage") Integer gitHubPage,
                                @Context UriInfo uriInfo) throws URISyntaxException {
        WebTarget target = client.target(githubUrlForLanguage(language, gitHubPage));
        Builder builder = target.request();
        Response response = builder.get();
        if(!response.getStatusInfo().equals(Response.Status.OK)) {
            return Response.status(Response.Status.BAD_GATEWAY).build();
        }
        RepositorySearchResponse searchResponse = response.readEntity(RepositorySearchResponse.class);
        if(!validateSearchResponse(searchResponse)){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        ArrayList<Repository> repositories = processSearchResponse(searchResponse);
        Optional<Integer> page = getPage(response);
        if(page.isPresent()) {
            return Response
                    .ok()
                    .link(new URI(uriInfo.getRequestUri()+"?gitHubPage="+page.get()), "next")
                    .entity(repositories)
                    .build();
        } else {
            return Response
                    .ok()
                    .entity(repositories)
                    .build();
        }

    }

    @Path("{language}")
    @GET
    public Response getLanguageAllRepos(@PathParam("language") String language) {
        StreamingOutput streamingOutput = output -> {
            String next = githubUrlForLanguage(language);
            Writer writer = new BufferedWriter(new OutputStreamWriter(output));
            writer.write("[\n");
            ObjectMapper mapper = new ObjectMapper();
            while(null != next){
                LOGGER.info("Looping");
                WebTarget target = client.target(next);
                Builder builder = target.request();
                Response response = builder.get();
                if(response.getStatusInfo().equals(Response.Status.FORBIDDEN)) {
                    if(!handlePossibleRateLimitViolation(response)){
                        LOGGER.info("Failure that was not a rate violation");
                        break;
                    }
                }else if(!response.getStatusInfo().equals(Response.Status.OK)) {
                    break;
                } else {
                    RepositorySearchResponse searchResponse = response.readEntity(RepositorySearchResponse.class);
                    List<Repository> repos = processSearchResponse(searchResponse);
                    for(Repository repo: repos) {
                        StringWriter sw = new StringWriter();
                        mapper.writeValue(sw, repo);
                        writer.write(sw.toString());
                    }
                    if(response.hasLink("next")) {
                        next = response.getLink("next").getUri().toString();
                    } else {
                        next = null;
                    }
                }
            }
            writer.write("]");
            writer.flush();
        };
        return Response.ok(streamingOutput).build();
    }

    private boolean handlePossibleRateLimitViolation(Response response){
        if(!"0".equals(response.getHeaderString("X-RateLimit-Remaining"))){
            return false;
        }
        try {
            String waitUntilStr = response.getHeaderString("X-RateLimit-Reset");
            LOGGER.info("Waiting rate violation finish");
            long waitUntil = Long.parseLong(waitUntilStr);
            long now = Instant.now().getEpochSecond();
            long waitFor = (waitUntil - now) + 1;
            Thread.sleep(((waitFor < 0) ? 1 : waitFor) * 1000);
        } catch (InterruptedException | NumberFormatException e) {
            return false;
        }
        return true;
    }

    private static boolean validateSearchResponse(RepositorySearchResponse searchResponse){
        return !((null == searchResponse) || (null == searchResponse.getItems()));
    }

    static ArrayList<Repository> processSearchResponse(RepositorySearchResponse searchResponse) {
        ArrayList<Repository> repositories = new ArrayList<>(searchResponse.getItems().size());
        for(GithubRepository repo: searchResponse.getItems()) {
            repositories.add(new Repository(
                    repo.getId(),
                    repo.getName(),
                    repo.getUrl(),
                    repo.getOwner().getLogin()
            ));
        }
        return repositories;
    }

    static Optional<Integer> getPage(Response response) {
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
