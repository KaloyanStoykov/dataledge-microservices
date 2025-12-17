package org.dataledge.datasourceservice.data.datasources;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DataSourceRepo extends PagingAndSortingRepository<DataSource, Integer>, JpaRepository<DataSource, Integer> {
    @Modifying
    @Transactional // Ensures the query runs in a transaction even if called elsewhere
    @Query("DELETE FROM DataSource d WHERE d.userId = :userId")
    void deleteAllByUserId(int userId);
    Optional<DataSource> findByIdAndUserId(Long id, int userId);
    Page<DataSource> findAllByUserId(int userId, Pageable pageable);
}
