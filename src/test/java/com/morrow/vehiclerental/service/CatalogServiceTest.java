package com.morrow.vehiclerental.service;

import com.morrow.vehiclerental.model.Vehicle;
import com.morrow.vehiclerental.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {
    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private CatalogService catalogService;

    @Test
    void detailShouldIncreaseViewCount() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setActive(true);
        vehicle.setViewCount(2);

        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

        Vehicle result = catalogService.detail(1L);

        assertEquals(3, result.getViewCount());
        verify(vehicleRepository).findById(1L);
    }

    @Test
    void detailShouldFailWhenVehicleDoesNotExist() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> catalogService.detail(99L));
    }
}
