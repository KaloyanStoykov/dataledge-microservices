package org.dataledge.datasourceservice.manager.impl;

import jakarta.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import org.dataledge.datasourceservice.data.DataSource;
import org.dataledge.datasourceservice.data.DataSourceRepo;
import org.dataledge.datasourceservice.dto.DataSourceResponse;
import org.dataledge.datasourceservice.dto.GetDataSourcesResponse;
import org.dataledge.datasourceservice.manager.IDataSourceMapper;
import org.dataledge.datasourceservice.manager.IDataSourceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Kaloyan Stoykov
 */
@Service
@AllArgsConstructor
public class DataSourceManager implements IDataSourceManager {

    // JPA Pageable repository
    private final DataSourceRepo dataSourceRepo;
    // Mapper to responses

    private final IDataSourceMapper mapper;

    /**
     * @param pageNumber  contains the pageNumber and pageSize properties for repository. pageNumber is zero-based
     * @param pageSize refers to count of elements to get.
     * @return the response object containing List<> of data sources
     * @throws NotFoundException when no items were found from the repository
     */
    @Override
    public GetDataSourcesResponse getDataSources(int pageNumber, int pageSize) {

        Page<DataSource> pageResult = dataSourceRepo.findAll(PageRequest.of(pageNumber, pageSize));

        if (pageResult.isEmpty()) {
            throw new NotFoundException();
        }

        List<DataSourceResponse> items = pageResult.getContent()
                .stream()
                .map(mapper::toDataSourceResponse)
                .toList();

        return new GetDataSourcesResponse(items, pageResult.getTotalElements(), pageNumber, pageSize);

    }
}
