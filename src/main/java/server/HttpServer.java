package server;

import akka.NotUsed;
import akka.actor.ActorContext;
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
import model.response.PriceComparisonResponse;
import server.service.ComparisonService;
import server.util.ComparisonUtil;

import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.PathMatchers.segment;

public class HttpServer extends AllDirectives implements Runnable {
    private final ActorSystem system;
    private final ComparisonService comparisonService = new ComparisonService();
    @Getter
    private CompletionStage<ServerBinding> binding;
    private final Materializer materializer;
    private final ActorContext context;
    private final String HTTP_SERVER_LOG_STRING = "[HTTP SERVER]: ";

    public HttpServer(ActorSystem system, ActorContext context) {
        this.system = system;
        materializer = Materializer.matFromSystem(system);
        this.context = context;
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

                            PriceComparisonResponse priceComparisonResponse = ComparisonUtil.getPriceComparisonResponse(context, comparisonRequest, HTTP_SERVER_LOG_STRING);

                            return complete(priceComparisonResponse.getStatus() != null ? priceComparisonResponse.getStatus() : priceComparisonResponse.getSmallerPrice().toString());
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