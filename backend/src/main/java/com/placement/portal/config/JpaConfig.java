package com.placement.portal.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile({"oracle", "h2", "default", "!neo4j"})
@EnableJpaRepositories(basePackages = {
        "com.placement.portal.student",
        "com.placement.portal.company",
        "com.placement.portal.recruitment",
        "com.placement.portal.program",
        "com.placement.portal.application",
        "com.placement.portal.interview",
        "com.placement.portal.offer",
        "com.placement.portal.auth"
})
@EntityScan(basePackages = {
        "com.placement.portal.student",
        "com.placement.portal.company",
        "com.placement.portal.recruitment",
        "com.placement.portal.program",
        "com.placement.portal.application",
        "com.placement.portal.interview",
        "com.placement.portal.offer",
        "com.placement.portal.auth"
})
public class JpaConfig {
}
