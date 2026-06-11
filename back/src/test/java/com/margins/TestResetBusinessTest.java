package com.margins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.margins.testsupport.business.TestResetBusiness;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

class TestResetBusinessTest {

    @Test
    void resetAllowedInTestProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        TestResetBusiness business = new TestResetBusiness(environment);

        assertThat(business.reset().isReset()).isTrue();
    }

    @Test
    void resetRejectedOutsideLocalOrTestProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        TestResetBusiness business = new TestResetBusiness(environment);

        assertThatThrownBy(business::reset)
            .isInstanceOf(ResponseStatusException.class);
    }
}
