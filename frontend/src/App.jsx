import React, { useState } from 'react';
import { AuthProvider } from './context/AuthContext';
import Navbar from './components/Navbar';
import HomePage from './pages/HomePage';
import CertificateVerificationPage from './pages/CertificateVerificationPage';
import AuthModal from './components/AuthModal';
import './styles/variables.css';

function MainApp() {
  const [authModal, setAuthModal] = useState({ isOpen: false, mode: 'signup' });
  const [activeTab, setActiveTab] = useState(() => {
    if (window.location.pathname.startsWith('/verify-certificate/')) {
      return 'verify-certificate';
    }
    return 'discovery';
  });

  const isCertRoute = activeTab === 'verify-certificate' || window.location.pathname.startsWith('/verify-certificate/');

  const handleOpenAuth = (mode = 'signup') => {
    setAuthModal({ isOpen: true, mode });
  };

  const handleCloseAuth = () => {
    setAuthModal(prev => ({ ...prev, isOpen: false }));
  };

  return (
    <div className="app-root">
      <Navbar 
        onOpenAuth={handleOpenAuth} 
        activeTab={activeTab} 
        onSelectTab={setActiveTab} 
      />
      <main>
        {isCertRoute ? (
          <CertificateVerificationPage
            onBack={() => {
              window.history.pushState({}, '', '/');
              setActiveTab('discovery');
            }}
          />
        ) : (
          <HomePage 
            onOpenAuth={handleOpenAuth} 
            activeTab={activeTab} 
            onSelectTab={setActiveTab} 
          />
        )}
      </main>
      <AuthModal
        isOpen={authModal.isOpen}
        initialMode={authModal.mode}
        onClose={handleCloseAuth}
      />
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}
