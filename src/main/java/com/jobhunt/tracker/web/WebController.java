package com.jobhunt.tracker.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
public class WebController {

    @GetMapping("/")
    public String landing() {
        return "landing";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    @GetMapping("/verify-email")
    public String verifyEmail() {
        return "auth/verify-email";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword() {
        return "auth/reset-password";
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
    public String jobDetail(@PathVariable UUID id, Model model) {
        model.addAttribute("jobId", id.toString());
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