package com.placement.portal.neo4j.config;

import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.config.EnableNeo4jAuditing;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@org.springframework.context.annotation.Configuration
@Profile("neo4j")
@EnableNeo4jRepositories(basePackages = "com.placement.portal.neo4j.repository")
@EntityScan(basePackages = "com.placement.portal.neo4j.model")
public class Neo4jConfig {

    @Bean
    public Configuration cypherDslConfiguration() {
        return Configuration.newConfig()
                .withDialect(Dialect.NEO4J_5)
                .build();
    }

    @Bean
    public org.springframework.data.neo4j.core.convert.Neo4jConversions neo4jConversions() {
        java.util.List<org.springframework.core.convert.converter.Converter<?, ?>> converters = new java.util.ArrayList<>();
        converters.add(new org.springframework.core.convert.converter.Converter<Double, java.math.BigDecimal>() {
            @Override
            public java.math.BigDecimal convert(Double source) {
                return source != null ? java.math.BigDecimal.valueOf(source) : null;
            }
        });
        converters.add(new org.springframework.core.convert.converter.Converter<java.math.BigDecimal, Double>() {
            @Override
            public Double convert(java.math.BigDecimal source) {
                return source != null ? source.doubleValue() : null;
            }
        });
        converters.add(new org.springframework.core.convert.converter.Converter<org.neo4j.driver.Value, java.math.BigDecimal>() {
            @Override
            public java.math.BigDecimal convert(org.neo4j.driver.Value source) {
                if (source == null || source.isNull()) return null;
                try {
                    return java.math.BigDecimal.valueOf(source.asDouble());
                } catch (Exception e) {
                    return new java.math.BigDecimal(source.asString());
                }
            }
        });
        converters.add(new org.springframework.core.convert.converter.Converter<java.time.ZonedDateTime, java.time.LocalDateTime>() {
            @Override
            public java.time.LocalDateTime convert(java.time.ZonedDateTime source) {
                return source != null ? source.toLocalDateTime() : null;
            }
        });
        converters.add(new org.springframework.core.convert.converter.Converter<org.neo4j.driver.Value, java.time.LocalDateTime>() {
            @Override
            public java.time.LocalDateTime convert(org.neo4j.driver.Value source) {
                if (source == null || source.isNull()) return null;
                try {
                    return source.asZonedDateTime().toLocalDateTime();
                } catch (Exception e) {
                    try {
                        return source.asLocalDateTime();
                    } catch (Exception ex) {
                        return java.time.LocalDateTime.parse(source.asString().replace("Z", ""));
                    }
                }
            }
        });
        return new org.springframework.data.neo4j.core.convert.Neo4jConversions(converters);
    }
}
