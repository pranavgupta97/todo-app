package com.pranavgupta.todoapp.service;

import com.pranavgupta.todoapp.domain.Todo;
import com.pranavgupta.todoapp.dto.TodoStatusFilter;
import com.pranavgupta.todoapp.exception.TodoNotFoundException;
import com.pranavgupta.todoapp.repository.TodoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.pranavgupta.todoapp.test.TestTodos.todo;
import static com.pranavgupta.todoapp.test.TestTodos.transientTodo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TodoService}. Pure JUnit 5 + Mockito — no Spring
 * context, no DB. Exhaustively covers branching/business logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    private static final Long USER_ID = 42L;

    @Mock
    private TodoRepository todoRepository;

    @InjectMocks
    private TodoService todoService;

    // -------------------------------------------------------------------- //
    //  findAllForUser                                                      //
    // -------------------------------------------------------------------- //

    @Test
    void findAllForUser_withAllFilter_callsUnfilteredRepoMethod() {
        when(todoRepository.findAllByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(transientTodo(USER_ID, "a"), transientTodo(USER_ID, "b")));

        List<Todo> result = todoService.findAllForUser(USER_ID, TodoStatusFilter.ALL);

        assertThat(result).extracting(Todo::getTitle).containsExactly("a", "b");
        verify(todoRepository).findAllByUserIdOrderByCreatedAtDesc(USER_ID);
        verify(todoRepository, never()).findAllByUserIdAndCompletedOrderByCreatedAtDesc(any(), anyBoolean());
    }

    @Test
    void findAllForUser_withActiveFilter_passesCompletedFalse() {
        when(todoRepository.findAllByUserIdAndCompletedOrderByCreatedAtDesc(USER_ID, false))
                .thenReturn(List.of(transientTodo(USER_ID, "active")));

        List<Todo> result = todoService.findAllForUser(USER_ID, TodoStatusFilter.ACTIVE);

        assertThat(result).extracting(Todo::getTitle).containsExactly("active");
        verify(todoRepository).findAllByUserIdAndCompletedOrderByCreatedAtDesc(USER_ID, false);
    }

    @Test
    void findAllForUser_withCompletedFilter_passesCompletedTrue() {
        when(todoRepository.findAllByUserIdAndCompletedOrderByCreatedAtDesc(USER_ID, true))
                .thenReturn(List.of(transientTodo(USER_ID, "done")));

        List<Todo> result = todoService.findAllForUser(USER_ID, TodoStatusFilter.COMPLETED);

        assertThat(result).extracting(Todo::getTitle).containsExactly("done");
        verify(todoRepository).findAllByUserIdAndCompletedOrderByCreatedAtDesc(USER_ID, true);
    }

    // -------------------------------------------------------------------- //
    //  findByIdForUser                                                     //
    // -------------------------------------------------------------------- //

    @Test
    void findByIdForUser_whenTodoExistsAndOwned_returnsTodo() {
        Todo owned = todo(1L, USER_ID, "owned");
        when(todoRepository.findById(1L)).thenReturn(Optional.of(owned));

        Todo result = todoService.findByIdForUser(USER_ID, 1L);

        assertThat(result).isSameAs(owned);
    }

    @Test
    void findByIdForUser_whenTodoMissing_throwsTodoNotFound() {
        when(todoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.findByIdForUser(USER_ID, 99L))
                .isInstanceOf(TodoNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findByIdForUser_whenTodoExistsButOwnedByOtherUser_throwsTodoNotFound() {
        // Marquee security assertion at the unit level: a foreign-owned todo
        // is not visible — it raises the same 404 as a missing one, by design,
        // to avoid leaking the existence of other users' rows.
        Todo otherUsers = todo(1L, USER_ID + 1, "not yours");
        when(todoRepository.findById(1L)).thenReturn(Optional.of(otherUsers));

        assertThatThrownBy(() -> todoService.findByIdForUser(USER_ID, 1L))
                .isInstanceOf(TodoNotFoundException.class);
    }

    // -------------------------------------------------------------------- //
    //  createForUser                                                       //
    // -------------------------------------------------------------------- //

    @Test
    void createForUser_savesTodoWithTitleAndUserIdAndCompletedFalse() {
        when(todoRepository.save(any(Todo.class))).thenAnswer(inv -> inv.getArgument(0));

        Todo result = todoService.createForUser(USER_ID, "Buy milk");

        ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).save(captor.capture());
        Todo saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Buy milk");
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.isCompleted()).isFalse();
        assertThat(result).isSameAs(saved);
    }

    // -------------------------------------------------------------------- //
    //  updateForUser                                                       //
    // -------------------------------------------------------------------- //

    @Test
    void updateForUser_titleOnly_changesTitle_preservesCompleted() {
        Todo existing = todo(1L, USER_ID, "old", true);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(existing));

        Todo result = todoService.updateForUser(USER_ID, 1L, "new", null);

        assertThat(result.getTitle()).isEqualTo("new");
        assertThat(result.isCompleted()).isTrue();
    }

    @Test
    void updateForUser_completedOnly_changesCompleted_preservesTitle() {
        Todo existing = todo(1L, USER_ID, "kept", false);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(existing));

        Todo result = todoService.updateForUser(USER_ID, 1L, null, true);

        assertThat(result.getTitle()).isEqualTo("kept");
        assertThat(result.isCompleted()).isTrue();
    }

    @Test
    void updateForUser_bothFields_changesBoth() {
        Todo existing = todo(1L, USER_ID, "a", false);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(existing));

        Todo result = todoService.updateForUser(USER_ID, 1L, "b", true);

        assertThat(result.getTitle()).isEqualTo("b");
        assertThat(result.isCompleted()).isTrue();
    }

    @Test
    void updateForUser_bothNull_makesNoChanges() {
        // Edge case: PATCH with empty body is a no-op (after validation passes)
        Todo existing = todo(1L, USER_ID, "untouched", true);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(existing));

        Todo result = todoService.updateForUser(USER_ID, 1L, null, null);

        assertThat(result.getTitle()).isEqualTo("untouched");
        assertThat(result.isCompleted()).isTrue();
    }

    @Test
    void updateForUser_missingTodo_throws() {
        when(todoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.updateForUser(USER_ID, 99L, "x", true))
                .isInstanceOf(TodoNotFoundException.class);
    }

    @Test
    void updateForUser_otherUsersTodo_throws() {
        Todo theirs = todo(1L, USER_ID + 1, "theirs", false);
        when(todoRepository.findById(1L)).thenReturn(Optional.of(theirs));

        assertThatThrownBy(() -> todoService.updateForUser(USER_ID, 1L, "x", true))
                .isInstanceOf(TodoNotFoundException.class);
    }

    // -------------------------------------------------------------------- //
    //  deleteForUser                                                       //
    // -------------------------------------------------------------------- //

    @Test
    void deleteForUser_existingOwnedTodo_callsDelete() {
        Todo owned = todo(1L, USER_ID, "delete me");
        when(todoRepository.findById(1L)).thenReturn(Optional.of(owned));

        todoService.deleteForUser(USER_ID, 1L);

        verify(todoRepository).delete(owned);
    }

    @Test
    void deleteForUser_missingTodo_throwsAndDoesNotCallDelete() {
        when(todoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> todoService.deleteForUser(USER_ID, 99L))
                .isInstanceOf(TodoNotFoundException.class);
        verify(todoRepository, never()).delete(any());
    }

    @Test
    void deleteForUser_otherUsersTodo_throwsAndDoesNotCallDelete() {
        Todo theirs = todo(1L, USER_ID + 1, "not yours");
        when(todoRepository.findById(1L)).thenReturn(Optional.of(theirs));

        assertThatThrownBy(() -> todoService.deleteForUser(USER_ID, 1L))
                .isInstanceOf(TodoNotFoundException.class);
        verify(todoRepository, never()).delete(any());
    }
}
