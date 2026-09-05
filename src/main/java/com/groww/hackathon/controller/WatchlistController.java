package com.groww.hackathon.controller;

import com.groww.hackathon.dto.WatchlistItemView;
import com.groww.hackathon.model.WatchlistItem;
import com.groww.hackathon.service.ChangeDetectionService;
import com.groww.hackathon.service.WatchlistService;
import com.groww.hackathon.util.UserIdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final ChangeDetectionService changeDetectionService;
    private final UserIdentityResolver userIdentityResolver;

    @GetMapping
    public List<WatchlistItemView> getWatchlist(HttpServletRequest req, HttpServletResponse res) {
        return watchlistService.getWatchlistView(userIdentityResolver.resolve(req, res));
    }

    @PostMapping("/{symbol}")
    public WatchlistItem addSymbol(@PathVariable String symbol,
                                   HttpServletRequest req, HttpServletResponse res) {
        return watchlistService.addSymbol(userIdentityResolver.resolve(req, res), symbol);
    }

    @DeleteMapping("/{symbol}")
    public void removeSymbol(@PathVariable String symbol,
                             HttpServletRequest req, HttpServletResponse res) {
        watchlistService.removeSymbol(userIdentityResolver.resolve(req, res), symbol);
    }

    // "This wasn't useful" feedback — raises this user's alert threshold for
    // this specific symbol going forward. See ChangeDetectionService.recordDismissal.
    @PostMapping("/{symbol}/dismiss")
    public void dismissChange(@PathVariable String symbol,
                              HttpServletRequest req, HttpServletResponse res) {
        String userId = userIdentityResolver.resolve(req, res);
        changeDetectionService.recordDismissal(userId, symbol.trim().toUpperCase());
    }
}