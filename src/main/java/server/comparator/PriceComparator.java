package server.comparator;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import model.request.ComparisonRequest;
import model.response.OccurrenceCountResponse;
import model.response.PriceComparisonResponse;
import model.response.SinglePriceResponse;
import server.finder.OccurrencesCountFinder;
import server.finder.PriceFinder;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static akka.pattern.Patterns.ask;

public class PriceComparator extends AbstractActor {
    private final String PRICE_COMPARATOR_LOG_STRING = "[PRICE COMP.] ";
    private final Duration timeoutDuration = Duration.ofMillis(300);
    private final ActorRef server;

    PriceComparator(ActorRef server) {
        this.server = server;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ComparisonRequest.class, comparisonRequest -> {
                    System.out.println(PRICE_COMPARATOR_LOG_STRING + " received : " + comparisonRequest);
                    prepareAndSendResponseToClient(comparisonRequest);
                })
                .matchAny(m -> System.out.println(PRICE_COMPARATOR_LOG_STRING + "Received UNKNOWN MSG: " + m))
                .build();
    }

    private void prepareAndSendResponseToClient(ComparisonRequest comparisonRequest) {
        ActorRef client = getSender();
        ActorRef priceFinderActor1 = context().actorOf(Props.create(PriceFinder.class), "priceFinder1");
        ActorRef priceFinderActor2 = context().actorOf(Props.create(PriceFinder.class), "priceFinder2");
        ActorRef occurrencesCountActor = context().actorOf(Props.create(OccurrencesCountFinder.class), "occurrenceFinder1");

        final CompletableFuture<Object> price1 = ask(priceFinderActor1, comparisonRequest, timeoutDuration).toCompletableFuture();
        final CompletableFuture<Object> price2 = ask(priceFinderActor2, comparisonRequest, timeoutDuration).toCompletableFuture();
        final CompletableFuture<Object> occurrencesCount1 = ask(occurrencesCountActor, comparisonRequest, timeoutDuration).toCompletableFuture();

        final AtomicInteger smallerPrice = new AtomicInteger(-1);
        final AtomicInteger occurrencesCount = new AtomicInteger(-1);

        price1.whenComplete((res, err) -> tryToSetSmallerPrice((SinglePriceResponse) res, err, smallerPrice));
        price2.whenComplete((res, err) -> tryToSetSmallerPrice((SinglePriceResponse) res, err, smallerPrice));
        occurrencesCount1.whenComplete((res, err) -> tryToSetOccurrencesCount((OccurrenceCountResponse) res, err, occurrencesCount));

        CompletableFuture.allOf(price1, price2, occurrencesCount1).whenComplete((res, err) -> {
            buildAndSendResponse(comparisonRequest, client, smallerPrice, occurrencesCount);
            context().stop(self());
        });
    }

    private void buildAndSendResponse(ComparisonRequest comparisonRequest, ActorRef client, AtomicInteger smallerPrice, AtomicInteger occurrencesCount) {
        int smallerPriceValue = smallerPrice.get();
        int occurrencesCountValue = occurrencesCount.get();

        PriceComparisonResponse priceComparisonResponse = PriceComparisonResponse.builder()
                .occurrenceCount(occurrencesCountValue == -1 ? null : occurrencesCountValue)
                .smallerPrice(smallerPriceValue == -1 ? null : smallerPriceValue)
                .build();

        logSendingResponse(comparisonRequest, priceComparisonResponse);
        client.tell(priceComparisonResponse, server);
    }

    private void logSendingResponse(ComparisonRequest comparisonRequest, PriceComparisonResponse priceComparisonResponse) {
        String productName = comparisonRequest.getProductName();
        Integer occurrenceCount = priceComparisonResponse.getOccurrenceCount();
        System.out.println((PRICE_COMPARATOR_LOG_STRING + productName.toUpperCase() + " handled " +
                ((occurrenceCount == null) ? " not available" : occurrenceCount + " times")));

        System.out.println(PRICE_COMPARATOR_LOG_STRING + "SENT MSG: " + priceComparisonResponse.toString());

    }

    private void tryToSetSmallerPrice(SinglePriceResponse res, Object err, AtomicInteger smallerPrice) {
        if (err == null) {
            int smallerPriceValue = smallerPrice.get();
            int resInt = res.getPrice();
            if (smallerPriceValue == -1) {
                smallerPrice.set(resInt);
            } else {
                smallerPrice.getAndSet(Math.min(resInt, smallerPrice.get()));
            }
        } else {
            System.out.println(PRICE_COMPARATOR_LOG_STRING + ": price unavailable");
        }
    }

    private void tryToSetOccurrencesCount(OccurrenceCountResponse res, Object err, AtomicInteger occurrencesCount) {
        if (err == null) {
            int resInt = res.getOccurrenceCount();
            occurrencesCount.set(resInt);
        } else {
            System.out.println(PRICE_COMPARATOR_LOG_STRING + ": occurrencesCount unavailable");
        }
    }
}