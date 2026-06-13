package com.margins.metric.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.margins.metric.dto.MetricSnapshotResponse;
import com.margins.metric.mapper.MetricMapper;
import com.margins.metric.model.MetricRecord;
import com.margins.metric.model.SessionMetricSourceRecord;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class MetricBusiness {

    private static final long DEFAULT_USER_ID = 1L;
    private static final String SESSION_SNAPSHOT = "session_snapshot";

    private final MetricMapper metricMapper;
    private final ObjectMapper objectMapper;

    public MetricSnapshotResponse createSessionSnapshot(Long sessionId) {
        SessionMetricSourceRecord source = metricMapper.findSessionSource(sessionId, DEFAULT_USER_ID);
        if (source == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reading session not found");
        }

        BigDecimal progressPercent = progressPercent(source.getCurrentPage(), source.getTargetPage());
        MetricRecord record = MetricRecord.builder()
            .userId(source.getUserId())
            .bookId(source.getBookId())
            .sessionId(source.getSessionId())
            .metricName(SESSION_SNAPSHOT)
            .metricScope("session")
            .metricValue(progressPercent)
            .metricUnit(progressPercent == null ? null : "percent")
            .metricDetails(detailsJson(source, progressPercent))
            .sourceRef("session:" + source.getSessionId() + ":snapshot")
            .generatedBy("reader")
            .testData(true)
            .build();

        if (metricMapper.insert(record) <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Metric snapshot could not be saved");
        }
        return toResponse(record, source);
    }

    private MetricSnapshotResponse toResponse(MetricRecord record, SessionMetricSourceRecord source) {
        return MetricSnapshotResponse.builder()
            .metricId(record.getId())
            .sessionId(source.getSessionId())
            .metricName(record.getMetricName())
            .metricValue(record.getMetricValue())
            .metricUnit(record.getMetricUnit())
            .windowCount(source.getWindowCount())
            .questionCount(source.getQuestionCount())
            .answeredQuestionCount(source.getAnsweredQuestionCount())
            .highlightCount(source.getHighlightCount())
            .messageCount(source.getMessageCount())
            .personaCount(source.getPersonaCount())
            .pagesReadEstimate(source.getPagesReadEstimate())
            .build();
    }

    private String detailsJson(SessionMetricSourceRecord source, BigDecimal progressPercent) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("progressPercent", progressPercent);
        details.put("startPage", source.getStartPage());
        details.put("currentPage", source.getCurrentPage());
        details.put("targetPage", source.getTargetPage());
        details.put("pagesReadEstimate", source.getPagesReadEstimate());
        details.put("windowCount", source.getWindowCount());
        details.put("questionCount", source.getQuestionCount());
        details.put("answeredQuestionCount", source.getAnsweredQuestionCount());
        details.put("highlightCount", source.getHighlightCount());
        details.put("messageCount", source.getMessageCount());
        details.put("personaCount", source.getPersonaCount());

        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Metric details could not be serialized", exception);
        }
    }

    private BigDecimal progressPercent(Integer currentPage, Integer targetPage) {
        if (currentPage == null || targetPage == null || targetPage <= 0) {
            return null;
        }

        int percent = Math.round((currentPage * 100.0f) / targetPage);
        return BigDecimal.valueOf(Math.max(0, Math.min(100, percent)));
    }
}
