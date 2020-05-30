package server;

import akka.actor.AbstractActor;
import akka.actor.AllForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import client.DbClient;
import model.ComparisonRequest;
import model.PriceComparisonResponse;
import service.ComparisonService;

import java.time.Duration;

public class Server extends AbstractActor {
    private final String SERVER_LOG_STRING = "[SERVER] ";
    private final DbClient dbClient = new DbClient();
    private final ComparisonService comparisonService = new ComparisonService();

    private static final SupervisorStrategy strategy //todo: modify
            = new AllForOneStrategy( // OneForOneStrategy
            10,
            Duration.ofMinutes(1),
            DeciderBuilder
                    .match(ArithmeticException.class, e -> SupervisorStrategy.resume())
                    .matchAny(o -> SupervisorStrategy.restart())
                    .build());
    // for logging
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    // must be implemented -> creates initial behaviour
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ComparisonRequest.class, comparisonRequest -> {
                    String productName = comparisonRequest.getProductName();
                    System.out.println(SERVER_LOG_STRING + "RECEIVED msg: " + productName);

                    PriceComparisonResponse priceComparisonResponse = comparisonService.getPriceComparisonResponse(comparisonRequest);

                    System.out.println(SERVER_LOG_STRING + productName.toUpperCase() + " handled " + priceComparisonResponse.getOccurrenceCount() + " times");

                    getSender().tell(priceComparisonResponse, getSelf());

                    System.out.println(SERVER_LOG_STRING + "SENT MSG: " + priceComparisonResponse.toString());

//                    if (s.startsWith("m")) {
//                        context().child("multiplyWorker").get().tell(s, getSelf()); // send task to child
//                    } else if (s.startsWith("d")) {
//                        context().child("divideWorker").get().tell(s, getSelf()); // send task to child
//                    } else if (s.startsWith("result")) {
//                        System.out.println(s);              // result from child
//                    }

                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    // optional
    @Override
    public void preStart() {
//        context().actorOf(Props.create(Z1_MultiplyWorker.class), "multiplyWorker"); // todo: remove or change to shopActors?
//        context().actorOf(Props.create(Z1_DivideWorker.class), "divideWorker");

        dbClient.createRequestTable();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }
}
