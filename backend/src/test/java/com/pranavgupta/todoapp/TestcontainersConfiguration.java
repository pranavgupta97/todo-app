package com.pranavgupta.todoapp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Provides the Postgres {@link PostgreSQLContainer} used by tests in any
 * package — {@code @Import(TestcontainersConfiguration.class)} on
 * {@code @SpringBootTest} or {@code @DataJpaTest} classes wires the container
 * as the active datasource via {@code @ServiceConnection}.
 *
 * <p>Was package-private from Initializr; promoted to {@code public} so tests
 * in {@code repository/}, {@code integration/}, etc. (Phase 7) can import it.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
	}

}
