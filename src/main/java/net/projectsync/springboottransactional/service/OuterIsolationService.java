package net.projectsync.springboottransactional.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OuterIsolationService {

    private final InnerIsolationService innerService;

    /*
     * =========================================
     * 1️⃣ READ_COMMITTED (PostgreSQL default)
     * =========================================
     *
     * - Only committed data is visible.
     * - Dirty reads are NOT allowed.
     *
     * Example:
     * T1 updates value (not committed).
     * T2 reads → still sees old value.
     *
     * If T1 commits,
     * T2 reads again → sees new value.
     *
     * Problem:
     * Same row can change inside same transaction
     * (non-repeatable read possible).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void readCommittedExample() {
        innerService.readData();
    }


    /*
     * =========================================
     * 2️⃣ REPEATABLE_READ
     * =========================================
     *
     * - Once a row is read,
     *   it stays same inside that transaction.
     *
     * Example:
     * T2 reads balance = 1000
     * T1 updates to 2000 and commits
     * T2 reads again → still sees 1000
     *
     * Prevents:
     * - Dirty reads
     * - Non-repeatable reads
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void repeatableReadExample() {
        innerService.readData();
    }


    /*
     * =========================================
     * 3️⃣ SERIALIZABLE
     * =========================================
     *
     * - Strictest isolation.
     * - Transactions behave as if executed one by one.
     *
     * If two transactions update same row,
     * PostgreSQL may throw:
     * "could not serialize access due to concurrent update"
     *
     * Prevents:
     * - Dirty reads
     * - Non-repeatable reads
     * - Phantom reads
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void serializableExample() {
        innerService.readData();
    }


    /*
     * =========================================
     * 4️⃣ READ_UNCOMMITTED
     * =========================================
     *
     * - Allows dirty reads (theoretically).
     * - Not supported by PostgreSQL
     *
     * BUT:
     * PostgreSQL treats it as READ_COMMITTED.
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void readUncommittedExample() {
        innerService.readData();
    }
}
