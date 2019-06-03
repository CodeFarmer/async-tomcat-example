package io.gluth.examples.async;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.asynchttpclient.Dsl.asyncHttpClient;


public class AsyncExampleServlet
    extends HttpServlet

{

    AsyncHttpClient client;

    @Override
    public void init()
    {
        this.client = asyncHttpClient();
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse resp) {

        String q = req.getParameter("q");
        System.out.println("JGG-DEBUG: service(q=" + q + ") start...");

        CompletableFuture<String> googleBody = this.client
                .prepareGet("https://www.google.com?q=" + q)
                .execute()
                .toCompletableFuture()
                .thenApply(Response::getResponseBody);

        CompletableFuture<String> duckBody = this.client
                .prepareGet("https://duckduckgo.com?q=" + q + "&ia=web")
                .execute()
                .toCompletableFuture()
                .thenApply(Response::getResponseBody);

        duckBody.thenCombine(googleBody, AsyncExampleServlet::consumeBodies)
        .thenAccept(writeResponseBody(req.startAsync(), resp));

        System.out.println("JGG-DEBUG: service(q=" + q + ") done.");

    }

    @Override
    public void destroy()
    {
        try {
            this.client.close();
        } catch (IOException e) {
            // Not a lot else we can do here, but it's probably a WARN anyway
            e.printStackTrace();
        }
    }


    static String consumeBodies(String ddgBody, String googleBody) {

        // System.out.println("JGG-DEBUG: ddgBody: " + ddgBody);

        return String.format("DuckDuckGo response length: %d\nGoogle response length: %d", ddgBody.length(), googleBody.length());

    }

    /**
     * This is necessary to capture the AsyncContext and make sure nothing gets garbage collected before its time
     */
    static Consumer<String> writeResponseBody(AsyncContext ctx, HttpServletResponse resp) {

        return s -> {
            try {
                resp.setContentType("text/plain");
                resp.getWriter().write(s);
                resp.getWriter().flush();
                System.out.println("JGG-DEBUG: wrote response body");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            finally {
                ctx.complete();
            }
        };

    }

}
