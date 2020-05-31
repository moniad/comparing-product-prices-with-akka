package server;

import akka.actor.AbstractActor;
import akka.actor.AllForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import client.DbClient;
import model.ComparisonRequest;

import java.time.Duration;

public class Server extends AbstractActor {
    private final String SERVER_LOG_STRING = "[SERVER] ";
    private final DbClient dbClient = new DbClient();

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

                    // server sends ComparisonRequest on client's behalf
                    context().actorOf(Props.create(PriceComparator.class)).tell(comparisonRequest, getSender());
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    // optional
    @Override
    public void preStart() {
        dbClient.createRequestTable();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }
}
