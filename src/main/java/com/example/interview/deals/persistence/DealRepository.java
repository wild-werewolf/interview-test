package com.example.interview.deals.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DealRepository extends JpaRepository<DealEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DealEntity d where d.id = :id")
    Optional<DealEntity> findByIdForUpdate(@Param("id") Long id);
}
