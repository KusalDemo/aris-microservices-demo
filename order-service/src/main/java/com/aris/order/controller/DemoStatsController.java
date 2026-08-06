package com.aris.order.controller;

import com.aris.order.dto.DemoStatsResponse;
import com.aris.order.service.DemoStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo/stats")
public class DemoStatsController {

    private final DemoStatsService demoStatsService;

    public DemoStatsController(DemoStatsService demoStatsService) {
        this.demoStatsService = demoStatsService;
    }

    @GetMapping
    public DemoStatsResponse stats() {
        return demoStatsService.snapshot();
    }

    @PostMapping("/reset")
    public DemoStatsResponse reset() {
        demoStatsService.reset();
        return demoStatsService.snapshot();
    }
}
