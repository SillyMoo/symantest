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
So to start with we'll go with an API that returns all repo's for a given language,
as that meets the requirements. I worry about the size of the returned content in
that case, but we'll see how it goes.

- URI: /v1/github/repos/language/{language}
- Parameters: URI parameter for the language (again let's keep this simple)
- Response type: Json
- Response: ```
{
  "id": "...",
  "name": "...",
  "url": "...",
  "owner": "..."
}
```
# How to use

# Technologies
I have implemented using Spring 4, spring boot and Jax-RS. I considered using 
Spring 4 and reactive web as I want to learn those technologies, but felt it 
best to stick to what I know for the test.
I went with Jax-RS as this code will act effectively as glue code doing a call out
to the github API. Since I don't know the rate of incoming requests or the likely
performance of the github API I felt that the asynchronous nature of jax-rs was a
good call to prevent being thread bound.