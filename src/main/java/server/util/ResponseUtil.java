package server.util;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;
import lombok.Data;
import model.request.ComparisonRequest;
import server.finder.OccurrencesCountFinder;
import server.finder.PriceFinder;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static akka.pattern.Patterns.ask;

@Data
public class ResponseUtil {
    ActorContext context;
    ActorRef client;
    ActorRef priceFinderActor1;
    ActorRef priceFinderActor2;
    ActorRef occurrencesCountActor;

    final CompletableFuture<Object> price1;
    final CompletableFuture<Object> price2;
    final CompletableFuture<Object> occurrencesCount1;

    AtomicInteger smallerPrice;
    AtomicInteger occurrencesCount;

    public ResponseUtil(ActorContext context, ActorRef sender, ComparisonRequest comparisonRequest, Duration timeoutDuration) {
        client = sender;
        priceFinderActor1 = context.actorOf(Props.create(PriceFinder.class), "priceFinder1");
        priceFinderActor2 = context.actorOf(Props.create(PriceFinder.class), "priceFinder2");
        occurrencesCountActor = context.actorOf(Props.create(OccurrencesCountFinder.class), "occurrenceFinder1");

        price1 = ask(priceFinderActor1, comparisonRequest, timeoutDuration).toCompletableFuture();
        price2 = ask(priceFinderActor2, comparisonRequest, timeoutDuration).toCompletableFuture();
        occurrencesCount1 = ask(occurrencesCountActor, comparisonRequest, timeoutDuration).toCompletableFuture();

        smallerPrice = new AtomicInteger(-1);
        occurrencesCount = new AtomicInteger(-1);
    }

    public void setSmallerPrice(int smallerPrice) {
        this.smallerPrice.set(smallerPrice);
    }

    public void setOccurrencesCount(int occurrencesCount) {
        this.occurrencesCount.set(occurrencesCount);
    }
}
