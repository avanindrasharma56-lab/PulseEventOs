import React, { useState, useEffect } from 'react';
import { 
  Scan, 
  CheckCircle2, 
  AlertTriangle, 
  XCircle, 
  Search, 
  Users, 
  Calendar, 
  QrCode,
  ShieldCheck,
  RefreshCw,
  Radio,
  Tag
} from 'lucide-react';
import { ticketPassService, eventService } from '../services/api';
import LiveDoorCockpit from './LiveDoorCockpit';
import './CheckInScanner.css';

export default function CheckInScanner() {
  const [ticketCodeInput, setTicketCodeInput] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [verificationResult, setVerificationResult] = useState(null); // { status: 'success' | 'warning' | 'error', message, ticket }
  const [showCockpit, setShowCockpit] = useState(false);

  // Event Roster Management
  const [myEvents, setMyEvents] = useState([]);
  const [selectedEventId, setSelectedEventId] = useState('');
  const [attendeeTickets, setAttendeeTickets] = useState([]);
  const [isLoadingRoster, setIsLoadingRoster] = useState(false);

  useEffect(() => {
    async function loadEvents() {
      try {
        const events = await eventService.getMyEvents();
        setMyEvents(events);
        if (events.length > 0) {
          setSelectedEventId(events[0].id.toString());
        }
      } catch (err) {
        console.error('Failed to load organizer events', err);
      }
    }
    loadEvents();
  }, []);

  const loadRoster = async (eventId) => {
    if (!eventId) return;
    setIsLoadingRoster(true);
    try {
      const tickets = await ticketPassService.getEventAttendeeTickets(eventId);
      setAttendeeTickets(tickets);
    } catch (err) {
      console.error('Failed to load roster', err);
      setAttendeeTickets([]);
    } finally {
      setIsLoadingRoster(false);
    }
  };

  useEffect(() => {
    if (selectedEventId) {
      loadRoster(selectedEventId);
    }
  }, [selectedEventId]);

  const handleVerify = async (codeToVerify) => {
    const code = (codeToVerify || ticketCodeInput).trim();
    if (!code) return;

    setIsVerifying(true);
    setVerificationResult(null);

    try {
      const res = await ticketPassService.checkInTicket(code);
      setVerificationResult({
        status: 'success',
        message: res.message || 'Check-in successful! Welcome to the event.',
        ticket: res.data?.ticket || res.ticket,
      });
      setTicketCodeInput('');
      if (selectedEventId) loadRoster(selectedEventId);
    } catch (err) {
      const message = err.message || 'Verification failed';
      const isAlreadyCheckedIn = message.toLowerCase().includes('already checked in');

      setVerificationResult({
        status: isAlreadyCheckedIn ? 'warning' : 'error',
        message,
        ticket: null,
      });
    } finally {
      setIsVerifying(false);
    }
  };

  return (
    <div className="checkin-scanner-page">
      {/* Header */}
      <div className="scanner-header">
        <div className="scanner-title-group">
          <div className="scanner-icon-box">
            <Scan size={22} />
          </div>
          <div>
            <h2>Door Check-In & Scanner Desk</h2>
            <p>Verify attendee QR codes and enforce single-use entry validation in real time.</p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <button
            className="btn-bulk-badges"
            onClick={async () => {
              if (!selectedEventId) return;
              try {
                await ticketPassService.downloadBulkBadgesPdf(Number(selectedEventId), `event-${selectedEventId}-all-badges.pdf`);
              } catch (e) {
                alert('Failed to generate bulk badges: ' + e.message);
              }
            }}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.6rem',
              background: 'rgba(255, 255, 255, 0.06)',
              border: '1px solid rgba(255, 255, 255, 0.15)',
              color: '#cbd5e1',
              padding: '0.65rem 1.25rem',
              borderRadius: '12px',
              fontWeight: 600,
              fontSize: '0.875rem',
              cursor: 'pointer',
              transition: 'all 0.2s ease',
            }}
          >
            <Tag size={16} />
            <span>Print All Badges (Bulk PDF)</span>
          </button>

          <button
            className="btn-cockpit-launch"
            onClick={() => setShowCockpit(true)}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.6rem',
              background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.25), rgba(16, 185, 129, 0.2))',
              border: '1px solid rgba(16, 185, 129, 0.4)',
              color: '#34d399',
              padding: '0.65rem 1.25rem',
              borderRadius: '12px',
              fontWeight: 700,
              fontSize: '0.875rem',
              cursor: 'pointer',
              transition: 'all 0.2s ease',
              boxShadow: '0 0 15px rgba(16, 185, 129, 0.15)',
            }}
          >
            <Radio size={16} />
            <span>Launch Live Turnstile Cockpit (STOMP WS)</span>
          </button>
        </div>
      </div>

      {showCockpit && (
        <LiveDoorCockpit
          initialEventId={selectedEventId ? Number(selectedEventId) : null}
          initialEventTitle={myEvents.find(e => e.id.toString() === selectedEventId)?.title || ''}
          onClose={() => {
            setShowCockpit(false);
            if (selectedEventId) loadRoster(selectedEventId);
          }}
        />
      )}

      <div className="scanner-layout-grid">
        {/* Left Column: Direct Ticket Code Entry & Verification HUD */}
        <div className="scanner-hud-card">
          <div className="hud-card-header">
            <QrCode size={18} className="hud-icon" />
            <h3>Scan or Input Ticket Code</h3>
          </div>

          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleVerify();
            }}
            className="hud-form"
          >
            <div className="hud-input-wrap">
              <input
                type="text"
                placeholder="Scan or type code: e.g. TCK-ABCD-1"
                value={ticketCodeInput}
                onChange={(e) => setTicketCodeInput(e.target.value)}
                className="hud-input"
                autoFocus
              />
              <button
                type="submit"
                className="btn-verify-submit"
                disabled={isVerifying || !ticketCodeInput.trim()}
              >
                <span>{isVerifying ? 'Verifying...' : 'Check In'}</span>
              </button>
            </div>
            <span className="hud-hint">
              Compatible with barcode / 2D laser scanners and camera QR parsers.
            </span>
          </form>

          {/* Verification Status Banner */}
          {verificationResult && (
            <div className={`verification-banner ${verificationResult.status}`}>
              <div className="banner-icon-wrap">
                {verificationResult.status === 'success' && <CheckCircle2 size={28} />}
                {verificationResult.status === 'warning' && <AlertTriangle size={28} />}
                {verificationResult.status === 'error' && <XCircle size={28} />}
              </div>

              <div className="banner-details">
                <span className="banner-title">
                  {verificationResult.status === 'success' && 'Entry Authorized'}
                  {verificationResult.status === 'warning' && 'Duplicate Check-In Detected'}
                  {verificationResult.status === 'error' && 'Invalid / Rejected Ticket'}
                </span>
                <p className="banner-msg">{verificationResult.message}</p>

                {verificationResult.ticket && (
                  <div className="banner-ticket-meta">
                    <span>Attendee: <strong>{verificationResult.ticket.attendeeName}</strong></span>
                    <span>Tier: <strong>{verificationResult.ticket.tierName}</strong></span>
                    <span>Code: <code>{verificationResult.ticket.ticketCode}</code></span>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Right Column: Event Attendee Roster & Quick Action Toggle */}
        <div className="roster-card">
          <div className="roster-card-header">
            <div className="roster-title-wrap">
              <Users size={18} className="hud-icon" />
              <h3>Event Attendee Roster</h3>
            </div>

            <div className="roster-controls">
              <select
                value={selectedEventId}
                onChange={(e) => setSelectedEventId(e.target.value)}
                className="event-selector"
              >
                {myEvents.map(e => (
                  <option key={e.id} value={e.id}>
                    {e.title}
                  </option>
                ))}
              </select>

              <button
                className="btn-refresh-roster"
                onClick={() => loadRoster(selectedEventId)}
                title="Refresh roster"
              >
                <RefreshCw size={14} className={isLoadingRoster ? 'spinning' : ''} />
              </button>
            </div>
          </div>

          <div className="roster-body">
            {isLoadingRoster ? (
              <div className="roster-loading">Updating attendee roster...</div>
            ) : attendeeTickets.length === 0 ? (
              <div className="roster-empty">
                No tickets issued yet for this event.
              </div>
            ) : (
              <div className="roster-table-wrap">
                <table className="roster-table">
                  <thead>
                    <tr>
                      <th>Attendee</th>
                      <th>Tier</th>
                      <th>Ticket Code</th>
                      <th>Status</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {attendeeTickets.map(t => {
                      const isCheckedIn = t.status === 'CHECKED_IN';

                      return (
                        <tr key={t.id} className={isCheckedIn ? 'row-checked-in' : ''}>
                          <td>
                            <div className="attendee-col">
                              <strong>{t.attendeeName}</strong>
                              <span className="attendee-sub">{t.attendeeEmail}</span>
                            </div>
                          </td>
                          <td>
                            <span className="roster-tier-pill">{t.tierName}</span>
                          </td>
                          <td>
                            <code className="roster-code">{t.ticketCode}</code>
                          </td>
                          <td>
                            <span className={`roster-status-badge ${isCheckedIn ? 'badge-in' : 'badge-valid'}`}>
                              {isCheckedIn ? 'Checked In' : 'Valid'}
                            </span>
                          </td>
                          <td>
                            {isCheckedIn ? (
                              <span className="roster-checked-time">
                                {t.checkedInAt ? new Date(t.checkedInAt).toLocaleTimeString() : 'Verified'}
                              </span>
                            ) : (
                              <button
                                className="btn-quick-checkin"
                                onClick={() => handleVerify(t.ticketCode)}
                              >
                                Check In
                              </button>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
