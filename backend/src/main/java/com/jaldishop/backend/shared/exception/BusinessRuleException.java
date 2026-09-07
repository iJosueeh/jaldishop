package com.jaldishop.backend.shared.exception;

public class BusinessRuleException extends AppException {

    private static final String DEFAULT_CODE = "BUSINESS_RULE_VIOLATION";

    public BusinessRuleException(String message){
        super(DEFAULT_CODE, message);
    }

    public BusinessRuleException(String code, String message) {
        super(code, message);
    }

}
