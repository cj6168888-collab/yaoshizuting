package com.yaoshizuting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yaoshizuting.dto.ProductUpsertRequest;
import com.yaoshizuting.entity.Product;
import com.yaoshizuting.exception.BusinessException;
import com.yaoshizuting.mapper.ProductMapper;
import com.yaoshizuting.service.ProductService;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    @Override
    public List<Product> listActiveProducts(Integer productType) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .orderByAsc(Product::getProductType)
                .orderByDesc(Product::getUpdateTime);

        if (productType != null) {
            wrapper.eq(Product::getProductType, productType);
        }

        return productMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product createProduct(ProductUpsertRequest request) {
        validateProductType(request.getProductType());
        ensureProductCodeUnique(request.getProductCode(), null);

        Product product = new Product();
        fillProduct(product, request);
        product.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        productMapper.insert(product);
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product updateProduct(Long productId, ProductUpsertRequest request) {
        validateProductType(request.getProductType());
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        ensureProductCodeUnique(request.getProductCode(), productId);

        fillProduct(product, request);
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        productMapper.updateById(product);
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product updateStatus(Long productId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "商品状态无效");
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        product.setStatus(status);
        productMapper.updateById(product);
        return product;
    }

    private void fillProduct(Product product, ProductUpsertRequest request) {
        product.setProductName(request.getProductName());
        product.setProductCode(blankToNull(request.getProductCode()));
        product.setProductType(request.getProductType());
        product.setMarketPrice(request.getMarketPrice());
        product.setJoinPrice(request.getJoinPrice());
        product.setAgentPrice(request.getAgentPrice());
        product.setPartnerPrice(request.getPartnerPrice());
        product.setStock(request.getStock() == null ? 0 : request.getStock());
        product.setUnit(StrUtil.blankToDefault(request.getUnit(), "套"));
        product.setImage(blankToNull(request.getImage()));
        product.setDescription(blankToNull(request.getDescription()));
    }

    private void validateProductType(Integer productType) {
        if (productType == null || productType < 1 || productType > 3) {
            throw new BusinessException(400, "商品类型无效");
        }
    }

    private void ensureProductCodeUnique(String productCode, Long currentId) {
        if (StrUtil.isBlank(productCode)) {
            return;
        }
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getProductCode, productCode);
        if (currentId != null) {
            wrapper.ne(Product::getId, currentId);
        }
        if (productMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "商品编码已存在");
        }
    }

    private String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : value;
    }
}
