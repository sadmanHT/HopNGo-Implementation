import { useState, useEffect } from 'react'
import './App.css'

function App() {
  const [backendStatus, setBackendStatus] = useState<string>('Checking...')

  useEffect(() => {
    // Check backend health
    fetch('/api/health')
      .then(response => response.text())
      .then(data => setBackendStatus(data))
      .catch(() => setBackendStatus('Backend not available'))
  }, [])

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      <div className="container mx-auto px-4 py-8">
        <header className="text-center mb-12">
          <h1 className="text-4xl font-bold text-gray-900 mb-4">
            🚌 HopNGo
          </h1>
          <p className="text-xl text-gray-600">
            Your Ultimate Bus Booking Experience
          </p>
        </header>

        <main className="max-w-4xl mx-auto">
          <div className="bg-white rounded-lg shadow-lg p-8 mb-8">
            <h2 className="text-2xl font-semibold text-gray-800 mb-6">
              Welcome to HopNGo Development Environment
            </h2>
            
            <div className="grid md:grid-cols-2 gap-6">
              <div className="bg-green-50 border border-green-200 rounded-lg p-6">
                <h3 className="text-lg font-medium text-green-800 mb-2">
                  ✅ Frontend Status
                </h3>
                <p className="text-green-700">
                  React application is running successfully!
                </p>
                <p className="text-sm text-green-600 mt-2">
                  Port: 3000 | Vite Dev Server
                </p>
              </div>

              <div className={`border rounded-lg p-6 ${
                backendStatus.includes('running') 
                  ? 'bg-green-50 border-green-200' 
                  : 'bg-yellow-50 border-yellow-200'
              }`}>
                <h3 className={`text-lg font-medium mb-2 ${
                  backendStatus.includes('running') 
                    ? 'text-green-800' 
                    : 'text-yellow-800'
                }`}>
                  {backendStatus.includes('running') ? '✅' : '⚠️'} Backend Status
                </h3>
                <p className={backendStatus.includes('running') 
                  ? 'text-green-700' 
                  : 'text-yellow-700'
                }>
                  {backendStatus}
                </p>
                <p className={`text-sm mt-2 ${
                  backendStatus.includes('running') 
                    ? 'text-green-600' 
                    : 'text-yellow-600'
                }`}>
                  Port: 8080 | Spring Boot API
                </p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-lg shadow-lg p-8">
            <h2 className="text-2xl font-semibold text-gray-800 mb-6">
              🚀 Quick Start Guide
            </h2>
            
            <div className="space-y-4">
              <div className="flex items-start space-x-3">
                <span className="flex-shrink-0 w-6 h-6 bg-blue-500 text-white rounded-full flex items-center justify-center text-sm font-medium">
                  1
                </span>
                <div>
                  <h4 className="font-medium text-gray-900">Frontend Development</h4>
                  <p className="text-gray-600 text-sm">
                    React + TypeScript + Vite + Tailwind CSS
                  </p>
                </div>
              </div>
              
              <div className="flex items-start space-x-3">
                <span className="flex-shrink-0 w-6 h-6 bg-blue-500 text-white rounded-full flex items-center justify-center text-sm font-medium">
                  2
                </span>
                <div>
                  <h4 className="font-medium text-gray-900">Backend API</h4>
                  <p className="text-gray-600 text-sm">
                    Spring Boot + PostgreSQL + Redis + JWT Authentication
                  </p>
                </div>
              </div>
              
              <div className="flex items-start space-x-3">
                <span className="flex-shrink-0 w-6 h-6 bg-blue-500 text-white rounded-full flex items-center justify-center text-sm font-medium">
                  3
                </span>
                <div>
                  <h4 className="font-medium text-gray-900">Database & Cache</h4>
                  <p className="text-gray-600 text-sm">
                    PostgreSQL (localhost:5432) + Redis (localhost:6379)
                  </p>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}

export default App