package com.placement.portal.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<PortalUser, String> {
    Optional<PortalUser> findByUsername(String username);
    Optional<PortalUser> findByUsernameIgnoreCase(String username);
    Optional<PortalUser> findByEmailIgnoreCase(String email);
    Optional<PortalUser> findByFullNameIgnoreCase(String fullName);
    Optional<PortalUser> findByReferenceId(String referenceId);
    boolean existsByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
}
