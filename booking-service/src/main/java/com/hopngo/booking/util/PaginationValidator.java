package com.hopngo.booking.util;

import org.springframework.stereotype.Component;

@Component
public class PaginationValidator {
    
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 50;
    public static final int MIN_PAGE_SIZE = 1;
    
    public static class ValidatedPagination {
        private final int page;
        private final int size;
        
        public ValidatedPagination(int page, int size) {
            this.page = page;
            this.size = size;
        }
        
        public int getPage() { return page; }
        public int getSize() { return size; }
    }
    
    public static ValidatedPagination validate(int page, int size) {
        // Validate page number
        if (page < 0) {
            page = 0;
        }
        
        // Validate page size
        if (size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        } else if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }
        
        return new ValidatedPagination(page, size);
    }
    
    public static ValidatedPagination validateWithDefaults(Integer page, Integer size) {
        int validatedPage = (page != null) ? page : 0;
        int validatedSize = (size != null) ? size : DEFAULT_PAGE_SIZE;
        
        return validate(validatedPage, validatedSize);
    }
}