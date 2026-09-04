package com.groww.hackathon.model;

public enum ChangeSeverity {
    NEW,        // never viewed before — no baseline to diff against
    QUIET,      // within normal noise for this symbol
    NOTABLE,    // 1–2 std devs from this symbol's normal move
    SIGNIFICANT // more than 2 std devs — genuinely unusual for THIS symbol specifically
}