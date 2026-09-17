package vn.itstar.controllers.api;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.Page;

public final class ApiResponses {
    private ApiResponses() {}
    public static Map<String, Object> body(boolean success, String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", success);
        body.put("message", message);
        body.put("data", data);
        return body;
    }
    public static Map<String, Object> page(Page<?> result) {
        return body(true, "Lấy danh sách thành công", Map.of(
                "content", result.getContent(), "page", result.getNumber(),
                "size", result.getSize(), "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()));
    }
}
