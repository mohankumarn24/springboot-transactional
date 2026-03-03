package net.projectsync.springboottransactional.service;

import lombok.RequiredArgsConstructor;
import net.projectsync.springboottransactional.repository.AccountRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InnerIsolationService {

    private final AccountRepository repository;

    /*
     * This method simply reads data.
     * It will execute using the isolation level
     * defined in the outer transaction.
     *
     * Important:
     * Isolation level is fixed when the transaction starts.
     * Inner method cannot change it.
     */
    public void readData() {
        repository.findAll()
                .forEach(account ->
                        System.out.println("Account: " + account.getName()));
    }
}
