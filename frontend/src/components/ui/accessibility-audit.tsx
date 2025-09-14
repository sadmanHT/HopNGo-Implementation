'use client';

import React, { useEffect, useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

// Accessibility Audit Types
interface AccessibilityIssue {
  id: string;
  type: 'error' | 'warning' | 'info';
  category: 'keyboard' | 'aria' | 'color' | 'focus' | 'semantic' | 'images';
  element: Element;
  message: string;
  suggestion: string;
  wcagLevel: 'A' | 'AA' | 'AAA';
}

interface AuditResults {
  issues: AccessibilityIssue[];
  score: number;
  totalChecks: number;
  passedChecks: number;
}

// Accessibility Audit Hook
export const useAccessibilityAudit = (enabled = false) => {
  const [results, setResults] = useState<AuditResults | null>(null);
  const [isAuditing, setIsAuditing] = useState(false);

  const runAudit = async (): Promise<AuditResults> => {
    setIsAuditing(true);
    const issues: AccessibilityIssue[] = [];
    let totalChecks = 0;
    let passedChecks = 0;

    // Helper function to add issue
    const addIssue = (
      type: AccessibilityIssue['type'],
      category: AccessibilityIssue['category'],
      element: Element,
      message: string,
      suggestion: string,
      wcagLevel: AccessibilityIssue['wcagLevel'] = 'AA'
    ) => {
      issues.push({
        id: `${category}-${issues.length}`,
        type,
        category,
        element,
        message,
        suggestion,
        wcagLevel,
      });
    };

    // Check for missing alt text on images
    totalChecks++;
    const images = document.querySelectorAll('img');
    let imageIssues = 0;
    images.forEach((img) => {
      if (!img.alt && !img.getAttribute('aria-label')) {
        addIssue(
          'error',
          'images',
          img,
          'Image missing alt text',
          'Add descriptive alt text or aria-label to the image',
          'A'
        );
        imageIssues++;
      }
    });
    if (imageIssues === 0) passedChecks++;

    // Check for proper heading hierarchy
    totalChecks++;
    const headings = document.querySelectorAll('h1, h2, h3, h4, h5, h6');
    let lastLevel = 0;
    let headingIssues = 0;
    headings.forEach((heading) => {
      const level = parseInt(heading.tagName.charAt(1));
      if (level > lastLevel + 1) {
        addIssue(
          'warning',
          'semantic',
          heading,
          `Heading level skipped from h${lastLevel} to h${level}`,
          'Use proper heading hierarchy without skipping levels',
          'AA'
        );
        headingIssues++;
      }
      lastLevel = level;
    });
    if (headingIssues === 0) passedChecks++;

    // Check for buttons without accessible names
    totalChecks++;
    const buttons = document.querySelectorAll('button');
    let buttonIssues = 0;
    buttons.forEach((button) => {
      const hasText = button.textContent?.trim();
      const hasAriaLabel = button.getAttribute('aria-label');
      const hasAriaLabelledBy = button.getAttribute('aria-labelledby');
      
      if (!hasText && !hasAriaLabel && !hasAriaLabelledBy) {
        addIssue(
          'error',
          'aria',
          button,
          'Button without accessible name',
          'Add text content, aria-label, or aria-labelledby to the button',
          'A'
        );
        buttonIssues++;
      }
    });
    if (buttonIssues === 0) passedChecks++;

    // Check for links without accessible names
    totalChecks++;
    const links = document.querySelectorAll('a');
    let linkIssues = 0;
    links.forEach((link) => {
      const hasText = link.textContent?.trim();
      const hasAriaLabel = link.getAttribute('aria-label');
      const hasAriaLabelledBy = link.getAttribute('aria-labelledby');
      
      if (!hasText && !hasAriaLabel && !hasAriaLabelledBy) {
        addIssue(
          'error',
          'aria',
          link,
          'Link without accessible name',
          'Add descriptive text content, aria-label, or aria-labelledby to the link',
          'A'
        );
        linkIssues++;
      }
    });
    if (linkIssues === 0) passedChecks++;

    // Check for form inputs without labels
    totalChecks++;
    const inputs = document.querySelectorAll('input, textarea, select');
    let inputIssues = 0;
    inputs.forEach((input) => {
      const id = input.getAttribute('id');
      const hasLabel = id && document.querySelector(`label[for="${id}"]`);
      const hasAriaLabel = input.getAttribute('aria-label');
      const hasAriaLabelledBy = input.getAttribute('aria-labelledby');
      
      if (!hasLabel && !hasAriaLabel && !hasAriaLabelledBy) {
        addIssue(
          'error',
          'aria',
          input,
          'Form input without label',
          'Associate the input with a label element or add aria-label',
          'A'
        );
        inputIssues++;
      }
    });
    if (inputIssues === 0) passedChecks++;

    // Check for sufficient color contrast (simplified check)
    totalChecks++;
    const textElements = document.querySelectorAll('p, span, div, h1, h2, h3, h4, h5, h6, a, button');
    let contrastIssues = 0;
    textElements.forEach((element) => {
      const styles = window.getComputedStyle(element);
      const color = styles.color;
      const backgroundColor = styles.backgroundColor;
      
      // Simplified contrast check (in real implementation, use proper contrast calculation)
      if (color === 'rgb(128, 128, 128)' && backgroundColor === 'rgb(255, 255, 255)') {
        addIssue(
          'warning',
          'color',
          element,
          'Potentially insufficient color contrast',
          'Ensure text has sufficient contrast ratio (4.5:1 for normal text, 3:1 for large text)',
          'AA'
        );
        contrastIssues++;
      }
    });
    if (contrastIssues === 0) passedChecks++;

    // Check for keyboard accessibility
    totalChecks++;
    const interactiveElements = document.querySelectorAll('button, a, input, textarea, select, [tabindex]');
    let keyboardIssues = 0;
    interactiveElements.forEach((element) => {
      const tabIndex = element.getAttribute('tabindex');
      if (tabIndex && parseInt(tabIndex) > 0) {
        addIssue(
          'warning',
          'keyboard',
          element,
          'Positive tabindex found',
          'Avoid positive tabindex values. Use 0 or -1, or rely on natural tab order',
          'A'
        );
        keyboardIssues++;
      }
    });
    if (keyboardIssues === 0) passedChecks++;

    // Check for focus indicators
    totalChecks++;
    const focusableElements = document.querySelectorAll('button, a, input, textarea, select');
    let focusIssues = 0;
    focusableElements.forEach((element) => {
      const styles = window.getComputedStyle(element, ':focus');
      const outline = styles.outline;
      const boxShadow = styles.boxShadow;
      
      if (outline === 'none' && boxShadow === 'none') {
        addIssue(
          'warning',
          'focus',
          element,
          'Element may lack visible focus indicator',
          'Ensure focusable elements have visible focus indicators',
          'AA'
        );
        focusIssues++;
      }
    });
    if (focusIssues === 0) passedChecks++;

    // Check for ARIA roles and properties
    totalChecks++;
    const ariaElements = document.querySelectorAll('[role], [aria-expanded], [aria-selected], [aria-checked]');
    let ariaIssues = 0;
    ariaElements.forEach((element) => {
      const role = element.getAttribute('role');
      const ariaExpanded = element.getAttribute('aria-expanded');
      const ariaSelected = element.getAttribute('aria-selected');
      const ariaChecked = element.getAttribute('aria-checked');
      
      // Check for invalid ARIA values
      if (ariaExpanded && !['true', 'false'].includes(ariaExpanded)) {
        addIssue(
          'error',
          'aria',
          element,
          'Invalid aria-expanded value',
          'aria-expanded must be "true" or "false"',
          'A'
        );
        ariaIssues++;
      }
      
      if (ariaSelected && !['true', 'false'].includes(ariaSelected)) {
        addIssue(
          'error',
          'aria',
          element,
          'Invalid aria-selected value',
          'aria-selected must be "true" or "false"',
          'A'
        );
        ariaIssues++;
      }
      
      if (ariaChecked && !['true', 'false', 'mixed'].includes(ariaChecked)) {
        addIssue(
          'error',
          'aria',
          element,
          'Invalid aria-checked value',
          'aria-checked must be "true", "false", or "mixed"',
          'A'
        );
        ariaIssues++;
      }
    });
    if (ariaIssues === 0) passedChecks++;

    const score = totalChecks > 0 ? Math.round((passedChecks / totalChecks) * 100) : 0;
    
    const results: AuditResults = {
      issues,
      score,
      totalChecks,
      passedChecks,
    };

    setResults(results);
    setIsAuditing(false);
    return results;
  };

  useEffect(() => {
    if (enabled && process.env.NODE_ENV === 'development') {
      const timer = setTimeout(() => {
        runAudit();
      }, 1000); // Delay to allow page to fully render
      
      return () => clearTimeout(timer);
    }
  }, [enabled]);

  return {
    results,
    isAuditing,
    runAudit,
  };
};

// Accessibility Audit Panel Component
interface AccessibilityAuditPanelProps {
  isOpen: boolean;
  onClose: () => void;
  results: AuditResults | null;
  isAuditing: boolean;
  onRunAudit: () => void;
}

export const AccessibilityAuditPanel: React.FC<AccessibilityAuditPanelProps> = ({
  isOpen,
  onClose,
  results,
  isAuditing,
  onRunAudit,
}) => {
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [highlightedElement, setHighlightedElement] = useState<Element | null>(null);

  const categories = ['all', 'keyboard', 'aria', 'color', 'focus', 'semantic', 'images'];
  
  const filteredIssues = results?.issues.filter(
    issue => selectedCategory === 'all' || issue.category === selectedCategory
  ) || [];

  const getScoreColor = (score: number) => {
    if (score >= 90) return 'text-green-600';
    if (score >= 70) return 'text-yellow-600';
    return 'text-red-600';
  };

  const getIssueTypeColor = (type: AccessibilityIssue['type']) => {
    switch (type) {
      case 'error': return 'text-red-600 bg-red-50 border-red-200';
      case 'warning': return 'text-yellow-600 bg-yellow-50 border-yellow-200';
      case 'info': return 'text-blue-600 bg-blue-50 border-blue-200';
    }
  };

  const highlightElement = (element: Element) => {
    // Remove previous highlights
    document.querySelectorAll('.a11y-highlight').forEach(el => {
      el.classList.remove('a11y-highlight');
    });
    
    // Add highlight to current element
    element.classList.add('a11y-highlight');
    element.scrollIntoView({ behavior: 'smooth', block: 'center' });
    setHighlightedElement(element);
    
    // Remove highlight after 3 seconds
    setTimeout(() => {
      element.classList.remove('a11y-highlight');
      setHighlightedElement(null);
    }, 3000);
  };

  useEffect(() => {
    // Add CSS for highlighting
    const style = document.createElement('style');
    style.textContent = `
      .a11y-highlight {
        outline: 3px solid #f59e0b !important;
        outline-offset: 2px !important;
        background-color: rgba(245, 158, 11, 0.1) !important;
      }
    `;
    document.head.appendChild(style);
    
    return () => {
      document.head.removeChild(style);
    };
  }, []);

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ x: '100%' }}
          animate={{ x: 0 }}
          exit={{ x: '100%' }}
          transition={{ type: 'spring', stiffness: 300, damping: 30 }}
          className="fixed top-0 right-0 h-full w-96 bg-white shadow-2xl z-50 overflow-hidden flex flex-col"
        >
          {/* Header */}
          <div className="bg-gray-50 border-b border-gray-200 p-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-gray-900">
                Accessibility Audit
              </h2>
              <button
                onClick={onClose}
                className="p-1 rounded-md hover:bg-gray-200 transition-colors"
                aria-label="Close accessibility audit panel"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            
            <div className="mt-4 flex items-center gap-4">
              <button
                onClick={onRunAudit}
                disabled={isAuditing}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {isAuditing ? 'Auditing...' : 'Run Audit'}
              </button>
              
              {results && (
                <div className={`text-2xl font-bold ${getScoreColor(results.score)}`}>
                  {results.score}%
                </div>
              )}
            </div>
          </div>

          {/* Content */}
          <div className="flex-1 overflow-y-auto">
            {results && (
              <>
                {/* Summary */}
                <div className="p-4 border-b border-gray-200">
                  <div className="grid grid-cols-3 gap-4 text-center">
                    <div>
                      <div className="text-2xl font-bold text-gray-900">{results.totalChecks}</div>
                      <div className="text-sm text-gray-600">Total Checks</div>
                    </div>
                    <div>
                      <div className="text-2xl font-bold text-green-600">{results.passedChecks}</div>
                      <div className="text-sm text-gray-600">Passed</div>
                    </div>
                    <div>
                      <div className="text-2xl font-bold text-red-600">{results.issues.length}</div>
                      <div className="text-sm text-gray-600">Issues</div>
                    </div>
                  </div>
                </div>

                {/* Category Filter */}
                <div className="p-4 border-b border-gray-200">
                  <div className="flex flex-wrap gap-2">
                    {categories.map((category) => {
                      const count = category === 'all' 
                        ? results.issues.length 
                        : results.issues.filter(issue => issue.category === category).length;
                      
                      return (
                        <button
                          key={category}
                          onClick={() => setSelectedCategory(category)}
                          className={`px-3 py-1 rounded-full text-sm transition-colors ${
                            selectedCategory === category
                              ? 'bg-blue-600 text-white'
                              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                          }`}
                        >
                          {category} ({count})
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Issues List */}
                <div className="p-4">
                  {filteredIssues.length === 0 ? (
                    <div className="text-center text-gray-500 py-8">
                      {selectedCategory === 'all' 
                        ? 'No accessibility issues found! 🎉'
                        : `No ${selectedCategory} issues found`
                      }
                    </div>
                  ) : (
                    <div className="space-y-3">
                      {filteredIssues.map((issue) => (
                        <motion.div
                          key={issue.id}
                          initial={{ opacity: 0, y: 10 }}
                          animate={{ opacity: 1, y: 0 }}
                          className={`p-3 rounded-lg border cursor-pointer transition-all hover:shadow-md ${
                            getIssueTypeColor(issue.type)
                          }`}
                          onClick={() => highlightElement(issue.element)}
                        >
                          <div className="flex items-start justify-between">
                            <div className="flex-1">
                              <div className="flex items-center gap-2 mb-1">
                                <span className="text-xs font-medium uppercase tracking-wide">
                                  {issue.type}
                                </span>
                                <span className="text-xs bg-gray-200 text-gray-700 px-2 py-0.5 rounded">
                                  WCAG {issue.wcagLevel}
                                </span>
                              </div>
                              <h4 className="font-medium text-sm mb-1">{issue.message}</h4>
                              <p className="text-xs opacity-75">{issue.suggestion}</p>
                            </div>
                            <svg className="w-4 h-4 ml-2 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                            </svg>
                          </div>
                        </motion.div>
                      ))}
                    </div>
                  )}
                </div>
              </>
            )}
            
            {!results && !isAuditing && (
              <div className="p-4 text-center text-gray-500">
                Click "Run Audit" to analyze accessibility issues
              </div>
            )}
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

// Accessibility Audit Trigger Button
interface AccessibilityAuditTriggerProps {
  className?: string;
}

export const AccessibilityAuditTrigger: React.FC<AccessibilityAuditTriggerProps> = ({
  className = '',
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const { results, isAuditing, runAudit } = useAccessibilityAudit(false);

  // Only show in development
  if (process.env.NODE_ENV !== 'development') {
    return null;
  }

  return (
    <>
      <button
        onClick={() => setIsOpen(true)}
        className={`fixed bottom-4 right-4 p-3 bg-blue-600 text-white rounded-full shadow-lg hover:bg-blue-700 transition-colors z-40 ${className}`}
        aria-label="Open accessibility audit panel"
        title="Accessibility Audit"
      >
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        {results && results.issues.length > 0 && (
          <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs rounded-full w-6 h-6 flex items-center justify-center">
            {results.issues.length}
          </span>
        )}
      </button>
      
      <AccessibilityAuditPanel
        isOpen={isOpen}
        onClose={() => setIsOpen(false)}
        results={results}
        isAuditing={isAuditing}
        onRunAudit={runAudit}
      />
    </>
  );
};

// Auto Accessibility Audit Component (runs automatically)
export const AutoAccessibilityAudit: React.FC = () => {
  const { results } = useAccessibilityAudit(true);
  
  // Log results to console in development
  useEffect(() => {
    if (results && process.env.NODE_ENV === 'development') {
      console.group('🔍 Accessibility Audit Results');
      console.log(`Score: ${results.score}%`);
      console.log(`Issues found: ${results.issues.length}`);
      
      if (results.issues.length > 0) {
        console.group('Issues:');
        results.issues.forEach((issue, index) => {
          console.log(`${index + 1}. [${issue.type.toUpperCase()}] ${issue.message}`);
          console.log(`   Suggestion: ${issue.suggestion}`);
          console.log(`   Element:`, issue.element);
        });
        console.groupEnd();
      }
      
      console.groupEnd();
    }
  }, [results]);
  
  return null;
};