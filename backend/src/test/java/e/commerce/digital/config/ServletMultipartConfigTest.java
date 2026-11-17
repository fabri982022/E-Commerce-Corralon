package e.commerce.digital.config;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

class ServletMultipartConfigTest {

    @Test
    void createsServletMultipartConfigurationWithConfiguredLimits() {
        var config = new ServletMultipartConfig().multipartConfigElement(
                DataSize.ofMegabytes(5),
                DataSize.ofMegabytes(6));

        assertThat(config.getMaxFileSize()).isEqualTo(5L * 1024 * 1024);
        assertThat(config.getMaxRequestSize()).isEqualTo(6L * 1024 * 1024);
    }
}
