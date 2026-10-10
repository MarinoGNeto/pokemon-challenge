package com.example.taskapi.task.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.example.taskapi.common.BusinessRuleViolationException;
import com.example.taskapi.security.ProblemDetailSecurityHandlers;
import com.example.taskapi.security.SecurityConfig;
import com.example.taskapi.task.TestData;
import com.example.taskapi.task.domain.Task;
import com.example.taskapi.task.domain.TaskStatus;
import com.example.taskapi.task.service.TaskCommands.ChangeStatus;
import com.example.taskapi.task.service.TaskCommands.CreateTask;
import com.example.taskapi.task.service.TaskCommands.TaskFilter;
import com.example.taskapi.task.service.TaskCommands.UpdateTask;
import com.example.taskapi.task.service.TaskNotFoundException;
import com.example.taskapi.task.service.TaskService;
import com.example.taskapi.task.service.TaskVersionConflictException;
import com.example.taskapi.user.domain.User;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, ProblemDetailSecurityHandlers.class})
class TaskControllerTest {

    private static final UUID CALLER_ID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID TASK_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TaskService taskService;

    private static RequestPostProcessor caller() {
        return jwt().jwt(j -> j.subject(CALLER_ID.toString()));
    }

    private static Task task(TaskStatus status, long version) {
        User owner = TestData.user(CALLER_ID, "alice");
        return TestData.task(owner, TASK_ID, version, "Write tests", status, LocalDate.of(2026, 7, 1), NOW);
    }

    // ---- 201 ------------------------------------------------------------------------

    @Nested
    class Create {

        @Test
        void returns201WithLocationAndBody() throws Exception {
            when(taskService.create(eq(CALLER_ID), any())).thenReturn(task(TaskStatus.TODO, 0));

            mvc.perform(post("/api/v1/tasks").with(caller())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title":"Write tests","dueDate":"2026-07-01"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/api/v1/tasks/" + TASK_ID))
                    .andExpect(jsonPath("$.id").value(TASK_ID.toString()))
                    .andExpect(jsonPath("$.title").value("Write tests"))
                    .andExpect(jsonPath("$.status").value("TODO"))
                    .andExpect(jsonPath("$.dueDate").value("2026-07-01"))
                    .andExpect(jsonPath("$.version").value(0))
                    .andExpect(jsonPath("$.owner").doesNotExist());
        }

