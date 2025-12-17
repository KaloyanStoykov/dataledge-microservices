package org.dataledge.datasourceservice.data.filesnaps;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlobMetadataRepo extends PagingAndSortingRepository<BlobMetadata, Integer>, JpaRepository<BlobMetadata, Integer> {
    Page<BlobMetadata> findAllByUserId(int userId, Pageable pageable);
    @Modifying
    @Query("delete from BlobMetadata b where b.userId = :userId")
    void deleteAllByUserId(int userId);
}
