# symantest

This is my implementation of the following:

"Write a REST service in Java which takes a language as input (i.e. rust, go, 
coffeescript) and provides a JSON output listing all the projects using that 
language in GitHub.  The output must contain only project id, name, url and 
the owner login.  We expect working code to be provided with instructions on 
how to execute it. You may use open source libraries where available, the 
relevant GitHub API is : 
https://developer.github.com/v3/search/#search-repositories."

# The API
This api allows us to iterate through the github repositories associated with a 
programming language. The spec implies that we should return all repo's, but this is non-trivial, 
in part because of API rate limiting, but also I suspect we would quickly blow
up our memory allocation. So this API is paged.

- URI: /v1/github/repos/language/{language}/paged
- Parameters:
  - URI parameter for the language (again let's keep this simple)
- Response type: Json
- Response:
```
{
  "id": "...",
  "name": "...",
  "url": "...",
  "owner": "..."
}
Reponse headers:
- Link: provides a link to the next page
```
# How to use

You can download an executable jar from ... To use this jar simply type:

java -jar <jar_name>.jar

By default the server shall be listening on port 8080

# Key technologies
- Spring 4
- Spring Boot
- Jax-RS
- Jackson
- Junit
- Mockito

# Productisation
Due to the limit time available there were a few short cuts taken which could be addressed:
- Limited testing. Test coverage is not too bad, but error paths have mostly been ignored in order to get the
basic functionality working.
- Logging. This is pretty much non-existant at this stage
- Paging support. I parsed the 'page' query parameter in the link returned by github, this works but this
parameter is undocumented so it's a risk. It may be better to urlencode the query string of the link returned
by github.
- Error responses. If there is an 'issue' at the github end, we just return an error, we should probably
propagate some details of the github response to aid investigations.
- Input validation. It would be possible to inject additional query parameters in to the 'language' parameter. We should
probably validate the language to confirm it is compliant with github naming rules.