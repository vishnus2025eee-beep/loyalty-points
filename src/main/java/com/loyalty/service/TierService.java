package com.loyalty.service;

import com.loyalty.dto.TierDto;
import com.loyalty.entity.Tier;
import com.loyalty.repository.TierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TierService {

    private final TierRepository tierRepository;

    @Cacheable("tiers")
    @Transactional(readOnly = true)
    public List<TierDto> getAllTiers() {
        return tierRepository.findAllByOrderByMinLifetimePointsAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Tier> getAllTierEntities() {
        return tierRepository.findAllByOrderByMinLifetimePointsAsc();
    }

    @Transactional(readOnly = true)
    public Tier getBaseTier() {
        return tierRepository.findTopByMinLifetimePointsLessThanEqualOrderByMinLifetimePointsDesc(0L)
                .orElseGet(() -> tierRepository.findAllByOrderByMinLifetimePointsAsc().stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("Base tier not initialized")));
    }

    @Transactional(readOnly = true)
    public Tier getTierForPoints(Long lifetimePoints) {
        return tierRepository.findTopByMinLifetimePointsLessThanEqualOrderByMinLifetimePointsDesc(lifetimePoints)
                .orElseGet(this::getBaseTier);
    }

    public TierDto toDto(Tier tier) {
        return new TierDto(
                tier.getId(),
                tier.getName(),
                tier.getMinLifetimePoints(),
                tier.getDiscountPercent()
        );
    }
}
