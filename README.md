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
programming language.

## Paged version:
- URI: /v1/github/repos/language/{language}/paged
- Parameters:
  - URI parameter for the language (again let's keep this simple)
- Response type: Json
- Response:
```
[
  {
    "id": "...",
    "name": "...",
    "url": "...",
    "owner": "..."
  },
...
]
Reponse headers:
- Link: provides a link to the next page
```

## Get all
This version returns all repositories for a given language, it uses a StreamedOutput to ensure we don't blow up memory, 
and parses the X-RateLimit headers to pause for the correct amount of time when we hit the limit. I was running short of 
time so this api is purely manually tested.

- URI: /v1/github/repos/language/{language}
- Parameters:
  - URI parameter for the language (again let's keep this simple)
- Response type: Json
- Response:
```
[
  {
    "id": "...",
    "name": "...",
    "url": "...",
    "owner": "..."
  },
...
]
```

# How to use

You can download an executable jar from [https://github.com/SillyMoo/symantest/releases]() To use this jar simply type:

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
basic functionality working. The 'get everything' version has only been manually tested so far.
- Logging. This is pretty much non-existent at this stage
- Paging support. I parsed the 'page' query parameter in the link returned by github, this works but this
parameter is undocumented so it's a risk. It may be better to urlencode the query string of the link returned
by github.
- Error responses. If there is an 'issue' at the github end, we just return an error, we should probably
propagate some details of the github response to aid investigations.
- Input validation. It would be possible to inject additional query parameters in to the 'language' parameter. We should
probably validate the language to confirm it is compliant with github naming rules.
- Performance. For the 'get everything' version we are very much beholdent to github performance and rate limit rules. Going
asynchronous won't help that much here, because ultimately we'll still hit the same rate issues no matter how many
concurrent clients we let connect.
