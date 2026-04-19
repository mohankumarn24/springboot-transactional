// =============================================================
// COMPLETE @Transactional EXAMPLE — SERVICE LAYER
// Covers: readOnly, rollback, propagation, isolation, timeout
// =============================================================

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/*
// If any exception occurs, the whole operation rolls back.
@Transactional
public void transferMoney(Account from, Account to, double amount) {
    from.debit(amount);
    to.credit(amount);
}

Key Properties:
--------------
 - readOnly=true	: optimization hint for read-only operations
 - rollbackFor		: which exceptions trigger rollback (default: unchecked only)
 - propagation		: how transactions interact (e.g., REQUIRED, REQUIRES_NEW, NESTED)
 - isolation		: concurrency control (READ_COMMITTED, SERIALIZABLE, etc.)
 - timeout			: max seconds before forced rollback
*/

@Service
@Transactional(readOnly = true)
// ✅ Class-level default: all methods are read-only transactions
// WHY: Most service methods are reads. This tells Hibernate:
//      → skip dirty checking (don't track entity changes)
//      → don't flush changes to DB
//      → result: faster, lighter queries
// Write methods below will OVERRIDE this with @Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final AuditRepository auditRepository;

    // =========================================================
    // READ OPERATIONS — inherits @Transactional(readOnly = true)
    // =========================================================

    public Order getOrderById(Long id) {
        // No annotation needed — inherits class-level readOnly=true
        // Session is open here, so lazy fields CAN be accessed safely
        return orderRepository.findById(id).orElseThrow();
    }

    public List<Order> getAllOrders() {
        // Same — read-only transaction, no override needed
        return orderRepository.findAll();
    }


    // =========================================================
    // WRITE OPERATIONS — override with @Transactional (readOnly=false by default)
    // =========================================================

    @Transactional
    // Overrides class-level readOnly=true
    // readOnly is false by default here, so Hibernate WILL:
    //   → track entity changes (dirty checking ON)
    //   → flush changes to DB on commit
    // If ANY exception occurs → entire method rolls back automatically
    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrder(Long id, Order updatedData) {
        Order existing = orderRepository.findById(id).orElseThrow();
        existing.setStatus(updatedData.getStatus());
        // No need to call .save() — Hibernate dirty checking detects the change
        // and auto-updates on commit (because readOnly=false)
        return existing;
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
        // If deleteById throws → transaction rolls back → nothing deleted
    }


    // =========================================================
    // ROLLBACK — what triggers a rollback?
    // =========================================================
    
    @Transactional
    // DEFAULT: only RuntimeException (unchecked) triggers rollback
    // Checked exceptions (like IOException) do NOT rollback by default
    public void processOrder(Long id) throws Exception {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus("PROCESSING");
        // If RuntimeException thrown here → ROLLBACK ✅
        // If IOException thrown here       → NO rollback ❌ (checked exception)
    }

    @Transactional(rollbackFor = Exception.class)
    // rollbackFor = Exception.class means:
    // → rollback on ALL exceptions (both checked and unchecked)
    // Use this when your method throws checked exceptions and you want safety
    public void processOrderSafe(Long id) throws Exception {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus("PROCESSING");
        riskyOperation(); // throws IOException (checked) → WILL rollback now ✅
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    // noRollbackFor: even if this RuntimeException occurs → do NOT rollback
    // Use case: you want to save partial data even when validation fails
    public void createOrderLenient(Order order) {
        orderRepository.save(order);
        // If IllegalArgumentException thrown → NO rollback, order is saved
    }

    // ⚠️ COMMON MISTAKE — swallowing exception kills rollback
    @Transactional
    public void badExample(Order order) {
        try {
            orderRepository.save(order);
            riskyOperation();
        } catch (Exception e) {
            log.error("something failed", e);
            // ❌ Exception swallowed here — Spring never sees it → NO rollback
            // Fix: either rethrow, or don't catch it here
        }
    }


    // =========================================================
    // PROPAGATION — how transactions behave when methods call each other
    // =========================================================

	 // =============================================================
	 // QUICK REFERENCE
	 // =============================================================
	
	 // PROPERTY         | DEFAULT          | USE FOR
	 // -----------------|------------------|---------------------------
	 // readOnly         | false            | performance on reads
	 // rollbackFor      | RuntimeException | what triggers rollback
	 // noRollbackFor    | (none)           | what skips rollback
	 // propagation      | REQUIRED         | how txns interact
	 // isolation        | DEFAULT (DB)     | concurrency / concurrent reads
	 // timeout          | -1 (none)        | prevent long-running txns
	
	 // PROPAGATION      | BEHAVIOR
	 // -----------------|-------------------------------------------
	 // REQUIRED         | join existing txn OR create new txn (default)
	 // REQUIRES_NEW     | always new txn, suspend existing txn(audit logs)
	 // NESTED           | savepoint inside existing txn (partial rollback)
	 // SUPPORTS         | use existing txn if present, else run non-transactional
	 // MANDATORY        | must have existing txn, else throws
	 // NEVER            | must NOT have txn, else throws
	 // NOT_SUPPORTED    | suspend existing txn, run without txn
    

    /*
    @Service
    public class ServiceA {
        @Autowired private ServiceB serviceB;

        @Transactional(propagation = Propagation.REQUIRED)
        public void methodA() {
            insertIntoTable("A");
            serviceB.methodB();
        }
    }

    @Service
    public class ServiceB {
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void methodB() {
            insertIntoTable("B");
            throw new RuntimeException("Fail B");
        }
    }
    
+------------------+--------------------------------------------------+--------------------------------------------------------------+----------------+-------------------------------------------------------------+
| Propagation Type | Behavior                                         | Example Scenario                                             | DB Before      | DB After                                                    |
+------------------+--------------------------------------------------+--------------------------------------------------------------+----------------+-------------------------------------------------------------+
| REQUIRED         | Joins existing transaction, or starts a new one  | ServiceA.methodA() calls ServiceB.methodB(). Both run in     | Table empty    | If methodB() fails → rollback everything. If success → both |
| (default)        | if none exists.                                  | the same transaction.                                        |                | inserts committed.                                          |
+------------------+--------------------------------------------------+--------------------------------------------------------------+----------------+-------------------------------------------------------------+
| REQUIRES_NEW     | Suspends current transaction, starts a new one.  | methodA() inserts record, then calls methodB() with          | Table empty    | If methodB() fails → only its transaction rolls back,       |
|                  |                                                  | REQUIRES_NEW.                                                |                | methodA() insert still committed.                           |
+------------------+--------------------------------------------------+--------------------------------------------------------------+----------------+-------------------------------------------------------------+
| SUPPORTS         | Runs in a transaction if one exists, otherwise   | methodA() has transaction, calls methodB() with SUPPORTS.    | Table empty    | If methodA() rolls back → methodB() changes also rolled     |
|                  | executes non-transactionally.                    |                                                              |                | back. If no transaction → methodB() commits immediately.    |
+------------------+--------------------------------------------------+--------------------------------------------------------------+----------------+-------------------------------------------------------------+
| MANDATORY        | Must run inside a transaction, else throws       | Call methodB() with MANDATORY from non-transactional         | Table empty    | Exception thrown, no DB changes.                            |
|                  | exception.                                       | methodA().                                                   |                |                                                             |
+------------------+--------------------------------------------------+--------------------------------------------------------------+----------------+-------------------------------------------------------------+
| NOT_SUPPORTED    | Suspends current transaction, runs               | methodA() starts transaction, calls methodB() with           | Table empty    | methodB() changes committed immediately, even if methodA()  |
|                  | non-transactionally.                             | NOT_SUPPORTED.                                               |                | rolls back later.                                           |
+------------------+--------------------------------------------------+--------------------------------------------------------------+----------------+-------------------------------------------------------------+
| NEVER            | Must not run inside a transaction, else throws   | Call methodB() with NEVER from transactional methodA().      | Table empty    | Exception thrown, no DB changes.                            |
|                  | exception.                                       |                                                              |                |                                                             |
+------------------+--------------------------------------------------+--------------------------------------------------------------+----------------+-------------------------------------------------------------+
| NESTED           | Runs within a nested transaction (savepoint).    | methodA() inserts record, calls methodB() with NESTED.       | Table empty    | If methodB() fails → only its changes rolled back,          |
|                  | Rollback affects only nested part unless parent  |                                                              |                | methodA() insert still committed. If methodA() fails →      |
|                  | rolls back.                                      |                                                              |                | everything rolled back.                                     |
+------------------+--------------------------------------------------+--------------------------------------------------------------+----------------+-------------------------------------------------------------+
	*/
    
    @Transactional(propagation = Propagation.REQUIRED)
    // DEFAULT propagation
    // → If a transaction already exists: JOIN it (share same transaction)
    // → If no transaction exists: CREATE a new one
    // Both the caller and this method share the SAME transaction
    // If this method fails → ENTIRE transaction (including caller) rolls back
    public void placeOrder(Order order) {
        orderRepository.save(order);
        chargePayment(order); // called inside same transaction
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    // → ALWAYS creates a BRAND NEW transaction
    // → Suspends the existing transaction while this runs
    // → This method's rollback does NOT affect the caller's transaction
    // USE CASE: audit logging — save the log even if main operation fails
    public void saveAuditLog(String message) {
        auditRepository.save(new AuditLog(message));
        // Even if placeOrder() rolls back, this audit log is SAVED ✅
    }

    @Transactional(propagation = Propagation.NESTED)
    // → Creates a SAVEPOINT inside the existing transaction
    // → If this method fails → rollback to savepoint (caller can continue)
    // → If caller fails → everything rolls back including this
    // Requires JDBC savepoint support (PostgreSQL, MySQL support it)
    // Reuses same transaction. No new transaction created
    public void chargePayment(Order order) {
        paymentRepository.charge(order);
        // If this fails → rollback to savepoint, placeOrder() can handle it
        // placeOrder() itself decides whether to continue or also rollback
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    // → If transaction exists: runs WITHIN it
    // → If no transaction: runs WITHOUT a transaction
    // USE CASE: utility/helper methods that work either way
    public Order findOrder(Long id) {
        return orderRepository.findById(id).orElseThrow();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    // → MUST be called within an existing transaction
    // → Throws IllegalTransactionStateException if no transaction exists
    // USE CASE: enforce that this method is never called standalone
    // Both caller and called method will use same transaction if present
    public void validateAndSave(Order order) {
        // Caller MUST have started a transaction before calling this
        orderRepository.save(order);
    }

    @Transactional(propagation = Propagation.NEVER)
    // → Must NOT be called within a transaction
    // → Throws exception if a transaction exists
    // USE CASE: operations that should never run inside a transaction (e.g., DDL)
    public void runDDLOperation() {
        // Will throw if called from inside a @Transactional method
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    // → Always runs WITHOUT a transaction
    // → If a transaction exists: suspends it, runs without, then resumes
    // USE CASE: batch processing or operations that perform better without txn
    public void bulkExport() {
        // Runs outside any transaction context
    }


    // =========================================================
    // ISOLATION — controls how concurrent transactions see each other
    // =========================================================

    /*
	 * The Three Problems Isolation Levels Solve:
	 * Dirty Read 			→ Transaction A reads data that Transaction B has written but not yet committed. If B rolls back, A has read “ghost” data.
	 * Non-Repeatable Read 	→ Transaction A reads the same row twice but gets different values because Transaction B updated it in between.
	 * Phantom Read 		→ Transaction A runs the same query twice but gets different sets of rows because Transaction B inserted/deleted rows in between.
     */
    
    // Problems isolation deals with:
    // Dirty Read          → reading UNCOMMITTED data from another transaction
    // Non-Repeatable Read → same row gives DIFFERENT values in same transaction
    // Phantom Read        → same query returns DIFFERENT rows in same transaction

    
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    // Can read uncommitted changes from other transactions
    // Allows: Dirty Read, Non-Repeatable Read, Phantom Read
    // Fastest but LEAST safe — rarely used in production
    // Use Case: 
    // 	Scenario: Transaction B updates an order but hasn’t committed yet. Transaction A reads it anyway.
    // 	Result: If B rolls back, A has read invalid data.
    // 	Use: Almost never in production.
    public Order getDirtyOrder(Long id) {
        return orderRepository.findById(id).orElseThrow();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    // Can only read COMMITTED data
    // Prevents: Dirty Read
    // Allows: Non-Repeatable Read, Phantom Read
    // DEFAULT in PostgreSQL — good balance for most apps
    // Use Case: 
    // 	Scenario: Transaction A can only read data that Transaction B has committed.
    // 	Result: No dirty reads. But if B updates the same row later, A may see different values (non-repeatable read).
    // 	Use: Default in many databases; safe for most apps.
    public Order getCommittedOrder(Long id) {
        return orderRepository.findById(id).orElseThrow();
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    // Same row always returns SAME value within this transaction
    // Prevents: Dirty Read, Non-Repeatable Read
    // Allows: Phantom Read
    // DEFAULT in MySQL InnoDB
    // Use Case: 
    // 	Scenario: Transaction A reads a row. Even if Transaction B updates it later, A will always see the same value during its transaction.
    // 	Result: Prevents dirty and non-repeatable reads. But if B inserts new rows, A’s queries may see “phantoms.”
    // 	Use: Ensures consistency of row values; common in MySQL.
    public Order getStableOrder(Long id) {
        return orderRepository.findById(id).orElseThrow();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    // Transactions run as if SEQUENTIAL (one after another)
    // Prevents: ALL three problems
    // SLOWEST — heavy locking — use only for critical financial operations
    // Use Case: 
    // 	Scenario: Transactions run as if one after another (sequential).
    // 	Result: Prevents dirty reads, non-repeatable reads, and phantom reads.
    //  Trade-off: Slowest, heavy locking, reduced concurrency.
    // 	Use: Banking, financial apps, critical consistency.
    public void processFinancialTransaction(Order order) {
        orderRepository.save(order);
    }


    // =========================================================
    // TIMEOUT — rollback if method takes too long
    // =========================================================

    @Transactional(timeout = 5)
    // Rolls back if this method doesn't complete within 5 seconds
    // Throws TransactionTimedOutException
    // Default is -1 (no timeout)
    // USE CASE: prevent long-running transactions from locking DB rows
    public void timeoutExample(Long id) {
        Order order = orderRepository.findById(id).orElseThrow();
        slowExternalCall(); // if this takes > 5s → rollback
        orderRepository.save(order);
    }


    // =========================================================
    // ⚠️ GOTCHAS SUMMARY (in code)
    // =========================================================

    // ❌ GOTCHA 1: Self-invocation — internal calls bypass the proxy
    public void placeOrderPublic() {
        processOrderInternal(); // ❌ @Transactional on this is IGNORED
    }
    @Transactional
    public void processOrderInternal() {
        // This transaction never starts because proxy was bypassed
        // Fix: move processOrderInternal() to a separate @Service class
    }

    // ❌ GOTCHA 2: Private methods — proxy can't intercept them
    @Transactional
    private void privateMethod() {
        // @Transactional has NO effect here
        // Fix: always make @Transactional methods public
    }

    // ❌ GOTCHA 3: @Transactional on Repository — don't do this yourself
    // Spring Data JPA already adds @Transactional on all repository methods
    // Adding it again on service is fine (joins existing), but redundant on repo
    // Rule: Put @Transactional on SERVICE layer, not on repositories manually

    // ❌ GOTCHA 4: Mixing checked exception handling with rollback
    @Transactional
    public void gotcha4(Order order) throws Exception {
        orderRepository.save(order);
        throw new IOException("disk full"); // ❌ checked exception → NO rollback
        // Fix: use rollbackFor = Exception.class
    }

    // =========================================================
    // HOW TRANSACTION LIFECYCLE WORKS (step by step)
    // =========================================================

    // When you call a @Transactional method, Spring does this:
    //
    // 1. Proxy intercepts the method call
    // 2. TransactionManager checks propagation rules
    // 3. Opens DB connection from connection pool
    // 4. Sets autoCommit = false on the connection
    // 5. Binds connection to current thread (ThreadLocal)
    // 6. Executes your method
    // 7a. If SUCCESS → flush changes → commit → release connection
    // 7b. If EXCEPTION → rollback → release connection
    //
    // All DB calls within the method reuse the SAME connection (from ThreadLocal)
    // That's how they all participate in the same transaction


    // =========================================================
    // DIRTY CHECKING — how Hibernate auto-detects changes
    // =========================================================

    @Transactional
    public void dirtyCheckingExample(Long id) {
        Order order = orderRepository.findById(id).orElseThrow();
        // At this point, Hibernate takes a SNAPSHOT of the order object

        order.setStatus("SHIPPED"); // you just changed the entity in memory

        // No .save() called — but Hibernate compares snapshot vs current state
        // on commit it detects: "status changed!" → fires UPDATE SQL automatically

        // This is DIRTY CHECKING
        // Works ONLY inside a transaction (session must be open)
        // readOnly=true disables dirty checking (no snapshot taken → faster)
    }


    // =========================================================
    // MULTIPLE DATASOURCES — which transaction manager to use
    // =========================================================

    @Transactional("primaryTransactionManager")
    // When you have multiple DBs, specify which transaction manager to use
    // Default @Transactional picks the primary/default one
    public void saveToMainDB(Order order) {
        orderRepository.save(order);
    }

    @Transactional("secondaryTransactionManager")
    // Uses a different datasource/transaction manager
    public void saveToReportingDB(Order order) {
        // saves to secondary DB
    }

    // In your config:
    // @Bean("primaryTransactionManager")
    // public PlatformTransactionManager primaryTransactionManager(...) { ... }
    //
    // @Bean("secondaryTransactionManager")
    // public PlatformTransactionManager secondaryTransactionManager(...) { ... }


    // =========================================================
    // PROGRAMMATIC TRANSACTION — when you need fine-grained control
    // =========================================================

    // Sometimes @Transactional annotation is not flexible enough.
    // Use TransactionTemplate for programmatic control:

    // @Autowired
    // private TransactionTemplate transactionTemplate;

    public Order programmaticTransaction(Order order) {
        return transactionTemplate.execute(status -> {
            // status.setRollbackOnly() → marks for rollback without throwing
            try {
                return orderRepository.save(order);
            } catch (Exception e) {
                status.setRollbackOnly(); // ← manual rollback trigger
                return null;
            }
        });
        // USE CASE: rollback based on business logic, not just exceptions
        // USE CASE: transaction spans only part of a method
    }


    // =========================================================
    // CHECK IF TRANSACTION IS ACTIVE (useful for debugging)
    // =========================================================

    public void checkTransaction() {
        boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
        String txName  = TransactionSynchronizationManager.getCurrentTransactionName();
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();

        // Useful for:
        // → debugging transaction boundaries
        // → asserting in unit tests that a transaction exists
        // → conditional logic based on transaction state
    }


    // =========================================================
    // @EnableTransactionManagement — how Spring activates all of this
    // =========================================================

    // In your main config or @SpringBootApplication:
    // @EnableTransactionManagement  ← activates proxy-based transaction handling
    //
    // Spring Boot AUTO-CONFIGURES this — you don't need to add it manually
    // BUT: if you're using plain Spring (not Boot), you MUST add it yourself
    //
    // @Configuration
    // @EnableTransactionManagement
    // public class AppConfig { ... }

    // =========================================================
}

// =============================================================
// QUICK REFERENCE
// =============================================================

// PROPERTY         | DEFAULT          | USE FOR
// -----------------|------------------|---------------------------
// readOnly         | false            | performance on reads
// rollbackFor      | RuntimeException | what triggers rollback
// noRollbackFor    | (none)           | what skips rollback
// propagation      | REQUIRED         | how txns interact
// isolation        | DEFAULT (DB)     | concurrency / concurrent reads
// timeout          | -1 (none)        | prevent long-running txns

// PROPAGATION      | BEHAVIOR
// -----------------|-------------------------------------------
// REQUIRED         | join existing txn OR create new txn (default)
// REQUIRES_NEW     | always new txn, suspend existing txn(audit logs)
// NESTED           | savepoint inside existing txn (partial rollback)
// SUPPORTS         | use existing txn if present, else run non-transactional
// MANDATORY        | must have existing txn, else throws
// NEVER            | must NOT have txn, else throws
// NOT_SUPPORTED    | suspend existing txn, run without txn

// ISOLATION        | PREVENTS
// -----------------|-------------------------------------------
// READ_UNCOMMITTED | nothing (fastest, unsafe)
// READ_COMMITTED   | Dirty Read
// REPEATABLE_READ  | Dirty Read + Non-Repeatable Read
// SERIALIZABLE     | all 3 problems (slowest)




// -- What happens to Rollback when we have GlobalExceptionHandler (Start) ---//
/*
Case 1: RuntimeException occurs and is NOT caught inside method

@Transactional
public void save() {
    repo.save(a);
    throw new RuntimeException("failed");
}

Result:
- Transaction rolls back
- Data is not saved
- Global exception handler catches exception later
*/


/*
Case 2: RuntimeException occurs and IS caught inside method

@Transactional
public void save() {
    repo.save(a);

    try {
        throw new RuntimeException("failed");
    } catch (Exception e) {
        log.error("error");
    }
}

Result:
- Transaction commits
- repo.save(a) remains in DB
- Because exception never went outside method
*/


/*
Case 3: RuntimeException occurs, caught and rethrown

@Transactional
public void save() {
    repo.save(a);

    try {
        throw new RuntimeException("failed");
    } catch (Exception e) {
        throw e;
    }
}

Result:
- Transaction rolls back
- Data is not saved
- Global exception handler catches exception
*/


/*
Case 4: RuntimeException occurs, caught but rollback manually marked

@Transactional
public void save() {
    repo.save(a);

    try {
        throw new RuntimeException("failed");
    } catch (Exception e) {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }
}

Result:
- Transaction rolls back
- Data is not saved
- Method may still return normally
*/


/*
Global exception handler does NOT control rollback.

Rollback decision happens inside transactional proxy
before controller advice / exception handler executes.
*/

//-- What happens to Rollback when we have GlobalExceptionHandler (End) ---//