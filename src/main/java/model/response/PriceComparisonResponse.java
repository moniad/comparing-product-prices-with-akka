package model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PriceComparisonResponse {
    private final Integer smallerPrice;
    private final Integer occurrenceCount;
}
