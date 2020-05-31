package server.comparator;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import model.request.ComparisonRequest;
import model.response.OccurrenceCountResponse;
import model.response.PriceComparisonResponse;
import model.response.SinglePriceResponse;
import server.util.ResponseUtil;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PriceComparator extends AbstractActor {
    private final String PRICE_COMPARATOR_LOG_STRING = "[PRICE COMP.] ";
    private final Duration timeoutDuration = Duration.ofMillis(300);
    private final ActorRef server;

    public PriceComparator(ActorRef server) {
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

    public PriceComparisonResponse getPriceComparisonResponse(ComparisonRequest comparisonRequest) {
        ResponseUtil responseUtil = prepareResponse(comparisonRequest);
        AtomicReference<PriceComparisonResponse> priceComparisonResponse = new AtomicReference<>();

        CompletableFuture.allOf(responseUtil.getPrice1(), responseUtil.getPrice2(), responseUtil.getOccurrencesCount1())
                .whenComplete((res, err) -> priceComparisonResponse.set(buildResponse(responseUtil.getSmallerPrice(),
                        responseUtil.getOccurrencesCount())));
        return priceComparisonResponse.get();
    }

    private ResponseUtil prepareResponse(ComparisonRequest comparisonRequest) {
        ResponseUtil responseUtil = new ResponseUtil(context(), getSender(), comparisonRequest, timeoutDuration);

        responseUtil.getPrice1().whenComplete((res, err) -> tryToSetSmallerPrice((SinglePriceResponse) res, err, responseUtil));
        responseUtil.getPrice2().whenComplete((res, err) -> tryToSetSmallerPrice((SinglePriceResponse) res, err, responseUtil));
        responseUtil.getOccurrencesCount1().whenComplete((res, err) -> tryToSetOccurrencesCount((OccurrenceCountResponse) res, err, responseUtil));

        return responseUtil;
    }

    private void prepareAndSendResponseToClient(ComparisonRequest comparisonRequest) {
        ResponseUtil responseUtil = prepareResponse(comparisonRequest);

        CompletableFuture.allOf(responseUtil.getPrice1(), responseUtil.getPrice2(), responseUtil.getOccurrencesCount1())
                .whenComplete((res, err) -> {
                    PriceComparisonResponse priceComparisonResponse = buildResponse(responseUtil.getSmallerPrice(),
                            responseUtil.getOccurrencesCount());
                    sendResponse(comparisonRequest, priceComparisonResponse, responseUtil.getClient());
                    context().stop(self());
                });
    }

    private PriceComparisonResponse buildResponse(AtomicInteger smallerPrice, AtomicInteger occurrencesCount) {
        int smallerPriceValue = smallerPrice.get();
        int occurrencesCountValue = occurrencesCount.get();

        return PriceComparisonResponse.builder()
                .occurrenceCount(occurrencesCountValue == -1 ? null : occurrencesCountValue)
                .smallerPrice(smallerPriceValue == -1 ? null : smallerPriceValue)
                .build();
    }

    private void sendResponse(ComparisonRequest comparisonRequest, PriceComparisonResponse priceComparisonResponse, ActorRef client) {
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

    private void tryToSetSmallerPrice(SinglePriceResponse res, Object err, ResponseUtil responseUtil) {
        AtomicInteger smallerPrice = responseUtil.getSmallerPrice();
        if (err == null) {
            int smallerPriceValue = smallerPrice.get();
            int resInt = res.getPrice();
            if (smallerPriceValue == -1) {
                responseUtil.setSmallerPrice(resInt);
            } else {
                smallerPrice.getAndSet(Math.min(resInt, smallerPrice.get()));
            }
        } else {
            System.out.println(PRICE_COMPARATOR_LOG_STRING + ": price unavailable");
        }
    }

    private void tryToSetOccurrencesCount(OccurrenceCountResponse res, Object err, ResponseUtil responseUtil) {
        if (err == null) {
            int resInt = res.getOccurrenceCount();
            responseUtil.setOccurrencesCount(resInt);
        } else {
            System.out.println(PRICE_COMPARATOR_LOG_STRING + ": occurrencesCount unavailable");
        }
    }
}