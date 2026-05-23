package com.example.demo.service;

import com.example.demo.entity.Order;
import com.example.demo.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;


/**
 * Database isolation vs JPA/Hibernate first-level cache (Persistence Context)
 */
@Service
@RequiredArgsConstructor
public class transactional_complete2 {

    private final OrderRepository orderRepository;
    private final EntityManager entityManager;

    /*
        INITIAL DB VALUE:
        -----------------
        id = 1
        status = "NEW"


        ASSUME:
        -------
        After first read,
        another transaction updates status to:

        status = "PAID"
        and commits.
     */


    // =========================================================
    // 1) READ_COMMITTED + NO detach()
    // =========================================================

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void readCommittedWithoutDetach(Long id) {

        // DB HIT
        Order order1 = orderRepository.findById(id).orElseThrow();

        System.out.println("First Read  : " + order1.getStatus());

        // -------------------------------------------------
        // another transaction:
        //
        // UPDATE orders SET status='PAID' WHERE id=1;
        // COMMIT;
        // -------------------------------------------------

        // NO DB HIT
        // Returned from Hibernate L1 cache
        Order order2 = orderRepository.findById(id).orElseThrow();

        System.out.println("Second Read : " + order2.getStatus());

        /*
            OUTPUT:
            -------
            First Read  : NEW
            Second Read : NEW

            WHY?
            ----
            Hibernate returned cached entity.
            READ_COMMITTED never got chance to work.
         */
    }



    // =========================================================
    // 2) READ_COMMITTED + detach()
    // =========================================================

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void readCommittedWithDetach(Long id) {

        // DB HIT
        Order order1 = orderRepository.findById(id).orElseThrow();

        System.out.println("First Read  : " + order1.getStatus());

        // another transaction updates + commits

        // Remove entity from persistence context
        entityManager.detach(order1);

        // DB HIT AGAIN
        Order order2 = orderRepository.findById(id).orElseThrow();

        System.out.println("Second Read : " + order2.getStatus());

        /*
            OUTPUT:
            -------
            First Read  : NEW
            Second Read : PAID

            WHY?
            ----
            detach() removed entity from L1 cache.

            Second query hit DB again.

            READ_COMMITTED allows latest committed value.
         */
    }



    // =========================================================
    // 3) REPEATABLE_READ + NO detach()
    // =========================================================

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void repeatableReadWithoutDetach(Long id) {

        // DB HIT
        Order order1 = orderRepository.findById(id).orElseThrow();

        System.out.println("First Read  : " + order1.getStatus());

        // another transaction updates + commits

        // NO DB HIT
        // Returned from Hibernate L1 cache
        Order order2 = orderRepository.findById(id).orElseThrow();

        System.out.println("Second Read : " + order2.getStatus());

        /*
            OUTPUT:
            -------
            First Read  : NEW
            Second Read : NEW

            WHY?
            ----
            Hibernate returned cached entity.

            DB isolation was not even involved
            because second query never hit DB.
         */
    }



    // =========================================================
    // 4) REPEATABLE_READ + detach()
    // =========================================================

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void repeatableReadWithDetach(Long id) {

        // DB HIT
        Order order1 = orderRepository.findById(id).orElseThrow();

        System.out.println("First Read  : " + order1.getStatus());

        // another transaction updates + commits

        // Remove entity from L1 cache
        entityManager.detach(order1);

        // DB HIT AGAIN
        Order order2 = orderRepository.findById(id).orElseThrow();

        System.out.println("Second Read : " + order2.getStatus());

        /*
            OUTPUT:
            -------
            First Read  : NEW
            Second Read : NEW

            WHY?
            ----
            detach() forced second query to hit DB.

            BUT database isolation is REPEATABLE_READ.

            DB returns same transaction snapshot,
            not latest committed value.
         */
    }
}