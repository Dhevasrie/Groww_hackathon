package com.groww.hackathon.service;

import com.groww.hackathon.model.ChangeSeverity;
import com.groww.hackathon.model.UserSymbolSensitivity;
import com.groww.hackathon.repository.MarketTickRepository;
import com.groww.hackathon.repository.SymbolStatsRepository;
import com.groww.hackathon.repository.UserSymbolSensitivityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeDetectionServiceThresholdTest {

    @Mock private MarketTickRepository tickRepository;
    @Mock private SymbolStatsRepository statsRepository;
    @Mock private UserSymbolSensitivityRepository sensitivityRepository;

    @Test
    void higherMultiplierRequiresABiggerMoveToReachSignificant() {
        ChangeDetectionService service =
                new ChangeDetectionService(tickRepository, statsRepository, sensitivityRepository);
        when(statsRepository.findById("RELIANCE")).thenReturn(Optional.empty()); // stddev floor 0.01

        // a move that's SIGNIFICANT at default sensitivity...
        ChangeDetectionService.ChangeResult atDefault =
                service.classify("RELIANCE", 102.5, 100.0, 1.0);
        assertThat(atDefault.severity()).isEqualTo(ChangeSeverity.SIGNIFICANT);

        // ...should no longer be, once the user has dismissed enough alerts here
        ChangeDetectionService.ChangeResult afterDismissals =
                service.classify("RELIANCE", 102.5, 100.0, 2.0);
        assertThat(afterDismissals.severity()).isNotEqualTo(ChangeSeverity.SIGNIFICANT);
    }

    @Test
    void resolveThresholdMultiplierDefaultsToOneWhenNeverAdjusted() {
        ChangeDetectionService service =
                new ChangeDetectionService(tickRepository, statsRepository, sensitivityRepository);
        when(sensitivityRepository.findByUserIdAndSymbol("user-1", "TCS")).thenReturn(Optional.empty());

        assertThat(service.resolveThresholdMultiplier("user-1", "TCS")).isEqualTo(1.0);
    }

    @Test
    void recordDismissalRaisesMultiplierAndCapsIt() {
        ChangeDetectionService service =
                new ChangeDetectionService(tickRepository, statsRepository, sensitivityRepository);

        UserSymbolSensitivity existing = new UserSymbolSensitivity();
        existing.setUserId("user-1");
        existing.setSymbol("ZOMATO");
        existing.setThresholdMultiplier(2.9); // one step from the 3.0 cap
        existing.setDismissCount(5);

        when(sensitivityRepository.findByUserIdAndSymbol("user-1", "ZOMATO")).thenReturn(Optional.of(existing));
        when(sensitivityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordDismissal("user-1", "ZOMATO");

        ArgumentCaptor<UserSymbolSensitivity> captor = ArgumentCaptor.forClass(UserSymbolSensitivity.class);
        verify(sensitivityRepository).save(captor.capture());

        assertThat(captor.getValue().getThresholdMultiplier()).isEqualTo(3.0); // capped, not 2.9*1.15
        assertThat(captor.getValue().getDismissCount()).isEqualTo(6);
    }
}