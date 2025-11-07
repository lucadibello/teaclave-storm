package ch.usi.inf.confidentialstorm.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public record SplitSentenceResponse(List<String> words) implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    public SplitSentenceResponse {
        if (words == null) {
            throw new IllegalArgumentException("Words cannot be null");
        }
        words = List.copyOf(new ArrayList<>(words));
    }
}
