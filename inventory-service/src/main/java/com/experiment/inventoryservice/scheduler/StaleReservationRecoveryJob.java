
package com.experiment.inventoryservice.scheduler;

import com.experiment.inventoryservice.repository.OrderSagaRepository;
import com.experiment.inventoryservice.service.StaleReservationRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaleReservationRecoveryJob {

    private static final int STALE_AFTER_HOURS = 1;

    private final OrderSagaRepository orderSagaRepository;
    private final StaleReservationRecoveryService recoveryService;

    @Scheduled(fixedDelay = 60000)
    public void recoverStaleReservations() {
        log.info("Recovering stale reservations");
        var staleReservations =
                orderSagaRepository
                        .findTop100StaleReservationsByHours(
                                STALE_AFTER_HOURS
                        );

        for (var candidate : staleReservations) {
            try {
                boolean recovered =
                        recoveryService.recover(candidate.getSagaId());

                if (recovered) {
                    log.info(
                            "Stale reservation recovered: {}",
                            candidate.getSagaId()
                    );
                }
            } catch (Exception exception) {
                // One bad reservation must not stop the rest of the batch.
                log.error(
                        "Failed to recover stale reservation: {}",
                        candidate.getSagaId(),
                        exception
                );
            }
        }
    }
}
