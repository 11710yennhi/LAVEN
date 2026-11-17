package iuh.fit.laven.controller;

import iuh.fit.laven.model.Color;
import iuh.fit.laven.service.ColorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ColorController {
    private final ColorService colorService;

    public ColorController(ColorService colorService) {
        this.colorService = colorService;
    }

    @GetMapping("/public/colors")
    public List<Color> getPublicColors(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String colorId,
        @RequestParam(required = false) String name
    ) {
        return colorService.search(keyword, colorId, name);
    }

    @GetMapping("/staff/colors")
    public List<Color> getColors(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String colorId,
        @RequestParam(required = false) String name
    ) {
        return colorService.search(keyword, colorId, name);
    }

    @PostMapping("/staff/colors")
    public Color createColor(@Valid @RequestBody Color color) {
        return colorService.create(color);
    }

    @PutMapping("/staff/colors/{colorId}")
    public Color updateColor(
        @PathVariable String colorId,
        @Valid @RequestBody Color color
    ) {
        return colorService.update(colorId, color);
    }
}
