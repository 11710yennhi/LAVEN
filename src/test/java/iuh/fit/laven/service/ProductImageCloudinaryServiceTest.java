package iuh.fit.laven.service;

import iuh.fit.laven.dto.response.ProductImageUploadSignatureResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class ProductImageCloudinaryServiceTest {
    private final ProductImageCloudinaryService service =
        new ProductImageCloudinaryService("laven-demo", "public-key", "private-secret");

    @Test
    void signsOnlyTheSelectedProductUpload() throws Exception {
        ProductImageUploadSignatureResponse result = service.signUpload("PRO123");
        String signedText = "public_id=" + result.getPublicId() + "&timestamp=" + result.getTimestamp()
            + "private-secret";
        byte[] digest = MessageDigest.getInstance("SHA-1")
            .digest(signedText.getBytes(StandardCharsets.UTF_8));

        assertTrue(result.getPublicId().startsWith("laven/products/PRO123/"));
        assertEquals(HexFormat.of().formatHex(digest), result.getSignature());
    }

    @Test
    void acceptsOnlyOwnCloudAndProductFolder() {
        String valid = "https://res.cloudinary.com/laven-demo/image/upload/v123/laven/products/PRO123/abc.jpg";
        assertEquals(valid, service.validateImageUrl(valid, "PRO123"));

        assertThrows(ResponseStatusException.class,
            () -> service.validateImageUrl(valid, "PRO999"));
        assertThrows(ResponseStatusException.class,
            () -> service.validateImageUrl(valid.replace("laven-demo", "other-cloud"), "PRO123"));
        assertThrows(ResponseStatusException.class,
            () -> service.validateImageUrl("http://res.cloudinary.com/laven-demo/image/upload/v123/laven/products/PRO123/abc.jpg", "PRO123"));
    }

    @Test
    void extractsPublicIdCorrectly() {
        String urlWithVersion = "https://res.cloudinary.com/laven-demo/image/upload/v1700000000/laven/products/PRO123/abc_xyz.jpg";
        assertEquals("laven/products/PRO123/abc_xyz", service.extractPublicId(urlWithVersion));

        String urlWithoutVersion = "https://res.cloudinary.com/laven-demo/image/upload/laven/products/PRO123/image_test.png";
        assertEquals("laven/products/PRO123/image_test", service.extractPublicId(urlWithoutVersion));

        assertNull(service.extractPublicId(null));
        assertNull(service.extractPublicId(""));
        assertNull(service.extractPublicId("https://example.com/invalid.jpg"));
    }
}
