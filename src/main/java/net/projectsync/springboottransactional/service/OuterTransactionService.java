package net.projectsync.springboottransactional.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OuterTransactionService {

    private final InnerTransactionService innerService;

    /*
     * Scenario 1: REQUIRED inside REQUIRED
     *
     * Outer starts transaction.
     * Inner joins same transaction.
     *
     * If inner throws exception:
     * → Entire transaction rolls back.
     */
    @Transactional
    public void testRequired() {
        innerService.required();
    }


    /*
     * Scenario 2: REQUIRES_NEW inside REQUIRED
     *
     * Outer starts transaction.
     * Inner suspends outer and starts new one.
     *
     * If inner fails:
     * → Inner rolls back
     * → Outer continues unless explicitly failed
     *
     * If outer throws exception later:
     * → Outer rolls back
     * → Inner commit remains (because separate transaction)
     */
    @Transactional
    public void testRequiresNew() {
        try {
            innerService.requiresNew();
        } catch (Exception ignored) {
            // inner rolled back already
        }

        throw new RuntimeException("Outer error");
    }


    /*
     * Scenario 3: NESTED inside REQUIRED
     *
     * Outer starts transaction.
     * Inner creates savepoint.
     *
     * If inner fails:
     * → Rolls back to savepoint
     * → Outer can still commit remaining work
     */
    @Transactional
    public void testNested() {
        try {
            innerService.nested();
        } catch (Exception ignored) {
            // rollback to savepoint only
        }
    }


    /*
     * Scenario 4: SUPPORTS inside REQUIRED
     *
     * Since outer has transaction:
     * → Inner joins same transaction.
     *
     * If this method has no transaction,
     * → then inner runs without transaction (auto-commit mode)
     */
    @Transactional
    public void testSupports() {
        innerService.supports();
    }


    /*
     * Scenario 5: MANDATORY inside REQUIRED
     *
     * Works fine because outer provides transaction.
     *
     * If this method was NOT transactional:
     * → IllegalTransactionStateException
     */
    @Transactional
    public void testMandatory() {
        innerService.mandatory();
    }


    /*
     * Scenario 6: NEVER without transaction
     *
     * This method is NOT transactional.
     * So NEVER works fine.
     *
     * If you annotate this method with @Transactional:
     * → It will throw IllegalTransactionStateException
     */
    public void testNever() {
        innerService.never();
    }
}