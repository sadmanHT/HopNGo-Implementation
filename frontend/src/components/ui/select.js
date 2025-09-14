import React from 'react';
import { cn } from '../../lib/utils';
import { ChevronDown } from 'lucide-react';

const Select = ({ children, value, onValueChange, ...props }) => {
  const [isOpen, setIsOpen] = React.useState(false);
  const [selectedValue, setSelectedValue] = React.useState(value || '');
  
  const handleValueChange = (newValue) => {
    setSelectedValue(newValue);
    onValueChange && onValueChange(newValue);
    setIsOpen(false);
  };
  
  return (
    <div className="relative" {...props}>
      {React.Children.map(children, child => {
        if (React.isValidElement(child)) {
          return React.cloneElement(child, {
            isOpen,
            setIsOpen,
            selectedValue,
            onValueChange: handleValueChange
          });
        }
        return child;
      })}
    </div>
  );
};

const SelectTrigger = React.forwardRef(({ className, children, isOpen, setIsOpen, ...props }, ref) => (
  <button
    ref={ref}
    type="button"
    className={cn(
      'flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50',
      className
    )}
    onClick={() => setIsOpen && setIsOpen(!isOpen)}
    {...props}
  >
    {children}
    <ChevronDown className="h-4 w-4 opacity-50" />
  </button>
));
SelectTrigger.displayName = 'SelectTrigger';

const SelectValue = ({ placeholder, selectedValue }) => {
  return (
    <span className={selectedValue ? '' : 'text-muted-foreground'}>
      {selectedValue || placeholder}
    </span>
  );
};
SelectValue.displayName = 'SelectValue';

const SelectContent = ({ className, children, isOpen, ...props }) => {
  if (!isOpen) return null;
  
  return (
    <div
      className={cn(
        'absolute top-full left-0 z-50 w-full mt-1 rounded-md border bg-popover text-popover-foreground shadow-md',
        className
      )}
      {...props}
    >
      <div className="p-1">
        {children}
      </div>
    </div>
  );
};
SelectContent.displayName = 'SelectContent';

const SelectItem = React.forwardRef(({ className, children, value, onValueChange, ...props }, ref) => (
  <div
    ref={ref}
    className={cn(
      'relative flex w-full cursor-pointer select-none items-center rounded-sm py-1.5 pl-8 pr-2 text-sm outline-none hover:bg-accent hover:text-accent-foreground focus:bg-accent focus:text-accent-foreground',
      className
    )}
    onClick={() => onValueChange && onValueChange(value)}
    {...props}
  >
    {children}
  </div>
));
SelectItem.displayName = 'SelectItem';

export { Select, SelectContent, SelectItem, SelectTrigger, SelectValue };