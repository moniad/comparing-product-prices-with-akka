import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.stream.Materializer;
import org.jsoup.Jsoup;

import java.util.Random;
import java.util.concurrent.CompletionStage;

public class ComparisonService {
    private static final DbClient dbClient = new DbClient();
    private static final String OPINEO_SEARCH_URL = "https://www.opineo.pl/?szukaj=";
    private final Random random = new Random();
    private final long TIMEOUT = 3000;

    public PriceComparisonResponse getPriceComparisonResponse(ComparisonRequest comparisonRequest) {
        int price1 = getPrice();
        int price2 = getPrice(); // TODO: this and db access as well need to be paralleled
        // todo: handle timeouts

        int occurrenceCount = dbClient.handleClientRequest(comparisonRequest.getProductName());
        int smallerPrice = Math.min(price1, price2);

        return PriceComparisonResponse.builder()
                .smallerPrice(smallerPrice)
                .occurrenceCount(occurrenceCount)
                .build();
    }

    public int getPrice() {
        try {
            Thread.sleep(random.nextInt(401) + 100); // 100-500 ms
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
        return random.nextInt(10) + 1; // 1-10
    }

    public CompletionStage<Object> getReviewResponse(ActorSystem system, Materializer materializer, ComparisonRequest comparisonRequest) {
        String url = buildPageUrl(comparisonRequest);
        return getProductAssets(system, url, materializer);
    }

    private String buildPageUrl(ComparisonRequest comparisonRequest) {
        return OPINEO_SEARCH_URL + String.join("%20", comparisonRequest.getProductName().split(" ")).concat("&s=2");
    }

    private CompletionStage<Object> getProductAssets(ActorSystem system, String url, Materializer materializer) {
        return Http.get(system).singleRequest(HttpRequest.create(url)).thenCompose(response ->
                response.entity().toStrict(TIMEOUT, materializer)).thenApply(
                entity -> Jsoup.parse(entity.getData().utf8String())
                        .body()
                        .getElementById("page")
                        .getElementById("content")
                        .getElementById("screen")
                        .getElementsByClass("pls")
                        .get(0)
                        .getElementsByClass("shl_i pl_i")
                        .get(0)
                        .getElementsByClass("pl_attr")
                        .get(0)
                        .getElementsByTag("li")
                        .eachText()
                        .toArray()
        );
    }
}
