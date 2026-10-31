package io.github.creezzi.circuitbreaker;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class CircuitBreakerTest {
    private final AtomicLong now = new AtomicLong();
    private final AtomicInteger calls = new AtomicInteger();

    private CircuitBreaker breaker() {
        return new CircuitBreaker(2, Duration.ofSeconds(10), now::get);
    }

    private void fail(CircuitBreaker b) {
        assertThrows(IllegalStateException.class, () -> b.call(() -> {
            calls.incrementAndGet();
            throw new IllegalStateException();
        }));
    }

    @Test
    void opensAfterThresholdAndShortCircuits() throws Exception {
        CircuitBreaker b = breaker();
        fail(b);
        assertEquals(CircuitBreaker.State.CLOSED, b.state());
        fail(b);
        assertEquals(CircuitBreaker.State.OPEN, b.state());
        assertThrows(CircuitBreaker.CircuitOpenException.class, () -> b.call(() -> "x"));
        assertEquals(2, calls.get());
    }

    @Test
    void successResetsTheFailureCount() throws Exception {
        CircuitBreaker b = breaker();
        fail(b);
        assertEquals("ok", b.call(() -> "ok"));
        fail(b);
        assertEquals(CircuitBreaker.State.CLOSED, b.state());
    }

    @Test
    void halfOpenProbeClosesOnSuccess() throws Exception {
        CircuitBreaker b = breaker();
        fail(b);
        fail(b);
        now.addAndGet(Duration.ofSeconds(10).toNanos());
        assertEquals(CircuitBreaker.State.HALF_OPEN, b.state());
        assertEquals("ok", b.call(() -> "ok"));
        assertEquals(CircuitBreaker.State.CLOSED, b.state());
    }

    @Test
    void halfOpenProbeReopensOnFailure() {
        CircuitBreaker b = breaker();
        fail(b);
        fail(b);
        now.addAndGet(Duration.ofSeconds(10).toNanos());
        fail(b);
        assertEquals(CircuitBreaker.State.OPEN, b.state());
    }

    @Test
    void rejectsBadThreshold() {
        assertThrows(IllegalArgumentException.class, () -> new CircuitBreaker(0, Duration.ofSeconds(1)));
    }
}
