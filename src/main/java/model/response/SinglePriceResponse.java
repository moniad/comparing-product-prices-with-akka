package model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SinglePriceResponse {
    private final String name;
    private final int price;
}
