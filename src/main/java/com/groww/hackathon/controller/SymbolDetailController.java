package com.groww.hackathon.controller;

import com.groww.hackathon.service.SymbolDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Separate controller from WatchlistViewController on purpose — this owns
 * one URL (/watchlist/{symbol}) and one concern (the drill-down), rather
 * than growing the main controller's responsibilities.
 */
@Controller
@RequestMapping("/watchlist")
@RequiredArgsConstructor
public class SymbolDetailController {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final SymbolDetailService symbolDetailService;

    @GetMapping("/{symbol}")
    public String detail(@PathVariable String symbol, Model model) {
        String normalized = symbol.trim().toUpperCase();
        List<SymbolDetailService.TickPoint> history = symbolDetailService.getHistory(normalized);

        model.addAttribute("symbol", normalized);
        model.addAttribute("history", history);
        model.addAttribute("chartLabelsJson", toLabelsJson(history));
        model.addAttribute("chartPricesJson", toPricesJson(history));
        return "symbol-detail";
    }

    // Built server-side as plain JSON strings so the template can drop them
    // straight into a <script> tag with th:utext — no Jackson/th:inline
    // serialization of a record containing an Instant to worry about.
    private String toLabelsJson(List<SymbolDetailService.TickPoint> newestFirst) {
        List<SymbolDetailService.TickPoint> chronological = new ArrayList<>(newestFirst);
        Collections.reverse(chronological);
        return chronological.stream()
                .map(p -> "\"" + TIME_FORMAT.format(p.timestamp()) + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String toPricesJson(List<SymbolDetailService.TickPoint> newestFirst) {
        List<SymbolDetailService.TickPoint> chronological = new ArrayList<>(newestFirst);
        Collections.reverse(chronological);
        return chronological.stream()
                .map(p -> String.valueOf(p.price()))
                .collect(Collectors.joining(",", "[", "]"));
    }
}