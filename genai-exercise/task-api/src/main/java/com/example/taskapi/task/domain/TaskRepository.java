package com.example.taskapi.task.domain;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Every query that returns tasks is scoped by owner. The interface deliberately extends the
 * bare {@link Repository} marker instead of {@code JpaRepository}, so unscoped operations such
 * as {@code findById}, {@code findAll} or {@code deleteById} do not exist and cannot be called
 * by mistake. The write methods below are implemented by Spring Data's standard JPA repository.
 */
public interface TaskRepository extends Repository<Task, UUID> {

    Task saveAndFlush(Task task);

    /** Callers pass a task they already loaded through an owner-scoped query. */
    void delete(Task task);

    Optional<Task> findByIdAndOwnerId(UUID id, UUID ownerId);

    /**
     * Owner-scoped listing with optional filters.
     *
     * @param status    exact status match, or {@code null} for any
     * @param dueBefore exclusive upper bound on due date, or {@code null} for no bound;
     *                  when set, tasks without a due date are excluded
     */
    @Query("""
            select t from Task t
            where t.owner.id = :ownerId
              and (:status is null or t.status = :status)
              and (:dueBefore is null or t.dueDate < :dueBefore)
            """)
    Page<Task> search(@Param("ownerId") UUID ownerId,
                      @Param("status") TaskStatus status,
                      @Param("dueBefore") LocalDate dueBefore,
                      Pageable pageable);
}
