
package com.labresource.platform;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisTestRunner implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;

    public RedisTestRunner(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(String... args) {

        System.out.println("RedisTestRunner started!");

        try {
            String key = "redis-test";

            // Save value with 5-minute expiration
            redisTemplate.opsForValue().set(
                    key,
                    "Connected",
                    Duration.ofMinutes(5)
            );

            // Read saved value
            String result = redisTemplate.opsForValue().get(key);

            // Get remaining TTL
            Long ttlSeconds = redisTemplate.getExpire(key);

            System.out.println("Redis Write/Read Result: " + result);
            System.out.println("Redis TTL: " + ttlSeconds + " seconds");

            if ("Connected".equals(result)
                    && ttlSeconds != null
                    && ttlSeconds > 0) {

                System.out.println("Redis cache test successful.");

            } else {
                System.out.println("Redis cache test failed.");
            }

        } catch (Exception e) {
            System.out.println(
                    "Redis cache test failed: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }
}
