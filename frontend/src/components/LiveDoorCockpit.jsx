import React, { useState, useEffect, useRef } from 'react';
import {
  Radio,
  Users,
  CheckCircle2,
  Clock,
  TrendingUp,
  ScanLine,
  X,
  ShieldCheck,
  AlertCircle,
  RefreshCw,
  Zap,
} from 'lucide-react';
import { ticketPassService, eventService } from '../services/api';
import './LiveDoorCockpit.css';

export default function LiveDoorCockpit({ initialEventId = null, initialEventTitle = '', onClose }) {
  const [events, setEvents] = useState([]);
  const [selectedEventId, setSelectedEventId] = useState(initialEventId);
  const [selectedEventTitle, setSelectedEventTitle] = useState(initialEventTitle);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [scannerCode, setScannerCode] = useState('');
  const [scanning, setScanning] = useState(false);
  const [scanAlert, setScanAlert] = useState(null); // { type: 'success' | 'error', message: string }
  const [socketConnected, setSocketConnected] = useState(false);
  const wsRef = useRef(null);
  const scanInputRef = useRef(null);

  // Play subtle feedback beep using Web Audio API
  const playBeep = (isSuccess) => {
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = isSuccess ? 'sine' : 'sawtooth';
      osc.frequency.setValueAtTime(isSuccess ? 880 : 220, ctx.currentTime);
      gain.gain.setValueAtTime(0.15, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.25);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.25);
    } catch {
      // Audio context restricted by autoplay policies
    }
  };

  // 1. Fetch available events if eventId not pre-selected
  useEffect(() => {
    const loadEvents = async () => {
      try {
        const res = await eventService.getEvents({ page: 0, size: 20 });
        const list = res.content || [];
        setEvents(list);
        if (!selectedEventId && list.length > 0) {
          setSelectedEventId(list[0].id);
          setSelectedEventTitle(list[0].title);
        }
      } catch (err) {
        console.error('Failed to load events for cockpit:', err);
      }
    };
    loadEvents();
  }, []);

  // 2. Fetch Door Stats for active event
  const fetchStats = async (silent = false) => {
    if (!selectedEventId) return;
    if (!silent) setLoading(true);
    try {
      const data = await ticketPassService.getDoorStats(selectedEventId);
      setStats(data);
      if (data.eventTitle && !selectedEventTitle) {
        setSelectedEventTitle(data.eventTitle);
      }
    } catch (err) {
      console.error('Failed to fetch door stats:', err);
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedEventId) {
      fetchStats();
      const pollTimer = setInterval(() => {
        fetchStats(true);
      }, 3000);
      return () => clearInterval(pollTimer);
    }
  }, [selectedEventId]);

  // 3. Connect to WebSocket / STOMP endpoint
  useEffect(() => {
    if (!selectedEventId) return;

    let ws;
    try {
      const wsUrl = (window.location.protocol === 'https:' ? 'wss://' : 'ws://') +
        (window.location.hostname || 'localhost') + ':8085/ws-eventflow/websocket';

      ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        // Send STOMP Connect frame
        ws.send('CONNECT\naccept-version:1.2,1.1,1.0\nheart-beat:10000,10000\n\n\0');
      };

      ws.onmessage = (event) => {
        const msg = event.data;
        if (msg.includes('CONNECTED')) {
          setSocketConnected(true);
          // Subscribe to live turnstile topic
          ws.send(`SUBSCRIBE\nid:sub-0\ndestination:/topic/turnstile/live\n\n\0`);
        } else if (msg.includes('MESSAGE')) {
          // Parse STOMP body
          const bodyIndex = msg.indexOf('\n\n');
          if (bodyIndex !== -1) {
            const rawJson = msg.substring(bodyIndex + 2).replace(/\0$/, '').trim();
            try {
              const liveEvent = JSON.parse(rawJson);
              if (liveEvent.eventId === selectedEventId) {
                // Instantly update stats state
                setStats(prev => {
                  if (!prev) return prev;
                  return {
                    ...prev,
                    checkedInCount: liveEvent.totalCheckedIn,
                    totalIssuedTickets: liveEvent.totalCapacity,
                    checkInPercentage: liveEvent.checkInPercentage,
                    arrivalsLast15Min: liveEvent.arrivalVelocity15Min,
                  };
                });
              }
            } catch {
              // Non-JSON STOMP frame
            }
          }
        }
      };

      ws.onerror = () => {
        setSocketConnected(false);
      };

      ws.onclose = () => {
        setSocketConnected(false);
      };
    } catch (e) {
      console.warn('WebSocket connection not initialized:', e);
    }

    return () => {
      if (ws) {
        try { ws.close(); } catch {}
      }
    };
  }, [selectedEventId]);

  // Handle Turnstile Check-in
  const handleCheckInSubmit = async (e) => {
    e.preventDefault();
    if (!scannerCode.trim() || scanning) return;

    const codeToScan = scannerCode.trim().toUpperCase();
    setScanning(true);
    setScanAlert(null);

    try {
      const result = await ticketPassService.checkInTicket(codeToScan);
      playBeep(true);
      setScanAlert({
        type: 'success',
        message: result.message || `Check-in Verified for ${result.ticket?.attendeeName || codeToScan}`,
      });
      setScannerCode('');
      fetchStats(true);
    } catch (err) {
      playBeep(false);
      setScanAlert({
        type: 'error',
        message: err.message || 'Ticket verification failed. Invalid or already used pass.',
      });
    } finally {
      setScanning(false);
      if (scanInputRef.current) {
        scanInputRef.current.focus();
      }
    }
  };

  const pct = stats?.checkInPercentage || 0;

  return (
    <div className="cockpit-modal-backdrop" onClick={onClose}>
      <div className="cockpit-container" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="cockpit-header">
          <div className="cockpit-title-row">
            <div className="cockpit-icon-badge">
              <ScanLine size={24} />
            </div>
            <div className="cockpit-title-text">
              <h2>
                Turnstile Door Operations
                <span className="cockpit-status-badge live">
                  <span className="cockpit-pulse-dot"></span>
                  {socketConnected ? 'LIVE STOMP WS' : 'LIVE 3S STREAM'}
                </span>
              </h2>
              <p className="cockpit-event-subtitle">
                {selectedEventTitle || 'Real-Time Access Control & Arrival Velocity'}
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            {events.length > 1 && (
              <select
                value={selectedEventId || ''}
                onChange={(e) => {
                  const ev = events.find(x => x.id === Number(e.target.value));
                  if (ev) {
                    setSelectedEventId(ev.id);
                    setSelectedEventTitle(ev.title);
                  }
                }}
                style={{
                  background: 'rgba(15, 23, 42, 0.8)',
                  border: '1px solid rgba(255, 255, 255, 0.15)',
                  borderRadius: '10px',
                  color: '#e2e8f0',
                  padding: '0.4rem 0.8rem',
                  fontSize: '0.8rem',
                  cursor: 'pointer',
                }}
              >
                {events.map((ev) => (
                  <option key={ev.id} value={ev.id}>{ev.title}</option>
                ))}
              </select>
            )}

            <button
              className="cockpit-close-btn"
              onClick={() => fetchStats()}
              title="Manual Refresh"
            >
              <RefreshCw size={16} />
            </button>

            {onClose && (
              <button className="cockpit-close-btn" onClick={onClose} title="Close Cockpit">
                <X size={18} />
              </button>
            )}
          </div>
        </div>

        {/* Cockpit Body */}
        <div className="cockpit-body">
          {/* Telemetry Metrics */}
          <div className="cockpit-metrics-grid">
            <div className="cockpit-metric-card accent-indigo">
              <div className="cockpit-metric-header">
                <span>Total Issued</span>
                <Users size={16} color="#818cf8" />
              </div>
              <div className="cockpit-metric-val">
                {stats?.totalIssuedTickets ?? '—'}
              </div>
              <div className="cockpit-metric-subtext">
                Capacity / Tickets sold
              </div>
            </div>

            <div className="cockpit-metric-card accent-emerald">
              <div className="cockpit-metric-header">
                <span>Checked In</span>
                <CheckCircle2 size={16} color="#34d399" />
              </div>
              <div className="cockpit-metric-val" style={{ color: '#34d399' }}>
                {stats?.checkedInCount ?? 0}
              </div>
              <div className="cockpit-metric-subtext">
                {pct}% of registered attendees
              </div>
            </div>

            <div className="cockpit-metric-card accent-amber">
              <div className="cockpit-metric-header">
                <span>Pending Entry</span>
                <Clock size={16} color="#fbbf24" />
              </div>
              <div className="cockpit-metric-val" style={{ color: '#fbbf24' }}>
                {stats?.pendingCheckInCount ?? 0}
              </div>
              <div className="cockpit-metric-subtext">
                Attendees yet to arrive
              </div>
            </div>

            <div className="cockpit-metric-card accent-cyan">
              <div className="cockpit-metric-header">
                <span>Door Velocity</span>
                <TrendingUp size={16} color="#22d3ee" />
              </div>
              <div className="cockpit-metric-val" style={{ color: '#22d3ee' }}>
                {stats?.arrivalsLast15Min ?? 0}
              </div>
              <div className="cockpit-metric-subtext">
                Arrivals in last 15 mins
              </div>
            </div>
          </div>

          {/* Real-Time Radial / Gauge Bar */}
          <div className="cockpit-progress-container">
            <div className="cockpit-progress-header">
              <span>Gate Entry Progress</span>
              <span>{stats?.checkedInCount ?? 0} / {stats?.totalIssuedTickets ?? 0} Checked In ({pct}%)</span>
            </div>
            <div className="cockpit-progress-bar-bg">
              <div
                className="cockpit-progress-bar-fill"
                style={{ width: `${Math.min(100, pct)}%` }}
              ></div>
            </div>
          </div>

          {/* Hardware Barcode Scanner & Rapid Input */}
          <div className="cockpit-scanner-panel">
            <div className="cockpit-scanner-title">
              <h3>
                <ScanLine size={18} color="#818cf8" />
                Live Turnstile Scanner / Pass Verification
              </h3>
              <span style={{ fontSize: '0.75rem', color: '#64748b' }}>
                Autofocus enabled • Press ENTER to scan
              </span>
            </div>

            <form className="cockpit-scanner-form" onSubmit={handleCheckInSubmit}>
              <div className="cockpit-scanner-input-wrap">
                <ScanLine size={18} />
                <input
                  ref={scanInputRef}
                  type="text"
                  className="cockpit-scanner-input"
                  placeholder="Scan QR / Enter Ticket Code (e.g. TCK-90918527-1)..."
                  value={scannerCode}
                  onChange={(e) => setScannerCode(e.target.value)}
                  autoFocus
                />
              </div>
              <button
                type="submit"
                className="cockpit-scanner-submit-btn"
                disabled={!scannerCode.trim() || scanning}
              >
                {scanning ? <RefreshCw size={16} className="spin" /> : <ShieldCheck size={16} />}
                Verify & Admit
              </button>
            </form>

            {/* Scan Feedback Banner */}
            {scanAlert && (
              <div className={`cockpit-alert-banner ${scanAlert.type}`}>
                {scanAlert.type === 'success' ? (
                  <CheckCircle2 size={18} color="#34d399" />
                ) : (
                  <AlertCircle size={18} color="#f87171" />
                )}
                <span>{scanAlert.message}</span>
              </div>
            )}
          </div>

          {/* Recent Arrivals Activity Feed */}
          <div className="cockpit-feed-panel">
            <div className="cockpit-feed-header">
              <h3>
                <Clock size={16} color="#94a3b8" />
                Live Arrivals Log
              </h3>
              <span style={{ fontSize: '0.75rem', color: '#64748b' }}>
                Showing last verified check-ins
              </span>
            </div>

            {stats?.recentCheckIns && stats.recentCheckIns.length > 0 ? (
              <div className="cockpit-feed-list">
                {stats.recentCheckIns.map((item, idx) => (
                  <div key={item.id || idx} className="cockpit-feed-item">
                    <div className="cockpit-feed-user">
                      <div className="cockpit-feed-avatar">
                        {(item.attendeeName || 'A').charAt(0).toUpperCase()}
                      </div>
                      <div className="cockpit-feed-details">
                        <span className="cockpit-feed-name">{item.attendeeName}</span>
                        <span className="cockpit-feed-code">{item.ticketCode} • {item.attendeeEmail}</span>
                      </div>
                    </div>

                    <div className="cockpit-feed-meta">
                      <span className="cockpit-tier-pill">
                        {item.tierName || 'Standard'}
                      </span>
                      <span className="cockpit-feed-time">
                        <Clock size={12} />
                        {item.checkedInAt ? new Date(item.checkedInAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : 'Just now'}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="cockpit-empty-state">
                <Users size={28} color="#475569" />
                <span>No attendees checked in yet today. Ready for door intake.</span>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
