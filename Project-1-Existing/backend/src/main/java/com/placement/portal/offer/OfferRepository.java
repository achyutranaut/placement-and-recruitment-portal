package com.placement.portal.offer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OfferRepository extends JpaRepository<OfferLetter, String> {
    Optional<OfferLetter> findByApplicationId(String applicationId);
    java.util.List<OfferLetter> findByApplicationIdIn(java.util.Collection<String> applicationIds);
}
