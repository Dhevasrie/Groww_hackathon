package com.groww.hackathon.controller;

import com.groww.hackathon.service.DailyRollupService;
import com.groww.hackathon.util.UserIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/watchlist/insights")
@RequiredArgsConstructor
public class PortfolioInsightsController {

    private final DailyRollupService dailyRollupService;
    private final UserIdentityResolver userIdentityResolver;

    @GetMapping
    public String view(Model model, HttpServletRequest request, HttpServletResponse response) {
        String userId = userIdentityResolver.resolve(request, response);
        model.addAttribute("summary", dailyRollupService.getRollup(userId));
        return "portfolio-insights";
    }
}