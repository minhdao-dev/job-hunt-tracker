package com.jobhunt.tracker.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
public class WebController {

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard/index";
    }

    @GetMapping("/jobs")
    public String jobs() {
        return "job/list";
    }

    @GetMapping("/jobs/{id}")
    public String jobDetail(@PathVariable UUID id) {
        return "job/detail";
    }

    @GetMapping("/companies")
    public String companies() {
        return "company/list";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings/index";
    }
}