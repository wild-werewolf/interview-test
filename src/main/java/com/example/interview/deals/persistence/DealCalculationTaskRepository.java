package com.example.interview.deals.persistence;

import com.example.interview.deals.domain.TaskStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DealCalculationTaskRepository extends JpaRepository<DealCalculationTaskEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DealCalculationTaskEntity t where t.id = :id")
    Optional<DealCalculationTaskEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("select t from DealCalculationTaskEntity t where t.dealId = :dealId and t.status in :statuses")
    List<DealCalculationTaskEntity> findByDealIdAndStatusIn(
            @Param("dealId") Long dealId,
            @Param("statuses") Collection<TaskStatus> statuses
    );

    @Query(value = """
            select *
            from deal_calculation_tasks
            where ((status = 'NEW' and available_at <= :now)
                or (status = 'PROCESSING' and locked_until < :now))
            order by available_at, id
            limit 1
            for update skip locked
            """, nativeQuery = true)
    Optional<DealCalculationTaskEntity> findNextAvailableForUpdate(@Param("now") Instant now);
}
