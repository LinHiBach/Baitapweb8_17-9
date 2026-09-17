package vn.itstar.controllers.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// Multipart có thể bị từ chối trước khi Spring xác định Controller.
// Vì thế handler giới hạn upload không được giới hạn bằng basePackages.
@RestControllerAdvice
public class UploadExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> tooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(413).body(ApiResponses.body(false,
                "Mỗi ảnh tối đa 5 MB; tổng request tối đa 10 MB.", null));
    }
}
