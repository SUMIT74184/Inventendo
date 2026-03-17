package org.example.warehousemcs.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDTO {
    private Long id;
    private String warehouseCode;
    private String name;
    private String location; // Moved up to match your mapToDTO order
    private String address;
    private String city;
    private String state;
    private String country;
    private String zipCode;
    private String managerName;
    private String contactNumber;
    private String email;
    private Double capacity; // Changed from int to Double to match warehouse.getCapacity()
    private Double currentUtilization;
    private String status;
    private String tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean active;
}