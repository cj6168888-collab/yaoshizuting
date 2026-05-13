package com.yaoshizuting.controller;

import com.yaoshizuting.testing.TestDistributedLockConfig;
import com.yaoshizuting.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDistributedLockConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Test
    void listProducts_WithValidToken_ReturnsActiveProducts() throws Exception {
        String token = jwtUtils.generateToken(1L, "13800138000", 1);

        mockMvc.perform(get("/api/product/list")
                .contextPath("/api")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data[0].productName").exists())
                .andExpect(jsonPath("$.data[0].description").isString());
    }

    @Test
    void listProducts_WithProductType_ReturnsFilteredProducts() throws Exception {
        String token = jwtUtils.generateToken(1L, "13800138000", 1);

        mockMvc.perform(get("/api/product/list")
                .contextPath("/api")
                .param("productType", "2")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].productType").value(2));
    }

    @Test
    void listProducts_WithInvalidProductType_ReturnsBusinessError() throws Exception {
        String token = jwtUtils.generateToken(1L, "13800138000", 1);

        mockMvc.perform(get("/api/product/list")
                .contextPath("/api")
                .param("productType", "99")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("商品类型无效"));
    }
}
