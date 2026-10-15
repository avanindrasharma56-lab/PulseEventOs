import React, { useState, useEffect } from 'react';
import { 
  ShieldCheck, 
  CheckCircle, 
  XCircle, 
  AlertTriangle, 
  Calendar, 
  MapPin, 
  User, 
  RefreshCw,
  Eye,
  X
} from 'lucide-react';
import { adminEventService } from '../services/api';
import { useAuth } from '../context/AuthContext';
import EventDetailModal from './EventDetailModal';
import './AdminModerationDesk.css';

export default function AdminModerationDesk({ onModerationAction }) {
  const { user } = useAuth();
  const [pendingEvents, setPendingEvents] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [previewEvent, setPreviewEvent] = useState(null);

  // Rejection Dialog State
  const [rejectingEvent, setRejectingEvent] = useState(null);
  const [rejectReason, setRejectReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const loadPending = async () => {
    setIsLoading(true);
    setErrorMsg('');
    try {
      const pageData = await adminEventService.getPendingEvents(0, 50);
      setPendingEvents(pageData?.content || []);
    } catch (err) {
      console.error('Failed to load pending events', err);
      setErrorMsg(err.message || 'Failed to fetch moderation queue');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadPending();
  }, []);

  const handleApprove = async (event) => {
    if (event.organizerId === user?.id) {
      alert('Self-approval prohibited: An administrator cannot approve an event they personally organized.');
      return;
    }

    if (!window.confirm(`Are you sure you want to approve "${event.title}"?`)) return;

    try {
      await adminEventService.approveEvent(event.id);
      await loadPending();
      if (onModerationAction) onModerationAction();
    } catch (err) {
      alert(err.message || 'Failed to approve event');
    }
  };

  const openRejectDialog = (event) => {
    setRejectingEvent(event);
    setRejectReason('');
    setErrorMsg('');
  };

  const handleConfirmReject = async (e) => {
    e.preventDefault();
    if (!rejectReason.trim() || rejectReason.trim().length < 5) {
      setErrorMsg('A detailed rejection reason (minimum 5 characters) is required');
      return;
    }

    setIsSubmitting(true);
    try {
      await adminEventService.rejectEvent(rejectingEvent.id, rejectReason.trim());
      setRejectingEvent(null);
      await loadPending();
      if (onModerationAction) onModerationAction();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to reject event');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="moderation-desk">
      {/* Header */}
      <div className="desk-header">
        <div className="desk-title-group">
          <div className="desk-icon">
            <ShieldCheck size={22} />
          </div>
          <div>
            <h2>Admin Moderation Queue</h2>
            <p>Review submitted events, verify schedule accuracy, and enforce platform quality guidelines.</p>
          </div>
        </div>

        <button className="btn-refresh" onClick={loadPending} disabled={isLoading}>
          <RefreshCw size={15} className={isLoading ? 'spinning' : ''} />
          <span>Refresh Queue</span>
        </button>
      </div>

      {errorMsg && <div className="desk-error-banner">{errorMsg}</div>}

      {/* Queue Count Summary */}
      <div className="queue-status-banner">
        <span className="queue-count-badge">
          {pendingEvents.length} Event{pendingEvents.length === 1 ? '' : 's'} Pending Review
        </span>
        <span className="queue-policy-note">
          🛡️ Self-approval rule enforced: Organizers cannot approve their own events.
        </span>
      </div>

      {/* Queue Items */}
      <div className="queue-container">
        {isLoading ? (
          <div className="queue-loading">Loading pending moderation submissions...</div>
        ) : pendingEvents.length === 0 ? (
          <div className="queue-empty">
            <CheckCircle size={40} className="check-done-icon" />
            <h3>Moderation Queue is Clean</h3>
            <p>All submitted events have been reviewed. New organizer submissions will appear here automatically.</p>
          </div>
        ) : (
          <div className="queue-list">
            {pendingEvents.map((evt) => {
              const isSelfOrganized = evt.organizerId === user?.id;

              return (
                <div key={evt.id} className="queue-card">
                  <div className="queue-card-main">
                    <div className="queue-badge-row">
                      <span className="category-tag">{evt.category?.name || 'Event'}</span>
                      <span className="pending-tag">Pending Review</span>
                      {isSelfOrganized && (
                        <span className="self-org-warning">
                          <AlertTriangle size={12} />
                          <span>Your Own Event (Approval Blocked)</span>
                        </span>
                      )}
                    </div>

                    <h3 className="queue-event-title">{evt.title}</h3>
                    <p className="queue-summary">{evt.shortSummary}</p>

                    <div className="queue-meta-grid">
                      <div className="queue-meta-item">
                        <User size={13} />
                        <span>Organizer: <strong>{evt.organizerName}</strong></span>
                      </div>
                      <div className="queue-meta-item">
                        <Calendar size={13} />
                        <span>Date: {new Date(evt.startTime).toLocaleDateString()}</span>
                      </div>
                      <div className="queue-meta-item">
                        <MapPin size={13} />
                        <span>Venue: {evt.venue?.name || 'Online'}</span>
                      </div>
                    </div>
                  </div>

                  {/* Moderation Actions */}
                  <div className="queue-actions">
                    <button
                      className="btn-mod view"
                      onClick={() => setPreviewEvent(evt)}
                      title="Inspect full details"
                    >
                      <Eye size={15} />
                      <span>Inspect</span>
                    </button>

                    <button
                      className="btn-mod reject"
                      onClick={() => openRejectDialog(evt)}
                    >
                      <XCircle size={15} />
                      <span>Reject</span>
                    </button>

                    <button
                      className="btn-mod approve"
                      onClick={() => handleApprove(evt)}
                      disabled={isSelfOrganized}
                      title={isSelfOrganized ? 'Self-approval is forbidden by system policy' : 'Approve event'}
                    >
                      <CheckCircle size={15} />
                      <span>Approve</span>
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Inspect Event Modal */}
      {previewEvent && (
        <EventDetailModal
          event={previewEvent}
          onClose={() => setPreviewEvent(null)}
        />
      )}

      {/* Reject Modal Dialog */}
      {rejectingEvent && (
        <div className="modal-backdrop" onClick={() => setRejectingEvent(null)}>
          <div className="reject-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="reject-header">
              <div className="reject-title-wrap">
                <AlertTriangle size={18} className="warn-icon" />
                <h3>Reject Event Submission</h3>
              </div>
              <button className="btn-close" onClick={() => setRejectingEvent(null)}>
                <X size={16} />
              </button>
            </div>

            <p className="reject-explanation">
              Please provide constructive feedback for <strong>{rejectingEvent.title}</strong>. 
              The organizer will see this reason in their studio and can update their draft to re-submit.
            </p>

            <form onSubmit={handleConfirmReject} className="reject-form">
              {errorMsg && <div className="form-error-box">{errorMsg}</div>}

              <div className="form-group">
                <label>Rejection Reason *</label>
                <textarea
                  rows={4}
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  placeholder="e.g. Please update the banner image to higher resolution and specify the keynote agenda..."
                  required
                ></textarea>
              </div>

              <div className="reject-actions">
                <button
                  type="button"
                  className="btn-cancel"
                  onClick={() => setRejectingEvent(null)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-confirm-reject"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? 'Rejecting...' : 'Confirm Rejection'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
