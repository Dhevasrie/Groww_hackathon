package com.groww.hackathon.controller;

import com.groww.hackathon.service.ChangeDetectionService;
import com.groww.hackathon.util.UserIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/watchlist/settings/sensitivity")
@RequiredArgsConstructor
public class SensitivitySettingsController {

    private final ChangeDetectionService changeDetectionService;
    private final UserIdentityResolver userIdentityResolver;

    @GetMapping
    public String view(Model model, HttpServletRequest request, HttpServletResponse response) {
        String userId = userIdentityResolver.resolve(request, response);
        model.addAttribute("sensitivities", changeDetectionService.getSensitivities(userId));
        return "sensitivity-settings";
    }

    @PostMapping("/reset")
    public String reset(@RequestParam String symbol, HttpServletRequest request, HttpServletResponse response) {
        String userId = userIdentityResolver.resolve(request, response);
        changeDetectionService.resetSensitivity(userId, symbol);
        return "redirect:/watchlist/settings/sensitivity";
    }
}