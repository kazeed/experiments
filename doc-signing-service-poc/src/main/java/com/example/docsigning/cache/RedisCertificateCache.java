package com.example.docsigning.cache;

import com.example.docsigning.model.CertificateBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisCertificateCache implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCertificateCache.class);

    private final JedisPool jedisPool;
    private final String cacheKey;
    private final int ttlSeconds;

    public RedisCertificateCache(String redisEndpoint, int redisPort, String cacheKey, int ttlSeconds) {
        this.jedisPool = new JedisPool(redisEndpoint, redisPort);
        this.cacheKey = cacheKey;
        this.ttlSeconds = ttlSeconds;
    }

    public CertificateBundle get() {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(cacheKey);
            if (value == null) {
                LOGGER.info("Redis cache miss for key {}", cacheKey);
                return null;
            }
            LOGGER.info("Redis cache hit for key {}", cacheKey);
            return CertificateBundle.deserialize(value);
        }
    }

    public void put(CertificateBundle bundle) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(cacheKey, ttlSeconds, bundle.serialize());
            LOGGER.info("Stored certificate bundle in Redis under key {} with TTL {} seconds", cacheKey, ttlSeconds);
        }
    }

    @Override
    public void close() {
        jedisPool.close();
    }
}
