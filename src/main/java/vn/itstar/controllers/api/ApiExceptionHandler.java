package vn.itstar.controllers.api;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vn.itstar.services.*;

@RestControllerAdvice(basePackages = "vn.itstar.controllers.api")
public class ApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> missing(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(ApiResponses.body(false, ex.getMessage(), null));
    }
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> conflict(ConflictException ex) {
        return ResponseEntity.status(409).body(ApiResponses.body(false, ex.getMessage(), null));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> integrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(ApiResponses.body(false,
                "Dữ liệu bị trùng hoặc bản ghi đang được sử dụng. Vui lòng kiểm tra lại.", null));
    }
    @ExceptionHandler(BindException.class)
    public ResponseEntity<?> validation(BindException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(),
                error.isBindingFailure() ? "Giá trị không đúng định dạng" : error.getDefaultMessage()));
        var body = ApiResponses.body(false, "Vui lòng kiểm tra dữ liệu nhập", null);
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> parameter(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiResponses.body(false,
                "Tham số '" + ex.getName() + "' không đúng định dạng", null));
    }
    @ExceptionHandler(ImageStorageException.class)
    public ResponseEntity<?> image(ImageStorageException ex) {
        return ResponseEntity.badRequest().body(ApiResponses.body(false, ex.getMessage(), null));
    }
}
