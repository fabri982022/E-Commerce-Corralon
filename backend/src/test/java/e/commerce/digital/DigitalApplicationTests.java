package e.commerce.digital;

import e.commerce.digital.config.InteractionLoggingAspect;
import e.commerce.digital.config.RequestTracingFilter;
import e.commerce.digital.config.JwtService;
import e.commerce.digital.controller.AuthController;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"app.seed.enabled=false"
})
@AutoConfigureMockMvc
class DigitalApplicationTests {

	@Autowired
	private AuthController authController;

	@Autowired
	private InteractionLoggingAspect interactionLoggingAspect;

	@Autowired
	private RequestTracingFilter requestTracingFilter;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
		assertThat(interactionLoggingAspect).isNotNull();
		assertThat(requestTracingFilter).isNotNull();
		assertThat(AopUtils.isAopProxy(authController)).isTrue();
		assertThat(AopUtils.isAopProxy(jwtService)).isTrue();
	}

	@Test
	void httpRequestsIncludeRequestIdForCorrelatedLogs() throws Exception {
		mockMvc.perform(post("/api/auth/logout"))
				.andExpect(status().isNoContent())
				.andExpect(header().exists(RequestTracingFilter.REQUEST_ID_HEADER));
	}

}
