package com.demo.resilience4j.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class Resilience4jConfig {

	@Bean
	RouterFunction<ServerResponse> redirectRoot() {
		return RouterFunctions.route(RequestPredicates.GET("/"),
				req -> ServerResponse.permanentRedirect(URI.create("/actuator")).build());
	}
	
	/**
	 * can override the configuration of a particular CircuitBreaker, Bulkhead, Retry, RateLimiter or TimeLimiter instance.
	 * @return CircuitBreakerConfigCustomizer
	 */
	@Bean
	public CircuitBreakerConfigCustomizer testCustomizer() {
		return CircuitBreakerConfigCustomizer.of("backendA", builder -> builder.slidingWindowSize(100));
	}

	@Bean
	public RegistryEventConsumer<CircuitBreaker> myRegistryEventConsumer() {

		return new RegistryEventConsumer<CircuitBreaker>() {

			@Override
			public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
				entryAddedEvent.getAddedEntry().getEventPublisher().onEvent(event -> log.info(event.toString()));
			}

			@Override
			public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {

			}

			@Override
			public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {

			}
		};
	}

	@Bean
	public RegistryEventConsumer<Retry> myRetryRegistryEventConsumer() {

		return new RegistryEventConsumer<Retry>() {

			@Override
			public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
				entryAddedEvent.getAddedEntry().getEventPublisher().onEvent(event -> log.info(event.toString()));
			}

			@Override
			public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemoveEvent) {

			}

			@Override
			public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {

			}
		};
	}

}
