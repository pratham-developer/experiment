package com.experiment.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(
        name = "item_inventory",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_item_inventory_item_seller",
                        columnNames = {"item_id", "seller_id"}
                )
        }
)
public class ItemInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    @ToString.Exclude
    private Item item;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Builder.Default
    @Column(nullable = false)
    private Long unitsAvailable = 0L;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal sellingPrice = BigDecimal.ZERO;
}