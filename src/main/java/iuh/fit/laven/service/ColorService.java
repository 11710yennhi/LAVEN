package iuh.fit.laven.service;

import iuh.fit.laven.model.Color;
import iuh.fit.laven.repository.ColorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ColorService {
    private final ColorRepository colorRepository;

    public ColorService(ColorRepository colorRepository) {
        this.colorRepository = colorRepository;
    }

    @Transactional(readOnly = true)
    public List<Color> findAll() {
        return colorRepository.findAllByOrderByNameAsc();
    }

    public Color create(Color color) {
        if (color.getColorId() == null || !color.getColorId().matches("^#[0-9A-Fa-f]{6}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã màu phải là HEX, ví dụ #093F69");
        }
        String colorId = color.getColorId().toUpperCase();
        if (colorRepository.existsById(colorId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã màu đã tồn tại");
        }
        color.setColorId(colorId);
        return colorRepository.save(color);
    }

    public Color update(String id, Color request) {
        Color color = colorRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Màu sắc không tồn tại"));
        color.setName(request.getName());
        return colorRepository.save(color);
    }
}
