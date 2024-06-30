package com.demo.external;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExternalController {
	
	// https://www.baeldung.com/spring-boot-resilience4j
	
	private final ExternalService externalService;
	
	@ExceptionHandler(CallNotPermittedException.class)
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	public void handleCallNotPermittedException(CallNotPermittedException e) {
		log.error(e.getMessage(), e);
	}

	@ExceptionHandler(TimeoutException.class)
	@ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
	public void handleTimeoutException(TimeoutException e) {
		log.error(e.getMessage(), e);
	}

	@ExceptionHandler(BulkheadFullException.class)
	@ResponseStatus(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED)
	public void handleBulkheadFullException(BulkheadFullException e) {
		log.error(e.getMessage(), e);
	}
	
	@ExceptionHandler(RequestNotPermitted.class)
	@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
	public void handleRequestNotPermitted(RequestNotPermitted e) {
		log.error(e.getMessage(), e);
	}
	
	@GetMapping("/circuit-breaker")
	@CircuitBreaker(name = "CircuitBreakerService") // 회로 차단기
	public String circuitBreakerApi() {
	    return externalService.callApi();
	}
	
	@GetMapping("/retry")
	@Retry(name = "retryApi", fallbackMethod = "fallbackAfterRetry")
	public String retryApi() {
	    return externalService.callApi();
	}
	
	public String fallbackAfterRetry(Exception ex) {
	    return "all retries have exhausted";
	}
	
	@GetMapping("/time-limiter")
	@TimeLimiter(name = "timeLimiterApi")
	public CompletableFuture<String> timeLimiterApi() {
	    return CompletableFuture.supplyAsync(externalService::callApiWithDelay);
	}
	
	@GetMapping("/bulkhead")
	@Bulkhead(name="bulkheadApi") // 외부 서비스에 대한 최대 동시 호출 수를 제한
	public String bulkheadApi() {
	    return externalService.callApi();
	}

	@GetMapping("/rate-limiter")
	@RateLimiter(name = "rateLimiterApi") // 리소스에 대한 요청 속도를 제한
	public String rateLimitApi() {
	    return externalService.callApi();
	}
	
}
