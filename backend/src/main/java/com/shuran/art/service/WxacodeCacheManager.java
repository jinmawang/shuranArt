package com.shuran.art.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 小程序码 Base64 数据内存缓存管理器
 *
 * L2 设计: PST-data-detail.md 3.x WxacodeCacheManager
 * 需求来源: REQ-PST-011（24h 缓存）、REQ-PST-023（LRU 500 条）
 *
 * 实现: LinkedHashMap(accessOrder=true) + Collections.synchronizedMap
 */
@Slf4j
@Component
public class WxacodeCacheManager {

    // L2 设计: MAX_CAPACITY = 500（REQ-PST-023）
    private static final int MAX_CAPACITY = 500;

    // L2 设计: TTL_MILLIS = 86400000（24 小时，REQ-PST-011）
    private static final long TTL_MILLIS = 86400000L;

    // L2 设计: CacheEntry 内部类
    private static class CacheEntry {
        String value;      // Base64 编码的小程序码图片
        long expireTime;   // 过期时间戳（毫秒）

        CacheEntry(String value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }
    }

    // L2 设计 3.3: LinkedHashMap(accessOrder=true) + removeEldestEntry 实现 LRU
    private final Map<String, CacheEntry> cache;

    public WxacodeCacheManager() {
        // L2 设计: 使用 LinkedHashMap 的 accessOrder=true 模式实现 LRU
        // removeEldestEntry 在超过容量时自动淘汰最久未使用的条目（BS-018）
        LinkedHashMap<String, CacheEntry> lruMap = new LinkedHashMap<String, CacheEntry>(
                MAX_CAPACITY, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > MAX_CAPACITY;
            }
        };
        // L2 设计: Collections.synchronizedMap 包装，保证线程安全
        this.cache = Collections.synchronizedMap(lruMap);
    }

    /**
     * 获取缓存的小程序码 Base64 数据
     * L2 设计: PST-data-detail.md 3.4 get()
     *
     * key 格式: "{userId}_{activityId}"
     * 返回 null 表示缓存未命中或已过期
     */
    public String get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            log.info("wxacode 缓存未命中: key={}", key);
            return null;
        }

        // L2: 检查 TTL 过期
        if (System.currentTimeMillis() > entry.expireTime) {
            // L2: 已过期，移除并返回 null
            cache.remove(key);
            log.info("wxacode 缓存已过期: key={}", key);
            return null;
        }

        log.info("wxacode 缓存命中: key={}", key);
        return entry.value;
    }

    /**
     * 存入小程序码 Base64 数据，TTL 24 小时
     * L2 设计: PST-data-detail.md 3.4 put()
     *
     * 超过 MAX_CAPACITY 时 LinkedHashMap 自动淘汰最久未使用条目（BS-018）
     */
    public void put(String key, String value) {
        CacheEntry entry = new CacheEntry(value, System.currentTimeMillis() + TTL_MILLIS);
        cache.put(key, entry);
        log.info("wxacode 缓存写入: key={}, cacheSize={}", key, cache.size());
    }
}
