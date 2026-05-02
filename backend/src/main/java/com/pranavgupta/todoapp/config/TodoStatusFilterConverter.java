package com.pranavgupta.todoapp.config;

import com.pranavgupta.todoapp.dto.TodoStatusFilter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Allows {@code ?status=all|active|completed} (case-insensitive) on REST
 * endpoints. Without this, Spring's default enum binding would require
 * exact-case uppercase ({@code ?status=ALL}) and reject anything else.
 *
 * <p>Spring MVC discovers {@link Converter} beans automatically and adds them
 * to its {@code ConversionService} — no further wiring needed.</p>
 */
@Component
public class TodoStatusFilterConverter implements Converter<String, TodoStatusFilter> {

    @Override
    public TodoStatusFilter convert(String source) {
        return TodoStatusFilter.from(source);
    }
}
