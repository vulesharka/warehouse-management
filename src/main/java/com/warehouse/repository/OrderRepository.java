package com.warehouse.repository;

import com.warehouse.entity.Order;
import com.warehouse.entity.User;
import com.warehouse.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByClientOrderByCreatedAtDesc(User client, Pageable pageable);

    Page<Order> findByClientAndStatusOrderByCreatedAtDesc(User client, OrderStatus status, Pageable pageable);

    @Query(value = "SELECT o FROM Order o JOIN FETCH o.client ORDER BY o.submittedDate DESC NULLS LAST, o.createdAt DESC",
           countQuery = "SELECT COUNT(o) FROM Order o")
    Page<Order> findAllSortedBySubmittedDate(Pageable pageable);

    @Query(value = "SELECT o FROM Order o JOIN FETCH o.client WHERE o.status = :status ORDER BY o.submittedDate DESC NULLS LAST, o.createdAt DESC",
           countQuery = "SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    Page<Order> findByStatusSortedBySubmittedDate(@Param("status") OrderStatus status, Pageable pageable);

    Optional<Order> findByIdAndClient(Long id, User client);

    boolean existsByClientId(Long clientId);
}
