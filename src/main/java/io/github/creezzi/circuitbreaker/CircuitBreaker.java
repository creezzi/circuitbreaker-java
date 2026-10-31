package io.github.creezzi.circuitbreaker;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.LongSupplier;

/** CLOSED -> OPEN after N consecutive failures -> HALF_OPEN after the cool-down -> CLOSED on success. */
public final class CircuitBreaker {
    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final long openNanos;
    private final LongSupplier nanoClock;
    private State state = State.CLOSED;
    private int failures;
    private long openedAt;

    public CircuitBreaker(int failureThreshold, Duration openFor) {
        this(failureThreshold, openFor, System::nanoTime);
    }

    public CircuitBreaker(int failureThreshold, Duration openFor, LongSupplier nanoClock) {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("failureThreshold must be >= 1");
        }
        this.failureThreshold = failureThreshold;
        this.openNanos = openFor.toNanos();
        this.nanoClock = nanoClock;
    }

    public synchronized State state() {
        if (state == State.OPEN && nanoClock.getAsLong() - openedAt >= openNanos) {
            state = State.HALF_OPEN;
        }
        return state;
    }

    public <T> T call(Callable<T> action) throws Exception {
        if (state() == State.OPEN) {
            throw new CircuitOpenException();
        }
        T result;
        try {
            result = action.call();
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
        recordSuccess();
        return result;
    }

    private synchronized void recordSuccess() {
        failures = 0;
        state = State.CLOSED;
    }

    private synchronized void recordFailure() {
        failures++;
        if (state == State.HALF_OPEN || failures >= failureThreshold) {
            state = State.OPEN;
            openedAt = nanoClock.getAsLong();
        }
    }

    /** Thrown without invoking the action while the circuit is open. */
    public static final class CircuitOpenException extends RuntimeException {
        public CircuitOpenException() {
            super("circuit is open");
        }
    }
}
