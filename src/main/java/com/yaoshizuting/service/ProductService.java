package com.yaoshizuting.service;

import com.yaoshizuting.entity.Product;
import com.yaoshizuting.dto.ProductUpsertRequest;

import java.util.List;

public interface ProductService {

    List<Product> listActiveProducts(Integer productType);

    Product createProduct(ProductUpsertRequest request);

    Product updateProduct(Long productId, ProductUpsertRequest request);

    Product updateStatus(Long productId, Integer status);
}
