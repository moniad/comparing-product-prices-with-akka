import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import lombok.Getter;

import java.util.concurrent.CompletionStage;

public class HttpServer extends AllDirectives implements Runnable {
    private ActorSystem system;
    @Getter
    private CompletionStage<ServerBinding> binding;

    HttpServer(ActorSystem system) {
        this.system = system;
    }

    @Override
    public void run() {
        final Http http = Http.get(system);
        final Materializer materializer = Materializer.matFromSystem(system);

        //In order to access all directives we need an instance where the routes are defined.

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = createRoute().flow(system, materializer);
        binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("localhost", 8080), materializer);

        System.out.println("Server online at http://localhost:8080/");
    }

    private Route createRoute() {
        return concat(
                path("hello", () ->
                        get(() ->
                                complete("<h1>Say hello to akka-http</h1>"))));
    }
}