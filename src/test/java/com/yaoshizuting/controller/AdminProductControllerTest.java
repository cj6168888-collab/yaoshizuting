package com.yaoshizuting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaoshizuting.dto.ProductUpsertRequest;
import com.yaoshizuting.testing.TestDistributedLockConfig;
import com.yaoshizuting.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDistributedLockConfig.class)
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private static final AtomicInteger CODE_SEQUENCE = new AtomicInteger(1000);

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        adminToken = "Bearer " + jwtUtils.generateToken(9L, "13800000000", 9);
        userToken = "Bearer " + jwtUtils.generateToken(1L, "13800138000", 1);
    }

    @Test
    void uploadImage_WithAdminToken_ReturnsPublicUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "product.png",
                "image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a});

        mockMvc.perform(multipart("/api/admin/product/upload")
                .file(file)
                .contextPath("/api")
                .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/uploads/products/")));
    }

    @Test
    void uploadImage_WithUserToken_ReturnsForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "product.png",
                "image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G'});

        mockMvc.perform(multipart("/api/admin/product/upload")
                .file(file)
                .contextPath("/api")
                .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadImage_WithoutFile_ReturnsBusinessError() throws Exception {
        mockMvc.perform(multipart("/api/admin/product/upload")
                .contextPath("/api")
                .header("Authorization", adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("file不能为空"));
    }

    @Test
    void createProduct_WithAdminToken_ReturnsCreatedProduct() throws Exception {
        ProductUpsertRequest request = buildRequest(nextProductCode());

        mockMvc.perform(post("/api/admin/product")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.productName").value("测试商品"))
                .andExpect(jsonPath("$.data.image").value("/uploads/products/test.png"));
    }

    @Test
    void createProduct_WithInvalidStatus_ReturnsBadRequest() throws Exception {
        ProductUpsertRequest request = buildRequest(nextProductCode());
        request.setStatus(9);

        mockMvc.perform(post("/api/admin/product")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProduct_WithAdminToken_ReturnsUpdatedProduct() throws Exception {
        ProductUpsertRequest request = buildRequest(nextProductCode());
        String content = mockMvc.perform(post("/api/admin/product")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long id = objectMapper.readTree(content).path("data").path("id").asLong();

        request.setProductName("更新后的商品");
        request.setDescription("更新后的描述");
        request.setStock(35);

        mockMvc.perform(put("/api/admin/product/{id}", id)
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.productName").value("更新后的商品"))
                .andExpect(jsonPath("$.data.description").value("更新后的描述"))
                .andExpect(jsonPath("$.data.stock").value(35));
    }

    @Test
    void updateStatus_WithAdminToken_UpdatesProductStatus() throws Exception {
        ProductUpsertRequest request = buildRequest(nextProductCode());
        String content = mockMvc.perform(post("/api/admin/product")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long id = objectMapper.readTree(content).path("data").path("id").asLong();

        mockMvc.perform(patch("/api/admin/product/{id}/status", id)
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(0));
    }

    @Test
    void listProducts_WithAdminToken_ReturnsInactiveProducts() throws Exception {
        String productCode = nextProductCode();
        ProductUpsertRequest request = buildRequest(productCode);
        String content = mockMvc.perform(post("/api/admin/product")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long id = objectMapper.readTree(content).path("data").path("id").asLong();

        mockMvc.perform(patch("/api/admin/product/{id}/status", id)
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":0}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/product/list")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .param("keyword", productCode)
                .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].productCode").value(productCode))
                .andExpect(jsonPath("$.data.records[0].status").value(0));
    }

    @Test
    void exportProducts_WithAdminToken_ReturnsCsv() throws Exception {
        String productCode = nextProductCode();
        ProductUpsertRequest request = buildRequest(productCode);
        mockMvc.perform(post("/api/admin/product")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/product/export")
                .contextPath("/api")
                .header("Authorization", adminToken)
                .param("keyword", productCode)
                .param("productType", "2")
                .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getHeader("Content-Disposition").contains("products.csv")))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getContentAsString().contains(productCode)));
    }

    private String nextProductCode() {
        return "YST-TEST-UPLOAD-" + CODE_SEQUENCE.getAndIncrement();
    }

    private ProductUpsertRequest buildRequest(String productCode) {
        ProductUpsertRequest request = new ProductUpsertRequest();
        request.setProductName("测试商品");
        request.setProductCode(productCode);
        request.setProductType(2);
        request.setMarketPrice(new BigDecimal("199.00"));
        request.setJoinPrice(new BigDecimal("99.00"));
        request.setAgentPrice(new BigDecimal("89.00"));
        request.setPartnerPrice(new BigDecimal("79.00"));
        request.setStock(20);
        request.setUnit("套");
        request.setImage("/uploads/products/test.png");
        request.setDescription("测试商品描述");
        request.setStatus(1);
        return request;
    }
}
