package com.kinplatform.kin.context;

import java.util.Collections;
import java.util.Map;

public record AnalysisResult(Map<AnalyzedDimension, String> extracted) {

    public AnalysisResult {
        extracted = (extracted != null)
                ? Collections.unmodifiableMap(extracted)
                : Collections.emptyMap();
    }

    public boolean isEmpty() {
        return extracted.isEmpty();
    }

    public static AnalysisResult empty() {
        return new AnalysisResult(Collections.emptyMap());
    }
}
