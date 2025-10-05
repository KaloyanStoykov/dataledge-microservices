package org.dataledge.datasourceservice;

import jakarta.ws.rs.NotFoundException;
import org.dataledge.datasourceservice.data.DataType;
import org.dataledge.datasourceservice.data.DataTypeRepo;
import org.dataledge.datasourceservice.dto.datatypesDTO.DataTypeListResponse;
import org.dataledge.datasourceservice.dto.datatypesDTO.DataTypeResponse;
import org.dataledge.datasourceservice.manager.IDataTypeMapper;
import org.dataledge.datasourceservice.manager.impl.DataTypesManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class DataTypesManagerTest {
    @Mock
    private DataTypeRepo dataTypeRepo;

    @Mock
    private IDataTypeMapper mapper;

    @InjectMocks
    private DataTypesManager dataTypesManager;


    @Test
    void getAllDataTypes_returns_items(){
        DataType entity1 = new DataType(1L, "API", "An API url to gather publicly available data", List.of());
        DataType entity2 = new DataType(2L, "File", "Upload a file to Dataledge system.", List.of());

        DataTypeResponse dto1 = new DataTypeResponse(1L, "API", "An API url to gather publicly available data");
        DataTypeResponse dto2 = new DataTypeResponse(2L, "File", "Upload a file to Dataledge system.");

        when(mapper.mapToResponse(entity1)).thenReturn(dto1);
        when(mapper.mapToResponse(entity2)).thenReturn(dto2);

        List<DataType> dataTypes = List.of(entity1, entity2);

        when(dataTypeRepo.findAll()).thenReturn(dataTypes);

        DataTypeListResponse response = dataTypesManager.getDataTypes();

        assertThat(response).isNotNull();
        assertThat(response.getDataTypes().size()).isEqualTo(2);

        verify(dataTypeRepo).findAll();
        verify(mapper).mapToResponse(entity1);
        verify(mapper).mapToResponse(entity2);

    }

    @Test
    void getAllDataTypes_throws_not_found_exception(){
        when(dataTypeRepo.findAll()).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> dataTypesManager.getDataTypes());
    }

}