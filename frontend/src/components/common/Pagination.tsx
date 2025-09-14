import React from 'react';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (page: number) => void;
  disabled?: boolean;
  showInfo?: boolean;
  size?: number;
}

const Pagination: React.FC<PaginationProps> = ({ 
  currentPage, 
  totalPages, 
  totalElements, 
  onPageChange, 
  disabled = false,
  showInfo = true,
  size = 10
}) => {
  if (totalPages <= 1) {
    return null;
  }

  const getVisiblePages = () => {
    const delta = 2;
    const range = [];
    const rangeWithDots = [];

    for (let i = Math.max(2, currentPage - delta); 
         i <= Math.min(totalPages - 1, currentPage + delta); 
         i++) {
      range.push(i);
    }

    if (currentPage - delta > 2) {
      rangeWithDots.push(1, '...');
    } else {
      rangeWithDots.push(1);
    }

    rangeWithDots.push(...range);

    if (currentPage + delta < totalPages - 1) {
      rangeWithDots.push('...', totalPages);
    } else {
      rangeWithDots.push(totalPages);
    }

    return rangeWithDots;
  };

  const handlePageChange = (page: number) => {
    if (page !== currentPage && page >= 1 && page <= totalPages && !disabled) {
      onPageChange(page - 1); // Convert to 0-based index
    }
  };

  const visiblePages = getVisiblePages();

  return (
    <div className="flex items-center justify-between">
      {/* Page info */}
      {showInfo && (
        <div className="flex-1 flex justify-between sm:hidden">
          <button
            onClick={() => handlePageChange(currentPage)}
            disabled={currentPage <= 1 || disabled}
            className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:bg-gray-50 disabled:text-gray-400 disabled:cursor-not-allowed"
          >
            Previous
          </button>
          <button
            onClick={() => handlePageChange(currentPage + 2)}
            disabled={currentPage >= totalPages || disabled}
            className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:bg-gray-50 disabled:text-gray-400 disabled:cursor-not-allowed"
          >
            Next
          </button>
        </div>
      )}

      {/* Desktop pagination */}
      <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
        {showInfo && (
          <div>
            <p className="text-sm text-gray-700">
              Showing{' '}
              <span className="font-medium">
                {Math.min(currentPage * size + 1, totalElements)}
              </span>{' '}
              to{' '}
              <span className="font-medium">
                {Math.min((currentPage + 1) * size, totalElements)}
              </span>{' '}
              of{' '}
              <span className="font-medium">{totalElements}</span>{' '}
              results
            </p>
          </div>
        )}
        
        <div>
          <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
            {/* Previous button */}
            <button
              onClick={() => handlePageChange(currentPage)}
              disabled={currentPage <= 1 || disabled}
              className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:bg-gray-50 disabled:text-gray-300 disabled:cursor-not-allowed"
            >
              <span className="sr-only">Previous</span>
              <svg className="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clipRule="evenodd" />
              </svg>
            </button>

            {/* Page numbers */}
            {visiblePages.map((page, index) => {
              if (page === '...') {
                return (
                  <span
                    key={`dots-${index}`}
                    className="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700"
                  >
                    ...
                  </span>
                );
              }

              const isCurrentPage = page === currentPage + 1; // Convert from 0-based to 1-based
              
              return (
                <button
                  key={page}
                  onClick={() => typeof page === 'number' && handlePageChange(page)}
                  disabled={disabled}
                  className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium ${
                    isCurrentPage
                      ? 'z-10 bg-blue-50 border-blue-500 text-blue-600'
                      : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
                  } ${disabled ? 'cursor-not-allowed opacity-50' : ''}`}
                >
                  {page}
                </button>
              );
            })}

            {/* Next button */}
            <button
              onClick={() => handlePageChange(currentPage + 2)}
              disabled={currentPage >= totalPages || disabled}
              className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:bg-gray-50 disabled:text-gray-300 disabled:cursor-not-allowed"
            >
              <span className="sr-only">Next</span>
              <svg className="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
              </svg>
            </button>
          </nav>
        </div>
      </div>
    </div>
  );
};

export default Pagination;