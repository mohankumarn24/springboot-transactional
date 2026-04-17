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
     * 1️. READ_COMMITTED (PostgreSQL default)
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
     * 
     * Social Media Feeds: The industry standard; you only see posts that have been finalized and "committed" by other users.
     * Bank Account History: Ensuring that when you refresh your app, you don't see a "ghost" transaction that was eventually cancelled or failed.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void readCommittedExample() {
        innerService.readData();
    }


    /*
     * =========================================
     * 2️. REPEATABLE_READ
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
     * 
     * Generating Reports: Ensuring that if you read a user's balance at the start of a multi-step audit, it stays the same until the end of that audit.
     * End-of-Month Billing: Locking the "current" price of a subscription while a 30-second billing script calculates taxes and discounts so the price doesn't change mid-calculation.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void repeatableReadExample() {
        innerService.readData();
    }


    /*
     * =========================================
     * 3️. SERIALIZABLE
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
     * 
     * - SERIALIZABLE ensures that the transaction behaves as if it has exclusive access to the tables it's reading/writing.
     * 
     * Inventory Management: Preventing two people from buying the last bag of Cheetos at the exact same millisecond (The Gopuff Solution).
     * Ticket Reservations: Preventing two different fans from picking the exact same front-row seat at a concert (similar to the Ticketmaster problem).
     * 
     * Mandatory: The database will actively kill one of two conflicting transactions. Your code needs to handle the TransactionSystemException or OptimisticLockingFailureException.
     */
    /*
    @Retryable(
    	    value = {org.springframework.dao.ConcurrencyFailureException.class}, 
    	    maxAttempts = 3, 
    	    backoff = @Backoff(delay = 100)
    	)
    */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void serializableExample() {
        innerService.readData();
    }


    /*
     * =========================================
     * 4️. READ_UNCOMMITTED
     * =========================================
     *
     * - Allows dirty reads (theoretically).
     * - Not supported by PostgreSQL
     *
     * BUT:
     * PostgreSQL treats it as READ_COMMITTED.
     * 
     * Log Analysis: Viewing a live count of non-critical data (like page views) where speed is more important than 100% accuracy.
     * Internal Dashboards: Showing an "approximate" count of active users on a website where a 1-2% margin of error doesn't matter.
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void readUncommittedExample() {
        innerService.readData();
    }
}


/*
Scenario: Two Users vs. One Remaining Item
Imagine there is only 1 bag of Cheetos left at DC_NYC. Both User A and User B click "Place Order" at the exact same millisecond.

1. From HTTP to Database
Request A: Hits Server Instance 1 -> OrdersService.placeOrder() is called -> Opens DB Connection 1.
Request B: Hits Server Instance 2 -> OrdersService.placeOrder() is called -> Opens DB Connection 2.

| --------- | -------------------------------------- | -------------------------------------------------------------------------------- |
| Step      | User A (Transaction 1)                 | User B (Transaction 2)                                                           |
| --------- | -------------------------------------- | -------------------------------------------------------------------------------- |
| 1. Start  | Starts SERIALIZABLE transaction.       | Starts SERIALIZABLE transaction.                                                 |
| 2. Read   | Reads `quantity = 1`.                  | Reads `quantity = 1`.                                                            |
| 3. Logic  | Sees `1 > 0`, so proceeds with order.  | Sees `1 > 0`, so proceeds with order.                                            |
| 4. Write  | Updates `quantity = 0`.                | Also tries to update `quantity = 0`.                                             |
| 5. Commit | Commits first. PostgreSQL allows it.   | Tries to commit after User A.                                                    |
| 6. Result | Order succeeds. Inventory becomes `0`. | PostgreSQL detects conflict and throws SQLState `40001` (serialization failure). |
| 7. Retry  | No retry needed.                       | Application should retry transaction automatically.                              |
| --------- | -------------------------------------- | -------------------------------------------------------------------------------- |

The Final User Experience
 - User A's HTTP Request: Receives a 200 OK (Order Placed!).
 - User B's HTTP Request: The Orders Service catches the DB error. If the Retry logic fails (because the stock is now truly 0), User B receives a 409 Conflict or 400 Bad Request (Out of Stock).

How SERIALIZABLE Solves the Problem
In a lower isolation level (like READ COMMITTED), User B might succeed, resulting in -1 bags of Cheetos (an "oversell").

Another Example: 
 - 5 items in stock
 - UserA & userB both places order for 3 items each, exactly at the same millisecond
 - UserA succeeds and then stock updated to 2
 - UserB order fails and gets message "Sorry, only 2 bags remaining. Would you like to adjust your order?"" 
*/


/*
 * import org.springframework.dao.CannotAcquireLockException;
 * import org.springframework.dao.ConcurrencyFailureException;
 * import org.springframework.dao.PessimisticLockingFailureException;
 * import org.springframework.retry.annotation.Backoff;
 * import org.springframework.retry.annotation.Retryable;
 * import org.springframework.stereotype.Service;
 * import org.springframework.transaction.annotation.Isolation;
 * import org.springframework.transaction.annotation.Transactional;
 * 
 * @Service
 * public class OrderService {
 * 
 *     private final InventoryRepository inventoryRepository;
 *     private final OrderRepository orderRepository;
 * 
 *     public OrderService(InventoryRepository inventoryRepository, OrderRepository orderRepository) {
 *         this.inventoryRepository = inventoryRepository;
 *         this.orderRepository = orderRepository;
 *     }
 * 
 *     @Retryable(
 *         value = {
 *             CannotAcquireLockException.class,
 *             ConcurrencyFailureException.class,
 *             PessimisticLockingFailureException.class
 *         },
 *         maxAttempts = 3,
 *         backoff = @Backoff(delay = 100)
 *     )
 *     @Transactional(isolation = Isolation.SERIALIZABLE)
 *     public OrderResponse placeOrder(OrderRequest request) {
 *         for (ItemRequest item : request.getItems()) {
 *             // 1. Read current inventory
 *             Inventory inventory = inventoryRepository.findByItemIdAndDcId(item.getItemId(), item.getDcId())
 *                     .orElseThrow(() -> new OutOfStockException("Item not found: " + item.getItemId()));
 * 
 *             // 2. Validate available stock
 *             if (inventory.getQuantity() < item.getQuantity()) {
 *                 throw new OutOfStockException("Insufficient stock for item: " + item.getItemId()
 *                 );
 *             }
 * 
 *             // 3. Decrement inventory
 *             inventory.setQuantity(inventory.getQuantity() - item.getQuantity()
 *             );
 * 
 *             inventoryRepository.save(inventory);
 *         }
 * 
 *         // 4. Create and save order
 *         Order order = new Order(request.getCustomerId(), request.getItems());
 *         Order savedOrder = orderRepository.save(order);
 *         return OrderMapper.toResponse(savedOrder);
 *     }
 * }
*/
