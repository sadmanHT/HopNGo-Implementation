package com.hopngo.market.service;

import com.hopngo.market.entity.Product;
import com.hopngo.market.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ProductService {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    
    @Autowired
    private ProductRepository productRepository;
    
    // Create or update product
    @CacheEvict(value = {"products", "productCategories", "productBrands", "featuredProducts"}, allEntries = true)
    public Product saveProduct(Product product) {
        logger.info("Saving product: {}", product.getName());
        return productRepository.save(product);
    }
    
    // Get product by ID with caching
    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(UUID id) {
        logger.debug("Fetching product by ID: {}", id);
        return productRepository.findById(id);
    }
    
    // Get product by SKU with caching
    @Cacheable(value = "products", key = "'sku:' + #sku")
    @Transactional(readOnly = true)
    public Optional<Product> getProductBySku(String sku) {
        logger.debug("Fetching product by SKU: {}", sku);
        return productRepository.findBySku(sku);
    }
    
    // Get all active products with caching
    @Cacheable(value = "products", key = "'active:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Product> getAllActiveProducts(Pageable pageable) {
        logger.debug("Fetching all active products, page: {}", pageable.getPageNumber());
        return productRepository.findByIsActiveTrue(pageable);
    }
    
    // Search products with caching
    @Cacheable(value = "products", key = "'search:' + #search + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String search, Pageable pageable) {
        logger.debug("Searching products with term: {}", search);
        if (search == null || search.trim().isEmpty()) {
            return getAllActiveProducts(pageable);
        }
        return productRepository.findBySearchTerm(search.trim(), pageable);
    }
    
    // Get products with filters and caching
    @Cacheable(value = "products", key = "'filter:' + #search + ':' + #category + ':' + #brand + ':' + #availableForPurchase + ':' + #availableForRental + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Product> getProductsWithFilters(
            String search, String category, String brand, 
            Boolean availableForPurchase, Boolean availableForRental, 
            Pageable pageable) {
        logger.debug("Fetching products with filters - search: {}, category: {}, brand: {}", search, category, brand);
        return productRepository.findWithFilters(
            search, category, brand, availableForPurchase, availableForRental, pageable
        );
    }
    
    // Get products by category with caching
    @Cacheable(value = "products", key = "'category:' + #category + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Product> getProductsByCategory(String category, Pageable pageable) {
        logger.debug("Fetching products by category: {}", category);
        return productRepository.findByCategoryAndIsActiveTrue(category, pageable);
    }
    
    // Get products by brand with caching
    @Cacheable(value = "products", key = "'brand:' + #brand + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Product> getProductsByBrand(String brand, Pageable pageable) {
        logger.debug("Fetching products by brand: {}", brand);
        return productRepository.findByBrandAndIsActiveTrue(brand, pageable);
    }
    
    // Get products available for purchase with caching
    @Cacheable(value = "products", key = "'purchase:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Product> getProductsAvailableForPurchase(Pageable pageable) {
        logger.debug("Fetching products available for purchase");
        return productRepository.findByIsAvailableForPurchaseTrue(pageable);
    }
    
    // Get products available for rental with caching
    @Cacheable(value = "products", key = "'rental:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Product> getProductsAvailableForRental(Pageable pageable) {
        logger.debug("Fetching products available for rental");
        return productRepository.findByIsAvailableForRentalTrue(pageable);
    }
    
    // Get products by price range for purchase
    @Cacheable(value = "products", key = "'priceRange:' + #minPrice + ':' + #maxPrice + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Product> getProductsByPurchasePriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        logger.debug("Fetching products by purchase price range: {} - {}", minPrice, maxPrice);
        return productRepository.findByPurchasePriceRange(minPrice, maxPrice, pageable);
    }
    
    // Get products by rental price range
    @Cacheable(value = "products", key = "'rentalPriceRange:' + #minPrice + ':' + #maxPrice + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Product> getProductsByRentalPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        logger.debug("Fetching products by rental price range: {} - {}", minPrice, maxPrice);
        return productRepository.findByRentalPriceRange(minPrice, maxPrice, pageable);
    }
    
    // Get featured products with caching
    @Cacheable(value = "featuredProducts", key = "#pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Product> getFeaturedProducts(Pageable pageable) {
        logger.debug("Fetching featured products");
        return productRepository.findFeaturedProducts(pageable);
    }
    
    // Get distinct categories with caching
    @Cacheable(value = "productCategories")
    @Transactional(readOnly = true)
    public List<String> getDistinctCategories() {
        logger.debug("Fetching distinct product categories");
        return productRepository.findDistinctCategories();
    }
    
    // Get distinct brands with caching
    @Cacheable(value = "productBrands")
    @Transactional(readOnly = true)
    public List<String> getDistinctBrands() {
        logger.debug("Fetching distinct product brands");
        return productRepository.findDistinctBrands();
    }
    
    // Update product stock
    @CacheEvict(value = "products", key = "#productId")
    public boolean updatePurchaseStock(UUID productId, int quantity) {
        logger.info("Updating purchase stock for product: {}, quantity: {}", productId, quantity);
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            if (product.canReducePurchaseStock(quantity)) {
                product.reducePurchaseStock(quantity);
                productRepository.save(product);
                return true;
            }
        }
        return false;
    }
    
    // Update rental stock
    @CacheEvict(value = "products", key = "#productId")
    public boolean updateRentalStock(UUID productId, int quantity) {
        logger.info("Updating rental stock for product: {}, quantity: {}", productId, quantity);
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            if (product.canReduceRentalStock(quantity)) {
                product.reduceRentalStock(quantity);
                productRepository.save(product);
                return true;
            }
        }
        return false;
    }
    
    // Reserve stock
    @CacheEvict(value = "products", key = "#productId")
    public boolean reserveStock(UUID productId, int quantity) {
        logger.info("Reserving stock for product: {}, quantity: {}", productId, quantity);
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.reserveStock(quantity);
            productRepository.save(product);
            return true;
        }
        return false;
    }
    
    // Release reserved stock
    @CacheEvict(value = "products", key = "#productId")
    public boolean releaseReservedStock(UUID productId, int quantity) {
        logger.info("Releasing reserved stock for product: {}, quantity: {}", productId, quantity);
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.releaseReservedStock(quantity);
            productRepository.save(product);
            return true;
        }
        return false;
    }
    
    // Delete product
    @CacheEvict(value = {"products", "productCategories", "productBrands", "featuredProducts"}, allEntries = true)
    public void deleteProduct(UUID productId) {
        logger.info("Deleting product: {}", productId);
        productRepository.deleteById(productId);
    }
    
    // Deactivate product
    @CacheEvict(value = {"products", "featuredProducts"}, allEntries = true)
    public boolean deactivateProduct(UUID productId) {
        logger.info("Deactivating product: {}", productId);
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setActive(false);
            productRepository.save(product);
            return true;
        }
        return false;
    }
    
    // Get low stock products
    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts(int threshold) {
        logger.debug("Fetching low stock products with threshold: {}", threshold);
        return productRepository.findLowStockProducts(threshold);
    }
}