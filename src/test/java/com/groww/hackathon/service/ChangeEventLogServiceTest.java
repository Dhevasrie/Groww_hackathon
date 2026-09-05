package com.groww.hackathon.service;

import com.groww.hackathon.model.ChangeEventLog;
import com.groww.hackathon.model.ChangeSeverity;
import com.groww.hackathon.repository.ChangeEventLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeEventLogServiceTest {

    @Mock
    private ChangeEventLogRepository repository;

    @Test
    void logsWhenNoPriorEventExists() {
        var service = new ChangeEventLogService(repository);
        when(repository.findTopByUserIdAndSymbolOrderByOccurredAtDesc("u1", "TCS"))
                .thenReturn(Optional.empty());

        var result = new ChangeDetectionService.ChangeResult(
                ChangeSeverity.SIGNIFICANT, -0.042, 3.0, "Unusual move");
        service.recordIfSignificant("u1", "TCS", result);

        verify(repository, times(1)).save(any(ChangeEventLog.class));
    }

    @Test
    void skipsNonSignificantSeverity() {
        var service = new ChangeEventLogService(repository);
        var result = new ChangeDetectionService.ChangeResult(
                ChangeSeverity.NOTABLE, 0.01, 1.2, "somewhat above range");

        service.recordIfSignificant("u1", "TCS", result);

        verifyNoInteractions(repository);
    }

    @Test
    void suppressesDuplicateWithinDedupWindow() {
        var service = new ChangeEventLogService(repository);
        ChangeEventLog recent = new ChangeEventLog();
        recent.setOccurredAt(Instant.now().minusSeconds(5));
        when(repository.findTopByUserIdAndSymbolOrderByOccurredAtDesc("u1", "TCS"))
                .thenReturn(Optional.of(recent));

        var result = new ChangeDetectionService.ChangeResult(
                ChangeSeverity.SIGNIFICANT, -0.042, 3.0, "Unusual move");
        service.recordIfSignificant("u1", "TCS", result);

        verify(repository, never()).save(any());
    }

    @Test
    void logsAgainAfterDedupWindowExpires() {
        var service = new ChangeEventLogService(repository);
        ChangeEventLog old = new ChangeEventLog();
        old.setOccurredAt(Instant.now().minusSeconds(45));
        when(repository.findTopByUserIdAndSymbolOrderByOccurredAtDesc("u1", "TCS"))
                .thenReturn(Optional.of(old));

        var result = new ChangeDetectionService.ChangeResult(
                ChangeSeverity.SIGNIFICANT, -0.042, 3.0, "Unusual move");
        service.recordIfSignificant("u1", "TCS", result);

        verify(repository, times(1)).save(any(ChangeEventLog.class));
    }
}