package com.groww.hackathon.controller;

import com.groww.hackathon.service.WatchlistService;
import com.groww.hackathon.util.UserIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Serves the server-rendered watchlist page. Kept separate from
 * WatchlistController (the REST API) — this controller is for browser
 * navigation/form posts, the REST one is for the JSON polling calls the
 * page's JS makes to refresh data live.
 */
@Controller
@RequestMapping("/watchlist")
@RequiredArgsConstructor
public class WatchlistViewController {

    private final WatchlistService watchlistService;
    private final UserIdentityResolver userIdentityResolver;

    @GetMapping
    public String view(Model model, HttpServletRequest request, HttpServletResponse response) {
        String userId = userIdentityResolver.resolve(request, response);
        model.addAttribute("items", watchlistService.getWatchlistView(userId));
        return "watchlist";
    }

    // Form-based add/remove as a no-JS fallback; the page's JS also calls
    // the JSON endpoints directly for the live-refresh experience.
    @PostMapping("/add")
    public String addSymbol(@RequestParam String symbol, HttpServletRequest request, HttpServletResponse response) {
        String userId = userIdentityResolver.resolve(request, response);
        watchlistService.addSymbol(userId, symbol);
        return "redirect:/watchlist";
    }

    @PostMapping("/remove")
    public String removeSymbol(@RequestParam String symbol, HttpServletRequest request, HttpServletResponse response) {
        String userId = userIdentityResolver.resolve(request, response);
        watchlistService.removeSymbol(userId, symbol);
        return "redirect:/watchlist";
    }
}
