package com.margins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.margins.testsupport.business.TestResetBusiness;
import com.margins.testsupport.business.TestDataResetExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

class TestResetBusinessTest {

    @Test
    void resetAllowedInTestProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        FakeResetExecutor executor = new FakeResetExecutor();

        TestResetBusiness business = new TestResetBusiness(environment, executor);

        assertThat(business.reset().isReset()).isTrue();
        assertThat(business.reset().getMode()).isEqualTo("jdbc-seed-reset");
        assertThat(executor.calls).isEqualTo(2);
    }

    @Test
    void resetRejectedOutsideLocalOrTestProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        FakeResetExecutor executor = new FakeResetExecutor();

        TestResetBusiness business = new TestResetBusiness(environment, executor);

        assertThatThrownBy(business::reset)
            .isInstanceOf(ResponseStatusException.class);
        assertThat(executor.calls).isZero();
    }

    private static class FakeResetExecutor implements TestDataResetExecutor {
        private int calls;

        @Override
        public void resetTestData() {
            calls++;
        }
    }
}
