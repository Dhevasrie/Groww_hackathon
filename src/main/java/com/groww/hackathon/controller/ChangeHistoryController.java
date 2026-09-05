package com.groww.hackathon.controller;

import com.groww.hackathon.service.ChangeEventLogService;
import com.groww.hackathon.util.UserIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/watchlist/history")
@RequiredArgsConstructor
public class ChangeHistoryController {

    private final ChangeEventLogService changeEventLogService;
    private final UserIdentityResolver userIdentityResolver;

    @GetMapping
    public String view(Model model, HttpServletRequest request, HttpServletResponse response) {
        String userId = userIdentityResolver.resolve(request, response);
        model.addAttribute("events", changeEventLogService.getHistory(userId));
        return "change-history";
    }
}