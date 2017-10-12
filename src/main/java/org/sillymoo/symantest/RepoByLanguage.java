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
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("github/repos/language")
@Produces("application/json")
@Component
public class RepoByLanguage {

    private final Client client;
    private final static Pattern pagePattern = Pattern.compile(".*page=(\\d+).*");
    private Logger LOGGER = Logger.getLogger(RepoByLanguage.class.getName());

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
        if(githubPage!=null){
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
        Invocation.Builder builder = target.request();
        Response response = builder.get();
        if(response.getStatus()!= 200) {
            return Response.status(502).build();
        }
        RepositorySearchResponse searchResponse = response.readEntity(RepositorySearchResponse.class);
        if(!validateSearchResponse(searchResponse)){
            return Response.status(500).build();
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
        StreamingOutput streamingOutput = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                String next = githubUrlForLanguage(language);
                Writer writer = new BufferedWriter(new OutputStreamWriter(output));
                writer.write("[\n");
                ObjectMapper mapper = new ObjectMapper();
                while(next!=null){
                    LOGGER.error("Looping");
                    WebTarget target = client.target(next);
                    Invocation.Builder builder = target.request();
                    Response response = builder.get();
                    if(response.getStatus()==403) {
                        if(!handlePossibleRateLimitViolation(response)){
                            LOGGER.error("Failure that was not a rate violation");
                            break;
                        }
                    }else if(response.getStatus()!=200) {
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
                    LOGGER.error("NEXT: "+next);
                }
                writer.write("]");
                writer.flush();

            }
        };
        return Response.ok(streamingOutput).build();
    }

    private boolean handlePossibleRateLimitViolation(Response response){
        if(!response.getHeaderString("X-RateLimit-Remaining").equals("0")){
            return false;
        }
        try {
            String waitUntilStr = response.getHeaderString("X-RateLimit-Reset");
            LOGGER.error("Waiting rate violation finish");
            long waitUntil = Long.parseLong(waitUntilStr);
            long now = Instant.now().getEpochSecond();
            Thread.sleep((waitUntil-now+1)*1000);
        } catch (InterruptedException | NumberFormatException e) {
            return false;
        }
        return true;
    }

    private static boolean validateSearchResponse(RepositorySearchResponse searchResponse){
        return !(searchResponse==null || searchResponse.getItems()==null);
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
