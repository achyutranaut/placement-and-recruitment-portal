package com.placement.portal.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Profile({"oracle", "h2", "default", "!neo4j"})
public class JpaUserContextService implements IUserContextService {

    private final UserRepository userRepository;

    public JpaUserContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<PortalUserDto> findByUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByUsernameIgnoreCase(username))
                .or(() -> userRepository.findByEmailIgnoreCase(username))
                .map(this::toDto);
    }

    @Override
    public Optional<PortalUserDto> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) return Optional.empty();
        return userRepository.findById(userId).map(this::toDto);
    }

    @Override
    public Optional<PortalUserDto> findByReferenceId(String referenceId) {
        if (referenceId == null || referenceId.isBlank()) return Optional.empty();
        return userRepository.findByReferenceId(referenceId).map(this::toDto);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByUserId(String userId) {
        return userRepository.existsById(userId);
    }

    private PortalUserDto toDto(PortalUser u) {
        return new PortalUserDto(
                u.getUserId(),
                u.getUsername(),
                u.getRole(),
                u.getReferenceId(),
                u.getFullName(),
                u.getEmail()
        );
    }
}
