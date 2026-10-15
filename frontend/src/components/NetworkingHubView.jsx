import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { eventService, networkingService } from '../services/api';
import {
  Users,
  Calendar,
  Clock,
  MapPin,
  Check,
  X,
  Search,
  UserCheck,
  Briefcase,
  AlertCircle,
  MessageSquare,
  Sparkles,
  ChevronRight,
  ShieldAlert,
  Send
} from 'lucide-react';
import './NetworkingHubView.css';

export default function NetworkingHubView({ eventId = 4, onOpenAuth }) {
  const { user, isAuthenticated } = useAuth();

  const [eventsList, setEventsList] = useState([]);
  const [selectedEventId, setSelectedEventId] = useState(eventId);
  const [activeSubTab, setActiveSubTab] = useState('DIRECTORY'); // 'DIRECTORY' | 'MY_SCHEDULE' | 'MY_PROFILE'
  const [loading, setLoading] = useState(false);

  // Directory State
  const [attendees, setAttendees] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedInterest, setSelectedInterest] = useState('');

  // My Meetings State
  const [myMeetings, setMyMeetings] = useState([]);

  // My Profile State
  const [myProfile, setMyProfile] = useState(null);
  const [headline, setHeadline] = useState('');
  const [company, setCompany] = useState('');
  const [jobTitle, setJobTitle] = useState('');
  const [interests, setInterests] = useState('');
  const [goals, setGoals] = useState('');
  const [isAvailable, setIsAvailable] = useState(true);
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileSuccessMsg, setProfileSuccessMsg] = useState('');

  // Meeting Request Modal
  const [scheduleTargetUser, setScheduleTargetUser] = useState(null);
  const [meetDate, setMeetDate] = useState('');
  const [meetTime, setMeetTime] = useState('11:00');
  const [meetDuration, setMeetDuration] = useState('30');
  const [meetMessage, setMeetMessage] = useState('');
  const [meetLocation, setMeetLocation] = useState('Networking Lounge');
  const [scheduleSubmitting, setScheduleSubmitting] = useState(false);
  const [scheduleError, setScheduleError] = useState('');

  // 1. Fetch available events for selector
  useEffect(() => {
    async function loadEvents() {
      try {
        const res = await eventService.getEvents({ size: 50 });
        const list = res.content || [];
        setEventsList(list);
        if (list.length > 0 && !selectedEventId) {
          setSelectedEventId(list[0].id);
        }
      } catch (err) {
        console.error('Failed to load events for networking hub', err);
      }
    }
    loadEvents();
  }, []);

  // 2. Fetch Directory, Meetings, Profile
  const loadNetworkingData = async () => {
    if (!selectedEventId) return;
    setLoading(true);
    try {
      // Load directory attendees
      const atts = await networkingService.searchAttendees(selectedEventId, {
        query: searchQuery,
        interest: selectedInterest,
      });
      setAttendees(atts || []);

      if (isAuthenticated) {
        // Load personal profile
        const prof = await networkingService.getMyProfile(selectedEventId);
        if (prof) {
          setMyProfile(prof);
          setHeadline(prof.headline || '');
          setCompany(prof.company || '');
          setJobTitle(prof.jobTitle || '');
          setInterests(prof.interests || '');
          setGoals(prof.networkingGoals || '');
          setIsAvailable(prof.isAvailable !== false);
        }

        // Load personal meetings
        const meets = await networkingService.getMyMeetings(selectedEventId);
        setMyMeetings(meets || []);
      }
    } catch (err) {
      console.error('Failed to load networking data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNetworkingData();
  }, [selectedEventId, isAuthenticated, selectedInterest]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    loadNetworkingData();
  };

  // Handle Save Profile
  const handleSaveProfile = async (e) => {
    e.preventDefault();
    if (!isAuthenticated) {
      if (onOpenAuth) onOpenAuth('signin');
      return;
    }
    setProfileSaving(true);
    setProfileSuccessMsg('');
    try {
      const updated = await networkingService.upsertProfile(selectedEventId, {
        headline,
        company,
        jobTitle,
        interests,
        networkingGoals: goals,
        isAvailable,
      });
      setMyProfile(updated);
      setProfileSuccessMsg('Networking profile published to attendee matchmaking directory!');
      await loadNetworkingData();
      setTimeout(() => setProfileSuccessMsg(''), 4000);
    } catch (err) {
      alert(err.message || 'Failed to save networking profile');
    } finally {
      setProfileSaving(false);
    }
  };

  // Open Schedule Modal
  const openScheduleModal = (attendee) => {
    if (!isAuthenticated) {
      if (onOpenAuth) onOpenAuth('signin');
      else alert('Please sign in to schedule 1-on-1 meetings.');
      return;
    }
    setScheduleTargetUser(attendee);
    setScheduleError('');
    // Default meeting time to tomorrow at 11:00
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    setMeetDate(tomorrow.toISOString().split('T')[0]);
    setMeetTime('11:00');
    setMeetDuration('30');
    setMeetMessage('');
    setMeetLocation('Networking Lounge');
  };

  // Submit Meeting Request
  const handleScheduleSubmit = async (e) => {
    e.preventDefault();
    setScheduleSubmitting(true);
    setScheduleError('');

    try {
      const startIso = `${meetDate}T${meetTime}:00`;
      const startDate = new Date(startIso);
      const endDate = new Date(startDate.getTime() + parseInt(meetDuration, 10) * 60000);
      const endIso = endDate.toISOString().split('.')[0];

      await networkingService.requestMeeting(selectedEventId, {
        recipientId: scheduleTargetUser.userId,
        startTime: startIso,
        endTime: endIso,
        locationName: meetLocation,
        message: meetMessage,
      });

      setScheduleTargetUser(null);
      setActiveSubTab('MY_SCHEDULE');
      await loadNetworkingData();
    } catch (err) {
      setScheduleError(err.message || 'Failed to request meeting.');
    } finally {
      setScheduleSubmitting(false);
    }
  };

  // Accept / Decline Meeting
  const handleRespond = async (meetingId, accept) => {
    try {
      await networkingService.respondMeeting(meetingId, accept);
      await loadNetworkingData();
    } catch (err) {
      alert(err.message || 'Failed to respond to meeting');
    }
  };

  // Cancel Meeting
  const handleCancelMeeting = async (meetingId) => {
    if (!window.confirm('Are you sure you want to cancel this scheduled meeting?')) return;
    try {
      await networkingService.cancelMeeting(meetingId);
      await loadNetworkingData();
    } catch (err) {
      alert(err.message || 'Failed to cancel meeting');
    }
  };

  // Filter incoming vs outgoing meetings
  const incomingRequests = myMeetings.filter(
    (m) => m.status === 'REQUESTED' && user && m.recipientId === user.id
  );
  const outgoingRequests = myMeetings.filter(
    (m) => m.status === 'REQUESTED' && user && m.requesterId === user.id
  );
  const confirmedMeetings = myMeetings.filter((m) => m.status === 'ACCEPTED');
  const pastOrCancelledMeetings = myMeetings.filter(
    (m) => m.status === 'CANCELLED' || m.status === 'DECLINED'
  );

  const interestTags = [
    'ALL',
    'AI',
    'Cloud Native',
    'Kubernetes',
    'Distributed Systems',
    'Security',
    'FinTech',
    'High-Frequency Trading',
    'Data Engineering'
  ];

  return (
    <div className="networking-hub-container">
      {/* Header & Event Selector */}
      <div className="networking-header">
        <div className="networking-header-titles">
          <div className="networking-header-icon">
            <Users size={22} />
          </div>
          <div>
            <h2>1-on-1 B2B Networking & Matchmaking Hub</h2>
            <p>Connect with enterprise peers, schedule dedicated 1-on-1 meetings, and reserve meeting tables.</p>
          </div>
        </div>

        <div className="networking-event-select-wrap">
          <label style={{ fontSize: '0.85rem', color: '#94a3b8' }}>Event:</label>
          <select
            className="networking-event-select"
            value={selectedEventId}
            onChange={(e) => setSelectedEventId(Number(e.target.value))}
          >
            {eventsList.map((ev) => (
              <option key={ev.id} value={ev.id}>
                {ev.title}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Navigation Subtabs */}
      <div className="networking-tabs-bar">
        <button
          className={`networking-tab-btn ${activeSubTab === 'DIRECTORY' ? 'active' : ''}`}
          onClick={() => setActiveSubTab('DIRECTORY')}
        >
          <Search size={16} />
          <span>Attendee Matchmaking Directory</span>
          <span className="tab-badge-count">{attendees.length}</span>
        </button>

        <button
          className={`networking-tab-btn ${activeSubTab === 'MY_SCHEDULE' ? 'active' : ''}`}
          onClick={() => {
            if (!isAuthenticated) onOpenAuth('signin');
            else setActiveSubTab('MY_SCHEDULE');
          }}
        >
          <Calendar size={16} />
          <span>My 1-on-1 Schedule</span>
          {incomingRequests.length > 0 && (
            <span className="tab-badge-count" style={{ background: '#f59e0b' }}>
              {incomingRequests.length} Req
            </span>
          )}
          {confirmedMeetings.length > 0 && (
            <span className="tab-badge-count" style={{ background: '#10b981' }}>
              {confirmedMeetings.length}
            </span>
          )}
        </button>

        <button
          className={`networking-tab-btn ${activeSubTab === 'MY_PROFILE' ? 'active' : ''}`}
          onClick={() => {
            if (!isAuthenticated) onOpenAuth('signin');
            else setActiveSubTab('MY_PROFILE');
          }}
        >
          <UserCheck size={16} />
          <span>My Networking Profile</span>
        </button>
      </div>

      {/* TAB 1: Attendee Directory */}
      {activeSubTab === 'DIRECTORY' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {/* Search & Tag Filter Bar */}
          <div className="networking-search-bar">
            <form onSubmit={handleSearchSubmit} className="search-input-row">
              <div className="search-input-wrap">
                <Search size={16} color="#94a3b8" />
                <input
                  type="text"
                  placeholder="Search attendees by name, company, role, or technology..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>
              <button type="submit" className="btn-schedule-meeting" style={{ padding: '8px 18px' }}>
                Search
              </button>
            </form>

            <div className="filter-tags-row">
              {interestTags.map((tag) => (
                <button
                  key={tag}
                  className={`filter-tag-pill ${
                    (tag === 'ALL' && !selectedInterest) || selectedInterest === tag ? 'active' : ''
                  }`}
                  onClick={() => setSelectedInterest(tag === 'ALL' ? '' : tag)}
                >
                  {tag}
                </button>
              ))}
            </div>
          </div>

          {/* Attendees Grid */}
          {loading ? (
            <div style={{ textAlign: 'center', padding: '40px', color: '#94a3b8' }}>
              Loading matchmaking directory...
            </div>
          ) : attendees.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '60px 20px', background: '#0e1526', borderRadius: '12px', border: '1px solid #1e293b' }}>
              <Users size={40} color="#475569" style={{ marginBottom: '12px' }} />
              <h3 style={{ color: '#f1f5f9', margin: '0 0 6px' }}>No Attendees Matching Filters</h3>
              <p style={{ color: '#94a3b8', fontSize: '0.9rem' }}>Be the first to publish your networking profile for this conference!</p>
            </div>
          ) : (
            <div className="attendees-grid">
              {attendees.map((att) => {
                const isSelf = user && user.id === att.userId;
                const initials = att.userName
                  ? att.userName
                      .split(' ')
                      .map((n) => n[0])
                      .join('')
                      .substring(0, 2)
                      .toUpperCase()
                  : 'AT';

                const interestChips = att.interests
                  ? att.interests
                      .split(',')
                      .map((i) => i.trim())
                      .filter(Boolean)
                  : [];

                return (
                  <div key={att.id} className="attendee-card">
                    <div className="attendee-card-top">
                      <div className="attendee-avatar">{initials}</div>
                      <div className="attendee-info">
                        <h4 className="attendee-name">
                          {att.userName} {isSelf && <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>(You)</span>}
                        </h4>
                        <div className="attendee-role">
                          {att.jobTitle || 'Attendee'}{att.company ? ` @ ${att.company}` : ''}
                        </div>
                      </div>
                    </div>

                    <div className="attendee-headline">
                      {att.headline || 'Active conference participant exploring networking opportunities.'}
                    </div>

                    {interestChips.length > 0 && (
                      <div className="attendee-interests-list">
                        {interestChips.map((chip, idx) => (
                          <span key={idx} className="interest-badge">
                            {chip}
                          </span>
                        ))}
                      </div>
                    )}

                    {att.networkingGoals && (
                      <div className="attendee-goals-box">
                        <strong>Goal:</strong> "{att.networkingGoals}"
                      </div>
                    )}

                    <div className="attendee-card-footer">
                      <div className={`availability-pill ${att.isAvailable ? '' : 'busy'}`}>
                        <span className={`availability-dot ${att.isAvailable ? '' : 'busy'}`}></span>
                        <span>{att.isAvailable ? 'Available for 1-on-1s' : 'Unavailable'}</span>
                      </div>

                      {!isSelf && att.isAvailable && (
                        <button
                          className="btn-schedule-meeting"
                          onClick={() => openScheduleModal(att)}
                        >
                          <Send size={13} />
                          <span>Request Meeting</span>
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* TAB 2: My 1-on-1 Schedule */}
      {activeSubTab === 'MY_SCHEDULE' && (
        <div className="meetings-section-wrap">
          {/* 1. Pending Incoming Meeting Requests */}
          {incomingRequests.length > 0 && (
            <div>
              <div className="meeting-group-title">
                <AlertCircle size={18} color="#f59e0b" />
                <span>Action Required: Incoming 1-on-1 Requests ({incomingRequests.length})</span>
              </div>
              <div className="meetings-grid">
                {incomingRequests.map((m) => (
                  <div key={m.id} className="meeting-card requested">
                    <div className="meeting-card-header">
                      <span className="meeting-table-badge">Table #{m.tableNumber}</span>
                      <span className="meeting-status-badge requested">ACTION NEEDED</span>
                    </div>

                    <div className="meeting-partner-meta">
                      <div>
                        <div className="meeting-partner-name">{m.requesterName}</div>
                        <div className="meeting-partner-sub">
                          {m.requesterHeadline || (m.requesterCompany ? `From ${m.requesterCompany}` : 'Conference Attendee')}
                        </div>
                      </div>
                    </div>

                    <div className="meeting-time-row">
                      <Clock size={15} color="#38bdf8" />
                      <span>
                        {new Date(m.startTime).toLocaleString([], {
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })} - {new Date(m.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                      <span style={{ margin: '0 4px', opacity: 0.5 }}>•</span>
                      <MapPin size={15} color="#10b981" />
                      <span>{m.locationName}</span>
                    </div>

                    {m.message && <div className="meeting-msg-quote">"{m.message}"</div>}

                    <div className="meeting-actions-row">
                      <button
                        className="btn-decline-meeting"
                        onClick={() => handleRespond(m.id, false)}
                      >
                        Decline
                      </button>
                      <button
                        className="btn-accept-meeting"
                        onClick={() => handleRespond(m.id, true)}
                      >
                        Accept & Lock Table #{m.tableNumber}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 2. Confirmed 1-on-1 Meetings */}
          <div>
            <div className="meeting-group-title">
              <Check size={18} color="#10b981" />
              <span>Confirmed 1-on-1 Meetings ({confirmedMeetings.length})</span>
            </div>
            {confirmedMeetings.length === 0 ? (
              <div style={{ padding: '30px', background: '#0e1526', borderRadius: '12px', border: '1px solid #1e293b', color: '#94a3b8', fontSize: '0.88rem' }}>
                No confirmed meetings yet. Browse the Attendee Directory to schedule your first 1-on-1!
              </div>
            ) : (
              <div className="meetings-grid">
                {confirmedMeetings.map((m) => {
                  const partnerName = user && user.id === m.requesterId ? m.recipientName : m.requesterName;
                  const partnerHeadline = user && user.id === m.requesterId ? m.recipientHeadline : m.requesterHeadline;

                  return (
                    <div key={m.id} className="meeting-card accepted">
                      <div className="meeting-card-header">
                        <span className="meeting-table-badge">Table #{m.tableNumber}</span>
                        <span className="meeting-status-badge accepted">CONFIRMED</span>
                      </div>

                      <div className="meeting-partner-meta">
                        <div>
                          <div className="meeting-partner-name">{partnerName}</div>
                          <div className="meeting-partner-sub">{partnerHeadline || 'Confirmed Partner'}</div>
                        </div>
                      </div>

                      <div className="meeting-time-row">
                        <Clock size={15} color="#38bdf8" />
                        <span>
                          {new Date(m.startTime).toLocaleString([], {
                            month: 'short',
                            day: 'numeric',
                            hour: '2-digit',
                            minute: '2-digit',
                          })} - {new Date(m.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                        <span style={{ margin: '0 4px', opacity: 0.5 }}>•</span>
                        <MapPin size={15} color="#10b981" />
                        <span>{m.locationName}</span>
                      </div>

                      {m.message && <div className="meeting-msg-quote">"{m.message}"</div>}

                      <div className="meeting-actions-row">
                        <button
                          className="btn-cancel-meeting"
                          onClick={() => handleCancelMeeting(m.id)}
                        >
                          Cancel Meeting
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* 3. Outgoing Pending Requests */}
          {outgoingRequests.length > 0 && (
            <div>
              <div className="meeting-group-title">
                <Clock size={18} color="#38bdf8" />
                <span>Outgoing Invitations Awaiting Reply ({outgoingRequests.length})</span>
              </div>
              <div className="meetings-grid">
                {outgoingRequests.map((m) => (
                  <div key={m.id} className="meeting-card">
                    <div className="meeting-card-header">
                      <span className="meeting-table-badge">Proposed Table #{m.tableNumber}</span>
                      <span className="meeting-status-badge requested">AWAITING REPLY</span>
                    </div>

                    <div className="meeting-partner-meta">
                      <div>
                        <div className="meeting-partner-name">With: {m.recipientName}</div>
                        <div className="meeting-partner-sub">{m.recipientHeadline || 'Attendee'}</div>
                      </div>
                    </div>

                    <div className="meeting-time-row">
                      <Clock size={15} color="#38bdf8" />
                      <span>
                        {new Date(m.startTime).toLocaleString([], {
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </span>
                    </div>

                    <div className="meeting-actions-row">
                      <button
                        className="btn-cancel-meeting"
                        onClick={() => handleCancelMeeting(m.id)}
                      >
                        Cancel Request
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 3: My Networking Profile */}
      {activeSubTab === 'MY_PROFILE' && (
        <div className="profile-edit-card">
          <h3 style={{ fontSize: '1.25rem', color: '#ffffff', marginBottom: '1.25rem' }}>
            Conference Matchmaking Profile
          </h3>

          {profileSuccessMsg && (
            <div style={{ background: 'rgba(16, 185, 129, 0.2)', border: '1px solid #10b981', color: '#6ee7b7', padding: '10px 14px', borderRadius: '8px', marginBottom: '1.25rem' }}>
              ✓ {profileSuccessMsg}
            </div>
          )}

          <form onSubmit={handleSaveProfile} className="profile-form-grid">
            <div className="profile-field-group">
              <label>Professional Headline *</label>
              <input
                type="text"
                required
                placeholder="e.g. Principal Cloud Architect @ FinTech Corp"
                value={headline}
                onChange={(e) => setHeadline(e.target.value)}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div className="profile-field-group">
                <label>Company / Organization</label>
                <input
                  type="text"
                  placeholder="e.g. Stripe, AWS, Startup"
                  value={company}
                  onChange={(e) => setCompany(e.target.value)}
                />
              </div>

              <div className="profile-field-group">
                <label>Job Title</label>
                <input
                  type="text"
                  placeholder="e.g. VP Engineering, Founder"
                  value={jobTitle}
                  onChange={(e) => setJobTitle(e.target.value)}
                />
              </div>
            </div>

            <div className="profile-field-group">
              <label>Areas of Expertise & Interests (Comma-separated)</label>
              <input
                type="text"
                placeholder="e.g. Distributed Systems, Kubernetes, LLM Deployment, Zero-Trust"
                value={interests}
                onChange={(e) => setInterests(e.target.value)}
              />
            </div>

            <div className="profile-field-group">
              <label>Networking Goals (What are you looking for?)</label>
              <textarea
                rows={3}
                placeholder="e.g. Exploring modern service mesh integrations, meeting potential technical co-founders, or hiring staff platform engineers."
                value={goals}
                onChange={(e) => setGoals(e.target.value)}
              ></textarea>
            </div>

            <div className="toggle-switch-row">
              <div>
                <strong style={{ display: 'block', color: '#ffffff' }}>Open for 1-on-1 Meeting Requests</strong>
                <span style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
                  Allow verified conference attendees to send meeting proposals.
                </span>
              </div>
              <input
                type="checkbox"
                checked={isAvailable}
                onChange={(e) => setIsAvailable(e.target.checked)}
                style={{ width: '20px', height: '20px', accentColor: '#10b981', cursor: 'pointer' }}
              />
            </div>

            <button type="submit" className="btn-save-profile" disabled={profileSaving}>
              {profileSaving ? 'Saving Profile...' : 'Save & Publish Profile'}
            </button>
          </form>
        </div>
      )}

      {/* Modal: Schedule 1-on-1 Meeting */}
      {scheduleTargetUser && (
        <div className="networking-modal-backdrop" onClick={() => setScheduleTargetUser(null)}>
          <div className="networking-modal" onClick={(e) => e.stopPropagation()}>
            <div className="networking-modal-header">
              <h3>Schedule 1-on-1 Meeting</h3>
              <button className="btn-modal-close" onClick={() => setScheduleTargetUser(null)}>
                <X size={18} />
              </button>
            </div>

            <div style={{ background: '#111827', padding: '10px 14px', borderRadius: '8px', border: '1px solid #1e293b' }}>
              <div style={{ fontSize: '0.9rem', fontWeight: 700, color: '#f8fafc' }}>
                With {scheduleTargetUser.userName}
              </div>
              <div style={{ fontSize: '0.8rem', color: '#38bdf8' }}>
                {scheduleTargetUser.headline || scheduleTargetUser.company}
              </div>
            </div>

            {scheduleError && (
              <div className="modal-conflict-banner">
                <ShieldAlert size={18} />
                <span>{scheduleError}</span>
              </div>
            )}

            <form onSubmit={handleScheduleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <div className="profile-field-group">
                  <label>Date *</label>
                  <input
                    type="date"
                    required
                    value={meetDate}
                    onChange={(e) => setMeetDate(e.target.value)}
                  />
                </div>

                <div className="profile-field-group">
                  <label>Start Time *</label>
                  <input
                    type="time"
                    required
                    value={meetTime}
                    onChange={(e) => setMeetTime(e.target.value)}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                <div className="profile-field-group">
                  <label>Duration</label>
                  <select
                    value={meetDuration}
                    onChange={(e) => setMeetDuration(e.target.value)}
                    style={{ background: '#111827', border: '1px solid #334155', color: '#fff', padding: '10px', borderRadius: '8px' }}
                  >
                    <option value="15">15 Minutes (Speed Intro)</option>
                    <option value="30">30 Minutes (Standard)</option>
                    <option value="45">45 Minutes (In-Depth)</option>
                  </select>
                </div>

                <div className="profile-field-group">
                  <label>Location / Area</label>
                  <input
                    type="text"
                    value={meetLocation}
                    onChange={(e) => setMeetLocation(e.target.value)}
                    placeholder="Networking Lounge Table"
                  />
                </div>
              </div>

              <div className="profile-field-group">
                <label>Intro Message / Discussion Topic</label>
                <textarea
                  rows={3}
                  placeholder="e.g. Would love to share our learnings running distributed Kafka clusters and explore partnership opportunities."
                  value={meetMessage}
                  onChange={(e) => setMeetMessage(e.target.value)}
                ></textarea>
              </div>

              <div className="modal-actions-row">
                <button type="button" className="btn-modal-cancel" onClick={() => setScheduleTargetUser(null)}>
                  Cancel
                </button>
                <button type="submit" className="btn-modal-submit" disabled={scheduleSubmitting}>
                  {scheduleSubmitting ? 'Checking Conflicts...' : 'Send Meeting Proposal'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
