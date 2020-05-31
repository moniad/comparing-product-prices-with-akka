package server.util;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import model.request.ComparisonRequest;
import model.response.OccurrenceCountResponse;
import model.response.PriceComparisonResponse;
import model.response.SinglePriceResponse;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ComparisonUtil {
    private static final Duration timeoutDuration = Duration.ofMillis(300);

    public static PriceComparisonResponse getPriceComparisonResponse(ActorContext context, ComparisonRequest comparisonRequest, String LOG_STRING) {
        ResponseUtil responseUtil = prepareResponse(context, comparisonRequest, null, LOG_STRING);
        AtomicReference<PriceComparisonResponse> priceComparisonResponse = new AtomicReference<>();

        CompletableFuture.allOf(responseUtil.getPrice1(), responseUtil.getPrice2(), responseUtil.getOccurrencesCount1())
                .whenComplete((res, err) -> priceComparisonResponse.set(buildResponse(responseUtil.getSmallerPrice(),
                        responseUtil.getOccurrencesCount())));

        try {
            responseUtil.getPrice1().join();
            responseUtil.getPrice2().join();
            responseUtil.getOccurrencesCount1().join();
        } catch (CompletionException e) {
            return PriceComparisonResponse.builder().status("Waited too long for answer").build();
        }
        return priceComparisonResponse.get();
    }

    public static ResponseUtil prepareResponse(ActorContext context, ComparisonRequest comparisonRequest, ActorRef sender, String LOG_STRING) {
        ResponseUtil responseUtil = new ResponseUtil(context, sender, comparisonRequest, timeoutDuration);

        responseUtil.getPrice1().whenComplete((res, err) -> tryToSetSmallerPrice((SinglePriceResponse) res, err, responseUtil, LOG_STRING));
        responseUtil.getPrice2().whenComplete((res, err) -> tryToSetSmallerPrice((SinglePriceResponse) res, err, responseUtil, LOG_STRING));
        responseUtil.getOccurrencesCount1().whenComplete((res, err) -> tryToSetOccurrencesCount((OccurrenceCountResponse) res, err, responseUtil, LOG_STRING));

        return responseUtil;
    }

    public static PriceComparisonResponse buildResponse(AtomicInteger smallerPrice, AtomicInteger occurrencesCount) {
        int smallerPriceValue = smallerPrice.get();
        int occurrencesCountValue = occurrencesCount.get();

        return PriceComparisonResponse.builder()
                .occurrenceCount(occurrencesCountValue == -1 ? null : occurrencesCountValue)
                .smallerPrice(smallerPriceValue == -1 ? null : smallerPriceValue)
                .build();
    }

    private static void tryToSetSmallerPrice(SinglePriceResponse res, Object err, ResponseUtil responseUtil, String LOG_STRING) {
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
            System.out.println(LOG_STRING + ": price unavailable");
        }
    }

    private static void tryToSetOccurrencesCount(OccurrenceCountResponse res, Object err, ResponseUtil responseUtil, String LOG_STRING) {
        if (err == null) {
            int resInt = res.getOccurrenceCount();
            responseUtil.setOccurrencesCount(resInt);
        } else {
            System.out.println(LOG_STRING + ": occurrencesCount unavailable");
        }
    }
}
