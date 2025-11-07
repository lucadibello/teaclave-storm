package ch.usi.inf.confidentialstorm.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record HistogramSnapshot(Map<String, Long> counts) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public HistogramSnapshot {
        if (counts == null) {
            throw new IllegalArgumentException("Counts cannot be null");
        }
        counts = Collections.unmodifiableMap(new HashMap<>(counts));
    }
}
