package com.demo.resilience4j.exception;

public class BusinessException extends RuntimeException {

	private static final long serialVersionUID = -7430432070356375143L;

	public BusinessException(String message) {
        super(message);
    }
	
}
