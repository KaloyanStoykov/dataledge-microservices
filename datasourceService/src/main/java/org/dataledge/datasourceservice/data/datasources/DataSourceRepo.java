package org.dataledge.datasourceservice.data.datasources;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataSourceRepo extends PagingAndSortingRepository<DataSource, Integer>, JpaRepository<DataSource, Integer> {

}
