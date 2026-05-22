package com.lapzone.api.product;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    public ProductResponse getProductById(UUID id) {
        Product product = findProductEntityById(id);
        return ProductResponse.fromEntity(product);
    }

    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();

        product.setName(request.name());
        product.setBrand(request.brand());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setAvailability(request.availability() != null ? request.availability() : true);
        product.setImageUrl(request.imageUrl());

        Product savedProduct = productRepository.save(product);

        return ProductResponse.fromEntity(savedProduct);
    }

    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = findProductEntityById(id);

        product.setName(request.name());
        product.setBrand(request.brand());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setAvailability(request.availability() != null ? request.availability() : true);
        product.setImageUrl(request.imageUrl());

        Product updatedProduct = productRepository.save(product);

        return ProductResponse.fromEntity(updatedProduct);
    }

    public void deleteProduct(UUID id) {
        Product product = findProductEntityById(id);
        productRepository.delete(product);
    }

    private Product findProductEntityById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    //para la seccion de inicio productos mas vendidos y productos nuevos
    public List<ProductResponse> getNewestProducts(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 12));

        return productRepository.findByOrderByCreatedAtDesc(
                        org.springframework.data.domain.PageRequest.of(0, safeLimit)
                )
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    public List<ProductResponse> getBestSellingProducts(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 12));

        return productRepository.findBestSellingProducts(
                        org.springframework.data.domain.PageRequest.of(0, safeLimit)
                )
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }
}