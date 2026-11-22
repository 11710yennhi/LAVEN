package iuh.fit.laven.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import iuh.fit.laven.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class CloudinaryUploadService {

    private final Cloudinary cloudinary;

    public CloudinaryUploadService(@Value("${CLOUDINARY_CLOUD_NAME:}") String cloudName,
                                   @Value("${CLOUDINARY_API_KEY:}") String apiKey,
                                   @Value("${CLOUDINARY_API_SECRET:}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }


    public Map uploadFile(MultipartFile file, String folder) {
        try {
            return cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", folder, "resource_type", "auto")
            );
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tải file lên Cloudinary");
        }
    }

    public void deleteFile(String publicId, String resourceType) {
        try {
            cloudinary
                    .uploader()
                    .destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xóa file khỏi Cloudinary");
        }
    }
}