package com.d5data.demo.web;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {
    public record CsrfResponse(String token, String headerName, String parameterName) {
    }

    @GetMapping("/api/auth/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName(), token.getParameterName());
    }
}
