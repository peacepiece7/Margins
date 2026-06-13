package com.margins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.margins.metric.business.MetricBusiness;
import com.margins.metric.dto.MetricSnapshotResponse;
import com.margins.metric.mapper.MetricMapper;
import com.margins.metric.model.MetricRecord;
import com.margins.metric.model.SessionMetricSourceRecord;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MetricBusinessTest {

    @Test
    void createSessionSnapshotPersistsDerivedMetric() {
        FakeMetricMapper mapper = new FakeMetricMapper();
        MetricBusiness business = new MetricBusiness(mapper, new ObjectMapper());

        MetricSnapshotResponse response = business.createSessionSnapshot(77L);

        assertThat(response.getMetricId()).isEqualTo(700L);
        assertThat(response.getSessionId()).isEqualTo(77L);
        assertThat(response.getMetricName()).isEqualTo("session_snapshot");
        assertThat(response.getMetricValue()).isEqualByComparingTo(BigDecimal.valueOf(40));
        assertThat(response.getMetricUnit()).isEqualTo("percent");
        assertThat(response.getMessageCount()).isEqualTo(6);
        assertThat(response.getAnsweredQuestionCount()).isEqualTo(2);
        assertThat(mapper.inserted.getMetricScope()).isEqualTo("session");
        assertThat(mapper.inserted.getSourceRef()).isEqualTo("session:77:snapshot");
        assertThat(mapper.inserted.getMetricDetails()).contains("\"messageCount\":6");
        assertThat(mapper.inserted.getMetricDetails()).contains("\"progressPercent\":40");
        assertThat(mapper.inserted.isTestData()).isTrue();
    }

    @Test
    void createSessionSnapshotRejectsMissingReadingSessionBeforeInsert() {
        FakeMetricMapper mapper = new FakeMetricMapper();
        mapper.source = null;
        MetricBusiness business = new MetricBusiness(mapper, new ObjectMapper());

        assertThatThrownBy(() -> business.createSessionSnapshot(404L))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(mapper.inserted).isNull();
    }

    @Test
    void createSessionSnapshotRejectsZeroRowInsert() {
        FakeMetricMapper mapper = new FakeMetricMapper();
        mapper.insertRows = 0;
        MetricBusiness business = new MetricBusiness(mapper, new ObjectMapper());

        assertThatThrownBy(() -> business.createSessionSnapshot(77L))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies((exception) -> {
                ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                assertThat(responseStatusException.getReason()).isEqualTo("Metric snapshot could not be saved");
            });
    }

    private static class FakeMetricMapper implements MetricMapper {
        private MetricRecord inserted;
        private int insertRows = 1;
        private SessionMetricSourceRecord source = SessionMetricSourceRecord.builder()
            .userId(1L)
            .bookId(7L)
            .sessionId(77L)
            .startPage(1)
            .currentPage(48)
            .targetPage(120)
            .pagesReadEstimate(48)
            .windowCount(2)
            .questionCount(6)
            .answeredQuestionCount(2)
            .highlightCount(1)
            .messageCount(6)
            .personaCount(1)
            .build();

        @Override
        public SessionMetricSourceRecord findSessionSource(Long sessionId, Long userId) {
            if (source == null) {
                return null;
            }
            source.setUserId(userId);
            source.setSessionId(sessionId);
            return source;
        }

        @Override
        public int insert(MetricRecord record) {
            this.inserted = record;
            record.setId(700L);
            return insertRows;
        }
    }
}
