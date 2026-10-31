# circuitbreaker

A circuit breaker that stops calling a failing dependency and probes for recovery.

## Usage

```java
CircuitBreaker breaker = new CircuitBreaker(5, Duration.ofSeconds(30));
String result = breaker.call(() -> client.fetch());
```

## Test

```bash
mvn test
```

## License

MIT
