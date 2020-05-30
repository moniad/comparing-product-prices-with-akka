import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PriceComparisonResponse {
    private final int smallerPrice;
    private final int occurrenceCount;
}
