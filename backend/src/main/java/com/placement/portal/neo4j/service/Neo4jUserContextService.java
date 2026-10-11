package com.placement.portal.neo4j.service;

import com.placement.portal.auth.IUserContextService;
import com.placement.portal.auth.PortalUserDto;
import com.placement.portal.neo4j.model.PortalUserNode;
import com.placement.portal.neo4j.repository.Neo4jUserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Profile("neo4j")
@Primary
public class Neo4jUserContextService implements IUserContextService {

    private final Neo4jUserRepository userRepository;

    public Neo4jUserContextService(Neo4jUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<PortalUserDto> findByUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        return userRepository.findByUsername(username).map(this::toDto);
    }

    @Override
    public Optional<PortalUserDto> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) return Optional.empty();
        return userRepository.findByUserId(userId).map(this::toDto);
    }

    @Override
    public Optional<PortalUserDto> findByReferenceId(String referenceId) {
        if (referenceId == null || referenceId.isBlank()) return Optional.empty();
        return userRepository.findAll().stream()
                .filter(u -> referenceId.equalsIgnoreCase(u.getReferenceId()))
                .findFirst()
                .map(this::toDto);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    @Override
    public boolean existsByUserId(String userId) {
        return userRepository.findByUserId(userId).isPresent();
    }

    private PortalUserDto toDto(PortalUserNode u) {
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
