import lombok.Builder;
import lombok.ToString;

@ToString
@Builder
public class ComparisonResponse {
    private final int smallerPrice;
    private final int occurrenceCount;
}
