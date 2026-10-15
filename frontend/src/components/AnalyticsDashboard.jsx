import React, { useState, useEffect } from 'react';
import { 
  BarChart3, 
  TrendingUp, 
  DollarSign, 
  Ticket, 
  Users, 
  Scan, 
  Download, 
  Calendar, 
  Layers, 
  CheckCircle2, 
  Clock, 
  ArrowUpRight,
  Filter,
  RefreshCw,
  FileSpreadsheet
} from 'lucide-react';
import { analyticsService, exportService, eventService, pricingService } from '../services/api';
import { useAuth } from '../context/AuthContext';
import './AnalyticsDashboard.css';

export default function AnalyticsDashboard() {
  const { isAdmin, isOrganizer } = useAuth();
  const [organizerData, setOrganizerData] = useState(null);
  const [adminData, setAdminData] = useState(null);
  const [selectedEventId, setSelectedEventId] = useState('ALL');
  const [selectedEventData, setSelectedEventData] = useState(null);
  const [viewMode, setViewMode] = useState('organizer'); // 'organizer' | 'admin'
  const [isLoading, setIsLoading] = useState(true);
  const [isExporting, setIsExporting] = useState(false);

  // Multi-Currency Adaptation State
  const [currency, setCurrency] = useState(localStorage.getItem('preferredCurrency') || 'USD');
  const [currencyRate, setCurrencyRate] = useState({ symbol: '$', rateToUsd: 1.0 });

  useEffect(() => {
    pricingService.getCurrencies().then(currencies => {
      const match = currencies.find(c => c.currencyCode === currency);
      if (match) {
        setCurrencyRate({ symbol: match.symbol || '$', rateToUsd: match.rateToUsd || 1.0 });
      }
    }).catch(() => {});
  }, [currency]);

  useEffect(() => {
    const handleCurrChange = (e) => {
      if (e.detail) setCurrency(e.detail);
    };
    window.addEventListener('currencyChanged', handleCurrChange);
    return () => window.removeEventListener('currencyChanged', handleCurrChange);
  }, []);

  const loadAnalytics = async () => {
    setIsLoading(true);
    try {
      if (isAdmin && viewMode === 'admin') {
        const data = await analyticsService.getAdminAnalytics();
        setAdminData(data);
      } else {
        const data = await analyticsService.getOrganizerAnalytics();
        setOrganizerData(data);
        if (data.events && data.events.length > 0 && selectedEventId === 'ALL') {
          // keep ALL
        }
      }
    } catch (err) {
      console.error('Failed to load analytics', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAnalytics();
  }, [viewMode]);

  // Handle event selection change
  useEffect(() => {
    if (selectedEventId === 'ALL') {
      setSelectedEventData(null);
    } else if (organizerData?.events) {
      const found = organizerData.events.find(e => e.eventId.toString() === selectedEventId);
      setSelectedEventData(found || null);
    }
  }, [selectedEventId, organizerData]);

  const handleExportCsv = async (evtId, evtSlug) => {
    const targetId = evtId || (selectedEventData ? selectedEventData.eventId : null);
    const targetSlug = evtSlug || (selectedEventData ? selectedEventData.slug : 'event');
    if (!targetId) return;

    setIsExporting(true);
    try {
      await exportService.downloadAttendeeCsv(targetId, targetSlug);
    } catch (err) {
      alert('Failed to export attendee roster: ' + err.message);
    } finally {
      setIsExporting(false);
    }
  };

  // Metrics to display (based on filter)
  const displayMetrics = selectedEventData ? {
    revenue: selectedEventData.totalRevenue || 0,
    sold: selectedEventData.ticketsSold || 0,
    capacity: selectedEventData.totalCapacity || 0,
    checkIns: selectedEventData.checkInCount || 0,
    checkInRate: selectedEventData.checkInRate || 0,
    eventsCount: 1,
    tiers: selectedEventData.tierBreakdowns || [],
    orders: selectedEventData.recentOrders || [],
  } : organizerData ? {
    revenue: organizerData.totalRevenue || 0,
    sold: organizerData.totalTicketsSold || 0,
    capacity: organizerData.totalCapacity || 0,
    checkIns: organizerData.totalCheckIns || 0,
    checkInRate: organizerData.overallCheckInRate || 0,
    eventsCount: organizerData.totalEvents || 0,
    publishedCount: organizerData.publishedEvents || 0,
    tiers: [],
    orders: organizerData.events?.flatMap(e => e.recentOrders || []).slice(0, 10) || [],
  } : {
    revenue: 0,
    sold: 0,
    capacity: 0,
    checkIns: 0,
    checkInRate: 0,
    eventsCount: 0,
    tiers: [],
    orders: [],
  };

  const occupancyRate = displayMetrics.capacity > 0 
    ? Math.min(100, ((displayMetrics.sold / displayMetrics.capacity) * 100).toFixed(1))
    : 0;

  return (
    <div className="analytics-dashboard-page">
      {/* Header Bar */}
      <div className="analytics-header">
        <div className="analytics-title-group">
          <div className="analytics-icon-box">
            <BarChart3 size={24} />
          </div>
          <div>
            <h2>Performance & Financial Analytics</h2>
            <p>Monitor real-time ticket turnover, capacity utilization, door arrival rate, and export attendee rosters.</p>
          </div>
        </div>

        {/* Header Controls */}
        <div className="analytics-controls">
          {isAdmin && (
            <div className="mode-pill-toggle">
              <button 
                className={`toggle-btn ${viewMode === 'organizer' ? 'active' : ''}`}
                onClick={() => setViewMode('organizer')}
              >
                My Events
              </button>
              <button 
                className={`toggle-btn ${viewMode === 'admin' ? 'active' : ''}`}
                onClick={() => setViewMode('admin')}
              >
                Platform Wide
              </button>
            </div>
          )}

          {viewMode === 'organizer' && organizerData?.events?.length > 0 && (
            <div className="event-filter-wrap">
              <select 
                value={selectedEventId}
                onChange={(e) => setSelectedEventId(e.target.value)}
                className="analytics-event-select"
              >
                <option value="ALL">All Managed Events ({organizerData.events.length})</option>
                {organizerData.events.map(e => (
                  <option key={e.eventId} value={e.eventId.toString()}>
                    {e.title}
                  </option>
                ))}
              </select>

              <button
                className="btn-export-csv"
                onClick={() => handleExportCsv()}
                disabled={selectedEventId === 'ALL' || isExporting}
                title={selectedEventId === 'ALL' ? 'Select an event to export CSV roster' : 'Export attendee CSV'}
              >
                <Download size={15} />
                <span>{isExporting ? 'Exporting...' : 'Export CSV'}</span>
              </button>
            </div>
          )}

          <button 
            className="btn-refresh-analytics" 
            onClick={loadAnalytics}
            title="Refresh analytics data"
          >
            <RefreshCw size={15} className={isLoading ? 'spinning' : ''} />
          </button>
        </div>
      </div>

      {isLoading && !organizerData && !adminData ? (
        <div className="analytics-loading-box">
          <div className="pulse-loader"></div>
          <p>Compiling database financial ledger and ticket metrics...</p>
        </div>
      ) : (
        <div className="analytics-content-layout">
          {/* 4 Primary KPI Metric Cards */}
          <div className="kpi-grid">
            {/* Card 1: Total Revenue */}
            <div className="kpi-card revenue-card">
              <div className="kpi-card-header">
                <span className="kpi-label">Gross Revenue ({currency})</span>
                <div className="kpi-icon-wrap icon-emerald">
                  <DollarSign size={18} />
                </div>
              </div>
              <div className="kpi-value-row">
                <span className="kpi-main-number">
                  {currencyRate.symbol || '$'}{(Number(displayMetrics.revenue) * (currencyRate.rateToUsd || 1.0)).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </span>
              </div>
              <span className="kpi-subtext">
                Confirmed orders across active ticket tiers
              </span>
            </div>

            {/* Card 2: Tickets Sold */}
            <div className="kpi-card tickets-card">
              <div className="kpi-card-header">
                <span className="kpi-label">Tickets Reserved</span>
                <div className="kpi-icon-wrap icon-indigo">
                  <Ticket size={18} />
                </div>
              </div>
              <div className="kpi-value-row">
                <span className="kpi-main-number">
                  {displayMetrics.sold.toLocaleString()}
                </span>
                <span className="kpi-ratio">
                  / {displayMetrics.capacity.toLocaleString()} cap
                </span>
              </div>
              <div className="kpi-progress-bar">
                <div 
                  className="kpi-progress-fill indigo-fill"
                  style={{ width: `${occupancyRate}%` }}
                ></div>
              </div>
              <span className="kpi-subtext">
                {occupancyRate}% total venue occupancy secured
              </span>
            </div>

            {/* Card 3: Check-in Velocity */}
            <div className="kpi-card checkin-card">
              <div className="kpi-card-header">
                <span className="kpi-label">Door Check-In Rate</span>
                <div className="kpi-icon-wrap icon-cyan">
                  <Scan size={18} />
                </div>
              </div>
              <div className="kpi-value-row">
                <span className="kpi-main-number">
                  {displayMetrics.checkInRate}%
                </span>
                <span className="kpi-ratio">
                  ({displayMetrics.checkIns} attended)
                </span>
              </div>
              <div className="kpi-progress-bar">
                <div 
                  className="kpi-progress-fill cyan-fill"
                  style={{ width: `${Math.min(100, displayMetrics.checkInRate)}%` }}
                ></div>
              </div>
              <span className="kpi-subtext">
                Real-time QR barcode entry verifications
              </span>
            </div>

            {/* Card 4: Event Reach */}
            <div className="kpi-card events-card">
              <div className="kpi-card-header">
                <span className="kpi-label">Events Managed</span>
                <div className="kpi-icon-wrap icon-amber">
                  <Calendar size={18} />
                </div>
              </div>
              <div className="kpi-value-row">
                <span className="kpi-main-number">
                  {displayMetrics.eventsCount}
                </span>
                {selectedEventData ? (
                  <span className={`status-pill status-${selectedEventData.status.toLowerCase()}`}>
                    {selectedEventData.status}
                  </span>
                ) : (
                  <span className="kpi-ratio">
                    ({organizerData?.publishedEvents || 0} live)
                  </span>
                )}
              </div>
              <span className="kpi-subtext">
                {selectedEventData ? selectedEventData.title : 'Comprehensive event portfolio'}
              </span>
            </div>
          </div>

          {/* Tier Breakdown & Attendance Progress Section */}
          <div className="analytics-details-grid">
            {/* Left: Tier Distribution */}
            <div className="analytics-panel-card">
              <div className="panel-header">
                <div className="panel-title-wrap">
                  <Layers size={18} className="panel-icon" />
                  <h3>Ticket Tier Velocity & Occupancy</h3>
                </div>
              </div>

              <div className="panel-body">
                {selectedEventData && selectedEventData.tierBreakdowns.length > 0 ? (
                  <div className="tiers-breakdown-list">
                    {selectedEventData.tierBreakdowns.map(tier => {
                      const pct = tier.capacity > 0 ? ((tier.soldCount / tier.capacity) * 100).toFixed(1) : 0;
                      return (
                        <div key={tier.tierId} className="tier-breakdown-item">
                          <div className="tier-item-top">
                            <div className="tier-item-info">
                              <span className="tier-item-name">{tier.name}</span>
                              <span className="tier-item-price">
                                {currencyRate.symbol || '$'}{(Number(tier.price) * (currencyRate.rateToUsd || 1.0)).toFixed(2)}
                              </span>
                            </div>
                            <div className="tier-item-revenue">
                              <strong>{currencyRate.symbol || '$'}{(Number(tier.revenue) * (currencyRate.rateToUsd || 1.0)).toFixed(2)}</strong>
                              <span className="tier-item-count">{tier.soldCount}/{tier.capacity} sold ({pct}%)</span>
                            </div>
                          </div>
                          <div className="tier-bar-bg">
                            <div 
                              className="tier-bar-fill" 
                              style={{ width: `${Math.min(100, pct)}%` }}
                            ></div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : organizerData?.events?.length > 0 ? (
                  <div className="events-revenue-list">
                    {organizerData.events.map(e => (
                      <div key={e.eventId} className="event-revenue-item">
                        <div className="item-details">
                          <strong className="item-title">{e.title}</strong>
                          <span className="item-sub">
                            {e.ticketsSold} passes sold • {e.checkInCount} checked in
                          </span>
                        </div>
                        <div className="item-right">
                          <span className="item-revenue">
                            {currencyRate.symbol || '$'}{(Number(e.totalRevenue) * (currencyRate.rateToUsd || 1.0)).toFixed(2)}
                          </span>
                          <button 
                            className="btn-item-export"
                            onClick={() => handleExportCsv(e.eventId, e.slug)}
                            title="Export CSV"
                          >
                            <FileSpreadsheet size={13} />
                            <span>CSV</span>
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="panel-empty">No active ticket sales recorded yet.</div>
                )}
              </div>
            </div>

            {/* Right: Real-Time Door Arrival Roster */}
            <div className="analytics-panel-card">
              <div className="panel-header">
                <div className="panel-title-wrap">
                  <Clock size={18} className="panel-icon" />
                  <h3>Recent Attendee Transactions</h3>
                </div>
              </div>

              <div className="panel-body">
                {displayMetrics.orders && displayMetrics.orders.length > 0 ? (
                  <div className="recent-orders-list">
                    {displayMetrics.orders.map(order => (
                      <div key={order.orderNumber} className="order-item-row">
                        <div className="order-item-left">
                          <code className="order-code">{order.orderNumber}</code>
                          <div className="order-attendee-info">
                            <strong>{order.attendeeName}</strong>
                            <span className="order-sub">{order.attendeeEmail}</span>
                          </div>
                        </div>
                        <div className="order-item-right">
                          <span className="order-amount">
                            {currencyRate.symbol || '$'}{(Number(order.totalAmount) * (currencyRate.rateToUsd || 1.0)).toFixed(2)}
                          </span>
                          <span className="order-tickets-badge">{order.ticketCount} pass{order.ticketCount === 1 ? '' : 'es'}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="panel-empty">No orders logged in this reporting window.</div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
