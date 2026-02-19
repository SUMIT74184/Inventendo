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

    private Long id;              // 1. Long
    private String warehouseCode; // 2. String
    private String name;          // 3. String
    private String managerName;   // 4. String
    private String location;      // 5. String
    private String address;       // 6. String
    private String city;          // 7. String
    private String state;         // 8. String
    private String country;       // 9. String
    private String zipCode;       // 10. String
    private int capacity;         // 11. int (The error shows an 'int' here)
    private String email;         // 12. String
    private Double currentUtilization; // 13. Double
    private Double maxUtilization;     // 14. Double (The error shows a second Double)
    private String status;        // 15. String
    private String tenantId;      // 16. String
    private LocalDateTime createdAt; // 17. LocalDateTime
    private LocalDateTime updatedAt; // 18. LocalDateTime
    private Boolean active;       // 19. Boolean

}