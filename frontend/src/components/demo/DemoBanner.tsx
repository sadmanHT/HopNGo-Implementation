import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useDemo } from '../../hooks/useDemo';
import { X, User, Building, RefreshCw } from 'lucide-react';

interface DemoBannerProps {
  className?: string;
}

export const DemoBanner: React.FC<DemoBannerProps> = ({ className = '' }) => {
  const { isDemoMode, demoUser, exitDemoMode, switchDemoUser, demoUsers } = useDemo();

  if (!isDemoMode) return null;

  const currentUser = demoUser ? demoUsers[demoUser] : null;
  const otherUserType = demoUser === 'traveler' ? 'provider' : 'traveler';

  return (
    <AnimatePresence>
      <motion.div
        initial={{ y: -100, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        exit={{ y: -100, opacity: 0 }}
        transition={{ type: 'spring', stiffness: 300, damping: 30 }}
        className={`fixed top-0 left-0 right-0 z-50 bg-gradient-to-r from-purple-600 to-blue-600 text-white shadow-lg ${className}`}
      >
        <div className="container mx-auto px-4 py-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              {/* Demo Mode Indicator */}
              <div className="flex items-center space-x-2">
                <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
                <span className="font-semibold text-sm">🎭 DEMO MODE</span>
              </div>

              {/* Current User Info */}
              {currentUser && (
                <div className="flex items-center space-x-2 bg-white/10 rounded-full px-3 py-1">
                  {demoUser === 'traveler' ? (
                    <User className="w-4 h-4" />
                  ) : (
                    <Building className="w-4 h-4" />
                  )}
                  <span className="text-sm font-medium">
                    {currentUser.name} ({demoUser})
                  </span>
                </div>
              )}

              {/* Switch User Button */}
              <button
                onClick={() => switchDemoUser(otherUserType)}
                className="flex items-center space-x-1 bg-white/10 hover:bg-white/20 rounded-full px-3 py-1 transition-colors text-sm"
                title={`Switch to ${otherUserType} account`}
              >
                <RefreshCw className="w-3 h-3" />
                <span>Switch to {otherUserType}</span>
              </button>
            </div>

            {/* Demo Info & Exit */}
            <div className="flex items-center space-x-4">
              <div className="hidden md:block text-xs opacity-90">
                Safe sandbox • Seeded content • Mock payments
              </div>
              
              <button
                onClick={exitDemoMode}
                className="flex items-center space-x-1 bg-white/10 hover:bg-white/20 rounded-full p-2 transition-colors"
                title="Exit demo mode"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </motion.div>
    </AnimatePresence>
  );
};

// Demo mode styles for global CSS
export const demoModeStyles = `
  .demo-mode {
    --demo-border: 2px solid #8b5cf6;
    --demo-shadow: 0 0 20px rgba(139, 92, 246, 0.3);
  }
  
  .demo-mode .demo-highlight {
    border: var(--demo-border);
    box-shadow: var(--demo-shadow);
  }
  
  .demo-mode .demo-badge::after {
    content: '🎭';
    position: absolute;
    top: -8px;
    right: -8px;
    background: linear-gradient(45deg, #8b5cf6, #3b82f6);
    color: white;
    border-radius: 50%;
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    z-index: 10;
  }
`;

export default DemoBanner;