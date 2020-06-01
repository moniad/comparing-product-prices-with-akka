package model;

import lombok.Getter;

public enum Status {
    OK("OK"), PRICE_ERROR("Prices for this product are currently unavailable"),
    OCCURRENCE_COUNTER_ERROR("Occurrence counter for this product is currently unavailable");

    @Getter
    private final String statusDescription;

    Status(String statusDescription) {
        this.statusDescription = statusDescription;
    }
}
