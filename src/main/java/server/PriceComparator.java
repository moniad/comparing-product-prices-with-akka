package server;

import akka.actor.AbstractActor;
import model.ComparisonRequest;
import model.PriceComparisonResponse;
import service.ComparisonService;

public class PriceComparator extends AbstractActor {
    private final String PRICE_COMPARATOR_LOG_STRING = "[PRICE COMP.] ";
    private final ComparisonService comparisonService = new ComparisonService();

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ComparisonRequest.class, comparisonRequest -> {
                    PriceComparisonResponse priceComparisonResponse = comparisonService.getPriceComparisonResponse(comparisonRequest);

                    String productName = comparisonRequest.getProductName();

                    System.out.println(PRICE_COMPARATOR_LOG_STRING + productName.toUpperCase() + " handled " + priceComparisonResponse.getOccurrenceCount() + " times");

                    getSender().tell(priceComparisonResponse, getSelf());

                    System.out.println(PRICE_COMPARATOR_LOG_STRING + "SENT MSG: " + priceComparisonResponse.toString());

                })
                .matchAny(m -> System.out.println(PRICE_COMPARATOR_LOG_STRING + "Received UNKNOWN MSG: " + m))
                .build();
    }
}
