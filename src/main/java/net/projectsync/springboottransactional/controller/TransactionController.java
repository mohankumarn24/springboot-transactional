package net.projectsync.springboottransactional.controller;

import lombok.RequiredArgsConstructor;
import net.projectsync.springboottransactional.service.OuterTransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class TransactionController {

    private final OuterTransactionService outerService;

    @GetMapping("/required")
    public ResponseEntity<String> required() {
        outerService.testRequired();
        return new ResponseEntity<>(String.format("Check logs. Current timestamp: %s", Instant.now().toString()), HttpStatus.OK);
    }

    @GetMapping("/requires-new")
    public ResponseEntity<String> requiresNew() {
        outerService.testRequiresNew();
        return new ResponseEntity<>(String.format("Check logs. Current timestamp: %s", Instant.now().toString()), HttpStatus.OK);
    }

    @GetMapping("/nested")
    public ResponseEntity<String> nested() {
        outerService.testNested();
        return new ResponseEntity<>(String.format("Check logs. Current timestamp: %s", Instant.now().toString()), HttpStatus.OK);
    }

    @GetMapping("/supports")
    public ResponseEntity<String> supports() {
        outerService.testSupports();
        return new ResponseEntity<>(String.format("Check logs. Current timestamp: %s", Instant.now().toString()), HttpStatus.OK);
    }

    @GetMapping("/mandatory")
    public ResponseEntity<String> mandatory() {
        outerService.testMandatory();
        return new ResponseEntity<>(String.format("Check logs. Current timestamp: %s", Instant.now().toString()), HttpStatus.OK);
    }

    @GetMapping("/never")
    public ResponseEntity<String> never() {
        outerService.testNever();
        return new ResponseEntity<>(String.format("Check logs. Current timestamp: %s", Instant.now().toString()), HttpStatus.OK);
    }
}
