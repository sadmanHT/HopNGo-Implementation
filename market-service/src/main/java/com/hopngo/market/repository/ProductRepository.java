package com.hopngo.market.repository;

import com.hopngo.market.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    // Find by SKU
    Optional<Product> findBySku(String sku);
    
    // Find active products
    Page<Product> findByIsActiveTrue(Pageable pageable);
    
    // Find products by category
    Page<Product> findByCategoryAndIsActiveTrue(String category, Pageable pageable);
    
    // Find products by brand
    Page<Product> findByBrandAndIsActiveTrue(String brand, Pageable pageable);
    
    // Find products available for purchase
    Page<Product> findByAvailableForPurchaseTrueAndIsActiveTrue(Pageable pageable);
    
    // Find products available for rental
    Page<Product> findByAvailableForRentalTrueAndIsActiveTrue(Pageable pageable);
    
    // Full-text search on name and description
    @Query("SELECT p FROM Product p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "p.isActive = true")
    Page<Product> findBySearchTerm(@Param("search") String search, Pageable pageable);
    
    // Advanced search with filters
    @Query("SELECT p FROM Product p WHERE " +
           "(:search IS NULL OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:brand IS NULL OR p.brand = :brand) AND " +
           "(:availableForPurchase IS NULL OR p.availableForPurchase = :availableForPurchase) AND " +
           "(:availableForRental IS NULL OR p.availableForRental = :availableForRental) AND " +
           "p.isActive = true")
    Page<Product> findWithFilters(
        @Param("search") String search,
        @Param("category") String category,
        @Param("brand") String brand,
        @Param("availableForPurchase") Boolean availableForPurchase,
        @Param("availableForRental") Boolean availableForRental,
        Pageable pageable
    );
    
    // Find products with low stock
    @Query("SELECT p FROM Product p WHERE " +
           "(p.availableForPurchase = true AND p.purchaseStock <= :threshold) OR " +
           "(p.availableForRental = true AND p.rentalStock <= :threshold) AND " +
           "p.isActive = true")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);
    
    // Find products by multiple categories
    @Query("SELECT p FROM Product p WHERE p.category IN :categories AND p.isActive = true")
    Page<Product> findByCategoriesAndIsActiveTrue(@Param("categories") List<String> categories, Pageable pageable);
    
    // Find products by price range for purchase
    @Query("SELECT p FROM Product p WHERE " +
           "p.purchasePrice BETWEEN :minPrice AND :maxPrice AND " +
           "p.availableForPurchase = true AND p.isActive = true")
    Page<Product> findByPurchasePriceRange(
        @Param("minPrice") java.math.BigDecimal minPrice,
        @Param("maxPrice") java.math.BigDecimal maxPrice,
        Pageable pageable
    );
    
    // Find products by rental price range
    @Query("SELECT p FROM Product p WHERE " +
           "p.rentalPricePerDay BETWEEN :minPrice AND :maxPrice AND " +
           "p.availableForRental = true AND p.isActive = true")
    Page<Product> findByRentalPriceRange(
        @Param("minPrice") java.math.BigDecimal minPrice,
        @Param("maxPrice") java.math.BigDecimal maxPrice,
        Pageable pageable
    );
    
    // Get distinct categories
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.isActive = true ORDER BY p.category")
    List<String> findDistinctCategories();
    
    // Get distinct brands
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND p.isActive = true ORDER BY p.brand")
    List<String> findDistinctBrands();
    
    // Count products by category
    @Query("SELECT p.category, COUNT(p) FROM Product p WHERE p.isActive = true GROUP BY p.category")
    List<Object[]> countProductsByCategory();
    
    // Find featured/popular products (can be based on some criteria)
    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.createdAt DESC")
    Page<Product> findFeaturedProducts(Pageable pageable);
}