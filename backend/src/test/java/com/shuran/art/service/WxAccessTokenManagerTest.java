package com.shuran.art.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PST Domain - WxAccessTokenManager Unit Tests
 *
 * Tests the access_token caching logic: cache hit (valid token),
 * cache miss/expired (triggers refresh), and error handling.
 *
 * Note: These tests set internal fields via reflection to test the
 * caching logic without calling the real WeChat API.
 * The refreshAccessToken() method itself creates a new RestTemplate
 * internally, so we test the caching layer around it.
 *
 * Test skeleton source: PST-test-detail.md Section 2.1 ~ 2.3
 * L0 trace: Decision 5
 */
class WxAccessTokenManagerTest {

    private WxAccessTokenManager tokenManager;

    @BeforeEach
    void setUp() throws Exception {
        tokenManager = new WxAccessTokenManager();

        // Set appid/secret via reflection since @Value is not processed in unit tests
        Field appidField = WxAccessTokenManager.class.getDeclaredField("appid");
        appidField.setAccessible(true);
        appidField.set(tokenManager, "test_appid");

        Field secretField = WxAccessTokenManager.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(tokenManager, "test_secret");
    }

    /**
     * Helper: set cachedToken via reflection.
     */
    private void setCachedToken(String token) throws Exception {
        Field cachedTokenField = WxAccessTokenManager.class.getDeclaredField("cachedToken");
        cachedTokenField.setAccessible(true);
        cachedTokenField.set(tokenManager, token);
    }

    /**
     * Helper: set expireTime via reflection.
     */
    private void setExpireTime(long expireTime) throws Exception {
        Field expireTimeField = WxAccessTokenManager.class.getDeclaredField("expireTime");
        expireTimeField.setAccessible(true);
        expireTimeField.setLong(tokenManager, expireTime);
    }

    // ========================================================================
    // TP-PST-001: WxAccessTokenManager.getAccessToken cache hit
    // ========================================================================
    @Nested
    @DisplayName("TP-PST-001: getAccessToken - cache valid")
    class CacheHitTests {

        /**
         * TC: TP-PST-001
         * L0 trace: Decision 5
         * L2 trace: PST-test-detail.md Section 2.1
         *
         * Verifies: when cachedToken is valid and not expired,
         * getAccessToken returns it directly without calling WeChat API.
         */
        @Test
        @DisplayName("getAccessToken - cached token valid - returns cached token directly")
        void getAccessToken_cachedTokenValid_returnsCachedToken() throws Exception {
            // Arrange: set a valid cached token with future expiry
            setCachedToken("valid_token_123");
            setExpireTime(System.currentTimeMillis() + 3600000); // 1 hour from now

            // Act
            String result = tokenManager.getAccessToken();

            // Assert
            assertThat(result).isEqualTo("valid_token_123");
        }
    }

    // ========================================================================
    // TP-PST-002: WxAccessTokenManager.getAccessToken cache expired
    // ========================================================================
    @Nested
    @DisplayName("TP-PST-002: getAccessToken - cache expired")
    class CacheExpiredTests {

        /**
         * TC: TP-PST-002
         * L0 trace: Decision 5
         * L2 trace: PST-test-detail.md Section 2.2
         *
         * Verifies: when token is expired, getAccessToken calls
         * refreshAccessToken. Since refreshAccessToken calls the real
         * WeChat API (which is unavailable in unit tests), this test
         * verifies the expired path is entered by expecting a RuntimeException.
         */
        @Test
        @DisplayName("getAccessToken - cached token expired - attempts refresh (throws in unit test env)")
        void getAccessToken_cachedTokenExpired_attemptsRefresh() throws Exception {
            // Arrange: set an expired token
            setCachedToken("expired_token");
            setExpireTime(System.currentTimeMillis() - 1000); // already expired

            // Act & Assert: refresh will fail since no real WeChat API
            // This verifies the expired-token code path is reached
            assertThatThrownBy(() -> tokenManager.getAccessToken())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("access_token 获取失败");
        }
    }

    // ========================================================================
    // TP-PST-003: WxAccessTokenManager.refreshAccessToken API failure
    // ========================================================================
    @Nested
    @DisplayName("TP-PST-003: refreshAccessToken - WeChat API failure")
    class RefreshFailureTests {

        /**
         * TC: TP-PST-003
         * L0 trace: BS-009
         * L2 trace: PST-test-detail.md Section 2.3
         *
         * Verifies: when cachedToken is null and WeChat API is unreachable,
         * refreshAccessToken throws RuntimeException with proper message.
         */
        @Test
        @DisplayName("refreshAccessToken - no cached token, API unavailable - throws RuntimeException")
        void refreshAccessToken_apiUnavailable_throwsException() throws Exception {
            // Arrange: no cached token, so getAccessToken must refresh
            setCachedToken(null);
            setExpireTime(0);

            // Act & Assert
            assertThatThrownBy(() -> tokenManager.getAccessToken())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("access_token 获取失败");
        }

        /**
         * TC: TP-PST-003 (direct refreshAccessToken call)
         * L0 trace: BS-009
         * L2 trace: PST-test-detail.md Section 2.3
         */
        @Test
        @DisplayName("refreshAccessToken - direct call with invalid config - throws RuntimeException")
        void refreshAccessToken_directCall_throwsException() {
            // Act & Assert
            assertThatThrownBy(() -> tokenManager.refreshAccessToken())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("access_token 获取失败");
        }
    }

    // ========================================================================
    // Thread safety test (supplementary)
    // ========================================================================
    @Nested
    @DisplayName("TP-PST-001 (supplementary): thread safety - double-check locking")
    class ThreadSafetyTests {

        /**
         * TC: TP-PST-001 (supplementary)
         * L0 trace: Decision 5
         * L2 trace: PST-test-detail.md Section 2.1
         *
         * Verifies: concurrent access to a valid cached token all return
         * the same value without error.
         */
        @Test
        @DisplayName("getAccessToken - concurrent reads with valid cache - all return same token")
        void getAccessToken_concurrentReads_allReturnSameToken() throws Exception {
            // Arrange
            setCachedToken("concurrent_valid_token");
            setExpireTime(System.currentTimeMillis() + 3600000);

            // Act: simulate concurrent access
            Thread[] threads = new Thread[10];
            String[] results = new String[10];
            for (int i = 0; i < 10; i++) {
                final int idx = i;
                threads[i] = new Thread(() -> {
                    results[idx] = tokenManager.getAccessToken();
                });
                threads[i].start();
            }
            for (Thread t : threads) {
                t.join(5000);
            }

            // Assert: all threads got the same cached token
            for (String result : results) {
                assertThat(result).isEqualTo("concurrent_valid_token");
            }
        }
    }
}
