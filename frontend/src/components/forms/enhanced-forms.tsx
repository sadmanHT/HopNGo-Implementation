'use client';

import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Eye, EyeOff, Check, X, AlertCircle, Info } from 'lucide-react';

// Form Field Types
interface FormFieldProps {
  id: string;
  label: string;
  type?: 'text' | 'email' | 'password' | 'tel' | 'url' | 'search';
  value: string;
  onChange: (value: string) => void;
  onBlur?: () => void;
  error?: string;
  success?: boolean;
  required?: boolean;
  disabled?: boolean;
  placeholder?: string;
  helpText?: string;
  autoComplete?: string;
  maxLength?: number;
  pattern?: string;
  'aria-describedby'?: string;
}

interface TextAreaProps extends Omit<FormFieldProps, 'type'> {
  rows?: number;
  resize?: 'none' | 'vertical' | 'horizontal' | 'both';
}

interface SelectProps extends Omit<FormFieldProps, 'type' | 'value' | 'onChange'> {
  value: string;
  onChange: (value: string) => void;
  options: Array<{ value: string; label: string; disabled?: boolean }>;
  multiple?: boolean;
}

interface CheckboxProps {
  id: string;
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  disabled?: boolean;
  error?: string;
  description?: string;
  required?: boolean;
}

interface RadioGroupProps {
  name: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: Array<{ value: string; label: string; description?: string; disabled?: boolean }>;
  error?: string;
  required?: boolean;
  orientation?: 'horizontal' | 'vertical';
}

