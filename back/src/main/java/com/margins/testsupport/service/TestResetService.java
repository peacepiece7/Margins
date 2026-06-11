package com.margins.testsupport.service;

import com.margins.testsupport.business.TestResetBusiness;
import com.margins.testsupport.dto.ResetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TestResetService {

    private final TestResetBusiness testResetBusiness;

    @Transactional
    public ResetResponse reset() {
        return testResetBusiness.reset();
    }
}
