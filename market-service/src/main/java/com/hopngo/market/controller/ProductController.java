package com.hopngo.market.controller;

import com.hopngo.market.entity.Product;
import com.hopngo.market.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/market/products")
@Tag(name = "Products", description = "Product management and catalog endpoints")
public class ProductController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    
    @Autowired
    private ProductService productService;
    
    // Get all products with search and filters
    @GetMapping
    @Operation(summary = "Get products with search and filters", 
               description = "Retrieve products with optional search query and filters")
    public ResponseEntity<Page<Product>> getProducts(
            @Parameter(description = "Search query for product name, description, or SKU")
            @RequestParam(required = false) String search,
            
            @Parameter(description = "Filter by category")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "Filter by brand")
            @RequestParam(required = false) String brand,
            
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) BigDecimal minPrice,
            
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) BigDecimal maxPrice,
            
            @Parameter(description = "Filter by availability for purchase")
            @RequestParam(required = false) Boolean availableForPurchase,
            
            @Parameter(description = "Filter by availability for rental")
            @RequestParam(required = false) Boolean availableForRental,
            
            @Parameter(description = "Filter by active status")
            @RequestParam(defaultValue = "true") Boolean active,
            
            @Parameter(description = "Sort by field (name, price, createdAt)")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "Sort direction (asc, desc)")
            @RequestParam(defaultValue = "desc") String sortDir,
            
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("Getting products - search: {}, category: {}, brand: {}, page: {}, size: {}", 
                   search, category, brand, page, size);
        
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Product> products;
        
        if (search != null && !search.trim().isEmpty()) {
            // Use search functionality
            products = productService.searchProducts(search.trim(), pageable);
        } else if (hasFilters(category, brand, minPrice, maxPrice, availableForPurchase, availableForRental)) {
            // Use advanced filtering
            products = productService.getProductsWithFilters(
                search, category, brand, 
                availableForPurchase, availableForRental, pageable);
        } else {
            // Get all active products
            products = productService.getAllActiveProducts(pageable);
        }
        
        logger.info("Retrieved {} products", products.getTotalElements());
        return ResponseEntity.ok(products);
    }
    
    // Get product by ID
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve a specific product by its ID")
    public ResponseEntity<Product> getProductById(
            @Parameter(description = "Product ID") @PathVariable UUID id) {
        
        logger.info("Getting product by ID: {}", id);
        
        Optional<Product> product = productService.getProductById(id);
        
        if (product.isPresent()) {
            logger.info("Product found: {}", product.get().getName());
            return ResponseEntity.ok(product.get());
        } else {
            logger.warn("Product not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    // Get product by SKU
    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU", description = "Retrieve a specific product by its SKU")
    public ResponseEntity<Product> getProductBySku(
            @Parameter(description = "Product SKU") @PathVariable String sku) {
        
        logger.info("Getting product by SKU: {}", sku);
        
        Optional<Product> product = productService.getProductBySku(sku);
        
        if (product.isPresent()) {
            logger.info("Product found: {}", product.get().getName());
            return ResponseEntity.ok(product.get());
        } else {
            logger.warn("Product not found with SKU: {}", sku);
            return ResponseEntity.notFound().build();
        }
    }
    
    // Get products by category
    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category", description = "Retrieve products in a specific category")
    public ResponseEntity<Page<Product>> getProductsByCategory(
            @Parameter(description = "Product category") @PathVariable String category,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDir) {
        
        logger.info("Getting products by category: {}", category);
        
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Product> products = productService.getProductsByCategory(category, pageable);
        
        logger.info("Retrieved {} products in category: {}", products.getTotalElements(), category);
        return ResponseEntity.ok(products);
    }
    
    // Get featured products
    @GetMapping("/featured")
    @Operation(summary = "Get featured products", description = "Retrieve featured products")
    public ResponseEntity<List<Product>> getFeaturedProducts(
            @Parameter(description = "Maximum number of featured products") @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("Getting featured products, limit: {}", limit);
        
        Page<Product> productsPage = productService.getFeaturedProducts(PageRequest.of(0, limit));
        List<Product> products = productsPage.getContent();
        
        logger.info("Retrieved {} featured products", products.size());
        return ResponseEntity.ok(products);
    }
    
    // Get product categories
    @GetMapping("/categories")
    @Operation(summary = "Get all product categories", description = "Retrieve list of all available product categories")
    public ResponseEntity<List<String>> getProductCategories() {
        logger.info("Getting all product categories");
        
        List<String> categories = productService.getDistinctCategories();
        
        logger.info("Retrieved {} categories", categories.size());
        return ResponseEntity.ok(categories);
    }
    
    // Get product brands
    @GetMapping("/brands")
    @Operation(summary = "Get all product brands", description = "Retrieve list of all available product brands")
    public ResponseEntity<List<String>> getProductBrands() {
        logger.info("Getting all product brands");
        
        List<String> brands = productService.getDistinctBrands();
        
        logger.info("Retrieved {} brands", brands.size());
        return ResponseEntity.ok(brands);
    }
    
    // Create product (admin endpoint)
    @PostMapping
    @Operation(summary = "Create new product", description = "Create a new product (admin only)")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        logger.info("Creating new product: {}", product.getName());
        
        Product createdProduct = productService.saveProduct(product);
        
        logger.info("Product created successfully: {}", createdProduct.getId());
        return ResponseEntity.ok(createdProduct);
    }
    
    // Update product (admin endpoint)
    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Update an existing product (admin only)")
    public ResponseEntity<Product> updateProduct(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Valid @RequestBody Product product) {
        
        logger.info("Updating product: {}", id);
        
        try {
            product.setId(id); // Ensure the product has the correct ID for update
            Product updatedProduct = productService.saveProduct(product);
            logger.info("Product updated successfully: {}", id);
            return ResponseEntity.ok(updatedProduct);
        } catch (IllegalArgumentException e) {
            logger.warn("Product not found for update: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    // Delete product (admin endpoint)
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Delete a product (admin only)")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID") @PathVariable UUID id) {
        
        logger.info("Deleting product: {}", id);
        
        try {
            productService.deleteProduct(id);
            logger.info("Product deleted successfully: {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Product not found for deletion: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    // Check product availability
    @GetMapping("/{id}/availability")
    @Operation(summary = "Check product availability", description = "Check if product is available for purchase or rental")
    public ResponseEntity<ProductAvailability> checkProductAvailability(
            @Parameter(description = "Product ID") @PathVariable UUID id) {
        
        logger.info("Checking availability for product: {}", id);
        
        Optional<Product> productOpt = productService.getProductById(id);
        
        if (productOpt.isEmpty()) {
            logger.warn("Product not found: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        Product product = productOpt.get();
        ProductAvailability availability = new ProductAvailability(
            product.getIsAvailableForPurchase() && product.getStockQuantity() > 0,
            product.getIsAvailableForRental() && product.getRentalStockQuantity() > 0,
            product.getStockQuantity(),
            product.getRentalStockQuantity()
        );
        
        return ResponseEntity.ok(availability);
    }
    
    // Helper method to check if any filters are applied
    private boolean hasFilters(String category, String brand, BigDecimal minPrice, BigDecimal maxPrice,
                              Boolean availableForPurchase, Boolean availableForRental) {
        return category != null || brand != null || minPrice != null || maxPrice != null ||
               availableForPurchase != null || availableForRental != null;
    }
    
    // Inner class for product availability response
    public static class ProductAvailability {
        private boolean availableForPurchase;
        private boolean availableForRental;
        private int purchaseStock;
        private int rentalStock;
        
        public ProductAvailability(boolean availableForPurchase, boolean availableForRental,
                                 int purchaseStock, int rentalStock) {
            this.availableForPurchase = availableForPurchase;
            this.availableForRental = availableForRental;
            this.purchaseStock = purchaseStock;
            this.rentalStock = rentalStock;
        }
        
        // Getters
        public boolean isAvailableForPurchase() { return availableForPurchase; }
        public boolean isAvailableForRental() { return availableForRental; }
        public int getPurchaseStock() { return purchaseStock; }
        public int getRentalStock() { return rentalStock; }
    }
}