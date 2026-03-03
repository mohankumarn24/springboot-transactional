package net.projectsync.springboottransactional.controller;

import lombok.RequiredArgsConstructor;
import net.projectsync.springboottransactional.service.OuterIsolationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class IsolationController {

    private final OuterIsolationService outerService;

    @GetMapping("/read-committed")
    public String readCommitted() {
        outerService.readCommittedExample();
        return "Check logs";
    }

    @GetMapping("/repeatable-read")
    public String repeatableRead() {
        outerService.repeatableReadExample();
        return "Check logs";
    }

    @GetMapping("/serializable")
    public String serializable() {
        outerService.serializableExample();
        return "Check logs";
    }
}
