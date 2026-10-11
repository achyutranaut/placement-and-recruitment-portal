package com.placement.portal.auth;

import java.util.Optional;

public interface IUserContextService {
    Optional<PortalUserDto> findByUsername(String username);
    Optional<PortalUserDto> findByUserId(String userId);
    Optional<PortalUserDto> findByReferenceId(String referenceId);
    boolean existsByUsername(String username);
    boolean existsByUserId(String userId);
}
