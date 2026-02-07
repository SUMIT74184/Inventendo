package com.example.microservice1.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import java.math.BigDecimal;

//Represents database table structure


@Entity
@Table(name="Inventory",indexes = {
        @Index(name = "idx_sku",columnList = "sku"),
        @Index(name = "idx_warehouse",columnList = "warehouse_id")
})

@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue()
    private Long id;

    @Column(nullable = false,unique = true,length = 100)
    private String sku;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer reservedQuantity=0;

    @Column(nullable = false)
    private Integer reorderLevel;

    @Column(nullable = false)
    private Integer maxStockLevel;

    @Column(nullable = false,precision = 10,scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private String warehouseId;

    @Column(length=50)
    private String Location;


    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;


    @Version
    private Long version;

    public Integer getAvailableQuantity(){
        return quantity - reservedQuantity;
    }

    public boolean isLowStock(){
        return quantity <= reorderLevel;
    }

    public enum InventoryStatus{
        ACTIVE,INACTIVE,DISCONTINUED
    }




}
