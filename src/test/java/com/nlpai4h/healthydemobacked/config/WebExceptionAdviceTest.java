package com.nlpai4h.healthydemobacked.config;

import com.nlpai4h.healthydemobacked.common.properties.JwtProperties;
import com.nlpai4h.healthydemobacked.common.result.Result;
import com.nlpai4h.healthydemobacked.interceptor.JwtTokenInterceptor;
import com.nlpai4h.healthydemobacked.model.dto.LoginFormDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = WebExceptionAdviceTest.TestBootConfig.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class WebExceptionAdviceTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void methodArgumentNotValid_returns400AndReadableMsg() throws Exception {
        mockMvc.perform(post("/test/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("账号不能为空")));
    }

    @Test
    void constraintViolation_returns400AndReadableMsg() throws Exception {
        mockMvc.perform(get("/test/min").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("page必须>=1")));
    }

    @Test
    void jwtMissingToken_returns401AndResultBody() throws Exception {
        mockMvc.perform(get("/api/test/secure"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void jwtMalformedToken_returns401AndResultBody() throws Exception {
        mockMvc.perform(get("/api/test/secure").header("authentication", "abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @RestController
    @Validated
    static class TestController {

        @PostMapping("/test/login")
        public Result<Void> login(@RequestBody @Valid LoginFormDTO dto) {
            return Result.success();
        }

        @GetMapping("/test/min")
        public Result<Void> min(@RequestParam @Min(value = 1, message = "page必须>=1") Integer page) {
            return Result.success();
        }

        @GetMapping("/api/test/secure")
        public Result<Void> secure() {
            return Result.success();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class
    })
    @Import({WebExceptionAdvice.class, MvcConfig.class, TestController.class})
    static class TestBootConfig {
    }

    @TestConfiguration
    static class MvcConfig implements WebMvcConfigurer {

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(jwtTokenInterceptor()).addPathPatterns("/api/**");
        }

        @Bean
        JwtTokenInterceptor jwtTokenInterceptor() {
            return new JwtTokenInterceptor();
        }

        @Bean
        JwtProperties jwtProperties() {
            JwtProperties p = new JwtProperties();
            p.setMxbTokenName("authentication");
            p.setMxbSecretKey("01234567890123456789012345678901");
            p.setMxbTtl(3600_000);
            return p;
        }
    }
}
