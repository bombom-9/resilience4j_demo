package com.demo.external;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExternalService {
	
	private final RestTemplate restTemplate;

	public String callApi() {
	    return restTemplate.getForObject("/api/external", String.class);
	}
	
	public String callApiWithDelay() {
	    String result = restTemplate.getForObject("/api/external", String.class);
	    try {
	        Thread.sleep(5000);
	    } catch (InterruptedException ignore) {
	    }
	    return result;
	}
	
}
