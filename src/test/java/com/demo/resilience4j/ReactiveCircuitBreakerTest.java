package com.demo.resilience4j;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

public class ReactiveCircuitBreakerTest extends AbstractCircuitBreakerTest {

	@Test
	public void shouldOpenBackendACircuitBreaker() {
		// When
		IntStream.rangeClosed(1, 2).forEach((count) -> produceFailure(BACKEND_A));

		// Then
		checkHealthStatus(BACKEND_A, State.OPEN);
	}

	@Test
	public void shouldOpenBackendBCircuitBreaker() {
		// When
		IntStream.rangeClosed(1, 4).forEach((count) -> produceFailure(BACKEND_B));

		// Then
		checkHealthStatus(BACKEND_B, State.OPEN);
	}

	@Test
	public void shouldCloseBackendACircuitBreaker() {
		transitionToOpenState(BACKEND_A);
		circuitBreakerRegistry.circuitBreaker(BACKEND_A).transitionToHalfOpenState();

		// When
		IntStream.rangeClosed(1, 3).forEach((count) -> produceSuccess(BACKEND_A));

		// Then
		checkHealthStatus(BACKEND_A, State.CLOSED);
	}

	@Test
	public void shouldCloseBackendBCircuitBreaker() {
		transitionToOpenState(BACKEND_B);
		circuitBreakerRegistry.circuitBreaker(BACKEND_B).transitionToHalfOpenState();

		// When
		IntStream.rangeClosed(1, 3).forEach((count) -> produceSuccess(BACKEND_B));

		// Then
		checkHealthStatus(BACKEND_B, State.CLOSED);
	}

	private void produceFailure(String backend) {
		webClient.get().uri("/" + backend + "/monoFailure").exchange().expectStatus().is5xxServerError();
	}

	private void produceSuccess(String backend) {
		webClient.get().uri("/" + backend + "/monoSuccess").exchange().expectStatus().isOk();
	}

}