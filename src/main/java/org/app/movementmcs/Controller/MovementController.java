package org.app.movementmcs.Controller;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.app.movementmcs.Dto.MovementRequest;
import org.app.movementmcs.Dto.MovementResponse;
import org.app.movementmcs.Service.MovementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/movements")
@Slf4j
@AllArgsConstructor
public class MovementController {

    private final MovementService movementService;


    @PostMapping
    public ResponseEntity<MovementResponse>createMovement(@Valid @RequestBody MovementRequest request){
        log.info("Received Movement request: type={}, productId={},quantity{},",
                request.getMovementType(),request.getProductId(),request.getQuantity()
        );

        MovementResponse response = movementService.createMovement(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<MovementResponse>> getMovementsByProduct(
            @PathVariable Long productId
    ){
        log.info("Fetching movements for productId={}",productId);
        List<MovementResponse>movements = movementService.getMovementsByProduct(productId);
        return ResponseEntity.ok(movements);
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<List<MovementResponse>> getMovementsByWarehouse(@PathVariable Long warehouseId){
        log.info("Fetching movements for warehouseId={}",warehouseId);
        List<MovementResponse> movements = movementService.getMovementsByWarehouse(warehouseId);
        return ResponseEntity.ok(movements);
    }


    @GetMapping("/date-range")
    public ResponseEntity<List<MovementResponse>>getMovementsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate)
            {
                log.info("Fetching movements for date range: {} to {}",startDate,endDate);
                List<MovementResponse>movements =movementService.getMovementsByDateRange(startDate,endDate);
                return ResponseEntity.ok(movements);
    }

}
