package org.dataledge.datasourceservice.data.filesnaps;


import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlobMetadataRepo extends PagingAndSortingRepository<BlobMetadata, Integer>, JpaRepository<BlobMetadata, Integer> {
    Page<BlobMetadata> findAllByUserId(int userId, Pageable pageable);

    @Modifying
    @Query("delete from BlobMetadata b where b.userId = :userId")
    void deleteAllByUserId(int userId);

    @Query("SELECT b FROM BlobMetadata b WHERE b.userId = :userId AND b.dataSource.id = :dsId")
    Page<BlobMetadata> findByUserAndDataSource(
            @Param("userId") int userId,
            @Param("dsId") long dsId,
            Pageable pageable
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM BlobMetadata b WHERE b.userId = :userId AND b.fileName IN :blobNames")
    void deleteByUserIdAndBlobNames(int userId, List<String> blobNames);
}
