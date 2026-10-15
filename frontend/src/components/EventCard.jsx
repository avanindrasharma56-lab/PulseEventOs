import React from 'react';
import { Calendar, MapPin, Globe, Sparkles, User, ArrowUpRight } from 'lucide-react';
import './EventCard.css';

export default function EventCard({ event, onClick, onAction, actionLabel, actionVariant = 'primary' }) {
  if (!event) return null;

  const formatDate = (dateStr) => {
    if (!dateStr) return 'TBA';
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'PUBLISHED':
        return { label: 'Live Event', className: 'status-live' };
      case 'APPROVED':
        return { label: 'Approved', className: 'status-approved' };
      case 'PENDING_APPROVAL':
        return { label: 'Pending Review', className: 'status-pending' };
      case 'REJECTED':
        return { label: 'Action Required', className: 'status-rejected' };
      case 'DRAFT':
        return { label: 'Draft', className: 'status-draft' };
      case 'CANCELLED':
        return { label: 'Cancelled', className: 'status-cancelled' };
      default:
        return { label: status, className: 'status-draft' };
    }
  };

  const statusBadge = getStatusBadge(event.status);
  const isOnline = event.venue?.online;

  return (
    <div className={`event-card ${event.featured ? 'is-featured' : ''}`} onClick={() => onClick && onClick(event)}>
      {/* Banner / Poster Header */}
      <div className="card-media">
        {event.bannerImageUrl ? (
          <img src={event.bannerImageUrl} alt={event.title} className="card-img" loading="lazy" />
        ) : (
          <div className="card-placeholder-banner">
            <span className="placeholder-brand">PulseEvent</span>
          </div>
        )}
        <div className="card-media-overlay"></div>

        {/* Top Badges */}
        <div className="media-badges-top">
          <span className={`status-pill ${statusBadge.className}`}>
            <span className="pill-dot"></span>
            {statusBadge.label}
          </span>
          {event.featured && (
            <span className="featured-pill">
              <Sparkles size={11} />
              <span>Featured</span>
            </span>
          )}
        </div>

        {/* Category Pill */}
        {event.category && (
          <div className="media-category-tag">
            <span>{event.category.name}</span>
          </div>
        )}
      </div>

      {/* Card Content */}
      <div className="card-body">
        <h3 className="card-title" title={event.title}>
          {event.title}
        </h3>

        <p className="card-summary">
          {event.shortSummary || 'Discover keynote speakers, community interactions, and industry breakthroughs at this premier gathering.'}
        </p>

        {/* Metadata List */}
        <div className="card-meta">
          <div className="meta-item">
            <Calendar size={14} className="meta-icon" />
            <span>{formatDate(event.startTime)}</span>
          </div>

          <div className="meta-item">
            {isOnline ? (
              <>
                <Globe size={14} className="meta-icon meta-online" />
                <span>Virtual Event & Stream</span>
              </>
            ) : (
              <>
                <MapPin size={14} className="meta-icon" />
                <span>{event.venue ? `${event.venue.name}, ${event.venue.city || ''}` : 'Location TBA'}</span>
              </>
            )}
          </div>
        </div>

        {/* Card Footer */}
        <div className="card-footer">
          <div className="organizer-badge">
            <div className="organizer-avatar">
              <User size={12} />
            </div>
            <span className="organizer-name">{event.organizerName || 'Pulse Host'}</span>
          </div>

          {actionLabel ? (
            <button
              className={`card-action-btn ${actionVariant}`}
              onClick={(e) => {
                e.stopPropagation();
                if (onAction) onAction(event);
              }}
            >
              <span>{actionLabel}</span>
              <ArrowUpRight size={14} />
            </button>
          ) : (
            <span className="view-detail-hint">
              <span>View Details</span>
              <ArrowUpRight size={14} />
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
