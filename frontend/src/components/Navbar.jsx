import React, { useState, useRef, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { 
  Sparkles, 
  LogOut, 
  User, 
  Shield, 
  Ticket, 
  Calendar,
  Compass, 
  Briefcase, 
  ShieldCheck, 
  Layers,
  Scan,
  BarChart3,
  Globe,
  Award,
  Radio,
  Trophy,
  ChevronDown
} from 'lucide-react';
import NotificationBell from './NotificationBell';
import './Navbar.css';

export default function Navbar({ onOpenAuth, activeTab, onSelectTab }) {
  const { user, isAuthenticated, logout, isAdmin, isOrganizer } = useAuth();
  const [currency, setCurrency] = useState(localStorage.getItem('preferredCurrency') || 'USD');
  const [isMoreOpen, setIsMoreOpen] = useState(false);
  const moreRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(event) {
      if (moreRef.current && !moreRef.current.contains(event.target)) {
        setIsMoreOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleCurrencyChange = (newCurr) => {
    setCurrency(newCurr);
    localStorage.setItem('preferredCurrency', newCurr);
    window.dispatchEvent(new CustomEvent('currencyChanged', { detail: newCurr }));
  };

  const getRoleBadge = () => {
    if (isAdmin) return { label: 'Admin', icon: <Shield size={13} />, className: 'role-admin' };
    if (isOrganizer) return { label: 'Organizer', icon: <Calendar size={13} />, className: 'role-organizer' };
    return { label: 'Attendee', icon: <Ticket size={13} />, className: 'role-attendee' };
  };

  const role = getRoleBadge();

  // Determine if any item in the "More / Workspace" menu is currently active
  const isMoreActive = ['checkin', 'analytics', 'moderation', 'leaderboard'].includes(activeTab);

  return (
    <header className="navbar">
      <div className="navbar-container">
        {/* Brand Logo */}
        <div className="navbar-brand" onClick={() => onSelectTab && onSelectTab('discovery')} style={{ cursor: 'pointer' }}>
          <div className="brand-logo-icon">
            <Sparkles size={18} />
          </div>
          <span className="brand-title">
            PulseEvent <span className="brand-suffix">OS</span>
          </span>
        </div>

        {/* Center Nav Links */}
        <nav className="nav-links">
          <button
            className={`nav-link-btn ${activeTab === 'discovery' ? 'active' : ''}`}
            onClick={() => onSelectTab && onSelectTab('discovery')}
          >
            <Compass size={15} />
            <span>Discover</span>
          </button>

          <button
            className={`nav-link-btn ${activeTab === 'sponsors' ? 'active' : ''}`}
            onClick={() => onSelectTab && onSelectTab('sponsors')}
          >
            <Award size={15} />
            <span>Expo</span>
          </button>

          <button
            className={`nav-link-btn ${activeTab === 'live-stage' ? 'active' : ''}`}
            onClick={() => onSelectTab && onSelectTab('live-stage')}
          >
            <Radio size={15} />
            <span>Live Stage</span>
          </button>

          <button
            className={`nav-link-btn ${activeTab === 'my-tickets' ? 'active' : ''}`}
            onClick={() => {
              if (!isAuthenticated) {
                onOpenAuth('signin');
              } else {
                onSelectTab && onSelectTab('my-tickets');
              }
            }}
          >
            <Ticket size={15} />
            <span>Attendee Portal</span>
          </button>

          <button
            className={`nav-link-btn ${activeTab === 'studio' ? 'active' : ''}`}
            onClick={() => {
              if (!isAuthenticated) {
                onOpenAuth('signin');
              } else {
                onSelectTab && onSelectTab('studio');
              }
            }}
          >
            <Briefcase size={15} />
            <span>Studio</span>
          </button>

          {/* More / Operations Dropdown to prevent navbar overflow */}
          <div className="nav-dropdown-wrapper" ref={moreRef}>
            <button
              className={`nav-link-btn nav-dropdown-trigger ${isMoreActive ? 'active' : ''}`}
              onClick={() => setIsMoreOpen(prev => !prev)}
            >
              <Layers size={15} />
              <span>Workspace</span>
              <ChevronDown size={13} className={`dropdown-chevron ${isMoreOpen ? 'open' : ''}`} />
            </button>

            {isMoreOpen && (
              <div className="nav-dropdown-menu">

                <button
                  className={`nav-dropdown-item ${activeTab === 'leaderboard' ? 'active' : ''}`}
                  onClick={() => {
                    onSelectTab && onSelectTab('leaderboard');
                    setIsMoreOpen(false);
                  }}
                >
                  <Trophy size={15} />
                  <span>Quests & Leaderboard</span>
                </button>

                {(isOrganizer || isAdmin) && (
                  <button
                    className={`nav-dropdown-item ${activeTab === 'checkin' ? 'active' : ''}`}
                    onClick={() => {
                      onSelectTab && onSelectTab('checkin');
                      setIsMoreOpen(false);
                    }}
                  >
                    <Scan size={15} />
                    <span>Door Scanner HUD</span>
                  </button>
                )}

                {(isOrganizer || isAdmin) && (
                  <button
                    className={`nav-dropdown-item ${activeTab === 'analytics' ? 'active' : ''}`}
                    onClick={() => {
                      onSelectTab && onSelectTab('analytics');
                      setIsMoreOpen(false);
                    }}
                  >
                    <BarChart3 size={15} />
                    <span>Financial & Attendance Analytics</span>
                  </button>
                )}

                {isAdmin && (
                  <button
                    className={`nav-dropdown-item admin-item ${activeTab === 'moderation' ? 'active' : ''}`}
                    onClick={() => {
                      onSelectTab && onSelectTab('moderation');
                      setIsMoreOpen(false);
                    }}
                  >
                    <ShieldCheck size={15} />
                    <span>Admin Moderation Desk</span>
                  </button>
                )}
              </div>
            )}
          </div>
        </nav>

        {/* Right Section */}
        <div className="navbar-actions">
          {/* Multi-Currency Engine Selector */}
          <div className="currency-selector-box">
            <Globe size={14} className="currency-globe-icon" />
            <select
              value={currency}
              onChange={(e) => handleCurrencyChange(e.target.value)}
              className="currency-dropdown"
              title="Global Multi-Currency Engine"
            >
              <option value="USD">$ USD</option>
              <option value="EUR">€ EUR</option>
              <option value="GBP">£ GBP</option>
              <option value="INR">₹ INR</option>
            </select>
          </div>

          {isAuthenticated ? (
            <div className="user-menu-wrapper">
              <NotificationBell isAuthenticated={isAuthenticated} />
              <div className="user-menu">
                <div className="user-info">
                  <span className="user-name">{user?.fullName}</span>
                  <span className={`role-badge ${role.className}`}>
                    {role.icon}
                    <span>{role.label}</span>
                  </span>
                </div>
                <button onClick={logout} className="logout-btn" title="Sign Out">
                  <LogOut size={15} />
                  <span>Logout</span>
                </button>
              </div>
            </div>
          ) : (
            <button onClick={() => onOpenAuth('signin')} className="signin-nav-btn">
              <User size={16} />
              <span>Sign In / Register</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
