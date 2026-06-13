package com.margins.metric.service;

import com.margins.metric.business.MetricBusiness;
import com.margins.metric.dto.MetricSnapshotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MetricService {

    private final MetricBusiness metricBusiness;

    @Transactional
    public MetricSnapshotResponse createSessionSnapshot(Long sessionId) {
        return metricBusiness.createSessionSnapshot(sessionId);
    }
}
