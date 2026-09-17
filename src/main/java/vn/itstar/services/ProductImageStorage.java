package vn.itstar.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImageStorage {
    private static final Logger log = LoggerFactory.getLogger(ProductImageStorage.class);
    private final Path directory;

    public ProductImageStorage(@Value("${app.upload.product-dir:uploads/products}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public String resourceLocation() {
        String location = directory.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new ImageStorageException("Ảnh tối đa 5 MB.");
        }
        String original = file.getOriginalFilename();
        String extension = original == null || !original.contains(".") ? ""
                : original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!Set.of("jpg", "jpeg", "png", "gif", "webp").contains(extension)) {
            throw new ImageStorageException("Chỉ chọn ảnh JPG, JPEG, PNG, GIF hoặc WEBP.");
        }

        String filename = UUID.randomUUID() + "." + extension;
        Path destination = directory.resolve(filename);
        try {
            try (InputStream input = file.getInputStream()) {
                if (!hasImageSignature(input.readNBytes(12), extension)) {
                    throw new ImageStorageException("Nội dung file không đúng định dạng ảnh đã chọn.");
                }
            }
            Files.createDirectories(directory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination);
            }
        } catch (IOException ex) {
            deleteQuietly(filename);
            throw new ImageStorageException("Không thể lưu ảnh. Vui lòng thử lại.", ex);
        }

        // Filesystem không rollback cùng database: xóa ảnh mới nếu giao dịch thất bại.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        deleteQuietly(filename);
                    }
                }
            });
        }
        return filename;
    }

    public void deleteAfterCommit(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteQuietly(filename);
                }
            });
        } else {
            deleteQuietly(filename);
        }
    }

    private void deleteQuietly(String filename) {
        // Chỉ xóa file thuộc thư mục upload, kể cả dữ liệu cũ có đường dẫn không hợp lệ.
        if (filename.contains("/") || filename.contains("\\") || filename.contains(":")) {
            return;
        }
        Path target = directory.resolve(filename).normalize();
        if (!directory.equals(target.getParent())) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.warn("Không thể xóa ảnh {}", filename, ex);
        }
    }

    private boolean hasImageSignature(byte[] header, String extension) {
        if (header.length < 12) {
            return false;
        }
        return switch (extension) {
            case "jpg", "jpeg" -> (header[0] & 255) == 255 && (header[1] & 255) == 216
                    && (header[2] & 255) == 255;
            case "png" -> Arrays.equals(Arrays.copyOf(header, 8),
                    new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
            case "gif" -> new String(header, 0, 6, StandardCharsets.US_ASCII).matches("GIF8[79]a");
            case "webp" -> new String(header, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(header, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            default -> false;
        };
    }
}
