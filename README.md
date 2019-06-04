# async-tomcat-example

End-to-end asynchronous servlet example with Tomcat Docker container, demonstrating nonblocking upstream and downstream calls

## What?

Non-blocking HTTP examples are fairly common, but a lot of them only do non-blocking connections to the server. If your service makes outbound connections of its own, then you can handle blocking calls using patterns with Runnable and executors (which then have the same problems you're trying to avoid by using the Servlet 3 nonblocking tools), or your service logic needs to be non-blocking from end to end.

The downstream calls need to coordinate, then when they have all returned, complete the response to your client.

Java CompletableFutures are a good solution to this problem: you can assemble all of your logic at incoming request time, start your nonblocking downstream requests, and leave them with a callback allowing writing to the final response, and return your servlet container's request handler thread immediately. The Futures are a very readable abstraction that hides details of the "callback hell" that was our original response to the non-blocking IO paradigm.

To demonstrate the idea very simply, we take an incoming query string, make identical searches to DuckDuckGo and Google, and report back to the user on their response sizes.

## How?

For the incoming thread I've just used Tomcat's Servlet 3 async support - the servlet is marked as `async-supported` in web.xml and it picks up an AsyncContext for the incoming HttpServletRequest.

For the downstream requests I have used [AsyncHttpClient](https://github.com/AsyncHttpClient/async-http-client), though you could equally use raw Netty (which it is a wrapper for) as long as you're willing to build your own Futures in callbacks.

The only nonobvious thing here is the passing of the HttpServletResponse and AsyncContext to the final step in the chain of Futures (`writeResponseBody`, which returns the handler proper), ensuring that they don't get prematurely garbage collected (because the returned lambda object is itself a closure, containing references to them) and can be used to write things back to the client socket once the downstream requests have returned. Once that is done, the controlling thread (in this case probably Netty's callback handler, though in other frameworks this is not the case) releases all of the Futures and they are garbage collected.

## Running

```bash
$ ./gradlew build war
$ docker build -t tomcat-async-example .
$ docker run -p 8082:8080 tomcat-async-example:latest

# another terminal - this next bit is Mac-specific but you get the idea
$ open 'http://localhost:8r82/at-ex?q=sometimes+i+wonder'
```

## What next?

This is a really simple example. It gets more exciting when the results of downstream requests are used to spawn further requests and conditionally do even more complcated things. Error handling is a whole other kettle of fish. And it's almost certainly better not to do all this in the servlet's `service` body... you very quickly start building bigger asynchronous abstractions. But the CompletableFutures are a very powerful starting point.
