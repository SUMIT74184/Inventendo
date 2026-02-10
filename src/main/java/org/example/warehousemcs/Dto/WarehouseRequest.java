package org.example.warehousemcs.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class WarehouseRequest {

    @NotBlank(message = "Warehouse code is required")
    private String warehouseCode;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Location is required")
    private String location;

    private String address;
    private String city;
    private String state;
    private String country;
    private String zipCode;


    @NotBlank(message = "Manager name is required")
    private String managerName;

    private String contactNumber;

    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Capacity is required")
    private Double capacity;

    @NotNull(message = "Current utilization is required")
    private Double currentUtilization;

    @NotBlank(message = "Status is required")
    private String status;

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

}
