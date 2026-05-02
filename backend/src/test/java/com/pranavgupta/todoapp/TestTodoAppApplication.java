package com.pranavgupta.todoapp;

import org.springframework.boot.SpringApplication;

public class TestTodoAppApplication {

	public static void main(String[] args) {
		// This entrypoint uses Testcontainers for the DataSource, so we must
		// disable docker-compose integration to avoid double-provisioning Postgres.
		System.setProperty("spring.docker.compose.enabled", "false");
		SpringApplication.from(TodoAppApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
