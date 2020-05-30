package model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ComparisonRequest {
    private final String productName;
}
