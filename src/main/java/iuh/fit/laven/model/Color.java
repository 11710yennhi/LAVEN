package iuh.fit.laven.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "colors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Color {
    @Id
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    @Column(length = 7, updatable = false)
    private String colorId;

    @NotBlank
    @Column(nullable = false, length = 60)
    private String name;
}
