package org.sillymoo.symantest;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.sillymoo.symantest.github.RepositorySearchResponse;
import org.sillymoo.symantest.github.model.GithubRepository;
import org.sillymoo.symantest.github.model.Owner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SymantestApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@MockBean
	private Client client;

	@Test
	public void successPath() {

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

		WebTarget webTarget = mock(WebTarget.class);
		Invocation.Builder builder = mock(Invocation.Builder.class);
		Response response = mock(Response.class);
		when(client.target(anyString())).thenReturn(webTarget);
		when(webTarget.request()).thenReturn(builder);
		when(builder.get()).thenReturn(response);
		when(response.readEntity(RepositorySearchResponse.class)).thenReturn(searchResponse);
		when(response.getStatus()).thenReturn(200);
		String body = this.restTemplate.getForObject("/v1/github/repos/language/rust/paged", String.class);
		assertEquals(
				"[{\"id\":\"12\",\"name\":\"repo 1\",\"url\":\"http://someplace\",\"owner\":\"fred\"},{\"id\":\"34\",\"name\":\"repo 2\",\"url\":\"http://someotherplace\",\"owner\":\"bob\"}]",
				body);
	}

}
