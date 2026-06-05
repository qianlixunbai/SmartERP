package com.smartoa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeoutScheduler {

    private final LeaveService leaveService;

    @Scheduled(fixedRate = 300000)
    public void checkTimeouts() {
        log.debug("checking for timed-out approvals...");
        int count = leaveService.checkTimeouts();
        if (count > 0) {
            log.info("processed {} timed-out approvals", count);
        }
    }
}
