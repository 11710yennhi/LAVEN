package iuh.fit.laven.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ship_costs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipCost {
    @Id
    @Column(nullable = false, updatable = false)
    private int shipCostId;

    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String city;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer priceShip;
}
