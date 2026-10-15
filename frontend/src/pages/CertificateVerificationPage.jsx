import React, { useState, useEffect } from 'react';
import {
  Award,
  CheckCircle2,
  AlertTriangle,
  Download,
  Share2,
  Calendar,
  Clock,
  Building2,
  User,
  ShieldCheck,
  Copy,
  Check,
  ArrowLeft,
  Sparkles,
  Loader2
} from 'lucide-react';
import { certificateService } from '../services/api';
import './CertificateVerificationPage.css';

export default function CertificateVerificationPage({ code: propCode, onBack }) {
  const [code, setCode] = useState(() => {
    if (propCode) return propCode;
    const path = window.location.pathname;
    if (path.includes('/verify-certificate/')) {
      return path.split('/verify-certificate/')[1].split('/')[0];
    }
    const params = new URLSearchParams(window.location.search);
    return params.get('code') || '';
  });

  const [verification, setVerification] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!code) {
      setError('No certificate code provided in URL or parameters.');
      setIsLoading(false);
      return;
    }

    const verify = async () => {
      setIsLoading(true);
      try {
        const data = await certificateService.verifyCertificate(code);
        setVerification(data);
      } catch (err) {
        setError(err.message || 'Verification service failed. Please check your network connection.');
      } finally {
        setIsLoading(false);
      }
    };

    verify();
  }, [code]);

  const copyVerificationLink = () => {
    const shareUrl = `${window.location.origin}/verify-certificate/${code}`;
    navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleBack = () => {
    if (onBack) {
      onBack();
    } else {
      window.location.href = '/';
    }
  };

  return (
    <div className="cert-verify-container">
      {/* Background Glow */}
      <div className="cert-bg-glow" />

      <div className="cert-verify-wrapper">
        <div className="cert-nav-back">
          <button type="button" onClick={handleBack} className="btn-back-home">
            <ArrowLeft size={16} />
            <span>Back to EventFlow</span>
          </button>
        </div>

        {isLoading ? (
          <div className="cert-card loading">
            <Loader2 size={40} className="spin cert-spin" />
            <h3>Cryptographic Registry Lookup</h3>
            <p>Querying immutable certificate ledger for credential #{code}...</p>
          </div>
        ) : error || !verification || !verification.valid ? (
          <div className="cert-card invalid">
            <div className="status-badge-icon invalid">
              <AlertTriangle size={36} />
            </div>
            <h2>Invalid or Unrecognized Credential</h2>
            <p className="cert-error-sub">
              The certificate code <code>{code || 'UNKNOWN'}</code> could not be validated against the active EventFlow registry.
            </p>
            <div className="cert-invalid-tips">
              <p>Possible reasons:</p>
              <ul>
                <li>The verification code was mistyped or truncated.</li>
                <li>The event organizer has not officially released credentials for this summit.</li>
                <li>The certificate was invalidated or superseded.</li>
              </ul>
            </div>
            <button type="button" onClick={handleBack} className="btn-return-home">
              Return to Events Directory
            </button>
          </div>
        ) : (
          <div className="cert-card valid">
            {/* Top Security Banner */}
            <div className="cert-verified-banner">
              <div className="banner-left">
                <ShieldCheck size={20} className="shield-icon" />
                <span className="banner-title">OFFICIAL CREDENTIAL VERIFIED</span>
              </div>
              <span className="banner-engine">EVENTFLOW ATTESTATION OS</span>
            </div>

            {/* Certificate Header */}
            <div className="cert-header">
              <div className="cert-seal">
                <Award size={44} className="seal-icon" />
                <Sparkles size={16} className="seal-sparkle" />
              </div>
              <span className="cert-type-pill">
                CERTIFICATE OF {verification.certificateType || 'COMPLETION'}
              </span>
              <h1 className="cert-title">Verified Credential Attestation</h1>
              <p className="cert-intro">This certifies the authentic completion and participation of:</p>
            </div>

            {/* Recipient Spotlight */}
            <div className="recipient-spotlight">
              <User size={22} className="rec-icon" />
              <h2 className="recipient-name">{verification.recipientName}</h2>
            </div>

            {/* Event Name */}
            <div className="cert-event-box">
              <span className="event-label">Participated in Summit:</span>
              <h3 className="event-name">{verification.eventTitle}</h3>
            </div>

            {/* Metadata Grid */}
            <div className="cert-meta-grid">
              <div className="meta-card">
                <Calendar size={18} className="meta-icon" />
                <div>
                  <span className="meta-label">Date Issued</span>
                  <p className="meta-val">{new Date(verification.issueDate).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })}</p>
                </div>
              </div>

              <div className="meta-card">
                <Clock size={18} className="meta-icon" />
                <div>
                  <span className="meta-label">Accreditation</span>
                  <p className="meta-val">{verification.hoursCompleted || 16} CPD Hours</p>
                </div>
              </div>

              <div className="meta-card">
                <Building2 size={18} className="meta-icon" />
                <div>
                  <span className="meta-label">Host Organizer</span>
                  <p className="meta-val">{verification.organizerName || 'EventFlow Host'}</p>
                </div>
              </div>

              <div className="meta-card">
                <CheckCircle2 size={18} className="meta-icon success" />
                <div>
                  <span className="meta-label">Ledger Identifier</span>
                  <p className="meta-val code-val">{verification.certificateCode}</p>
                </div>
              </div>
            </div>

            {/* Verification Statement */}
            <div className="cert-statement">
              <p>{verification.message || 'This digital credential has been cryptographically confirmed and permanently logged in the EventFlow event ledger.'}</p>
            </div>

            {/* Actions Bar */}
            <div className="cert-actions-bar">
              <a
                href={certificateService.getCertificatePdfUrl(verification.certificateCode)}
                target="_blank"
                rel="noopener noreferrer"
                className="btn-download-pdf"
              >
                <Download size={16} />
                <span>Download Landscape PDF Certificate</span>
              </a>

              <button
                type="button"
                className="btn-copy-link"
                onClick={copyVerificationLink}
              >
                {copied ? <Check size={16} className="copied" /> : <Copy size={16} />}
                <span>{copied ? 'Link Copied!' : 'Copy Verification URL'}</span>
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
