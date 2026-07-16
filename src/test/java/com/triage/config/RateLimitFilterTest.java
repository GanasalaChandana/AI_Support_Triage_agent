package com.triage.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter();

    @Test
    void allowsRequestsWithinLimit() throws ServletException, IOException {
        String ip = "10.0.0.1";
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(ticketPostRequest(ip), response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void blocksRequestsOverLimit() throws ServletException, IOException {
        String ip = "10.0.0.2";
        for (int i = 0; i < 5; i++) {
            filter.doFilter(ticketPostRequest(ip), new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(ticketPostRequest(ip), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void tracksLimitsPerIpIndependently() throws ServletException, IOException {
        String ipA = "10.0.0.3";
        String ipB = "10.0.0.4";
        for (int i = 0; i < 5; i++) {
            filter.doFilter(ticketPostRequest(ipA), new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(ticketPostRequest(ipB), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doesNotLimitOtherEndpoints() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tickets");
        request.setRemoteAddr("10.0.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < 10; i++) {
            filter.doFilter(request, response, new MockFilterChain());
        }

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest ticketPostRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/tickets");
        request.setRemoteAddr(ip);
        return request;
    }
}
