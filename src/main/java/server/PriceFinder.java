package server;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import model.ComparisonRequest;
import model.SinglePriceResponse;

import java.util.Random;

public class PriceFinder extends AbstractActor {
    private static final Random random = new Random();
    private final String PRICE_FINDER_LOG_STRING = "[PRICE FINDER]: ";
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ComparisonRequest.class, comparisonRequest -> {
                    int price = getPrice();
                    log.info(PRICE_FINDER_LOG_STRING + String.format("%s's price is %d", comparisonRequest.getProductName(), price));
                    getSender().tell(SinglePriceResponse.builder()
                                    .name(comparisonRequest.getProductName())
                                    .price(price)
                                    .build(),
                            getSelf());
                    context().stop(self());
                })
                .matchAny(m -> log.info(PRICE_FINDER_LOG_STRING + "unknown message: " + m))
                .build();
    }

    private int getPrice() throws InterruptedException {
        Thread.sleep(random.nextInt(401) + 100); // 100-500 ms
        return random.nextInt(10) + 1; // 1-10
    }
}