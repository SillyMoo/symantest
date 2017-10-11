package org.sillymoo.symantest;

import org.sillymoo.symantest.model.Repository;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import java.util.Arrays;

@Path("github/repos/language")
@Produces("application/json")
public class RepoByLanguage {

    @Path("{language}")
    @GET
    public void getLanguage(@PathParam("language") String language,
                              @Suspended final AsyncResponse asyncResponse) {
        asyncResponse.resume(Arrays.asList(new Repository()));
    }
}
