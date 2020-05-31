package server.service;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.stream.Materializer;
import model.request.ComparisonRequest;
import org.jsoup.Jsoup;
import server.finder.DbClient;

import java.util.concurrent.CompletionStage;

public class ComparisonService {
    private static final DbClient dbClient = new DbClient();
    private static final String OPINEO_SEARCH_URL = "https://www.opineo.pl/?szukaj=";
    private final long WEB_TIMEOUT = 3000;

    public Integer getOccurrencesCount(ComparisonRequest comparisonRequest) {
        return dbClient.getOccurrencesCount(comparisonRequest.getProductName());
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
                response.entity().toStrict(WEB_TIMEOUT, materializer)).thenApply(
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
