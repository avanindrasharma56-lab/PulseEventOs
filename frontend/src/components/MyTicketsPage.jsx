import React, { useState, useEffect } from 'react';
import {
  Calendar,
  Ticket,
  Bell,
  History,
  User,
  MapPin,
  Clock,
  QrCode,
  Download,
  RotateCcw,
  Tag,
  Award,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  DollarSign,
  ArrowRight,
  ShieldCheck,
  X,
  Sparkles,
  Save,
  CheckCheck
} from 'lucide-react';
import { 
  ticketPassService, 
  refundService, 
  certificateService, 
  notificationService, 
  orderService, 
  authService 
} from '../services/api';
import './MyTicketsPage.css';

export default function AttendeeDashboard({ onExploreEvents }) {
  const [activeTab, setActiveTab] = useState('upcoming'); // 'upcoming' | 'tickets' | 'updates' | 'history' | 'profile'
  const [tickets, setTickets] = useState([]);
  const [orders, setOrders] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [refundRequests, setRefundRequests] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  // Profile Form State
  const [profile, setProfile] = useState({ fullName: '', email: '', phone: '' });
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [profileMessage, setProfileMessage] = useState({ type: '', text: '' });

  // Modal & Download states
  const [activeQrModal, setActiveQrModal] = useState(null);
  const [downloadingCode, setDownloadingCode] = useState(null);
  const [downloadingBadgeCode, setDownloadingBadgeCode] = useState(null);
  const [claimingCertEventId, setClaimingCertEventId] = useState(null);

  // Refund Modal State
  const [refundModalTarget, setRefundModalTarget] = useState(null);
  const [refundReason, setRefundReason] = useState('');
  const [isSubmittingRefund, setIsSubmittingRefund] = useState(false);
  const [refundError, setRefundError] = useState('');

  const loadAllData = async () => {
    setIsLoading(true);
    try {
      const [ticketsData, ordersData, notifsData, refundsData, userData] = await Promise.all([
        ticketPassService.getMyTickets().catch(() => []),
        orderService.getMyOrders().catch(() => []),
        notificationService.getNotifications().catch(() => []),
        refundService.getMyRefundRequests().catch(() => []),
        authService.getCurrentUser().catch(() => null)
      ]);

      setTickets(ticketsData || []);
      setOrders(ordersData || []);
      setNotifications(notifsData || []);
      setRefundRequests(refundsData || []);

      if (userData) {
        setProfile({
          fullName: userData.fullName || '',
          email: userData.email || '',
          phone: userData.phone || ''
        });
      }
    } catch (err) {
      console.error('Failed to load attendee dashboard data', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAllData();
  }, []);

  const formatDate = (dateStr) => {
    if (!dateStr) return 'TBA';
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // Group tickets into unique upcoming events
  const now = new Date();
  const upcomingEventsMap = new Map();
  tickets.forEach(t => {
    const eventTime = t.eventStartTime ? new Date(t.eventStartTime) : null;
    const isUpcoming = !eventTime || eventTime >= now || t.status === 'VALID';
    if (isUpcoming && !upcomingEventsMap.has(t.eventId || t.eventTitle)) {
      upcomingEventsMap.set(t.eventId || t.eventTitle, {
        id: t.eventId,
        title: t.eventTitle,
        startTime: t.eventStartTime,
        venueName: t.venueName || 'Main Convention Center',
        venueCity: t.venueCity || '',
        passes: [t]
      });
    } else if (isUpcoming) {
      upcomingEventsMap.get(t.eventId || t.eventTitle).passes.push(t);
    }
  });
  const upcomingEvents = Array.from(upcomingEventsMap.values());

  // Downloads & Actions
  const handleDownloadPdf = async (ticket) => {
    try {
      setDownloadingCode(ticket.ticketCode);
      await ticketPassService.downloadTicketPdf(ticket.ticketCode, `ticket-${ticket.ticketCode}.pdf`);
    } catch (err) {
      alert('Failed to download PDF ticket pass: ' + err.message);
    } finally {
      setDownloadingCode(null);
    }
  };

  const handleDownloadLanyardBadge = async (ticket) => {
    try {
      setDownloadingBadgeCode(ticket.ticketCode);
      await ticketPassService.downloadLanyardBadgePdf(ticket.ticketCode, `badge-${ticket.ticketCode}.pdf`);
    } catch (err) {
      alert('Failed to download lanyard badge: ' + err.message);
    } finally {
      setDownloadingBadgeCode(null);
    }
  };

  const handleClaimCertificate = async (ticket) => {
    if (!ticket.eventId) return;
    try {
      setClaimingCertEventId(ticket.eventId);
      const cert = await certificateService.claimCertificate(ticket.eventId, { certificateType: 'COMPLETION' });
      const pdfUrl = certificateService.getCertificatePdfUrl(cert.certificateCode);
      window.open(pdfUrl, '_blank');
    } catch (err) {
      alert('Certificate claiming: ' + (err.message || 'Please ensure you checked in for this event.'));
    } finally {
      setClaimingCertEventId(null);
    }
  };

  const handleOpenRefundModal = (ticket) => {
    setRefundModalTarget(ticket);
    setRefundReason('');
    setRefundError('');
  };

  const handleSubmitRefund = async (e) => {
    e.preventDefault();
    if (!refundReason.trim() || refundReason.trim().length < 5) {
      setRefundError('Please provide a reason of at least 5 characters.');
      return;
    }
    setIsSubmittingRefund(true);
    setRefundError('');
    try {
      await refundService.requestRefund({
        orderId: refundModalTarget.orderId,
        reason: refundReason.trim()
      });
      setRefundModalTarget(null);
      await loadAllData();
      alert('Refund request submitted successfully! The organizer will review your submission.');
    } catch (err) {
      setRefundError(err.message || 'Failed to submit refund request.');
    } finally {
      setIsSubmittingRefund(false);
    }
  };

  const handleMarkNotificationRead = async (id) => {
    try {
      await notificationService.markAsRead(id);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
    } catch (err) {
      console.error('Failed to mark notification read', err);
    }
  };

  const handleMarkAllNotificationsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
    } catch (err) {
      console.error('Failed to mark all read', err);
    }
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    setIsSavingProfile(true);
    setProfileMessage({ type: '', text: '' });
    try {
      await authService.updateProfile({
        fullName: profile.fullName,
        phone: profile.phone
      });
      setProfileMessage({ type: 'success', text: 'Personal information & preferences updated successfully!' });
    } catch (err) {
      setProfileMessage({ type: 'error', text: err.message || 'Failed to update profile' });
    } finally {
      setIsSavingProfile(false);
    }
  };

  return (
    <div className="my-tickets-page attendee-dashboard-container">
      {/* Attendee Header */}
      <div className="tickets-header">
        <div className="header-text-group">
          <div className="tickets-icon-box">
            <User size={24} />
          </div>
          <div>
            <h2>Attendee Portal & Passbook</h2>
            <p>Manage your upcoming events, digital ticket purchases, live updates, registration history, and profile settings.</p>
          </div>
        </div>

        {/* 5 Core Attendee Dashboard Navigation Tabs */}
        <div className="ticket-filter-bar">
          <button
            className={`filter-btn ${activeTab === 'upcoming' ? 'active' : ''}`}
            onClick={() => setActiveTab('upcoming')}
          >
            <Calendar size={14} />
            <span>1. Upcoming Events ({upcomingEvents.length})</span>
          </button>

          <button
            className={`filter-btn ${activeTab === 'tickets' ? 'active' : ''}`}
            onClick={() => setActiveTab('tickets')}
          >
            <Ticket size={14} />
            <span>2. Ticket Purchases ({tickets.length})</span>
          </button>

          <button
            className={`filter-btn ${activeTab === 'updates' ? 'active' : ''}`}
            onClick={() => setActiveTab('updates')}
          >
            <Bell size={14} />
            <span>3. Event Updates {notifications.filter(n => !n.read).length > 0 && `(${notifications.filter(n => !n.read).length})`}</span>
          </button>

          <button
            className={`filter-btn ${activeTab === 'history' ? 'active' : ''}`}
            onClick={() => setActiveTab('history')}
          >
            <History size={14} />
            <span>4. Registration History ({orders.length})</span>
          </button>

          <button
            className={`filter-btn ${activeTab === 'profile' ? 'active' : ''}`}
            onClick={() => setActiveTab('profile')}
          >
            <User size={14} />
            <span>5. Profile Management</span>
          </button>
        </div>
      </div>

      {/* Main Content Area */}
      <div className="tickets-content">
        {isLoading ? (
          <div className="tickets-loading">Loading attendee dashboard data...</div>
        ) : (
          <>
            {/* 1. UPCOMING EVENTS */}
            {activeTab === 'upcoming' && (
              upcomingEvents.length === 0 ? (
                <div className="tickets-empty">
                  <div className="empty-qr-box">
                    <Calendar size={36} />
                  </div>
                  <h3>No Upcoming Event Registrations</h3>
                  <p>You have not registered for any upcoming conferences, summits, or festivals yet.</p>
                  {onExploreEvents && (
                    <button className="btn-explore-now" onClick={onExploreEvents}>
                      <span>Explore Live Events</span>
                      <ArrowRight size={16} />
                    </button>
                  )}
                </div>
              ) : (
                <div className="upcoming-events-grid">
                  {upcomingEvents.map(evt => (
                    <div key={evt.id || evt.title} className="upcoming-event-card">
                      <div className="upcoming-event-badge">
                        <span className="dot-pulse"></span>
                        <span>Registered Attendee</span>
                      </div>
                      <h3 className="upcoming-event-title">{evt.title}</h3>
                      <div className="upcoming-event-meta">
                        <div className="meta-line">
                          <Calendar size={14} />
                          <span>{formatDate(evt.startTime)}</span>
                        </div>
                        <div className="meta-line">
                          <MapPin size={14} />
                          <span>{evt.venueName}{evt.venueCity ? `, ${evt.venueCity}` : ''}</span>
                        </div>
                      </div>
                      <div className="upcoming-passes-summary">
                        <span>Allocated Passes: <strong>{evt.passes.length}</strong></span>
                        <div className="passes-pill-row">
                          {evt.passes.map(p => (
                            <span key={p.id} className="pass-pill-item">
                              {p.tierName} ({p.ticketCode})
                            </span>
                          ))}
                        </div>
                      </div>
                      <button
                        className="btn-view-tickets-quick"
                        onClick={() => setActiveTab('tickets')}
                      >
                        <Ticket size={14} />
                        <span>Manage Passes & Download QR</span>
                      </button>
                    </div>
                  ))}
                </div>
              )
            )}

            {/* 2. TICKET PURCHASES */}
            {activeTab === 'tickets' && (
              tickets.length === 0 ? (
                <div className="tickets-empty">
                  <div className="empty-qr-box">
                    <Ticket size={36} />
                  </div>
                  <h3>No Tickets Purchased Yet</h3>
                  <p>Purchase event tickets to receive your high-resolution QR entrance codes and PDF admission passes.</p>
                  {onExploreEvents && (
                    <button className="btn-explore-now" onClick={onExploreEvents}>
                      <span>Browse Events & Tickets</span>
                      <ArrowRight size={16} />
                    </button>
                  )}
                </div>
              ) : (
                <div className="passes-grid">
                  {tickets.map(t => {
                    const isValid = t.status === 'VALID';
                    const pendingRefund = refundRequests.find(r => r.orderNumber === t.orderNumber && r.status === 'PENDING');
                    const processedRefund = refundRequests.find(r => r.orderNumber === t.orderNumber && (r.status === 'APPROVED' || r.status === 'PROCESSED'));

                    return (
                      <div key={t.id} className={`pass-card ${!isValid ? 'checked-in' : ''}`}>
                        <div className="pass-main">
                          <div className="pass-top-row">
                            <span className="pass-order-num">Order #{t.orderNumber}</span>
                            <div className="pass-status-group">
                              {pendingRefund ? (
                                <span className="pass-status-pill status-refund-pending">
                                  <Clock size={12} />
                                  <span>Refund Pending</span>
                                </span>
                              ) : processedRefund ? (
                                <span className="pass-status-pill status-refunded">
                                  <RotateCcw size={12} />
                                  <span>Refunded</span>
                                </span>
                              ) : (
                                <span className={`pass-status-pill ${isValid ? 'status-valid' : 'status-used'}`}>
                                  {isValid ? (
                                    <>
                                      <span className="dot-pulse"></span>
                                      <span>Valid for Entry</span>
                                    </>
                                  ) : (
                                    <>
                                      <CheckCircle2 size={12} />
                                      <span>Checked In</span>
                                    </>
                                  )}
                                </span>
                              )}
                            </div>
                          </div>

                          <h3 className="pass-event-title">{t.eventTitle}</h3>

                          <div className="pass-tier-badge">
                            <Sparkles size={12} />
                            <span>{t.tierName}</span>
                          </div>

                          <div className="pass-details-list">
                            <div className="pass-detail-row">
                              <Calendar size={14} className="detail-icon" />
                              <span>{formatDate(t.eventStartTime)}</span>
                            </div>
                            <div className="pass-detail-row">
                              <MapPin size={14} className="detail-icon" />
                              <span>{t.venueName || 'Conference Center'}</span>
                            </div>
                          </div>

                          {isValid && !pendingRefund && !processedRefund && (
                            <button
                              className="btn-request-refund-link"
                              onClick={() => handleOpenRefundModal(t)}
                            >
                              <RotateCcw size={12} />
                              <span>Request Refund / Cancel Pass</span>
                            </button>
                          )}
                        </div>

                        {/* Right Pass Stub */}
                        <div className="pass-stub">
                          <div
                            className="qr-stub-box"
                            onClick={() => setActiveQrModal(t)}
                            title="Click to zoom admission QR code"
                          >
                            <img src={t.qrCodeData} alt="Admission QR Code" className="stub-qr-img" />
                            <span className="qr-zoom-hint">Tap to enlarge</span>
                          </div>
                          <code className="stub-ticket-code">{t.ticketCode}</code>

                          <button
                            className="btn-download-pdf"
                            onClick={() => handleDownloadPdf(t)}
                            disabled={downloadingCode === t.ticketCode}
                          >
                            <Download size={13} />
                            <span>{downloadingCode === t.ticketCode ? 'Saving...' : 'PDF Ticket'}</span>
                          </button>

                          <button
                            className="btn-download-pdf btn-lanyard"
                            onClick={() => handleDownloadLanyardBadge(t)}
                            disabled={downloadingBadgeCode === t.ticketCode}
                          >
                            <Tag size={13} />
                            <span>{downloadingBadgeCode === t.ticketCode ? 'Saving...' : 'Lanyard Badge'}</span>
                          </button>

                          <button
                            className="btn-download-pdf btn-certificate"
                            onClick={() => handleClaimCertificate(t)}
                            disabled={claimingCertEventId === t.eventId}
                          >
                            <Award size={13} />
                            <span>{claimingCertEventId === t.eventId ? 'Claiming...' : 'Certificate'}</span>
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )
            )}

            {/* 3. EVENT UPDATES */}
            {activeTab === 'updates' && (
              <div className="attendee-updates-section">
                <div className="updates-header-bar">
                  <h4>Live Notifications & Event Broadcasts</h4>
                  {notifications.some(n => !n.read) && (
                    <button className="btn-mark-all-read" onClick={handleMarkAllNotificationsRead}>
                      <CheckCheck size={14} />
                      <span>Mark all as read</span>
                    </button>
                  )}
                </div>

                {notifications.length === 0 ? (
                  <div className="tickets-empty">
                    <div className="empty-qr-box">
                      <Bell size={36} />
                    </div>
                    <h3>No Event Updates Yet</h3>
                    <p>When organizers publish updates, venue changes, or ticket notices, they will appear here in real time.</p>
                  </div>
                ) : (
                  <div className="updates-list">
                    {notifications.map(n => (
                      <div key={n.id} className={`update-item ${!n.read ? 'unread' : ''}`}>
                        <div className="update-icon-circle">
                          <Bell size={16} />
                        </div>
                        <div className="update-body">
                          <div className="update-title-row">
                            <h5>{n.title}</h5>
                            <span className="update-time">{new Date(n.createdAt).toLocaleString()}</span>
                          </div>
                          <p>{n.message}</p>
                        </div>
                        {!n.read && (
                          <button
                            className="btn-mark-single-read"
                            onClick={() => handleMarkNotificationRead(n.id)}
                            title="Mark as read"
                          >
                            <CheckCircle2 size={16} />
                          </button>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* 4. REGISTRATION HISTORY */}
            {activeTab === 'history' && (
              <div className="registration-history-section">
                <h4 className="section-subtitle">Past Registrations & Invoices</h4>
                {orders.length === 0 ? (
                  <div className="tickets-empty">
                    <div className="empty-qr-box">
                      <History size={36} />
                    </div>
                    <h3>No Order History Recorded</h3>
                    <p>Completed registrations, orders, and receipts will be cataloged in this table.</p>
                  </div>
                ) : (
                  <div className="history-table-wrapper">
                    <table className="history-table">
                      <thead>
                        <tr>
                          <th>Order #</th>
                          <th>Event Name</th>
                          <th>Date</th>
                          <th>Tickets</th>
                          <th>Total Amount</th>
                          <th>Status</th>
                          <th>Payment Ref</th>
                        </tr>
                      </thead>
                      <tbody>
                        {orders.map(o => (
                          <tr key={o.id || o.orderNumber}>
                            <td><code>{o.orderNumber}</code></td>
                            <td><strong>{o.eventTitle || 'Conference Pass'}</strong></td>
                            <td>{new Date(o.createdAt).toLocaleDateString()}</td>
                            <td>{o.items?.reduce((s, it) => s + (it.quantity || 1), 0) || o.ticketsCount || 1} pass(es)</td>
                            <td>${Number(o.totalAmount).toFixed(2)}</td>
                            <td>
                              <span className={`status-pill status-${(o.status || 'confirmed').toLowerCase()}`}>
                                {o.status}
                              </span>
                            </td>
                            <td><small>{o.paymentReference || 'N/A'}</small></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}

            {/* 5. PROFILE MANAGEMENT */}
            {activeTab === 'profile' && (
              <div className="attendee-profile-section">
                <div className="profile-card">
                  <div className="profile-card-header">
                    <h4>Personal Information & Preferences</h4>
                    <p>Update your attendee name, contact credentials, and communication preferences.</p>
                  </div>

                  {profileMessage.text && (
                    <div className={`profile-alert ${profileMessage.type}`}>
                      {profileMessage.type === 'success' ? <CheckCircle2 size={16} /> : <AlertTriangle size={16} />}
                      <span>{profileMessage.text}</span>
                    </div>
                  )}

                  <form onSubmit={handleUpdateProfile} className="profile-form">
                    <div className="form-group">
                      <label>Full Name *</label>
                      <input
                        type="text"
                        value={profile.fullName}
                        onChange={(e) => setProfile({ ...profile, fullName: e.target.value })}
                        required
                        placeholder="Your full name"
                      />
                    </div>

                    <div className="form-group">
                      <label>Registered Email Address</label>
                      <input
                        type="email"
                        value={profile.email}
                        disabled
                        className="input-disabled"
                      />
                      <small className="help-text">Email address is tied to your login account.</small>
                    </div>

                    <div className="form-group">
                      <label>Phone Number</label>
                      <input
                        type="tel"
                        value={profile.phone}
                        onChange={(e) => setProfile({ ...profile, phone: e.target.value })}
                        placeholder="+1 (555) 000-0000"
                      />
                    </div>

                    <div className="profile-actions">
                      <button
                        type="submit"
                        className="btn-save-profile"
                        disabled={isSavingProfile}
                      >
                        {isSavingProfile ? <Loader2 size={15} className="spin" /> : <Save size={15} />}
                        <span>{isSavingProfile ? 'Saving Changes...' : 'Save Profile Preferences'}</span>
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Enlarged QR Code Modal */}
      {activeQrModal && (
        <div className="qr-modal-backdrop" onClick={() => setActiveQrModal(null)}>
          <div className="qr-modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="qr-modal-header">
              <h4>Digital Admission Pass</h4>
              <button className="btn-close-qr" onClick={() => setActiveQrModal(null)}>
                <X size={18} />
              </button>
            </div>

            <div className="qr-modal-body">
              <span className="modal-event-name">{activeQrModal.eventTitle}</span>
              <span className="modal-tier-name">{activeQrModal.tierName}</span>

              <div className="large-qr-wrap">
                <img src={activeQrModal.qrCodeData} alt="Admission QR Code" className="large-qr-img" />
              </div>

              <code className="modal-ticket-code">{activeQrModal.ticketCode}</code>
              <p className="modal-attendee-line">
                Presented by: <strong>{activeQrModal.attendeeName}</strong>
              </p>

              <button
                className="btn-modal-download-pdf"
                onClick={() => handleDownloadPdf(activeQrModal)}
                disabled={downloadingCode === activeQrModal.ticketCode}
              >
                <Download size={16} />
                <span>
                  {downloadingCode === activeQrModal.ticketCode
                    ? 'Generating High-Def PDF Pass...'
                    : 'Download Printable PDF Ticket Pass'}
                </span>
              </button>

              <div className="scanner-instruction">
                <ShieldCheck size={16} />
                <span>Present this screen to event organizers at check-in for instant verification.</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Refund Request Modal */}
      {refundModalTarget && (
        <div className="refund-modal-backdrop" onClick={() => setRefundModalTarget(null)}>
          <div className="refund-modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="refund-modal-header">
              <div className="refund-modal-title-group">
                <RotateCcw size={18} className="text-amber" />
                <h4>Request Ticket Cancellation & Refund</h4>
              </div>
              <button className="btn-close-refund" onClick={() => setRefundModalTarget(null)}>
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleSubmitRefund} className="refund-modal-body">
              <div className="refund-pass-summary">
                <div className="summary-item">
                  <span className="summary-label">Event:</span>
                  <span className="summary-val">{refundModalTarget.eventTitle}</span>
                </div>
                <div className="summary-item">
                  <span className="summary-label">Tier & Pass:</span>
                  <span className="summary-val">{refundModalTarget.tierName} ({refundModalTarget.ticketCode})</span>
                </div>
                <div className="summary-item">
                  <span className="summary-label">Order Reference:</span>
                  <span className="summary-val">#{refundModalTarget.orderNumber}</span>
                </div>
              </div>

              <div className="refund-notice-box">
                <DollarSign size={16} className="text-emerald" />
                <p>
                  Upon approval by the organizer, the order will be marked <strong>REFUNDED</strong> and ticket pass voided.
                </p>
              </div>

              {refundError && (
                <div className="refund-error-banner">
                  <AlertTriangle size={15} />
                  <span>{refundError}</span>
                </div>
              )}

              <div className="refund-input-group">
                <label>Cancellation Reason *</label>
                <textarea
                  rows={3}
                  placeholder="e.g. Flight canceled, schedule conflict, medical emergency..."
                  value={refundReason}
                  onChange={(e) => setRefundReason(e.target.value)}
                  required
                />
              </div>

              <div className="refund-actions-row">
                <button
                  type="button"
                  className="btn-cancel-refund"
                  onClick={() => setRefundModalTarget(null)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-submit-refund"
                  disabled={isSubmittingRefund}
                >
                  {isSubmittingRefund ? <Loader2 size={15} className="spin" /> : <RotateCcw size={15} />}
                  <span>{isSubmittingRefund ? 'Submitting...' : 'Submit Refund Request'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
