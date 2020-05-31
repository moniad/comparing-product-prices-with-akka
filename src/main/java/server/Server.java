package server;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.ServerBinding;
import model.request.ComparisonRequest;
import model.request.TerminateRequest;
import server.comparator.PriceComparator;
import server.finder.DbClient;

public class Server extends AbstractActor {
    private HttpServer httpServer;
    private Thread httpServerThread;
    private final String SERVER_LOG_STRING = "[SERVER] ";
    private final DbClient dbClient = new DbClient();
    private static int usageCounter = 0;
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    // must be implemented -> creates initial behaviour
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ComparisonRequest.class, comparisonRequest -> {
                    String productName = comparisonRequest.getProductName();
                    System.out.println(SERVER_LOG_STRING + "RECEIVED msg: " + productName);

                    // server sends ComparisonRequest on client's behalf
                    context().actorOf(Props.create(PriceComparator.class, getSelf()), "priceComparator" + usageCounter++).tell(comparisonRequest, getSender());
                })
                .match(TerminateRequest.class, terminateRequest -> terminate())
                .matchAny(o -> log.info(SERVER_LOG_STRING + "received unknown message"))
                .build();
    }

    @Override
    public void preStart() {
        dbClient.createRequestTable();
        httpServer = new HttpServer(getContext().getSystem(), getContext());
        startHttpServer();
    }

    private void startHttpServer() {
        httpServerThread = new Thread(httpServer);
        httpServerThread.start();
    }

    private void terminate() {
        context().stop(self());
        unbindHttpServer();
    }

    private void unbindHttpServer() {
        try {
            httpServerThread.join();
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
        httpServer.getBinding()
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> getContext().getSystem().terminate()); // and shutdown when done
        System.out.println("Unbinded HTTP server. Closing app...");
    }
}
