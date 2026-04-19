package net.projectsync.springboottransactional.service;

import lombok.RequiredArgsConstructor;
import net.projectsync.springboottransactional.entity.Account;
import net.projectsync.springboottransactional.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InnerTransactionService {

    private final AccountRepository repository;

    /*
     * REQUIRED (Default Propagation)
     *
     * Behavior:
     * - If a transaction exists → join it
     * - If no transaction → create new one
     *
     * If exception occurs:
     * - Entire transaction rolls back (outer + inner)
     *
     * Most commonly used propagation in real projects.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void required() {
        repository.save(new Account("REQUIRED"));
        throw new RuntimeException("Error in REQUIRED");
    }


    /*
     * REQUIRES_NEW
     *
     * Behavior:
     * - Suspends existing transaction (if any)
     * - Starts a completely new transaction
     *
     * If exception occurs:
     * - Only this inner transaction rolls back
     * - Outer transaction continues
     *
     * Very useful for:
     * - Audit logging
     * - Payment logging
     * - Event tracking
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void requiresNew() {
        repository.save(new Account("REQUIRES_NEW"));
        throw new RuntimeException("Error in REQUIRES_NEW");
    }


    /*
     * NESTED
     *
     * Behavior:
     * - Uses savepoints inside existing transaction
     * - Rolls back only to savepoint if failure
     *
     * Important:
     * - Requires JDBC savepoint support
     * - Works properly with DataSourceTransactionManager
     * - PostgreSQL supports savepoints
     *
     * If exception occurs:
     * - Rolls back only nested part
     * - Outer transaction can still commit
     * 
     * If Outer fails → everything rolls back including inner
     */
    @Transactional(propagation = Propagation.NESTED)
    public void nested() {
        repository.save(new Account("NESTED"));
        throw new RuntimeException("Error in NESTED");
    }


    /*
     * SUPPORTS
     *
     * Behavior:
     * - If transaction exists → join it
     * - If no transaction → execute NON-transactionally
     *
     * Common use case:
     * - Read-only operations
     *
     * If called without transaction:
     * - No rollback support
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public void supports() {
        repository.save(new Account("SUPPORTS"));
    }


    /*
     * NOT_SUPPORTED
     *
     * Behavior:
     * - Suspends existing transaction
     * - Runs NON-transactionally
     *
     * If outer transaction fails:
     * - This operation will NOT roll back
     *
     * Use case:
     * - Reporting
     * - Logging
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void notSupported() {
        repository.save(new Account("NOT_SUPPORTED"));
    }


    /*
     * MANDATORY
     *
     * Behavior:
     * - Must run inside an existing transaction
     * - If no transaction → throws IllegalTransactionStateException
     *
     * Use case:
     * - Critical operations that must be part of larger transaction
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void mandatory() {
        repository.save(new Account("MANDATORY"));
    }


    /*
     * NEVER
     *
     * Behavior:
     * - Must NOT run inside a transaction
     * - If transaction exists → throws IllegalTransactionStateException
     *
     * Use case:
     * - System-level tasks
     * - Non-transactional operations
     */
    @Transactional(propagation = Propagation.NEVER)
    public void never() {
        repository.save(new Account("NEVER"));
    }
}