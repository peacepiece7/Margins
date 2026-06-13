package com.margins.metric.controller;

import com.margins.common.dto.ApiResponse;
import com.margins.metric.dto.MetricSnapshotResponse;
import com.margins.metric.service.MetricService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reading-sessions/{id}/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final MetricService metricService;

    @PostMapping("/snapshot")
    public ApiResponse<MetricSnapshotResponse> createSnapshot(@PathVariable("id") Long sessionId) {
        return ApiResponse.ok(metricService.createSessionSnapshot(sessionId));
    }
}
