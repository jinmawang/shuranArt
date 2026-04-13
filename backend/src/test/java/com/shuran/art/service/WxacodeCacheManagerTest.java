package com.shuran.art.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PST Domain - WxacodeCacheManager Unit Tests
 *
 * Tests the in-memory LRU cache for wxacode Base64 data:
 * cache hit, TTL expiry, LRU eviction at 500 cap.
 *
 * Test skeleton source: PST-test-detail.md Section 2.4 ~ 2.6
 * L0 trace: REQ-PST-011, REQ-PST-023, BS-018
 */
class WxacodeCacheManagerTest {

    private WxacodeCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new WxacodeCacheManager();
    }

    // ========================================================================
    // TP-PST-004: WxacodeCacheManager.get cache hit
    // ========================================================================
    @Nested
    @DisplayName("TP-PST-004: get - cache hit")
    class CacheHitTests {

        /**
         * TC: TP-PST-004
         * L0 trace: REQ-PST-011
         * L2 trace: PST-test-detail.md Section 2.4
         *
         * Verifies: put a value then get returns it immediately.
         */
        @Test
        @DisplayName("get - cache exists and not expired - returns cached value")
        void get_cacheExistsAndValid_returnsCachedValue() {
            // Arrange
            cacheManager.put("5_1", "base64data_abc");

            // Act
            String result = cacheManager.get("5_1");

            // Assert
            assertThat(result).isEqualTo("base64data_abc");
        }

        /**
         * TC: TP-PST-004 (supplementary - cache miss for unknown key)
         * L0 trace: REQ-PST-011
         * L2 trace: PST-test-detail.md Section 2.4
         */
        @Test
        @DisplayName("get - key not in cache - returns null")
        void get_keyNotInCache_returnsNull() {
            // Act
            String result = cacheManager.get("nonexistent_key");

            // Assert
            assertThat(result).isNull();
        }
    }

    // ========================================================================
    // TP-PST-005: WxacodeCacheManager.get cache expired
    // ========================================================================
    @Nested
    @DisplayName("TP-PST-005: get - cache expired")
    class CacheExpiredTests {

        /**
         * TC: TP-PST-005
         * L0 trace: REQ-PST-011
         * L2 trace: PST-test-detail.md Section 2.5
         *
         * Verifies: when cache entry TTL has expired, get returns null.
         */
        @Test
        @DisplayName("get - cache exists but expired - returns null")
        void get_cacheExpired_returnsNull() throws Exception {
            // Arrange
            cacheManager.put("5_1", "base64data_abc");

            // Manually set the expire time to the past via reflection
            Field cacheField = WxacodeCacheManager.class.getDeclaredField("cache");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> cache = (Map<String, Object>) cacheField.get(cacheManager);

            // Access the CacheEntry and modify its expireTime
            Object entry = cache.get("5_1");
            Field expireTimeField = entry.getClass().getDeclaredField("expireTime");
            expireTimeField.setAccessible(true);
            expireTimeField.setLong(entry, System.currentTimeMillis() - 1000);

            // Act
            String result = cacheManager.get("5_1");

            // Assert
            assertThat(result).isNull();
        }
    }

    // ========================================================================
    // TP-PST-006: WxacodeCacheManager LRU eviction at 500
    // ========================================================================
    @Nested
    @DisplayName("TP-PST-006: put - LRU eviction at 500 capacity")
    class LruEvictionTests {

        /**
         * TC: TP-PST-006
         * L0 trace: REQ-PST-023, BS-018
         * L2 trace: PST-test-detail.md Section 2.6
         *
         * Verifies: after filling 500 entries and accessing user_1 to make it
         * recently used, adding entry 501 evicts user_2 (least recently used)
         * but keeps user_1 and user_501.
         */
        @Test
        @DisplayName("put - exceeds 500 cap - LRU eviction removes least recently used entry")
        void put_exceeds500_lruEvictsLeastRecentlyUsed() {
            // Arrange: fill 500 entries
            for (int i = 1; i <= 500; i++) {
                cacheManager.put("user_" + i, "data_" + i);
            }

            // Access user_1 to make it recently used
            cacheManager.get("user_1");

            // Act: add entry 501 (triggers eviction)
            cacheManager.put("user_501", "data_501");

            // Assert
            assertThat(cacheManager.get("user_1")).isNotNull();     // recently accessed, not evicted
            assertThat(cacheManager.get("user_2")).isNull();         // least recently used, evicted
            assertThat(cacheManager.get("user_501")).isNotNull();    // newly added, exists
        }

        /**
         * TC: TP-PST-006 (supplementary - capacity check)
         * L0 trace: REQ-PST-023
         * L2 trace: PST-test-detail.md Section 2.6
         */
        @Test
        @DisplayName("put - exactly 500 entries - no eviction yet")
        void put_exactly500_noEviction() {
            // Arrange & Act: fill exactly 500
            for (int i = 1; i <= 500; i++) {
                cacheManager.put("user_" + i, "data_" + i);
            }

            // Assert: all 500 entries are present
            assertThat(cacheManager.get("user_1")).isEqualTo("data_1");
            assertThat(cacheManager.get("user_500")).isEqualTo("data_500");
        }
    }
}
