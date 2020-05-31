package server;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import lombok.Getter;
import model.request.ComparisonRequest;
import server.service.ComparisonService;

import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.PathMatchers.segment;

public class HttpServer extends AllDirectives implements Runnable {
    private final ActorSystem system;
    private final ComparisonService comparisonService = new ComparisonService();
    @Getter
    private CompletionStage<ServerBinding> binding;
    private final Materializer materializer;

    public HttpServer(ActorSystem system) {
        this.system = system;
        materializer = Materializer.matFromSystem(system);
    }

    @Override
    public void run() {
        final Http http = Http.get(system);

        //In order to access all directives we need an instance where the routes are defined.

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = createRoute().flow(system, materializer);
        binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("localhost", 8080), materializer);

        System.out.println("Server online at http://localhost:8080/");
    }

    private Route createRoute() {
        return concat(
                path(segment("price").slash(segment()), value ->
                        get(() -> {
                            ComparisonRequest comparisonRequest = ComparisonRequest.builder()
                                    .productName(value)
                                    .build();
                            return complete(Integer.valueOf(comparisonService.updateAndGetOccurrencesCount(comparisonRequest)).toString());
                        })
                ),
                path(segment("review").slash(segment()), value ->
                        get(() -> {
                            ComparisonRequest comparisonRequest = ComparisonRequest.builder().productName(value).build();
                            return completeOKWithFuture(comparisonService.getReviewResponse(system, materializer, comparisonRequest),
                                    Jackson.marshaller());
                        })));
    }
}