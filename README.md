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
programming language. The spec implies that we should return all repo's, but this is not
possible, in part because of API rate limiting, but also I suspect we would quickly blow
up our memory allocation. So this API is paged.

- URI: /v1/github/repos/language/{language}
- Parameters:
  - URI parameter for the language (again let's keep this simple)
- Response type: Json
- Response: ```
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

# Technologies
- Spring 4
- Spring Boot
- Jax-RS

As this is a pretty simple API I have not complicate matters with asynchronous jax-rs,
but would be pretty easy to restrospectively add it.