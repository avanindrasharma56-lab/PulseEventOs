import React, { useState, useEffect } from 'react';
import { 
  X, 
  Calendar, 
  MapPin, 
  Globe, 
  Users, 
  User, 
  Clock, 
  Sparkles, 
  AlertTriangle,
  ArrowRight,
  Ticket,
  Star,
  CheckCircle2,
  MessageSquare,
  Send,
  Loader2
} from 'lucide-react';
import { reviewService } from '../services/api';
import { useAuth } from '../context/AuthContext';
import './EventDetailModal.css';

export default function EventDetailModal({ event, onClose, onRegisterInterest, onOpenAuth }) {
  const { user, isAuthenticated } = useAuth();
  const [reviewSummary, setReviewSummary] = useState(null);
  const [isLoadingReviews, setIsLoadingReviews] = useState(true);
  
  // Review submission state
  const [ratingInput, setRatingInput] = useState(5);
  const [hoverRating, setHoverRating] = useState(0);
  const [reviewTitle, setReviewTitle] = useState('');
  const [reviewComment, setReviewComment] = useState('');
  const [isSubmittingReview, setIsSubmittingReview] = useState(false);
  const [reviewError, setReviewError] = useState('');
  const [reviewSuccess, setReviewSuccess] = useState('');

  const loadReviews = async () => {
    if (!event?.id) return;
    setIsLoadingReviews(true);
    try {
      const data = await reviewService.getEventReviews(event.id);
      setReviewSummary(data);
    } catch (err) {
      console.error('Failed to load reviews', err);
    } finally {
      setIsLoadingReviews(false);
    }
  };

  useEffect(() => {
    loadReviews();
  }, [event?.id]);

  const handleSubmitReview = async (e) => {
    e.preventDefault();
    if (!isAuthenticated) {
      if (onOpenAuth) onOpenAuth('signin');
      return;
    }
    if (!reviewTitle.trim() || !reviewComment.trim()) {
      setReviewError('Title and review description are required');
      return;
    }
    setIsSubmittingReview(true);
    setReviewError('');
    setReviewSuccess('');
    try {
      await reviewService.submitReview(event.id, {
        rating: ratingInput,
        title: reviewTitle.trim(),
        comment: reviewComment.trim(),
      });
      setReviewSuccess('Your review has been verified and posted!');
      setReviewTitle('');
      setReviewComment('');
      setRatingInput(5);
      await loadReviews();
    } catch (err) {
      setReviewError(err.message || 'Failed to submit review');
    } finally {
      setIsSubmittingReview(false);
    }
  };

  if (!event) return null;

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

  const isOnline = event.venue?.online;

  return (
    <div className="event-modal-overlay" onClick={onClose}>
      <div className="event-modal-content" onClick={(e) => e.stopPropagation()}>
        {/* Modal Header with Banner */}
        <div className="modal-banner-wrap">
          {event.bannerImageUrl ? (
            <img src={event.bannerImageUrl} alt={event.title} className="modal-banner-img" />
          ) : (
            <div className="modal-banner-placeholder">
              <span>PulseEvent OS</span>
            </div>
          )}
          <div className="modal-banner-gradient"></div>

          <button className="modal-close-btn" onClick={onClose} aria-label="Close modal">
            <X size={18} />
          </button>

          <div className="modal-banner-tags">
            {event.category && (
              <span className="modal-category-tag">{event.category.name}</span>
            )}
            {event.featured && (
              <span className="modal-featured-tag">
                <Sparkles size={12} />
                <span>Featured Event</span>
              </span>
            )}
          </div>
        </div>

        {/* Modal Body */}
        <div className="modal-scroll-body">
          <div className="modal-title-block">
            <div className="modal-status-badge">
              <span className={`status-pill status-${(event.status || 'DRAFT').toLowerCase()}`}>
                {event.status}
              </span>
            </div>
            <h2 className="modal-title">{event.title}</h2>
            {event.shortSummary && (
              <p className="modal-subtitle">{event.shortSummary}</p>
            )}
          </div>

          {/* Rejection Alert if Rejected */}
          {event.status === 'REJECTED' && event.rejectionReason && (
            <div className="rejection-alert">
              <AlertTriangle size={18} className="alert-icon" />
              <div>
                <strong>Rejection Feedback from Admin:</strong>
                <p>{event.rejectionReason}</p>
              </div>
            </div>
          )}

          {/* Key Schedule & Venue Grid */}
          <div className="modal-info-grid">
            <div className="info-card">
              <div className="info-icon-box">
                <Calendar size={18} />
              </div>
              <div>
                <span className="info-label">Start Time</span>
                <span className="info-value">{formatDate(event.startTime)}</span>
              </div>
            </div>

            <div className="info-card">
              <div className="info-icon-box">
                <Clock size={18} />
              </div>
              <div>
                <span className="info-label">End Time</span>
                <span className="info-value">{formatDate(event.endTime)}</span>
              </div>
            </div>

            <div className="info-card full-col">
              <div className="info-icon-box">
                {isOnline ? <Globe size={18} /> : <MapPin size={18} />}
              </div>
              <div>
                <span className="info-label">{isOnline ? 'Virtual Auditorium' : 'Venue Location'}</span>
                <span className="info-value">
                  {event.venue ? (
                    <>
                      <strong>{event.venue.name}</strong>
                      {event.venue.address && ` • ${event.venue.address}, ${event.venue.city || ''}`}
                      {event.venue.capacity && ` (Capacity: ${event.venue.capacity.toLocaleString()})`}
                      {isOnline && event.venue.streamUrl && (
                        <div className="stream-link">
                          Stream: <code>{event.venue.streamUrl}</code>
                        </div>
                      )}
                    </>
                  ) : (
                    'Location details announced soon'
                  )}
                </span>
              </div>
            </div>
          </div>

          {/* Description Section */}
          <div className="modal-section">
            <h4 className="section-title">About This Event</h4>
            <div className="event-description-text">
              {event.description ? (
                event.description.split('\n\n').map((para, i) => (
                  <p key={i}>{para}</p>
                ))
              ) : (
                <p>No detailed overview provided yet.</p>
              )}
            </div>
          </div>

          {/* Organizer Card */}
          <div className="modal-section">
            <h4 className="section-title">Hosted By</h4>
            <div className="organizer-profile-box">
              <div className="organizer-avatar-lg">
                <User size={20} />
              </div>
              <div className="organizer-meta">
                <span className="organizer-name-lg">{event.organizerName || 'Verified Organizer'}</span>
                {event.organizerEmail && (
                  <span className="organizer-email">{event.organizerEmail}</span>
                )}
                <span className="organizer-tag">Pulse Verified Host</span>
              </div>
            </div>
          </div>

          {/* Attendee Ratings & Reviews Section */}
          <div className="modal-section reviews-section">
            <div className="reviews-section-header">
              <div className="reviews-header-left">
                <h4 className="section-title">Attendee Ratings & Reviews</h4>
                <span className="reviews-header-sub">
                  Authentic verified attendee community feedback
                </span>
              </div>
              {reviewSummary && (
                <div className="rating-pill-badge">
                  <Star size={14} className="star-filled" fill="#F59E0B" />
                  <span className="rating-pill-score">
                    {reviewSummary.averageRating ? reviewSummary.averageRating.toFixed(1) : 'New'}
                  </span>
                  <span className="rating-pill-count">
                    ({reviewSummary.totalReviews} {reviewSummary.totalReviews === 1 ? 'review' : 'reviews'})
                  </span>
                </div>
              )}
            </div>

            {/* Overall Rating Overview Card */}
            {reviewSummary && reviewSummary.totalReviews > 0 && (
              <div className="rating-overview-card">
                <div className="overview-left-score">
                  <span className="big-rating-number">{reviewSummary.averageRating.toFixed(1)}</span>
                  <div className="stars-row">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <Star
                        key={star}
                        size={16}
                        className={star <= Math.round(reviewSummary.averageRating) ? 'star-filled' : 'star-empty'}
                        fill={star <= Math.round(reviewSummary.averageRating) ? '#F59E0B' : 'none'}
                      />
                    ))}
                  </div>
                  <span className="score-out-of">out of 5.0 stars</span>
                </div>

                <div className="overview-bars">
                  {[5, 4, 3, 2, 1].map((stars) => {
                    const count = reviewSummary.ratingCounts?.[stars] || 0;
                    const percent = reviewSummary.totalReviews > 0 ? (count / reviewSummary.totalReviews) * 100 : 0;
                    return (
                      <div key={stars} className="rating-bar-row">
                        <span className="star-label">{stars}★</span>
                        <div className="bar-track">
                          <div className="bar-fill" style={{ width: `${percent}%` }}></div>
                        </div>
                        <span className="bar-count">{count}</span>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Reviews List */}
            {isLoadingReviews ? (
              <div className="reviews-loading">
                <Loader2 size={16} className="spin-icon" />
                <span>Loading community reviews...</span>
              </div>
            ) : !reviewSummary || reviewSummary.reviews.length === 0 ? (
              <div className="reviews-empty-state">
                <MessageSquare size={24} className="empty-reviews-icon" />
                <p className="empty-reviews-title">No Reviews Yet</p>
                <p className="empty-reviews-desc">
                  Be the first to share your experience from this event with other attendees.
                </p>
              </div>
            ) : (
              <div className="reviews-list">
                {reviewSummary.reviews.map((rev) => (
                  <div key={rev.id} className="review-card">
                    <div className="review-card-header">
                      <div className="reviewer-info">
                        <div className="reviewer-avatar">
                          {(rev.authorName || rev.userName || 'A').charAt(0).toUpperCase()}
                        </div>
                        <div>
                          <div className="reviewer-name-row">
                            <span className="reviewer-name">{rev.authorName || rev.userName || 'Attendee'}</span>
                            {rev.verifiedAttendee && (
                              <span className="verified-attendee-tag" title="Ticket holder verified by PulseEvent Pass Engine">
                                <CheckCircle2 size={11} />
                                <span>Verified Attendee</span>
                              </span>
                            )}
                          </div>
                          <span className="review-date">{formatDate(rev.createdAt)}</span>
                        </div>
                      </div>

                      <div className="review-stars-pill">
                        {[1, 2, 3, 4, 5].map((s) => (
                          <Star
                            key={s}
                            size={12}
                            className={s <= rev.rating ? 'star-filled' : 'star-empty'}
                            fill={s <= rev.rating ? '#F59E0B' : 'none'}
                          />
                        ))}
                      </div>
                    </div>

                    <h5 className="review-title">{rev.title}</h5>
                    <p className="review-comment">{rev.comment}</p>
                  </div>
                ))}
              </div>
            )}

            {/* Leave a Review Section */}
            <div className="leave-review-card">
              <h5 className="leave-review-heading">
                <Star size={16} className="star-filled" fill="#F59E0B" />
                <span>Write an Attendee Review</span>
              </h5>

              {isAuthenticated ? (
                <form onSubmit={handleSubmitReview} className="review-form">
                  {reviewSuccess && (
                    <div className="review-success-banner">
                      <CheckCircle2 size={15} />
                      <span>{reviewSuccess}</span>
                    </div>
                  )}

                  {reviewError && (
                    <div className="review-error-banner">
                      <AlertTriangle size={15} />
                      <span>{reviewError}</span>
                    </div>
                  )}

                  <div className="form-group-star-rating">
                    <label>Your Rating *</label>
                    <div className="interactive-stars-picker">
                      {[1, 2, 3, 4, 5].map((star) => (
                        <button
                          key={star}
                          type="button"
                          className="star-pick-btn"
                          onMouseEnter={() => setHoverRating(star)}
                          onMouseLeave={() => setHoverRating(0)}
                          onClick={() => setRatingInput(star)}
                        >
                          <Star
                            size={22}
                            className={(hoverRating || ratingInput) >= star ? 'star-filled' : 'star-empty'}
                            fill={(hoverRating || ratingInput) >= star ? '#F59E0B' : 'none'}
                          />
                        </button>
                      ))}
                      <span className="rating-hint-text">
                        {hoverRating || ratingInput} of 5 stars
                      </span>
                    </div>
                  </div>

                  <div className="form-group">
                    <label>Review Headline *</label>
                    <input
                      type="text"
                      className="review-input"
                      placeholder="e.g. Inspiring keynotes and seamless venue experience"
                      value={reviewTitle}
                      onChange={(e) => setReviewTitle(e.target.value)}
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label>Detailed Feedback *</label>
                    <textarea
                      rows={3}
                      className="review-textarea"
                      placeholder="Share your thoughts on the speakers, production quality, venue, or takeaways..."
                      value={reviewComment}
                      onChange={(e) => setReviewComment(e.target.value)}
                      required
                    />
                  </div>

                  <div className="review-form-footer">
                    <span className="verified-hint">
                      <CheckCircle2 size={13} />
                      <span>Passholders automatically receive a Verified Attendee badge.</span>
                    </span>

                    <button
                      type="submit"
                      className="btn-submit-review"
                      disabled={isSubmittingReview || !reviewTitle.trim() || !reviewComment.trim()}
                    >
                      {isSubmittingReview ? (
                        <>
                          <Loader2 size={14} className="spin-icon" />
                          <span>Posting...</span>
                        </>
                      ) : (
                        <>
                          <Send size={14} />
                          <span>Post Review</span>
                        </>
                      )}
                    </button>
                  </div>
                </form>
              ) : (
                <div className="review-auth-prompt">
                  <p>Sign in with your attendee account to rate this event and share your feedback.</p>
                  <button
                    type="button"
                    className="btn-review-signin"
                    onClick={() => onOpenAuth && onOpenAuth('signin')}
                  >
                    <span>Sign In to Leave a Review</span>
                    <ArrowRight size={14} />
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Modal Footer */}
        <div className="modal-footer">
          <button className="btn-modal-secondary" onClick={onClose}>
            Close
          </button>

          {event.status === 'PUBLISHED' && (
            <button
              className="btn-modal-primary"
              onClick={() => {
                if (onRegisterInterest) onRegisterInterest(event);
              }}
            >
              <Ticket size={16} />
              <span>Get Tickets & Select Tier</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
