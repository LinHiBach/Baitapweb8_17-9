package vn.itstar.configs;

import lombok.RequiredArgsConstructor;
import vn.itstar.services.ProductImageStorage;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ProductImageStorage imageStorage;
    private final vn.itstar.services.CategoryImageStorage categoryImageStorage;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/uploads/categories/**")
                .addResourceLocations(categoryImageStorage.resourceLocation());
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations(imageStorage.resourceLocation());
    }
}
