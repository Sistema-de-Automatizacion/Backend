package com.automatization.comunications.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class ApiKeyFilterTest {

    private static final String SECRET = "test-key-1234567890";

    @Test
    void optionsRequestsBypassTheFilter() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(SECRET);
        MockHttpServletRequest req = new MockHttpServletRequest("OPTIONS", "/contracts/next-to-pay");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(chainReached(chain)).isTrue();
        assertThat(res.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void publicHealthPathBypassesTheFilter() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(SECRET);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(chainReached(chain)).isTrue();
    }

    @Test
    void publicInfoPathBypassesTheFilter() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(SECRET);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/info");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(chainReached(chain)).isTrue();
    }

    @Test
    void protectedPathWithoutHeaderReturns401() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(SECRET);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/contracts/next-to-pay");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(res.getContentAsString())
                .contains("\"status\":401")
                .contains("Invalid or missing X-API-Key header");
        assertThat(chainReached(chain)).isFalse();
    }

    @Test
    void protectedPathWithWrongHeaderReturns401() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(SECRET);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/contracts/paid-this-week");
        req.addHeader("X-API-Key", "wrong-key");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(chainReached(chain)).isFalse();
    }

    @Test
    void protectedPathWithCorrectHeaderPassesToChain() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(SECRET);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/contracts/next-to-pay");
        req.addHeader("X-API-Key", SECRET);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        assertThat(chainReached(chain)).isTrue();
    }

    @Test
    void whenServerKeyIsBlankEveryProtectedRequestIs401() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter("");
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/contracts/next-to-pay");
        req.addHeader("X-API-Key", "anything");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(res.getContentAsString()).contains("Server API key not configured");
    }

    @Test
    void whenServerKeyIsNullEveryProtectedRequestIs401() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(null);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/contracts/next-to-pay");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private static boolean chainReached(MockFilterChain chain) {
        // MockFilterChain lleva nota de la request que pasó — si la tiene, fue invocado.
        HttpServletRequest last = (HttpServletRequest) chain.getRequest();
        return last != null;
    }
}
