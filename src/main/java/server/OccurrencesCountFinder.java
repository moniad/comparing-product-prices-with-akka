package server;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import model.ComparisonRequest;
import model.OccurrenceCountResponse;
import service.ComparisonService;

public class OccurrencesCountFinder extends AbstractActor {
    private final ComparisonService comparisonService = new ComparisonService();
    private final String OCCURRENCE_COUNT_FINDER_LOG_STRING = "[OCCURRENCE COUNT FINDER]: ";
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ComparisonRequest.class, comparisonRequest -> {
                    int occurrenceCount = comparisonService.updateAndGetOccurrencesCount(comparisonRequest);

                    log.info(OCCURRENCE_COUNT_FINDER_LOG_STRING + String.format("Finding %s's occurrence count",
                            comparisonRequest.getProductName()));
                    getSender().tell(OccurrenceCountResponse.builder()
                                    .name(comparisonRequest.getProductName())
                                    .occurrenceCount(occurrenceCount)
                                    .build(),
                            getSelf());
                    context().stop(self());
                })
                .matchAny(m -> log.info("Unknown message: " + m))
                .build();
    }
}