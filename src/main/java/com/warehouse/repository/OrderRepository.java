package com.warehouse.repository;

import com.warehouse.entity.Order;
import com.warehouse.entity.User;
import com.warehouse.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByClientOrderByCreatedAtDesc(User client);

    List<Order> findByClientAndStatusOrderByCreatedAtDesc(User client, OrderStatus status);

    @Query("SELECT o FROM Order o ORDER BY o.submittedDate DESC NULLS LAST, o.createdAt DESC")
    List<Order> findAllSortedBySubmittedDate();

    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.submittedDate DESC NULLS LAST, o.createdAt DESC")
    List<Order> findByStatusSortedBySubmittedDate(@Param("status") OrderStatus status);

    Optional<Order> findByIdAndClient(Long id, User client);
}
