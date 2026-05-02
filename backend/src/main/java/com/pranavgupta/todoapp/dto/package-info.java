/**
 * Request and response DTOs — the wire shapes of the REST API. All DTOs are
 * Java {@code record}s for immutability and minimal boilerplate, with
 * {@code jakarta.validation} annotations applied to fields that need
 * server-side validation.
 *
 * <p>DTOs live at the top level (rather than nested under {@code controller/})
 * so the OpenAPI generator (Phase 8) and the future TypeScript client generator
 * (Phase 11) can locate them as a stable, contract-shaped package.</p>
 */
package com.pranavgupta.todoapp.dto;