// Enhanced Input Field
export const EnhancedInput: React.FC<FormFieldProps> = ({
  id,
  label,
  type = 'text',
  value,
  onChange,
  onBlur,
  error,
  success,
  required = false,
  disabled = false,
  placeholder,
  helpText,
  autoComplete,
  maxLength,
  pattern,
  'aria-describedby': ariaDescribedBy,
}) => {
  const [isFocused, setIsFocused] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const hasValue = value.length > 0;
  const isPassword = type === 'password';
  const inputType = isPassword && showPassword ? 'text' : type;

  const fieldVariants = {
    default: { scale: 1, borderColor: 'var(--color-border)' },
    focused: { scale: 1.02, borderColor: 'var(--color-focus)' },
    error: { scale: 1, borderColor: 'var(--color-status-error)' },
    success: { scale: 1, borderColor: 'var(--color-status-success)' },
  };

  const getFieldState = () => {
    if (error) return 'error';
    if (success) return 'success';
    if (isFocused) return 'focused';
    return 'default';
  };

  const describedBy = [
    error && `${id}-error`,
    helpText && `${id}-help`,
    ariaDescribedBy,
  ].filter(Boolean).join(' ');

  return (
    <div className="form-field">
      <div className="relative">
        <motion.div
          className="relative"
          variants={fieldVariants}
          animate={getFieldState()}
          transition={{ duration: 0.2 }}
        >
          <input
            ref={inputRef}
            id={id}
            type={inputType}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            onFocus={() => setIsFocused(true)}
            onBlur={() => {
              setIsFocused(false);
              onBlur?.();
            }}
            disabled={disabled}
            required={required}
            placeholder={placeholder}
            autoComplete={autoComplete}
            maxLength={maxLength}
            pattern={pattern}
            aria-describedby={describedBy || undefined}
            aria-invalid={error ? 'true' : 'false'}
            className={`
              w-full px-4 py-3 border-2 rounded-lg transition-all duration-200
              bg-white dark:bg-neutral-800
              text-neutral-900 dark:text-neutral-100
              placeholder-neutral-500 dark:placeholder-neutral-400
              focus:outline-none focus:ring-0
              disabled:bg-neutral-100 dark:disabled:bg-neutral-700
              disabled:text-neutral-500 dark:disabled:text-neutral-400
              disabled:cursor-not-allowed
              ${error ? 'border-error-500 bg-error-50 dark:bg-error-950' : ''}
              ${success ? 'border-success-500 bg-success-50 dark:bg-success-950' : ''}
              ${isPassword ? 'pr-12' : ''}
            `}
          />
          
          {/* Floating Label */}
          <motion.label
            htmlFor={id}
            className={`
              absolute left-4 transition-all duration-200 pointer-events-none
              ${isFocused || hasValue
                ? 'top-0 -translate-y-1/2 text-sm bg-white dark:bg-neutral-800 px-2'
                : 'top-1/2 -translate-y-1/2 text-base'
              }
              ${error ? 'text-error-600 dark:text-error-400' : ''}
              ${success ? 'text-success-600 dark:text-success-400' : ''}
              ${isFocused ? 'text-primary-600 dark:text-primary-400' : 'text-neutral-600 dark:text-neutral-400'}
            `}
            animate={{
              fontSize: isFocused || hasValue ? '0.875rem' : '1rem',
              y: isFocused || hasValue ? '-50%' : '-50%',
              top: isFocused || hasValue ? '0px' : '50%',
            }}
          >
            {label}
            {required && <span className="text-error-500 ml-1" aria-label="required">*</span>}
          </motion.label>

          {/* Password Toggle */}
          {isPassword && (
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-neutral-500 hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-neutral-200 transition-colors"
              aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
              {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          )}

          {/* Status Icons */}
          {(error || success) && (
            <div className="absolute right-3 top-1/2 -translate-y-1/2">
              {error && (
                <motion.div
                  initial={{ scale: 0, rotate: -180 }}
                  animate={{ scale: 1, rotate: 0 }}
                  className="text-error-500"
                >
                  <X size={20} />
                </motion.div>
              )}
              {success && (
                <motion.div
                  initial={{ scale: 0, rotate: -180 }}
                  animate={{ scale: 1, rotate: 0 }}
                  className="text-success-500"
                >
                  <Check size={20} />
                </motion.div>
              )}
            </div>
          )}
        </motion.div>
      </div>

      {/* Help Text */}
      {helpText && (
        <p id={`${id}-help`} className="mt-2 text-sm text-neutral-600 dark:text-neutral-400 flex items-center gap-1">
          <Info size={14} />
          {helpText}
        </p>
      )}

      {/* Error Message */}
      <AnimatePresence>
        {error && (
          <motion.p
            id={`${id}-error`}
            initial={{ opacity: 0, y: -10, height: 0 }}
            animate={{ opacity: 1, y: 0, height: 'auto' }}
            exit={{ opacity: 0, y: -10, height: 0 }}
            className="mt-2 text-sm text-error-600 dark:text-error-400 flex items-center gap-1"
            role="alert"
            aria-live="polite"
          >
            <AlertCircle size={14} />
            {error}
          </motion.p>
        )}
      </AnimatePresence>
    </div>
  );
};

// Enhanced TextArea
export const EnhancedTextArea: React.FC<TextAreaProps> = ({
  id,
  label,
  value,
  onChange,
  onBlur,
  error,
  success,
  required = false,
  disabled = false,
  placeholder,
  helpText,
  rows = 4,
  resize = 'vertical',
  maxLength,
  'aria-describedby': ariaDescribedBy,
}) => {
  const [isFocused, setIsFocused] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const hasValue = value.length > 0;
  const characterCount = maxLength ? `${value.length}/${maxLength}` : null;

  const describedBy = [
    error && `${id}-error`,
    helpText && `${id}-help`,
    ariaDescribedBy,
  ].filter(Boolean).join(' ');

  return (
    <div className="form-field">
      <div className="relative">
        <textarea
          ref={textareaRef}
          id={id}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onFocus={() => setIsFocused(true)}
          onBlur={() => {
            setIsFocused(false);
            onBlur?.();
          }}
          disabled={disabled}
          required={required}
          placeholder={placeholder}
          rows={rows}
          maxLength={maxLength}
          aria-describedby={describedBy || undefined}
          aria-invalid={error ? 'true' : 'false'}
          className={`
            w-full px-4 py-3 border-2 rounded-lg transition-all duration-200
            bg-white dark:bg-neutral-800
            text-neutral-900 dark:text-neutral-100
            placeholder-neutral-500 dark:placeholder-neutral-400
            focus:outline-none focus:ring-0 focus:border-primary-500
            disabled:bg-neutral-100 dark:disabled:bg-neutral-700
            disabled:text-neutral-500 dark:disabled:text-neutral-400
            disabled:cursor-not-allowed
            ${error ? 'border-error-500 bg-error-50 dark:bg-error-950' : 'border-neutral-300 dark:border-neutral-600'}
            ${success ? 'border-success-500 bg-success-50 dark:bg-success-950' : ''}
            ${resize === 'none' ? 'resize-none' : ''}
            ${resize === 'vertical' ? 'resize-y' : ''}
            ${resize === 'horizontal' ? 'resize-x' : ''}
          `}
        />
        
        {/* Floating Label */}
        <motion.label
          htmlFor={id}
          className={`
            absolute left-4 transition-all duration-200 pointer-events-none
            ${isFocused || hasValue
              ? 'top-0 -translate-y-1/2 text-sm bg-white dark:bg-neutral-800 px-2'
              : 'top-4 text-base'
            }
            ${error ? 'text-error-600 dark:text-error-400' : ''}
            ${success ? 'text-success-600 dark:text-success-400' : ''}
            ${isFocused ? 'text-primary-600 dark:text-primary-400' : 'text-neutral-600 dark:text-neutral-400'}
          `}
        >
          {label}
          {required && <span className="text-error-500 ml-1" aria-label="required">*</span>}
        </motion.label>
      </div>

      {/* Character Count */}
      {characterCount && (
        <div className="mt-1 text-right">
          <span className={`text-sm ${
            maxLength && value.length > maxLength * 0.9
              ? 'text-warning-600 dark:text-warning-400'
              : 'text-neutral-500 dark:text-neutral-400'
          }`}>
            {characterCount}
          </span>
        </div>
      )}

      {/* Help Text */}
      {helpText && (
        <p id={`${id}-help`} className="mt-2 text-sm text-neutral-600 dark:text-neutral-400 flex items-center gap-1">
          <Info size={14} />
          {helpText}
        </p>
      )}

      {/* Error Message */}
      <AnimatePresence>
        {error && (
          <motion.p
            id={`${id}-error`}
            initial={{ opacity: 0, y: -10, height: 0 }}
            animate={{ opacity: 1, y: 0, height: 'auto' }}
            exit={{ opacity: 0, y: -10, height: 0 }}
            className="mt-2 text-sm text-error-600 dark:text-error-400 flex items-center gap-1"
            role="alert"
            aria-live="polite"
          >
            <AlertCircle size={14} />
            {error}
          </motion.p>
        )}
      </AnimatePresence>
    </div>
  );
};

// Enhanced Select
export const EnhancedSelect: React.FC<SelectProps> = ({
  id,
  label,
  value,
  onChange,
  onBlur,
  error,
  success,
  required = false,
  disabled = false,
  helpText,
  options,
  'aria-describedby': ariaDescribedBy,
}) => {
  const [isFocused, setIsFocused] = useState(false);
  const selectRef = useRef<HTMLSelectElement>(null);

  const hasValue = value.length > 0;

  const describedBy = [
    error && `${id}-error`,
    helpText && `${id}-help`,
    ariaDescribedBy,
  ].filter(Boolean).join(' ');

  return (
    <div className="form-field">
      <div className="relative">
        <select
          ref={selectRef}
          id={id}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onFocus={() => setIsFocused(true)}
          onBlur={() => {
            setIsFocused(false);
            onBlur?.();
          }}
          disabled={disabled}
          required={required}
          aria-describedby={describedBy || undefined}
          aria-invalid={error ? 'true' : 'false'}
          className={`
            w-full px-4 py-3 border-2 rounded-lg transition-all duration-200
            bg-white dark:bg-neutral-800
            text-neutral-900 dark:text-neutral-100
            focus:outline-none focus:ring-0 focus:border-primary-500
            disabled:bg-neutral-100 dark:disabled:bg-neutral-700
            disabled:text-neutral-500 dark:disabled:text-neutral-400
            disabled:cursor-not-allowed
            appearance-none cursor-pointer
            ${error ? 'border-error-500 bg-error-50 dark:bg-error-950' : 'border-neutral-300 dark:border-neutral-600'}
            ${success ? 'border-success-500 bg-success-50 dark:bg-success-950' : ''}
          `}
        >
          <option value="" disabled hidden>
            Select {label.toLowerCase()}
          </option>
          {options.map((option) => (
            <option
              key={option.value}
              value={option.value}
              disabled={option.disabled}
            >
              {option.label}
            </option>
          ))}
        </select>
        
        {/* Floating Label */}
        <motion.label
          htmlFor={id}
          className={`
            absolute left-4 transition-all duration-200 pointer-events-none
            ${isFocused || hasValue
              ? 'top-0 -translate-y-1/2 text-sm bg-white dark:bg-neutral-800 px-2'
              : 'top-1/2 -translate-y-1/2 text-base'
            }
            ${error ? 'text-error-600 dark:text-error-400' : ''}
            ${success ? 'text-success-600 dark:text-success-400' : ''}
            ${isFocused ? 'text-primary-600 dark:text-primary-400' : 'text-neutral-600 dark:text-neutral-400'}
          `}
        >
          {label}
          {required && <span className="text-error-500 ml-1" aria-label="required">*</span>}
        </motion.label>

        {/* Dropdown Arrow */}
        <div className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none">
          <svg className="w-5 h-5 text-neutral-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </div>
      </div>

      {/* Help Text */}
      {helpText && (
        <p id={`${id}-help`} className="mt-2 text-sm text-neutral-600 dark:text-neutral-400 flex items-center gap-1">
          <Info size={14} />
          {helpText}
        </p>
      )}

      {/* Error Message */}
      <AnimatePresence>
        {error && (
          <motion.p
            id={`${id}-error`}
            initial={{ opacity: 0, y: -10, height: 0 }}
            animate={{ opacity: 1, y: 0, height: 'auto' }}
            exit={{ opacity: 0, y: -10, height: 0 }}
            className="mt-2 text-sm text-error-600 dark:text-error-400 flex items-center gap-1"
            role="alert"
            aria-live="polite"
          >
            <AlertCircle size={14} />
            {error}
          </motion.p>
        )}
      </AnimatePresence>
    </div>
  );
};

// Enhanced Checkbox
export const EnhancedCheckbox: React.FC<CheckboxProps> = ({
  id,
  label,
  checked,
  onChange,
  disabled = false,
  error,
  description,
  required = false,
}) => {
  const [isFocused, setIsFocused] = useState(false);

  return (
    <div className="form-field">
      <div className="flex items-start gap-3">
        <div className="relative flex items-center">
          <input
            id={id}
            type="checkbox"
            checked={checked}
            onChange={(e) => onChange(e.target.checked)}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            disabled={disabled}
            required={required}
            aria-describedby={error ? `${id}-error` : undefined}
            aria-invalid={error ? 'true' : 'false'}
            className="sr-only"
          />
          
          <motion.div
            className={`
              w-5 h-5 border-2 rounded transition-all duration-200 cursor-pointer
              flex items-center justify-center
              ${checked ? 'bg-primary-500 border-primary-500' : 'bg-white dark:bg-neutral-800 border-neutral-300 dark:border-neutral-600'}
              ${isFocused ? 'ring-2 ring-primary-500 ring-opacity-50' : ''}
              ${error ? 'border-error-500' : ''}
              ${disabled ? 'opacity-50 cursor-not-allowed' : ''}
            `}
            whileHover={!disabled ? { scale: 1.05 } : {}}
            whileTap={!disabled ? { scale: 0.95 } : {}}
            onClick={() => !disabled && onChange(!checked)}
          >
            <AnimatePresence>
              {checked && (
                <motion.div
                  initial={{ scale: 0, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  exit={{ scale: 0, opacity: 0 }}
                  transition={{ duration: 0.15 }}
                >
                  <Check size={14} className="text-white" />
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        </div>
        
        <div className="flex-1">
          <label
            htmlFor={id}
            className={`
              text-sm font-medium cursor-pointer
              ${error ? 'text-error-600 dark:text-error-400' : 'text-neutral-900 dark:text-neutral-100'}
              ${disabled ? 'opacity-50 cursor-not-allowed' : ''}
            `}
          >
            {label}
            {required && <span className="text-error-500 ml-1" aria-label="required">*</span>}
          </label>
          
          {description && (
            <p className="mt-1 text-sm text-neutral-600 dark:text-neutral-400">
              {description}
            </p>
          )}
        </div>
      </div>

      {/* Error Message */}
      <AnimatePresence>
        {error && (
          <motion.p
            id={`${id}-error`}
            initial={{ opacity: 0, y: -10, height: 0 }}
            animate={{ opacity: 1, y: 0, height: 'auto' }}
            exit={{ opacity: 0, y: -10, height: 0 }}
            className="mt-2 text-sm text-error-600 dark:text-error-400 flex items-center gap-1"
            role="alert"
            aria-live="polite"
          >
            <AlertCircle size={14} />
            {error}
          </motion.p>
        )}
      </AnimatePresence>
    </div>
  );
};

// Enhanced Radio Group
export const EnhancedRadioGroup: React.FC<RadioGroupProps> = ({
  name,
  label,
  value,
  onChange,
  options,
  error,
  required = false,
  orientation = 'vertical',
}) => {
  return (
    <fieldset className="form-field">
      <legend className={`
        text-sm font-medium mb-3
        ${error ? 'text-error-600 dark:text-error-400' : 'text-neutral-900 dark:text-neutral-100'}
      `}>
        {label}
        {required && <span className="text-error-500 ml-1" aria-label="required">*</span>}
      </legend>
      
      <div className={`
        ${orientation === 'horizontal' ? 'flex flex-wrap gap-6' : 'space-y-3'}
      `}>
        {options.map((option) => {
          const isSelected = value === option.value;
          const optionId = `${name}-${option.value}`;
          
          return (
            <div key={option.value} className="flex items-start gap-3">
              <div className="relative flex items-center">
                <input
                  id={optionId}
                  name={name}
                  type="radio"
                  value={option.value}
                  checked={isSelected}
                  onChange={(e) => onChange(e.target.value)}
                  disabled={option.disabled}
                  required={required}
                  aria-describedby={error ? `${name}-error` : undefined}
                  className="sr-only"
                />
                
                <motion.div
                  className={`
                    w-5 h-5 border-2 rounded-full transition-all duration-200 cursor-pointer
                    flex items-center justify-center
                    ${isSelected ? 'border-primary-500' : 'border-neutral-300 dark:border-neutral-600'}
                    ${error ? 'border-error-500' : ''}
                    ${option.disabled ? 'opacity-50 cursor-not-allowed' : ''}
                  `}
                  whileHover={!option.disabled ? { scale: 1.05 } : {}}
                  whileTap={!option.disabled ? { scale: 0.95 } : {}}
                  onClick={() => !option.disabled && onChange(option.value)}
                >
                  <AnimatePresence>
                    {isSelected && (
                      <motion.div
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        exit={{ scale: 0 }}
                        className="w-2.5 h-2.5 bg-primary-500 rounded-full"
                      />
                    )}
                  </AnimatePresence>
                </motion.div>
              </div>
              
              <div className="flex-1">
                <label
                  htmlFor={optionId}
                  className={`
                    text-sm font-medium cursor-pointer
                    ${error ? 'text-error-600 dark:text-error-400' : 'text-neutral-900 dark:text-neutral-100'}
                    ${option.disabled ? 'opacity-50 cursor-not-allowed' : ''}
                  `}
                >
                  {option.label}
                </label>
                
                {option.description && (
                  <p className="mt-1 text-sm text-neutral-600 dark:text-neutral-400">
                    {option.description}
                  </p>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Error Message */}
      <AnimatePresence>
        {error && (
          <motion.p
            id={`${name}-error`}
            initial={{ opacity: 0, y: -10, height: 0 }}
            animate={{ opacity: 1, y: 0, height: 'auto' }}
            exit={{ opacity: 0, y: -10, height: 0 }}
            className="mt-2 text-sm text-error-600 dark:text-error-400 flex items-center gap-1"
            role="alert"
            aria-live="polite"
          >
            <AlertCircle size={14} />
            {error}
          </motion.p>
        )}
      </AnimatePresence>
    </fieldset>
  );
};

// Form Validation Hook
export const useFormValidation = <T extends Record<string, any>>(
  initialValues: T,
  validationRules: Record<keyof T, (value: any) => string | null>
) => {
  const [values, setValues] = useState<T>(initialValues);
  const [errors, setErrors] = useState<Partial<Record<keyof T, string>>>({});
  const [touched, setTouched] = useState<Partial<Record<keyof T, boolean>>>({});

  const validateField = (name: keyof T, value: any) => {
    const rule = validationRules[name];
    return rule ? rule(value) : null;
  };

  const setValue = (name: keyof T, value: any) => {
    setValues(prev => ({ ...prev, [name]: value }));
    
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: undefined }));
    }
  };

  const setFieldTouched = (name: keyof T) => {
    setTouched(prev => ({ ...prev, [name]: true }));
    
    // Validate on blur
    const error = validateField(name, values[name]);
    setErrors(prev => ({ ...prev, [name]: error || undefined }));
  };

  const validateAll = () => {
    const newErrors: Partial<Record<keyof T, string>> = {};
    let isValid = true;

    Object.keys(validationRules).forEach(key => {
      const fieldName = key as keyof T;
      const error = validateField(fieldName, values[fieldName]);
      if (error) {
        newErrors[fieldName] = error;
        isValid = false;
      }
    });

    setErrors(newErrors);
    setTouched(Object.keys(validationRules).reduce((acc, key) => {
      acc[key as keyof T] = true;
      return acc;
    }, {} as Partial<Record<keyof T, boolean>>));

    return isValid;
  };

  const reset = () => {
    setValues(initialValues);
    setErrors({});
    setTouched({});
  };

  return {
    values,
    errors,
    touched,
    setValue,
    setFieldTouched,
    validateAll,
    reset,
    isValid: Object.keys(errors).length === 0,
  };
};

// Common Validation Rules
export const validationRules = {
  required: (value: any) => {
    if (!value || (typeof value === 'string' && !value.trim())) {
      return 'This field is required';
    }
    return null;
  },
  
  email: (value: string) => {
    if (!value) return null;
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(value) ? null : 'Please enter a valid email address';
  },
  
  minLength: (min: number) => (value: string) => {
    if (!value) return null;
    return value.length >= min ? null : `Must be at least ${min} characters`;
  },
  
  maxLength: (max: number) => (value: string) => {
    if (!value) return null;
    return value.length <= max ? null : `Must be no more than ${max} characters`;
  },
  
  phone: (value: string) => {
    if (!value) return null;
    const phoneRegex = /^[\+]?[1-9][\d]{0,15}$/;
    return phoneRegex.test(value.replace(/\s/g, '')) ? null : 'Please enter a valid phone number';
  },
  
  url: (value: string) => {
    if (!value) return null;
    try {
      new URL(value);
      return null;
    } catch {
      return 'Please enter a valid URL';
    }
  },
};