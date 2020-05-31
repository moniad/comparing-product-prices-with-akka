package client;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import model.ComparisonRequest;
import model.PriceComparisonResponse;

public class Client extends AbstractActor {
    private final int clientNumber;
    protected ActorRef server;
    private final String CLIENT_LOG_STRING;

    Client(int clientNumber, ActorRef server) {
        this.clientNumber = clientNumber;
        this.server = server;
        this.CLIENT_LOG_STRING = String.format("[CLIENT NO. %d] ", clientNumber);
    }

    @Override
    public Receive createReceive() {
        // result from server TODO
        return receiveBuilder()
                .match(String.class, productName -> {
                    System.out.println(CLIENT_LOG_STRING + "asking server for " + productName + "'s price...");
                    ComparisonRequest comparisonRequest = ComparisonRequest.builder()
                            .productName(productName)
                            .build();
                    server.tell(comparisonRequest, getSelf());
                })
                .match(PriceComparisonResponse.class, response -> {
                    System.out.println(CLIENT_LOG_STRING + "RECEIVED: " + response);
                })
                .matchAny(i -> System.out.println(CLIENT_LOG_STRING + "RECEIVED: unknown message"))
                .build();
    }

    protected void terminate() { // todo: ok?
//        server.terminate();
    }
}
