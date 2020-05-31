package model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OccurrenceCountResponse {
    private final String name;
    private final int occurrenceCount;
}
