package com.demo.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ResilientAppControllerUnitTest {

	@RegisterExtension
	static WireMockExtension EXTERNAL_SERVICE = WireMockExtension.newInstance()
			.options(WireMockConfiguration.wireMockConfig().port(9090)).build();

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void testCircuitBreaker() {
		EXTERNAL_SERVICE.stubFor(WireMock.get("/api/external").willReturn(WireMock.serverError()));

		IntStream.rangeClosed(1, 5).forEach(i -> {
			ResponseEntity<String> response = restTemplate.getForEntity("/api/circuit-breaker", String.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		});

		IntStream.rangeClosed(1, 3).forEach(i -> {
//		IntStream.rangeClosed(1, 5).forEach(i -> {
			ResponseEntity<String> response = restTemplate.getForEntity("/api/circuit-breaker", String.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		});

		EXTERNAL_SERVICE.verify(5, WireMock.getRequestedFor(WireMock.urlEqualTo("/api/external")));
	}

	@Test
	public void testRetryAndFallback() {
//		EXTERNAL_SERVICE.stubFor(WireMock.get("/api/external").willReturn(WireMock.ok()));
//		restTemplate.getForEntity("/api/retry", String.class);
//		EXTERNAL_SERVICE.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/api/external")));
//
//		EXTERNAL_SERVICE.resetRequests();

		EXTERNAL_SERVICE.stubFor(WireMock.get("/api/external").willReturn(WireMock.serverError()));
		ResponseEntity<String> response2 = restTemplate.getForEntity("/api/retry", String.class);
		assertEquals(response2.getBody(), "all retries have exhausted");
		EXTERNAL_SERVICE.verify(3, WireMock.getRequestedFor(WireMock.urlEqualTo("/api/external")));
	}

	@Test
	public void testTimeLimiter() {
		EXTERNAL_SERVICE.stubFor(WireMock.get("/api/external").willReturn(WireMock.ok()));
		ResponseEntity<String> response = restTemplate.getForEntity("/api/time-limiter", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.REQUEST_TIMEOUT);
		EXTERNAL_SERVICE.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/api/external")));
	}

	@Test
	void testBulkhead() throws Exception {
	  EXTERNAL_SERVICE.stubFor(WireMock.get("/api/external")
	      .willReturn(WireMock.ok()));
	  Map<Integer, Integer> responseStatusCount = new ConcurrentHashMap<>();
	  ExecutorService executorService = Executors.newFixedThreadPool(5);
	  CountDownLatch latch = new CountDownLatch(5);

	  IntStream.rangeClosed(1, 5)
	      .forEach(i -> executorService.execute(() -> { // parallel request
	          ResponseEntity<String> response = restTemplate.getForEntity("/api/bulkhead", String.class);
	          int statusCode = response.getStatusCode().value();
	          responseStatusCount.put(i, statusCode);
	          latch.countDown();
	      }));
	  latch.await();
	  executorService.shutdown();

	  log.info("Response statuses: " + responseStatusCount.entrySet());
	  assertTrue(responseStatusCount.containsValue(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED.value()));
	  assertTrue(responseStatusCount.containsValue(HttpStatus.OK.value()));
	  EXTERNAL_SERVICE.verify(3, WireMock.getRequestedFor(WireMock.urlEqualTo("/api/external")));
	}
	
	@Test
	public void testRatelimiter() {
	    EXTERNAL_SERVICE.stubFor(WireMock.get("/api/external")
	      .willReturn(WireMock.ok()));
	    Map<Integer, Integer> responseStatusCount = new ConcurrentHashMap<>();

	    IntStream.rangeClosed(1, 10)
	      .parallel()
	      .forEach(i -> {
	          ResponseEntity<String> response = restTemplate.getForEntity("/api/rate-limiter", String.class);
	          int statusCode = response.getStatusCode().value();
	          responseStatusCount.put(i, statusCode);
	      });

	    log.info("Response statuses: " + responseStatusCount.entrySet());
	    assertTrue(responseStatusCount.containsValue(HttpStatus.TOO_MANY_REQUESTS.value()));
	    assertTrue(responseStatusCount.containsValue(HttpStatus.OK.value()));
	    EXTERNAL_SERVICE.verify(5, WireMock.getRequestedFor(WireMock.urlEqualTo("/api/external")));
	}
	
}
