package client;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import model.ComparisonRequest;
import model.PriceComparisonResponse;
import model.TerminateRequest;

public class Client extends AbstractActor {
    protected ActorRef server;
    private final String CLIENT_LOG_STRING;
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    Client(int clientNumber, ActorRef server) {
        this.server = server;
        this.CLIENT_LOG_STRING = String.format("[CLIENT NO. %d] ", clientNumber);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, productName -> {
                    System.out.println(CLIENT_LOG_STRING + "asking server for " + productName + "'s price...");
                    ComparisonRequest comparisonRequest = ComparisonRequest.builder()
                            .productName(productName)
                            .build();
                    server.tell(comparisonRequest, getSelf());
                })
                // result from server
                .match(PriceComparisonResponse.class, response -> System.out.println(CLIENT_LOG_STRING + "RECEIVED: " + response))
                .match(TerminateRequest.class, terminateRequest -> terminate())
                .matchAny(i -> log.info(CLIENT_LOG_STRING + "RECEIVED: unknown message"))
                .build();
    }

    private void terminate() {
        context().stop(self());
    }
}
