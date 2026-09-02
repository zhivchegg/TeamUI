package com.teamui.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

/**
 * Explicit Flyway bootstrap.
 *
 * <p>Spring Boot 4's auto-configuration does not reliably trigger migrations before the
 * embedded web server starts accepting traffic in this project, so we configure Flyway
 * manually and execute it as soon as the {@link DataSource} is available.</p>
 *
 * @author TeamUI
 * @since 0.0.1
 */
@Configuration
public class FlywayMigrationConfig {

    /**
     * Configures and runs Flyway against the application {@link DataSource}.
     *
     * <p>The returned {@link Flyway} bean is initialized immediately (it eagerly calls
     * {@code migrate()}), guaranteeing that the schema is created before any HTTP request
     * reaches a repository.</p>
     *
     * @param dataSource the primary data source
     * @return configured Flyway instance with migrations applied
     */
    @Bean(initMethod = "migrate")
    @DependsOn("dataSource")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }
}
