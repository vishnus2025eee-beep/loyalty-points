package com.loyalty.repository;

import com.loyalty.entity.Tier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TierRepository extends JpaRepository<Tier, Long> {
    Optional<Tier> findByName(String name);
    List<Tier> findAllByOrderByMinLifetimePointsAsc();
    Optional<Tier> findTopByMinLifetimePointsLessThanEqualOrderByMinLifetimePointsDesc(Long minLifetimePoints);
}
