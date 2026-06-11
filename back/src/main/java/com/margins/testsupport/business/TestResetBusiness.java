package com.margins.testsupport.business;

import com.margins.testsupport.dto.ResetResponse;
import java.util.Arrays;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class TestResetBusiness {

    private final Environment environment;
    private final TestDataResetExecutor testDataResetExecutor;

    public TestResetBusiness(Environment environment, TestDataResetExecutor testDataResetExecutor) {
        this.environment = environment;
        this.testDataResetExecutor = testDataResetExecutor;
    }

    public ResetResponse reset() {
        if (!isResetAllowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "reset is only available in local/test profiles");
        }

        testDataResetExecutor.resetTestData();

        return ResetResponse.builder()
            .reset(true)
            .mode("jdbc-seed-reset")
            .build();
    }

    public boolean isResetAllowed() {
        return Arrays.stream(environment.getActiveProfiles())
            .anyMatch(profile -> profile.equals("local") || profile.equals("test"));
    }
}
