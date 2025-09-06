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
    
    // Find by SKU (custom query since SKU is dynamically generated)
    @Query("SELECT p FROM Product p WHERE CONCAT(REPLACE(LOWER(p.name), ' ', '-'), '-', CAST(p.id AS string)) = :sku")
    Optional<Product> findBySku(@Param("sku") String sku);
    
    // Find active products
    @Query("SELECT p FROM Product p WHERE (p.isAvailableForPurchase = true OR p.isAvailableForRental = true)")
    Page<Product> findByIsActiveTrue(Pageable pageable);
    
    // Find products by category
    @Query("SELECT p FROM Product p WHERE p.category = :category AND (p.isAvailableForPurchase = true OR p.isAvailableForRental = true)")
    Page<Product> findByCategoryAndIsActiveTrue(@Param("category") String category, Pageable pageable);
    
    // Find products by brand
    @Query("SELECT p FROM Product p WHERE p.brand = :brand AND (p.isAvailableForPurchase = true OR p.isAvailableForRental = true)")
    Page<Product> findByBrandAndIsActiveTrue(@Param("brand") String brand, Pageable pageable);
    
    // Find products available for purchase
    Page<Product> findByIsAvailableForPurchaseTrue(Pageable pageable);
    
    // Find products available for rental
    Page<Product> findByIsAvailableForRentalTrue(Pageable pageable);
    
    // Full-text search on name and description
    @Query("SELECT p FROM Product p WHERE " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(p.isAvailableForPurchase = true OR p.isAvailableForRental = true)")
    Page<Product> findBySearchTerm(@Param("search") String search, Pageable pageable);
    
    // Advanced search with filters
    @Query("SELECT p FROM Product p WHERE " +
           "(:search IS NULL OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:brand IS NULL OR p.brand = :brand) AND " +
           "(:availableForPurchase IS NULL OR p.isAvailableForPurchase = :availableForPurchase) AND " +
           "(:availableForRental IS NULL OR p.isAvailableForRental = :availableForRental) AND " +
           "(p.isAvailableForPurchase = true OR p.isAvailableForRental = true)")
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
           "(p.isAvailableForPurchase = true AND p.stockQuantity <= :threshold) OR " +
           "(p.isAvailableForRental = true AND p.stockQuantity <= :threshold) AND " +
           "(p.isAvailableForPurchase = true OR p.isAvailableForRental = true)")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);
    
    // Find products by multiple categories
    @Query("SELECT p FROM Product p WHERE p.category IN :categories AND (p.isAvailableForPurchase = true OR p.isAvailableForRental = true)")
    Page<Product> findByCategoriesAndIsActiveTrue(@Param("categories") List<String> categories, Pageable pageable);
    
    // Find products by price range for purchase
    @Query("SELECT p FROM Product p WHERE " +
           "p.price BETWEEN :minPrice AND :maxPrice AND " +
           "p.isAvailableForPurchase = true AND (p.isAvailableForPurchase = true OR p.isAvailableForRental = true)")
    Page<Product> findByPurchasePriceRange(
        @Param("minPrice") java.math.BigDecimal minPrice,
        @Param("maxPrice") java.math.BigDecimal maxPrice,
        Pageable pageable
    );
    
    // Find products by rental price range
    @Query("SELECT p FROM Product p WHERE " +
           "p.rentalPricePerDay BETWEEN :minPrice AND :maxPrice AND " +
           "p.isAvailableForRental = true AND (p.isAvailableForPurchase = true OR p.isAvailableForRental = true)")
    Page<Product> findByRentalPriceRange(
        @Param("minPrice") java.math.BigDecimal minPrice,
        @Param("maxPrice") java.math.BigDecimal maxPrice,
        Pageable pageable
    );
    
    // Get distinct categories
    @Query("SELECT DISTINCT p.category FROM Product p WHERE (p.isAvailableForPurchase = true OR p.isAvailableForRental = true) ORDER BY p.category")
    List<String> findDistinctCategories();
    
    // Get distinct brands
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND (p.isAvailableForPurchase = true OR p.isAvailableForRental = true) ORDER BY p.brand")
    List<String> findDistinctBrands();
    
    // Count products by category
    @Query("SELECT p.category, COUNT(p) FROM Product p WHERE (p.isAvailableForPurchase = true OR p.isAvailableForRental = true) GROUP BY p.category")
    List<Object[]> countProductsByCategory();
    
    // Find featured/popular products (can be based on some criteria)
    @Query("SELECT p FROM Product p WHERE (p.isAvailableForPurchase = true OR p.isAvailableForRental = true) ORDER BY p.createdAt DESC")
    Page<Product> findFeaturedProducts(Pageable pageable);
}