        @Test
        void ownerInBodyIsIgnoredAndPrincipalIsUsed() throws Exception {
            when(taskService.create(eq(CALLER_ID), any())).thenReturn(task(TaskStatus.TODO, 0));

            mvc.perform(post("/api/v1/tasks").with(caller())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title":"t","ownerId":"99999999-9999-9999-9999-999999999999",
                                     "owner":{"id":"99999999-9999-9999-9999-999999999999"}}
                                    """))
                    .andExpect(status().isCreated());

            verify(taskService).create(CALLER_ID, new CreateTask("t", null, null, null));
        }

        @Test
        void returns400WithFieldErrorsOnInvalidBody() throws Exception {
            mvc.perform(post("/api/v1/tasks").with(caller())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"\",\"description\":\"" + "d".repeat(2001) + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("Validation failed"))
                    .andExpect(jsonPath("$.instance").value("/api/v1/tasks"))
                    .andExpect(jsonPath("$.errors", hasSize(2)))
                    .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("title", "description")));
            verifyNoInteractions(taskService);
        }

        @Test
        void returns400WhenTitleTooLong() throws Exception {
            mvc.perform(post("/api/v1/tasks").with(caller())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"" + "x".repeat(121) + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("title"));
        }

        @Test
        void returns400WithFieldErrorForBusinessRuleViolation() throws Exception {
            when(taskService.create(eq(CALLER_ID), any()))
                    .thenThrow(new BusinessRuleViolationException("dueDate", "must not be in the past"));

            mvc.perform(post("/api/v1/tasks").with(caller())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title":"t","dueDate":"2000-01-01"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[0].field").value("dueDate"))
                    .andExpect(jsonPath("$.errors[0].message").value("must not be in the past"));
        }

        @Test
        void returns400OnMalformedJson() throws Exception {
            mvc.perform(post("/api/v1/tasks").with(caller())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(PROBLEM_JSON));
        }

        @Test
        void returns400OnUnknownStatusValue() throws Exception {
            mvc.perform(post("/api/v1/tasks").with(caller())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"t\",\"status\":\"ARCHIVED\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ---- 401 ------------------------------------------------------------------------

    @Nested
    class Unauthorized {

        @Test
        void returns401WithoutToken() throws Exception {
            mvc.perform(get("/api/v1/tasks"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", "Bearer"))
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.title").value("Unauthorized"));
            verifyNoInteractions(taskService);
        }

        @Test
        void returns401WithInvalidToken() throws Exception {
            mvc.perform(post("/api/v1/tasks")
                            .header("Authorization", "Bearer not.a.valid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"t\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(PROBLEM_JSON));
            verifyNoInteractions(taskService);
        }

        @Test
        void returns401OnEveryTaskEndpointWithoutToken() throws Exception {
            mvc.perform(get("/api/v1/tasks/" + TASK_ID)).andExpect(status().isUnauthorized());
            mvc.perform(put("/api/v1/tasks/" + TASK_ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(patch("/api/v1/tasks/" + TASK_ID + "/status").contentType(MediaType.APPLICATION_JSON)
                    .content("{}")).andExpect(status().isUnauthorized());
            mvc.perform(delete("/api/v1/tasks/" + TASK_ID)).andExpect(status().isUnauthorized());
        }
    }

    // ---- 200 (list / get) -------------------------------------------------------------

    @Nested
    class Read {

        @Test
        void listReturns200WithPageEnvelopeAndPassesFiltersAndPaging() throws Exception {
            Pageable expectedPageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "dueDate"));
            when(taskService.list(eq(CALLER_ID), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(task(TaskStatus.TODO, 0)), expectedPageable, 6));

            mvc.perform(get("/api/v1/tasks").with(caller())
                            .param("status", "TODO")
                            .param("dueBefore", "2026-08-01")
                            .param("page", "1")
                            .param("size", "5")
                            .param("sort", "dueDate,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id").value(TASK_ID.toString()))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.totalElements").value(6))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.sort[0]").value("dueDate,asc"));

            ArgumentCaptor<TaskFilter> filter = ArgumentCaptor.forClass(TaskFilter.class);
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(taskService).list(eq(CALLER_ID), filter.capture(), pageable.capture());
            assertThat(filter.getValue()).isEqualTo(new TaskFilter(TaskStatus.TODO, LocalDate.of(2026, 8, 1)));
            assertThat(pageable.getValue()).isEqualTo(expectedPageable);
        }

        @Test
        void listDefaultsToFirstPageOf20SortedByCreatedAtDesc() throws Exception {
            when(taskService.list(eq(CALLER_ID), any(), any())).thenReturn(new PageImpl<>(List.of()));

            mvc.perform(get("/api/v1/tasks").with(caller())).andExpect(status().isOk());

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(taskService).list(eq(CALLER_ID), eq(new TaskFilter(null, null)), pageable.capture());
            assertThat(pageable.getValue())
                    .isEqualTo(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
        }

        @Test
        void listReturns400OnInvalidDueBefore() throws Exception {
            mvc.perform(get("/api/v1/tasks").with(caller()).param("dueBefore", "tomorrow"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(PROBLEM_JSON));
        }

        @Test
        void getReturns200() throws Exception {
            when(taskService.get(CALLER_ID, TASK_ID)).thenReturn(task(TaskStatus.IN_PROGRESS, 2));

            mvc.perform(get("/api/v1/tasks/{id}", TASK_ID).with(caller()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.version").value(2))
                    .andExpect(jsonPath("$.createdAt").value("2026-06-15T10:00:00Z"));
        }

        @Test
        void getReturns404ForMissingOrForeignTask() throws Exception {
            when(taskService.get(CALLER_ID, TASK_ID)).thenThrow(new TaskNotFoundException(TASK_ID));

            mvc.perform(get("/api/v1/tasks/{id}", TASK_ID).with(caller()))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Task " + TASK_ID + " not found"));
        }

        @Test
        void getReturns400OnMalformedId() throws Exception {
            mvc.perform(get("/api/v1/tasks/not-a-uuid").with(caller()))
                    .andExpect(status().isBadRequest());
        }
    }

    // ---- PUT ------------------------------------------------------------------------

    @Nested
    class Update {

        private static final String BODY = """
                {"title":"Updated","description":null,"status":"DONE","dueDate":"2026-07-01","version":3}
                """;

        @Test
        void returns200() throws Exception {
            when(taskService.update(eq(CALLER_ID), eq(TASK_ID), any())).thenReturn(task(TaskStatus.DONE, 4));

            mvc.perform(put("/api/v1/tasks/{id}", TASK_ID).with(caller())
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.version").value(4));

            verify(taskService).update(CALLER_ID, TASK_ID,
                    new UpdateTask("Updated", null, TaskStatus.DONE, LocalDate.of(2026, 7, 1), 3));
        }

        @Test
        void returns400WhenVersionOrStatusMissing() throws Exception {
            mvc.perform(put("/api/v1/tasks/{id}", TASK_ID).with(caller())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"t\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("status", "version")));
            verifyNoInteractions(taskService);
        }

        @Test
        void returns404ForMissingOrForeignTask() throws Exception {
            when(taskService.update(eq(CALLER_ID), eq(TASK_ID), any())).thenThrow(new TaskNotFoundException(TASK_ID));

            mvc.perform(put("/api/v1/tasks/{id}", TASK_ID).with(caller())
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns409OnStaleVersion() throws Exception {
            when(taskService.update(eq(CALLER_ID), eq(TASK_ID), any()))
                    .thenThrow(new TaskVersionConflictException(TASK_ID, 3, 4));

            mvc.perform(put("/api/v1/tasks/{id}", TASK_ID).with(caller())
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.title").value("Conflict"));
        }

        @Test
        void returns409OnConcurrentFlushConflict() throws Exception {
            when(taskService.update(eq(CALLER_ID), eq(TASK_ID), any()))
                    .thenThrow(new ObjectOptimisticLockingFailureException(Task.class, TASK_ID));

            mvc.perform(put("/api/v1/tasks/{id}", TASK_ID).with(caller())
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(PROBLEM_JSON));
        }
    }

    // ---- PATCH ----------------------------------------------------------------------

    @Nested
    class PatchStatus {

        @Test
        void returns200() throws Exception {
            when(taskService.changeStatus(CALLER_ID, TASK_ID, new ChangeStatus(TaskStatus.DONE, 1)))
                    .thenReturn(task(TaskStatus.DONE, 2));

            mvc.perform(patch("/api/v1/tasks/{id}/status", TASK_ID).with(caller())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"DONE\",\"version\":1}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DONE"))
                    .andExpect(jsonPath("$.version").value(2));
        }

        @Test
        void returns400WhenStatusMissing() throws Exception {
            mvc.perform(patch("/api/v1/tasks/{id}/status", TASK_ID).with(caller())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"version\":1}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("status"));
        }

        @Test
        void returns404ForMissingOrForeignTask() throws Exception {
            when(taskService.changeStatus(eq(CALLER_ID), eq(TASK_ID), any()))
                    .thenThrow(new TaskNotFoundException(TASK_ID));

            mvc.perform(patch("/api/v1/tasks/{id}/status", TASK_ID).with(caller())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"DONE\",\"version\":1}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns409OnStaleVersion() throws Exception {
            when(taskService.changeStatus(eq(CALLER_ID), eq(TASK_ID), any()))
                    .thenThrow(new TaskVersionConflictException(TASK_ID, 1, 2));

            mvc.perform(patch("/api/v1/tasks/{id}/status", TASK_ID).with(caller())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"DONE\",\"version\":1}"))
                    .andExpect(status().isConflict());
        }
    }

    // ---- DELETE ---------------------------------------------------------------------

    @Nested
    class Delete {

        @Test
        void returns204() throws Exception {
            mvc.perform(delete("/api/v1/tasks/{id}", TASK_ID).with(caller()))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(taskService).delete(CALLER_ID, TASK_ID);
        }

        @Test
        void returns404ForMissingOrForeignTask() throws Exception {
            doThrow(new TaskNotFoundException(TASK_ID)).when(taskService).delete(CALLER_ID, TASK_ID);

            mvc.perform(delete("/api/v1/tasks/{id}", TASK_ID).with(caller()))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(PROBLEM_JSON));
        }
    }
}
