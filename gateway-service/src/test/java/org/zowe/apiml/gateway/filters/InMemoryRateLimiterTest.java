/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryRateLimiterTest {

    private InMemoryRateLimiter rateLimiter;
    String userId = "testUser";
    String routeId = "testRoute";

    @BeforeEach
    public void setUp() {
        rateLimiter = new InMemoryRateLimiter();
        rateLimiter.capacity = 3;
        rateLimiter.tokens = 3;
        rateLimiter.refillDuration = 1;
    }

    @Test
    public void isAllowed_shouldReturnTrue_whenTokensAvailable() {
        rateLimiter.capacity = 1;

        Mono<RateLimiter.Response> response = rateLimiter.isAllowed(routeId, userId);

        assertTrue(Objects.requireNonNull(response.block()).isAllowed());
    }

    @Test
    public void isAllowed_shouldReturnFalse_whenTokensExhausted() {
        for (int i = 0; i < rateLimiter.capacity; i++) {
            Mono<InMemoryRateLimiter.Response> responseMono = rateLimiter.isAllowed(routeId, userId);
            InMemoryRateLimiter.Response response = responseMono.block();
            assertTrue(response.isAllowed(), "Request " + (i + 1) + " should be allowed");
        }
        // Last request should be denied
        Mono<InMemoryRateLimiter.Response> responseMono = rateLimiter.isAllowed(routeId, userId);
        InMemoryRateLimiter.Response response = responseMono.block();
        assertFalse(response.isAllowed(), "Fourth request should not be allowed");
    }


    @Test
    public void testDifferentClientIdHasSeparateBucket() {
        String clientId1 = "client1";
        String clientId2 = "client2";

        // Allow first three requests for client1
        for (int i = 0; i < rateLimiter.capacity; i++) {
            Mono<InMemoryRateLimiter.Response> responseMono = rateLimiter.isAllowed(routeId, clientId1);
            InMemoryRateLimiter.Response response = responseMono.block();
            assertTrue(response.isAllowed(), "Request " + (i + 1) + " for client1 should be allowed");
        }

        // Fourth request for client1 should be denied
        Mono<InMemoryRateLimiter.Response> responseMono = rateLimiter.isAllowed(routeId, clientId1);
        InMemoryRateLimiter.Response response = responseMono.block();
        assertFalse(response.isAllowed(), "Fourth request for client1 should not be allowed");

        // Allow first request for client2, it should be allowed since it's a separate bucket
        Mono<InMemoryRateLimiter.Response> responseMono2 = rateLimiter.isAllowed(routeId, clientId2);
        InMemoryRateLimiter.Response response2 = responseMono2.block();
        assertTrue(response2.isAllowed(), "First request for client2 should be allowed");
    }

    @Test
    public void testNewConfig() {
        InMemoryRateLimiter.Config config = rateLimiter.newConfig();

        assertNotNull(config, "Config should not be null");
        assertEquals(rateLimiter.capacity, config.getCapacity(), "Config capacity should match the rate limiter capacity");
        assertEquals(rateLimiter.tokens, config.getTokens(), "Config tokens should match the rate limiter tokens");
        assertEquals(rateLimiter.refillDuration, config.getRefillDuration(), "Config refill duration should match the rate limiter refill duration");
    }

    @Test
    public void setNonNullParametersTest() {
        Integer newCapacity = 20;
        Integer newTokens = 20;
        Integer newRefillDuration = 2;
        rateLimiter.setParameters(newCapacity, newTokens, newRefillDuration);
        assertEquals(newCapacity, rateLimiter.capacity);
        assertEquals(newTokens, rateLimiter.tokens);
        assertEquals(newRefillDuration, rateLimiter.refillDuration);
    }

    @Test
    public void setParametersWithNullValuesTest() {
        Integer newCapacity = 30;
        rateLimiter.setParameters(newCapacity, 0, 0);
        assertEquals(newCapacity, rateLimiter.capacity);

    }
}
