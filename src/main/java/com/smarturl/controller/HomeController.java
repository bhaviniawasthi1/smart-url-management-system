package com.smarturl.controller;

import com.smarturl.dto.PlatformStatsResponse;
import com.smarturl.service.AdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Minimal Thymeleaf page controller for the frontend demonstration.
 * These endpoints serve HTML pages; the backend REST APIs live
 * under /api/v1/ in separate controller classes.
 */
@Controller
public class HomeController {

    private final AdminService adminService;

    public HomeController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Home");
        try {
            PlatformStatsResponse stats = adminService.getPlatformStats();
            model.addAttribute("platformStats", stats);
        } catch (Exception e) {
            model.addAttribute("platformStats", null);
        }
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("title", "Login");
        return "login";  // Will be created in the auth phase
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("title", "Register");
        return "register";  // Will be created in the auth phase
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("title", "Dashboard");
        return "dashboard";  // Will be created in the analytics phase
    }

    @GetMapping("/urls")
    public String myUrls(Model model) {
        model.addAttribute("title", "My URLs");
        return "url-list";  // Will be created in the URL management phase
    }

    @GetMapping("/urls/create")
    public String createUrlPage(Model model) {
        model.addAttribute("title", "Create URL");
        return "url-create";  // Will be created in the URL management phase
    }
}