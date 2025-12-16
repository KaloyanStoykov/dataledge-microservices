package org.dataledge.datasourceservice.data.filesnaps;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlobMetadataRepo extends PagingAndSortingRepository<BlobMetadata, Integer>, JpaRepository<BlobMetadata, Integer> {
}
