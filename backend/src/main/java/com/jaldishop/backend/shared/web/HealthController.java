package com.jaldishop.backend.shared.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    public String health() {
        return "Jaldi Shop is running";
    }

}
