package com.loyalty.repository;

import com.loyalty.entity.PointsLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointsLedgerRepository extends JpaRepository<PointsLedger, Long> {
    Page<PointsLedger> findByCustomerId(Long customerId, Pageable pageable);
    List<PointsLedger> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
