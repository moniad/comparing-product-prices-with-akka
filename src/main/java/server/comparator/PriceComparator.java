package server.comparator;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import model.request.ComparisonRequest;
import model.response.PriceComparisonResponse;
import server.util.ResponseUtil;

import java.util.concurrent.CompletableFuture;

import static server.util.ComparisonUtil.buildResponse;
import static server.util.ComparisonUtil.prepareResponse;

public class PriceComparator extends AbstractActor {
    private final String PRICE_COMPARATOR_LOG_STRING = "[PRICE COMP.] ";
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

    private void prepareAndSendResponseToClient(ComparisonRequest comparisonRequest) {
        ResponseUtil responseUtil = prepareResponse(context(), comparisonRequest, getSender(), PRICE_COMPARATOR_LOG_STRING);

        CompletableFuture.allOf(responseUtil.getPrice1(), responseUtil.getPrice2(), responseUtil.getOccurrencesCount1())
                .whenComplete((res, err) -> {
                    PriceComparisonResponse priceComparisonResponse = buildResponse(responseUtil.getSmallerPrice(),
                            responseUtil.getOccurrencesCount());
                    sendResponse(comparisonRequest, priceComparisonResponse, responseUtil.getClient());
                    context().stop(self());
                });
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
}