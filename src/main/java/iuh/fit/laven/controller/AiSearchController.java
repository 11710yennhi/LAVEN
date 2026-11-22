package iuh.fit.laven.controller;

import iuh.fit.laven.dto.request.ai.SemanticSearchRequest;
import iuh.fit.laven.dto.response.ai.SemanticSearchResponse;
import iuh.fit.laven.service.ai.SemanticSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API công khai: Guest cũng gọi được (/api/public/** đã permitAll). */
@RestController
@RequestMapping("/api/public")
public class AiSearchController {

    private final SemanticSearchService semanticSearchService;

    public AiSearchController(SemanticSearchService semanticSearchService) {
        this.semanticSearchService = semanticSearchService;
    }

    /** Ví dụ: /api/public/ai/search?query=vali nhẹ đi công tác 3 ngày&maxPrice=3000000 */
    @GetMapping("/ai/search")
    public SemanticSearchResponse search(@Valid @ModelAttribute SemanticSearchRequest request) {
        return semanticSearchService.searchWithScore(request.getQuery(), request.getCategoryId(),
                request.getMinPrice(), request.getMaxPrice(), request.getTopK());
    }

    /** Ví dụ: /api/public/products/PRO2026.../similar?topK=4 */
    @GetMapping("/products/{productId}/similar")
    public List<SemanticSearchResponse.Item> similar(@PathVariable String productId,
                                                     @RequestParam(defaultValue = "4") Integer topK) {
        return semanticSearchService.similarWithScore(productId, topK);
    }
}