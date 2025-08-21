package cz.martinzajdlik.recappy_book.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class ImageStorageService {

    private final Cloudinary cloudinary;

    // volitelné – ať jde složku měnit přes properties/ENV
    @Value("${app.images.folder:recappy-book}")
    private String folder;

    public ImageStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /** Nahraje obrázek na Cloudinary a vrátí veřejnou HTTPS URL. */
    public String upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("Soubor je prázdný.");
        }
        String ct = file.getContentType();
        if (ct == null || !(ct.equals("image/jpeg") || ct.equals("image/png") || ct.equals("image/webp"))) {
            throw new IOException("Povolené typy: JPEG, PNG, WEBP.");
        }

        Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,           // cílová složka (např. "recappy-book")
                        "resource_type", "image",
                        "use_filename", true,       // použije název souboru
                        "unique_filename", true     // přidá suffix → žádné kolize
                        // případně: "overwrite", false
                )
        );

        String url = (String) uploadResult.get("secure_url");
        if (url == null) {
            throw new IOException("Upload na Cloudinary selhal (secure_url je null).");
        }
        return url;
    }
}
