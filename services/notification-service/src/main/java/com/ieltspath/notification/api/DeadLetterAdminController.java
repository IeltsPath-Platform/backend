package com.ieltspath.notification.api;

import com.ieltspath.events.messaging.DeadLetterCount;
import com.ieltspath.events.messaging.DeadLetterReplayer;
import com.ieltspath.events.messaging.ReplayResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Lets an administrator see how many messages each dead-letter queue holds and send up to a set number back for another
 * try. Replay is for messages whose cause has been fixed; a message that fails again returns to its dead-letter queue.
 */
@RestController
@RequestMapping("/api/notifications/admin/dead-letters")
@PreAuthorize("hasRole('ADMIN')")
public class DeadLetterAdminController {
    private static final Logger log = LoggerFactory.getLogger(DeadLetterAdminController.class);

    private final DeadLetterReplayer replayer;

    public DeadLetterAdminController(DeadLetterReplayer replayer) {
        this.replayer = replayer;
    }

    @GetMapping
    public List<DeadLetterCount> counts() {
        return replayer.counts();
    }

    @PostMapping("/{deadLetterQueue}/replay")
    public ReplayResult replay(@PathVariable("deadLetterQueue") String deadLetterQueue,
                               @RequestParam(name = "max", defaultValue = "50") int max) {
        if (replayer.routes().stream().noneMatch(route -> route.deadLetterQueue().equals(deadLetterQueue))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown dead-letter queue");
        }
        if (max < 1 || max > DeadLetterReplayer.MAX_PER_CALL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "max must be between 1 and 500");
        }
        ReplayResult result = replayer.replay(deadLetterQueue, max);
        log.info("Dead-letter replay: deadLetterQueue={} moved={} remaining={}", deadLetterQueue, result.moved(),
                result.remaining());
        return result;
    }
}
