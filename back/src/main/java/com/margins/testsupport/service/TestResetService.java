package com.margins.testsupport.service;

import com.margins.testsupport.business.TestResetBusiness;
import com.margins.testsupport.dto.ResetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TestResetService {

    private final TestResetBusiness testResetBusiness;

    public ResetResponse reset() {
        return testResetBusiness.reset();
    }
}
