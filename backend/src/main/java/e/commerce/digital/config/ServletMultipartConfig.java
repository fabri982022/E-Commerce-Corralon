package e.commerce.digital.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration(proxyBeanMethods = false)
public class ServletMultipartConfig {

    @Bean
    MultipartConfigElement multipartConfigElement(
            @Value("${spring.servlet.multipart.max-file-size:5MB}") DataSize maxFileSize,
            @Value("${spring.servlet.multipart.max-request-size:6MB}") DataSize maxRequestSize) {
        return new MultipartConfigElement("", maxFileSize.toBytes(), maxRequestSize.toBytes(), 0);
    }
}
