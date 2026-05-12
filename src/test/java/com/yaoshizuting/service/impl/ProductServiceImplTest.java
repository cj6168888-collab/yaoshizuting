package com.yaoshizuting.service.impl;

import com.yaoshizuting.dto.ProductUpsertRequest;
import com.yaoshizuting.entity.Product;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void createProductStoresDefaultsAndNormalizesBlankFields() {
        ProductUpsertRequest request = buildRequest();
        request.setProductCode(" ");
        request.setUnit("");
        request.setImage(" ");
        request.setDescription("");
        request.setStock(null);
        request.setStatus(null);

        Product product = productService.createProduct(request);

        assertEquals("测试商品", product.getProductName());
        assertNull(product.getProductCode());
        assertEquals(1, product.getProductType());
        assertEquals(0, product.getStock());
        assertEquals("套", product.getUnit());
        assertNull(product.getImage());
        assertNull(product.getDescription());
        assertEquals(1, product.getStatus());
        verify(productMapper).insert(product);
    }

    @Test
    void createProductRejectsInvalidProductType() {
        ProductUpsertRequest request = buildRequest();
        request.setProductType(4);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.createProduct(request));

        assertEquals(400, exception.getCode());
        assertEquals("商品类型无效", exception.getMessage());
        verify(productMapper, never()).insert(any());
    }

    @Test
    void createProductRejectsDuplicateProductCode() {
        ProductUpsertRequest request = buildRequest();
        request.setProductCode("DUP-CODE");
        when(productMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.createProduct(request));

        assertEquals(400, exception.getCode());
        assertEquals("商品编码已存在", exception.getMessage());
        verify(productMapper, never()).insert(any());
    }

    @Test
    void updateProductUpdatesExistingProductAndPreservesStatusWhenAbsent() {
        Product existing = new Product();
        existing.setId(9L);
        existing.setStatus(0);
        ProductUpsertRequest request = buildRequest();
        request.setProductCode("UPDATED-CODE");
        request.setStatus(null);

        when(productMapper.selectById(9L)).thenReturn(existing);
        when(productMapper.selectCount(any())).thenReturn(0L);

        Product product = productService.updateProduct(9L, request);

        assertEquals("测试商品", product.getProductName());
        assertEquals("UPDATED-CODE", product.getProductCode());
        assertEquals(0, product.getStatus());
        verify(productMapper).updateById(product);
    }

    @Test
    void updateProductRejectsMissingProduct() {
        ProductUpsertRequest request = buildRequest();
        when(productMapper.selectById(9L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.updateProduct(9L, request));

        assertEquals(404, exception.getCode());
        assertEquals("商品不存在", exception.getMessage());
        verify(productMapper, never()).updateById(any());
    }

    @Test
    void updateStatusRejectsInvalidStatus() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> productService.updateStatus(9L, 2));

        assertEquals(400, exception.getCode());
        assertEquals("商品状态无效", exception.getMessage());
        verify(productMapper, never()).selectById(any());
    }

    @Test
    void updateStatusUpdatesExistingProduct() {
        Product product = new Product();
        product.setId(9L);
        product.setStatus(1);
        when(productMapper.selectById(9L)).thenReturn(product);

        Product updated = productService.updateStatus(9L, 0);

        assertEquals(0, updated.getStatus());
        verify(productMapper).updateById(product);
    }

    @Test
    void listActiveProductsDelegatesToMapperWithQueryWrapper() {
        Product product = new Product();
        product.setId(1L);
        when(productMapper.selectList(any())).thenReturn(List.of(product));

        List<Product> products = productService.listActiveProducts(2);

        assertEquals(1, products.size());
        assertEquals(1L, products.get(0).getId());
        verify(productMapper).selectList(any());
    }

    private ProductUpsertRequest buildRequest() {
        ProductUpsertRequest request = new ProductUpsertRequest();
        request.setProductName("测试商品");
        request.setProductCode("PROD-CODE");
        request.setProductType(1);
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
