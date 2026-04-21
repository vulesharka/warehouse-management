package com.warehouse.scheduler;

import com.warehouse.entity.Delivery;
import com.warehouse.enums.OrderStatus;
import com.warehouse.repository.DeliveryRepository;
import com.warehouse.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class DeliveryScheduler {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

//    @Scheduled(cron = "0 0 0 * * *")
@Scheduled(cron = "*/10 * * * * *")
@Transactional
    public void fulfillDueDeliveries() {
        List<Delivery> due = deliveryRepository.findDueDeliveries(LocalDate.now());
        if (due.isEmpty()) return;

        for (Delivery delivery : due) {
            delivery.getOrder().setStatus(OrderStatus.FULFILLED);
            orderRepository.save(delivery.getOrder());
        }
        log.info("Fulfilled {} order(s) with past delivery dates", due.size());
    }
}
