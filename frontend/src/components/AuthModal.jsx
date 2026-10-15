import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { 
  Sparkles, 
  Mail, 
  Lock, 
  User as UserIcon, 
  Phone, 
  Ticket, 
  CalendarPlus, 
  CheckCircle2, 
  AlertCircle, 
  X,
  ArrowRight,
  ShieldCheck,
  Eye,
  EyeOff
} from 'lucide-react';
import './AuthModal.css';

export default function AuthModal({ isOpen, initialMode = 'signup', onClose }) {
  const { login, register } = useAuth();
  const [mode, setMode] = useState(initialMode); // 'signin' | 'signup'
  const [role, setRole] = useState('ROLE_ATTENDEE'); // 'ROLE_ATTENDEE' | 'ROLE_ORGANIZER'
  const [showPassword, setShowPassword] = useState(false);

  // Form State
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [agreed, setAgreed] = useState(true);

  // UI State
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  // Password strength calculation
  const getPasswordStrength = (pass) => {
    if (!pass) return { score: 0, label: 'Empty', color: 'gray' };
    let score = 0;
    if (pass.length >= 8) score++;
    if (/[A-Z]/.test(pass)) score++;
    if (/[0-9]/.test(pass)) score++;
    if (/[^A-Za-z0-9]/.test(pass)) score++;

    if (score <= 1) return { score: 1, label: 'Weak', color: '#EF4444' };
    if (score <= 3) return { score: 2, label: 'Medium', color: '#F59E0B' };
    return { score: 3, label: 'Strong', color: '#10B981' };
  };

  const strength = getPasswordStrength(password);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setIsSubmitting(true);

    try {
      if (mode === 'signup') {
        if (!agreed) {
          throw new Error('You must agree to the Terms of Service to continue');
        }
        await register({
          fullName,
          email,
          password,
          phone: phone || null,
          role,
        });
        setSuccess('Account created successfully! Welcome to PulseEvent.');
      } else {
        await login(email, password);
        setSuccess('Welcome back! Login successful.');
      }

      setTimeout(() => {
        onClose();
      }, 1000);
    } catch (err) {
      setError(err.message || 'Authentication failed. Please check your credentials.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleFillDemoOrganizer = () => {
    setMode('signin');
    setEmail('organizer@eventflow.com');
    setPassword('OrganizerPassword2026!');
  };

  return (
    <div className="auth-overlay">
      <div className="auth-container">
        <button onClick={onClose} className="auth-close-btn" aria-label="Close modal">
          <X size={20} />
        </button>

        {/* Left Side: Brand Experience & Live Preview Hero */}
        <div className="auth-hero-panel">
          <div className="hero-glow-orb"></div>

          <div className="hero-pill-badge">
            <Sparkles size={14} className="sparkle-icon" />
            <span>⚡ Live Experiences & Premier Events</span>
          </div>

          <h2 className="hero-headline">
            Your gateway to discovering, organizing, and attending world-class events.
          </h2>

          <p className="hero-subtext">
            Join 120,000+ creators, tech leaders, and attendees streamlining conferences, festivals, and summits across 48 countries.
          </p>

          {/* Interactive Live Event Preview Card */}
          <div className="preview-card">
            <div className="preview-card-header">
              <span className="preview-tag">Featured Summit</span>
              <span className="preview-live-status">
                <span className="live-dot"></span>
                842 Live Registrations
              </span>
            </div>

            <h3 className="preview-title">Global Tech Summit 2026</h3>
            <p className="preview-venue">Moscone West • San Francisco, CA & Hybrid Stream</p>

            <div className="preview-meta-row">
              <span className="preview-date">Oct 14-16, 2026</span>
              <span className="preview-price">From $299 / Early Bird</span>
            </div>

            <div className="preview-countdown">
              <div className="countdown-unit">
                <span className="countdown-val">08</span>
                <span className="countdown-lbl">Days</span>
              </div>
              <div className="countdown-sep">:</div>
              <div className="countdown-unit">
                <span className="countdown-val">14</span>
                <span className="countdown-lbl">Hours</span>
              </div>
              <div className="countdown-sep">:</div>
              <div className="countdown-unit">
                <span className="countdown-val">32</span>
                <span className="countdown-lbl">Mins</span>
              </div>
            </div>
          </div>
        </div>

        {/* Right Side: Elevated Auth Form Card */}
        <div className="auth-form-panel">
          {/* Segmented Mode Tabs */}
          <div className="mode-toggle">
            <button
              type="button"
              className={`mode-tab ${mode === 'signin' ? 'active' : ''}`}
              onClick={() => { setMode('signin'); setError(''); }}
            >
              Sign In
            </button>
            <button
              type="button"
              className={`mode-tab ${mode === 'signup' ? 'active' : ''}`}
              onClick={() => { setMode('signup'); setError(''); }}
            >
              Create Account
            </button>
          </div>

          <div className="auth-form-header">
            <h3>{mode === 'signup' ? 'Get started with PulseEvent' : 'Welcome back'}</h3>
            <p>{mode === 'signup' ? 'Choose your role and register in seconds' : 'Enter your credentials to access your dashboard'}</p>
          </div>

          {/* Error & Success Banners */}
          {error && (
            <div className="alert-banner alert-error">
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}
          {success && (
            <div className="alert-banner alert-success">
              <CheckCircle2 size={16} />
              <span>{success}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="auth-form">
            {/* Role Selection Toggle (Signup Only) */}
            {mode === 'signup' && (
              <div className="role-selector">
                <label className="field-label">I want to:</label>
                <div className="role-options">
                  <div
                    className={`role-card ${role === 'ROLE_ATTENDEE' ? 'selected' : ''}`}
                    onClick={() => setRole('ROLE_ATTENDEE')}
                  >
                    <div className="role-icon">
                      <Ticket size={18} />
                    </div>
                    <div className="role-info">
                      <div className="role-title">Attendee</div>
                      <div className="role-desc">Discover & book tickets</div>
                    </div>
                  </div>

                  <div
                    className={`role-card ${role === 'ROLE_ORGANIZER' ? 'selected' : ''}`}
                    onClick={() => setRole('ROLE_ORGANIZER')}
                  >
                    <div className="role-icon">
                      <CalendarPlus size={18} />
                    </div>
                    <div className="role-info">
                      <div className="role-title">Organizer</div>
                      <div className="role-desc">Host & manage events</div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Full Name (Signup Only) */}
            {mode === 'signup' && (
              <div className="form-group">
                <label className="field-label">Full Name</label>
                <div className="input-wrapper">
                  <UserIcon size={16} className="input-icon" />
                  <input
                    type="text"
                    required
                    placeholder="e.g. Elena Rostova"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                  />
                </div>
              </div>
            )}

            {/* Email Address */}
            <div className="form-group">
              <label className="field-label">Email Address</label>
              <div className="input-wrapper">
                <Mail size={16} className="input-icon" />
                <input
                  type="email"
                  required
                  placeholder="elena@company.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>
            </div>

            {/* Phone (Optional, Signup Only) */}
            {mode === 'signup' && (
              <div className="form-group">
                <label className="field-label">Phone Number <span className="optional-tag">(Optional)</span></label>
                <div className="input-wrapper">
                  <Phone size={16} className="input-icon" />
                  <input
                    type="tel"
                    placeholder="+1 (555) 000-0000"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                  />
                </div>
              </div>
            )}

            {/* Password */}
            <div className="form-group">
              <label className="field-label">Password</label>
              <div className="input-wrapper">
                <Lock size={16} className="input-icon" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  placeholder={mode === 'signup' ? 'Min 8 characters' : 'Enter your password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                <button
                  type="button"
                  className="eye-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>

              {/* Password strength indicator for signup */}
              {mode === 'signup' && password.length > 0 && (
                <div className="strength-meter">
                  <div className="strength-bars">
                    <span className={`bar ${strength.score >= 1 ? 'filled' : ''}`} style={{ backgroundColor: strength.score >= 1 ? strength.color : '' }}></span>
                    <span className={`bar ${strength.score >= 2 ? 'filled' : ''}`} style={{ backgroundColor: strength.score >= 2 ? strength.color : '' }}></span>
                    <span className={`bar ${strength.score >= 3 ? 'filled' : ''}`} style={{ backgroundColor: strength.score >= 3 ? strength.color : '' }}></span>
                  </div>
                  <span className="strength-label" style={{ color: strength.color }}>
                    {strength.label} password
                  </span>
                </div>
              )}
            </div>

            {/* Terms checkbox for signup */}
            {mode === 'signup' && (
              <label className="checkbox-row">
                <input
                  type="checkbox"
                  checked={agreed}
                  onChange={(e) => setAgreed(e.target.checked)}
                />
                <span>I agree to the Terms of Service and Privacy Policy</span>
              </label>
            )}

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isSubmitting}
              className="submit-btn"
            >
              <span>{isSubmitting ? 'Processing...' : (mode === 'signup' ? 'Create Account' : 'Sign In')}</span>
              <ArrowRight size={16} />
            </button>
          </form>

          {/* Quick Demo Helper */}
          <div className="demo-helper">
            <span className="demo-hint">Quick test:</span>
            <button type="button" onClick={handleFillDemoOrganizer} className="demo-btn">
              Use Pre-seeded Organizer
            </button>
          </div>

          <div className="compliance-footer">
            <ShieldCheck size={14} />
            <span>256-bit TLS Encrypted • BCrypt Hashed Security</span>
          </div>
        </div>
      </div>
    </div>
  );
}
