import React, { useState, useEffect, useRef } from 'react';
import {
  X,
  Armchair,
  Clock,
  CheckCircle,
  AlertTriangle,
  RefreshCw,
  Sparkles,
  ShieldCheck,
  ChevronRight,
  Layers,
  Info
} from 'lucide-react';
import { seatMapService } from '../services/api';
import './VisualSeatSelectorModal.css';

export default function VisualSeatSelectorModal({ event, onClose, onSelectSeat, onOpenAuth, isAuthenticated }) {
  const [seatMap, setSeatMap] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedSeat, setSelectedSeat] = useState(null);
  const [heldSeat, setHeldSeat] = useState(null);
  const [holdTimeRemaining, setHoldTimeRemaining] = useState(0);
  const [actionLoading, setActionLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [activeSectionId, setActiveSectionId] = useState(null);
  const timerRef = useRef(null);

  const fetchMap = async () => {
    try {
      setLoading(true);
      setErrorMsg('');
      const data = await seatMapService.getSeatMap(event.id);
      setSeatMap(data);
      if (data.sections && data.sections.length > 0) {
        setActiveSectionId((prev) => prev || data.sections[0].id);
        
        // Check if user already holds a seat
        for (const sec of data.sections) {
          const myHold = sec.seats?.find(s => s.isMyHold && s.status === 'HOLD');
          if (myHold) {
            setHeldSeat({ ...myHold, sectionName: sec.name, price: sec.price });
            setSelectedSeat({ ...myHold, sectionName: sec.name, price: sec.price });
            setHoldTimeRemaining(300); // 5 mins
            break;
          }
        }
      }
    } catch (err) {
      setErrorMsg(err.message || 'Failed to load venue seat map');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (event?.id) {
      fetchMap();
    }
  }, [event?.id]);

  // Countdown timer for 5-minute hold
  useEffect(() => {
    if (holdTimeRemaining > 0) {
      timerRef.current = setInterval(() => {
        setHoldTimeRemaining((prev) => {
          if (prev <= 1) {
            clearInterval(timerRef.current);
            setHeldSeat(null);
            fetchMap();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } else {
      if (timerRef.current) clearInterval(timerRef.current);
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [holdTimeRemaining]);

  const handleSeatClick = (seat, section) => {
    if (seat.status === 'RESERVED') return;
    if (seat.status === 'HOLD' && !seat.isMyHold) return;

    setSelectedSeat({
      ...seat,
      sectionName: section.name,
      price: section.price,
      tierName: section.tierName,
      tierId: section.tierId
    });
    setErrorMsg('');
  };

  const handleHoldSeat = async () => {
    if (!isAuthenticated) {
      onOpenAuth?.();
      return;
    }
    if (!selectedSeat) return;

    try {
      setActionLoading(true);
      setErrorMsg('');
      const res = await seatMapService.holdSeat(event.id, selectedSeat.id);
      setHeldSeat({ ...selectedSeat, holdId: res.reservationId });
      setHoldTimeRemaining(res.remainingSeconds || 300);
      fetchMap();
    } catch (err) {
      setErrorMsg(err.message || 'Could not acquire hold lock. Seat may be taken.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleReleaseHold = async () => {
    if (!heldSeat) return;

    try {
      setActionLoading(true);
      setErrorMsg('');
      await seatMapService.releaseHold(event.id, heldSeat.id);
      setHeldSeat(null);
      setHoldTimeRemaining(0);
      setSelectedSeat(null);
      fetchMap();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to release hold lock');
    } finally {
      setActionLoading(false);
    }
  };

  const handleConfirmSeat = () => {
    if (heldSeat) {
      onSelectSeat?.(heldSeat);
      onClose();
    } else if (selectedSeat) {
      handleHoldSeat().then(() => {
        if (selectedSeat) {
          onSelectSeat?.(selectedSeat);
          onClose();
        }
      });
    }
  };

  const formatTimer = (seconds) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const activeSection = seatMap?.sections?.find(s => s.id === activeSectionId) || seatMap?.sections?.[0];

  return (
    <div className="seatmap-modal-backdrop" onClick={onClose}>
      <div className="seatmap-modal-container" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="seatmap-modal-header">
          <div className="seatmap-title-row">
            <div className="seatmap-icon-box">
              <Armchair className="seatmap-header-icon" />
            </div>
            <div>
              <h2 className="seatmap-heading">Venue Reserved Seating</h2>
              <p className="seatmap-subheading">
                {seatMap?.venueName || event?.venue?.name || 'Main Auditorium'} • Live 5-Minute Guaranteed Hold
              </p>
            </div>
          </div>
          <button className="seatmap-close-btn" onClick={onClose} aria-label="Close">
            <X size={20} />
          </button>
        </div>

        {/* Hold Banner if active */}
        {heldSeat && (
          <div className="seatmap-hold-banner">
            <div className="hold-banner-left">
              <Clock className="hold-clock-icon animate-pulse" size={18} />
              <div>
                <span className="hold-banner-title">Seat {heldSeat.seatCode} Locked for You</span>
                <span className="hold-banner-timer">Expires in {formatTimer(holdTimeRemaining)}</span>
              </div>
            </div>
            <button
              className="hold-release-btn"
              onClick={handleReleaseHold}
              disabled={actionLoading}
            >
              Release Seat
            </button>
          </div>
        )}

        {/* Error message */}
        {errorMsg && (
          <div className="seatmap-error-bar">
            <AlertTriangle size={16} />
            <span>{errorMsg}</span>
          </div>
        )}

        {loading ? (
          <div className="seatmap-loading-box">
            <RefreshCw className="animate-spin" size={32} />
            <p>Rendering interactive auditorium geometry...</p>
          </div>
        ) : (
          <div className="seatmap-body">
            {/* Section Tabs */}
            {seatMap?.sections && seatMap.sections.length > 1 && (
              <div className="seatmap-section-tabs">
                {seatMap.sections.map((sec) => (
                  <button
                    key={sec.id}
                    className={`seatmap-section-tab ${activeSectionId === sec.id ? 'active' : ''}`}
                    onClick={() => setActiveSectionId(sec.id)}
                  >
                    <Layers size={14} />
                    <span>{sec.name}</span>
                    <span className="section-badge">${sec.price ? sec.price.toFixed(2) : '0.00'}</span>
                  </button>
                ))}
              </div>
            )}

            {/* Stage Visual */}
            <div className="seatmap-stage-wrapper">
              <div className="seatmap-stage">
                <span className="stage-label">MAIN STAGE / PODIUM</span>
                <div className="stage-glow"></div>
              </div>
            </div>

            {/* Seats Grid */}
            {activeSection ? (
              <div className="seatmap-grid-container">
                <div className="section-info-header">
                  <span className="section-title">{activeSection.name}</span>
                  <span className="section-price-tag">
                    {activeSection.tierName} • ${activeSection.price ? activeSection.price.toFixed(2) : 'Free'}
                  </span>
                </div>

                <div className="seatmap-grid">
                  {/* Group seats by row */}
                  {Object.entries(
                    (activeSection.seats || []).reduce((acc, seat) => {
                      acc[seat.rowLabel] = acc[seat.rowLabel] || [];
                      acc[seat.rowLabel].push(seat);
                      return acc;
                    }, {})
                  ).map(([rowLabel, rowSeats]) => (
                    <div key={rowLabel} className="seatmap-row">
                      <span className="row-label-tag">{rowLabel}</span>
                      <div className="row-seats">
                        {rowSeats.map((s) => {
                          const isSelected = selectedSeat?.id === s.id;
                          const isHeldByMe = s.isMyHold && s.status === 'HOLD';
                          const isHeldOther = !s.isMyHold && s.status === 'HOLD';
                          const isReserved = s.status === 'RESERVED';
                          const isAvail = s.status === 'AVAILABLE' || !s.status;

                          let seatStateClass = 'available';
                          if (isSelected) seatStateClass = 'selected';
                          else if (isHeldByMe) seatStateClass = 'held-by-me';
                          else if (isHeldOther) seatStateClass = 'held-other';
                          else if (isReserved) seatStateClass = 'reserved';

                          return (
                            <button
                              key={s.id}
                              className={`seat-node ${seatStateClass}`}
                              onClick={() => handleSeatClick(s, activeSection)}
                              disabled={isReserved || isHeldOther}
                              title={`Seat ${s.seatCode} - ${isReserved ? 'Booked' : isHeldOther ? 'Held by another' : isHeldByMe ? 'Held by you' : 'Available'}`}
                            >
                              <span className="seat-num">{s.seatNumber}</span>
                            </button>
                          );
                        })}
                      </div>
                      <span className="row-label-tag">{rowLabel}</span>
                    </div>
                  ))}
                </div>
              </div>
            ) : null}

            {/* Legend */}
            <div className="seatmap-legend">
              <div className="legend-item">
                <span className="legend-swatch available"></span>
                <span>Available</span>
              </div>
              <div className="legend-item">
                <span className="legend-swatch selected"></span>
                <span>Selected</span>
              </div>
              <div className="legend-item">
                <span className="legend-swatch held-by-me"></span>
                <span>Your Hold (5m)</span>
              </div>
              <div className="legend-item">
                <span className="legend-swatch held-other"></span>
                <span>Held by Other</span>
              </div>
              <div className="legend-item">
                <span className="legend-swatch reserved"></span>
                <span>Occupied</span>
              </div>
            </div>
          </div>
        )}

        {/* Footer / Action Dock */}
        <div className="seatmap-footer">
          <div className="seatmap-selection-summary">
            {selectedSeat ? (
              <div>
                <div className="summary-seat-name">
                  Selected Seat: <strong>{selectedSeat.seatCode}</strong> ({selectedSeat.sectionName})
                </div>
                <div className="summary-seat-tier">
                  Tier: {selectedSeat.tierName || 'Standard'} • Price: <strong>${selectedSeat.price ? selectedSeat.price.toFixed(2) : '0.00'}</strong>
                </div>
              </div>
            ) : (
              <div className="summary-empty-state">
                <Info size={16} />
                <span>Select an open seat to place a 5-minute guaranteed hold lock.</span>
              </div>
            )}
          </div>

          <div className="seatmap-actions">
            {selectedSeat && !heldSeat && (
              <button
                className="seatmap-btn-hold"
                onClick={handleHoldSeat}
                disabled={actionLoading}
              >
                <Clock size={16} />
                <span>Lock Seat (5m)</span>
              </button>
            )}

            <button
              className="seatmap-btn-confirm"
              onClick={handleConfirmSeat}
              disabled={!selectedSeat && !heldSeat}
            >
              <CheckCircle size={16} />
              <span>{heldSeat ? 'Proceed with Locked Seat' : 'Lock & Book Seat'}</span>
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
