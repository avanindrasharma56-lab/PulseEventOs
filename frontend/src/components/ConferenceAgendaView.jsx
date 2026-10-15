import React, { useState, useEffect } from 'react';
import { 
  Calendar, 
  Clock, 
  MapPin, 
  User, 
  Bookmark, 
  BookmarkCheck, 
  Download, 
  Plus, 
  Trash2, 
  Sparkles, 
  Layers, 
  Share2, 
  Tag, 
  ExternalLink,
  Loader2,
  CheckCircle2,
  Filter,
  X,
  Armchair,
  AlertCircle,
  BarChart2
} from 'lucide-react';
import { agendaService, eventService, sessionBookingService } from '../services/api';
import { useAuth } from '../context/AuthContext';
import './ConferenceAgendaView.css';

export default function ConferenceAgendaView({ eventId, onOpenAuth }) {
  const { user, isAuthenticated, isOrganizer, isAdmin } = useAuth();
  const [eventsList, setEventsList] = useState([]);
  const [selectedEventId, setSelectedEventId] = useState(eventId || '');
  const [selectedEvent, setSelectedEvent] = useState(null);

  const [activeTab, setActiveTab] = useState('AGENDA'); // 'AGENDA' | 'SPEAKERS' | 'MY_SCHEDULE' | 'ROOM_HEATMAP'
  const [sessions, setSessions] = useState([]);
  const [speakers, setSpeakers] = useState([]);
  const [mySchedule, setMySchedule] = useState([]);
  const [myRsvps, setMyRsvps] = useState([]);
  const [roomHeatmaps, setRoomHeatmaps] = useState([]);
  const [selectedTrack, setSelectedTrack] = useState('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [isExporting, setIsExporting] = useState(false);
  const [conflictError, setConflictError] = useState('');
  const [bookingLoadingId, setBookingLoadingId] = useState(null);

  // Modals for Organizers
  const [isSessionModalOpen, setIsSessionModalOpen] = useState(false);
  const [isSpeakerModalOpen, setIsSpeakerModalOpen] = useState(false);

  // New Session Form
  const [sessionTitle, setSessionTitle] = useState('');
  const [sessionMaxCapacity, setSessionMaxCapacity] = useState(100);
  const [sessionDesc, setSessionDesc] = useState('');
  const [sessionSpeakerId, setSessionSpeakerId] = useState('');
  const [sessionRoom, setSessionRoom] = useState('');
  const [sessionTrack, setSessionTrack] = useState('General');
  const [sessionType, setSessionType] = useState('KEYNOTE');
  const [sessionStart, setSessionStart] = useState('');
  const [sessionEnd, setSessionEnd] = useState('');
  const [formError, setFormError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // New Speaker Form
  const [spkName, setSpkName] = useState('');
  const [spkTitle, setSpkTitle] = useState('');
  const [spkCompany, setSpkCompany] = useState('');
  const [spkBio, setSpkBio] = useState('');
  const [spkAvatar, setSpkAvatar] = useState('');
  const [spkSocial, setSpkSocial] = useState('');

  // 1. Load Events for selector
  useEffect(() => {
    async function fetchEvents() {
      try {
        const res = await eventService.getEvents({ size: 50 });
        const list = res.content || [];
        setEventsList(list);
        if (!selectedEventId && list.length > 0) {
          setSelectedEventId(list[0].id);
          setSelectedEvent(list[0]);
        } else if (selectedEventId) {
          const match = list.find(e => e.id.toString() === selectedEventId.toString());
          if (match) setSelectedEvent(match);
        }
      } catch (err) {
        console.error('Failed to fetch events', err);
      }
    }
    fetchEvents();
  }, [eventId]);

  // 2. Load Agenda, Speakers, Schedule, RSVPs & Room Heatmap
  const loadAgendaData = async () => {
    if (!selectedEventId) return;
    setIsLoading(true);
    setConflictError('');
    try {
      const [sessionsData, speakersData, heatmapsData] = await Promise.all([
        agendaService.getSessions(selectedEventId).catch(() => []),
        agendaService.getSpeakers(selectedEventId).catch(() => []),
        sessionBookingService.getRoomCapacityHeatmap(selectedEventId).catch(() => [])
      ]);
      setSessions(sessionsData || []);
      setSpeakers(speakersData || []);
      setRoomHeatmaps(heatmapsData || []);

      if (isAuthenticated) {
        const [mySched, rsvps] = await Promise.all([
          agendaService.getMySchedule(selectedEventId).catch(() => []),
          sessionBookingService.getMyRsvps(selectedEventId).catch(() => [])
        ]);
        setMySchedule(mySched || []);
        setMyRsvps(rsvps || []);
      }
    } catch (err) {
      console.error('Failed to load agenda items', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleReserveSeat = async (session) => {
    if (!isAuthenticated) {
      if (onOpenAuth) onOpenAuth('signin');
      else alert('Please sign in to reserve breakout session seats.');
      return;
    }

    setConflictError('');
    setBookingLoadingId(session.id);
    try {
      const rsvp = await sessionBookingService.reserveSeat(session.id);
      setMyRsvps(prev => [rsvp, ...prev.filter(r => r.sessionId !== session.id)]);
      await loadAgendaData();
    } catch (err) {
      setConflictError(err.message || 'Failed to reserve breakout seat.');
    } finally {
      setBookingLoadingId(null);
    }
  };

  const handleCancelRsvp = async (sessionId) => {
    if (!window.confirm('Are you sure you want to release your seat reservation for this session?')) {
      return;
    }
    setBookingLoadingId(sessionId);
    try {
      await sessionBookingService.cancelRsvp(sessionId);
      setMyRsvps(prev => prev.filter(r => r.sessionId !== sessionId));
      await loadAgendaData();
    } catch (err) {
      alert(err.message || 'Failed to cancel reservation');
    } finally {
      setBookingLoadingId(null);
    }
  };

  useEffect(() => {
    loadAgendaData();
  }, [selectedEventId, isAuthenticated]);

  const handleToggleBookmark = async (session) => {
    if (!isAuthenticated) {
      if (onOpenAuth) onOpenAuth('signin');
      else alert('Please sign in to save sessions to your personal timetable.');
      return;
    }

    try {
      const res = await agendaService.toggleBookmark(session.id);
      setSessions(prev => prev.map(s => s.id === session.id ? { ...s, bookmarked: res.bookmarked } : s));
      if (res.bookmarked) {
        setMySchedule(prev => [...prev, { ...session, bookmarked: true }]);
      } else {
        setMySchedule(prev => prev.filter(s => s.id !== session.id));
      }
    } catch (err) {
      alert(err.message || 'Failed to update schedule bookmark');
    }
  };

  const handleExportIcs = async () => {
    setIsExporting(true);
    try {
      const slug = selectedEvent?.slug || 'event';
      await agendaService.downloadIcal(selectedEventId, slug);
    } catch (err) {
      alert(err.message || 'Failed to export iCal schedule');
    } finally {
      setIsExporting(false);
    }
  };

  const handleCreateSpeaker = async (e) => {
    e.preventDefault();
    setFormError('');
    setIsSubmitting(true);
    try {
      await agendaService.createSpeaker(selectedEventId, {
        name: spkName,
        title: spkTitle,
        company: spkCompany,
        bio: spkBio,
        avatarUrl: spkAvatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80',
        socialUrl: spkSocial
      });
      setIsSpeakerModalOpen(false);
      setSpkName('');
      setSpkTitle('');
      setSpkCompany('');
      setSpkBio('');
      await loadAgendaData();
    } catch (err) {
      setFormError(err.message || 'Failed to create speaker');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCreateSession = async (e) => {
    e.preventDefault();
    setFormError('');
    setIsSubmitting(true);
    try {
      await agendaService.createSession(selectedEventId, {
        title: sessionTitle,
        description: sessionDesc,
        speakerId: sessionSpeakerId ? Number(sessionSpeakerId) : null,
        room: sessionRoom,
        track: sessionTrack,
        sessionType,
        startTime: sessionStart,
        endTime: sessionEnd,
        maxCapacity: Number(sessionMaxCapacity) || 100
      });
      setIsSessionModalOpen(false);
      setSessionTitle('');
      setSessionDesc('');
      setSessionRoom('');
      setSessionMaxCapacity(100);
      await loadAgendaData();
    } catch (err) {
      setFormError(err.message || 'Failed to create session');
    } finally {
      setIsSubmitting(false);
    }
  };

  const tracks = ['ALL', ...Array.from(new Set(sessions.map(s => s.track).filter(Boolean)))];

  const filteredSessions = sessions.filter(s => {
    if (selectedTrack !== 'ALL' && s.track !== selectedTrack) return false;
    return true;
  });

  const displayList = activeTab === 'MY_SCHEDULE' ? mySchedule : filteredSessions;

  const formatTime = (timeStr) => {
    if (!timeStr) return '';
    const d = new Date(timeStr);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const formatDate = (timeStr) => {
    if (!timeStr) return '';
    const d = new Date(timeStr);
    return d.toLocaleDateString([], { weekday: 'short', month: 'short', day: 'numeric' });
  };

  return (
    <div className="agenda-cockpit">
      {/* Top Header & Event Switcher */}
      <div className="agenda-header">
        <div className="agenda-title-group">
          <div className="agenda-badge-icon">
            <Calendar size={22} />
          </div>
          <div>
            <h2>Conference Agenda & Speaker Tracks</h2>
            <p>Explore keynote schedules, filter technical tracks, bookmark sessions, and export to iCalendar.</p>
          </div>
        </div>

        <div className="agenda-controls">
          <div className="event-dropdown-wrap">
            <label>Select Conference:</label>
            <select 
              value={selectedEventId} 
              onChange={(e) => {
                setSelectedEventId(e.target.value);
                const ev = eventsList.find(x => x.id.toString() === e.target.value);
                if (ev) setSelectedEvent(ev);
              }}
              className="event-select"
            >
              {eventsList.map(ev => (
                <option key={ev.id} value={ev.id}>{ev.title}</option>
              ))}
            </select>
          </div>

          <button 
            className="btn-export-ical" 
            onClick={handleExportIcs} 
            disabled={isExporting || sessions.length === 0}
            title="Download Apple, Outlook, & Google Calendar iCal Stream"
          >
            {isExporting ? <Loader2 size={16} className="spinning" /> : <Download size={16} />}
            <span>Export to iCal (.ics)</span>
          </button>

          {(isOrganizer || isAdmin) && (
            <div className="organizer-agenda-actions">
              <button className="btn-add-action" onClick={() => setIsSpeakerModalOpen(true)}>
                <User size={15} />
                <span>+ Speaker</span>
              </button>
              <button className="btn-add-action highlight" onClick={() => setIsSessionModalOpen(true)}>
                <Plus size={15} />
                <span>+ Session</span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Main Tab Controls */}
      <div className="agenda-tab-bar">
        <div className="tabs-left">
          <button 
            className={`tab-btn ${activeTab === 'AGENDA' ? 'active' : ''}`}
            onClick={() => setActiveTab('AGENDA')}
          >
            <Layers size={15} />
            <span>Master Agenda ({sessions.length})</span>
          </button>
          <button 
            className={`tab-btn ${activeTab === 'SPEAKERS' ? 'active' : ''}`}
            onClick={() => setActiveTab('SPEAKERS')}
          >
            <User size={15} />
            <span>Keynote Speakers ({speakers.length})</span>
          </button>
          <button 
            className={`tab-btn ${activeTab === 'MY_SCHEDULE' ? 'active' : ''}`}
            onClick={() => setActiveTab('MY_SCHEDULE')}
          >
            <BookmarkCheck size={15} />
            <span>My Bookmarked Timetable ({mySchedule.length})</span>
          </button>
          <button 
            className={`tab-btn ${activeTab === 'ROOM_HEATMAP' ? 'active' : ''}`}
            onClick={() => setActiveTab('ROOM_HEATMAP')}
          >
            <BarChart2 size={15} />
            <span>Room Load Heatmap ({roomHeatmaps.length})</span>
          </button>
        </div>

        {activeTab === 'AGENDA' && tracks.length > 1 && (
          <div className="track-filter-pills">
            <Filter size={14} className="filter-icon" />
            {tracks.map(t => (
              <button
                key={t}
                className={`track-pill ${selectedTrack === t ? 'active' : ''}`}
                onClick={() => setSelectedTrack(t)}
              >
                {t}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Conflict Toast Alert Banner */}
      {conflictError && (
        <div className="conflict-toast-banner">
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertCircle size={18} />
            <span>{conflictError}</span>
          </div>
          <button onClick={() => setConflictError('')}>✕</button>
        </div>
      )}

      {/* Content Rendering */}
      {isLoading ? (
        <div className="agenda-loading">
          <Loader2 size={32} className="spinning" />
          <p>Loading conference timetable and speaker roster...</p>
        </div>
      ) : activeTab === 'SPEAKERS' ? (
        /* Speakers Directory Grid */
        <div className="speakers-grid">
          {speakers.length === 0 ? (
            <div className="empty-agenda-state">
              <User size={48} className="empty-icon" />
              <h3>No Keynote Speakers Registered Yet</h3>
              <p>The organizer hasn't published the speaker roster for this conference yet.</p>
            </div>
          ) : (
            speakers.map(spk => (
              <div key={spk.id} className="speaker-card">
                <div className="speaker-avatar-wrap">
                  <img src={spk.avatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80'} alt={spk.name} />
                </div>
                <div className="speaker-info">
                  <h3 className="speaker-name">{spk.name}</h3>
                  <div className="speaker-role">
                    {spk.title && <span>{spk.title}</span>}
                    {spk.company && <span className="speaker-company"> • {spk.company}</span>}
                  </div>
                  <p className="speaker-bio">{spk.bio}</p>
                  {spk.socialUrl && (
                    <a href={spk.socialUrl} target="_blank" rel="noreferrer" className="speaker-social-link">
                      <ExternalLink size={13} />
                      <span>Connect Profile</span>
                    </a>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      ) : activeTab === 'ROOM_HEATMAP' ? (
        /* Multi-Track Room Load Heatmap Grid */
        <div className="room-heatmap-grid">
          {roomHeatmaps.length === 0 ? (
            <div className="empty-agenda-state">
              <BarChart2 size={48} className="empty-icon" />
              <h3>No Room Capacity Data Available</h3>
              <p>Schedule rooms with assigned seating capacities to inspect live conference loads.</p>
            </div>
          ) : (
            roomHeatmaps.map((h, i) => (
              <div key={i} className="room-heatmap-card">
                <div className="heatmap-room-header">
                  <h3 className="heatmap-room-title">{h.room}</h3>
                  <span className="heatmap-track-badge">{h.track}</span>
                </div>

                <div className="heatmap-stat-boxes">
                  <div>
                    <div className="heatmap-stat-num">{h.totalCapacity}</div>
                    <div className="heatmap-stat-lbl">Capacity</div>
                  </div>
                  <div>
                    <div className="heatmap-stat-num">{h.totalReserved}</div>
                    <div className="heatmap-stat-lbl">Reserved</div>
                  </div>
                  <div>
                    <div className="heatmap-stat-num" style={{ color: h.averageOccupancyPercentage > 85 ? '#ef4444' : '#10b981' }}>
                      {h.averageOccupancyPercentage}%
                    </div>
                    <div className="heatmap-stat-lbl">Avg Load</div>
                  </div>
                </div>

                <div className="heatmap-sessions-list">
                  <span style={{ fontSize: '0.78rem', color: '#94a3b8', fontWeight: '700', textTransform: 'uppercase' }}>
                    Scheduled Sessions ({h.sessions?.length || 0})
                  </span>
                  {h.sessions?.map(s => (
                    <div key={s.sessionId} className="heatmap-session-row">
                      <span className="heatmap-session-name" title={s.title}>{s.title}</span>
                      <span className="heatmap-session-occ">
                        {s.reservedSeats}/{s.maxCapacity} ({Math.round(s.occupancyPercentage || 0)}%)
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            ))
          )}
        </div>
      ) : (
        /* Sessions Timeline View */
        <div className="sessions-timeline">
          {displayList.length === 0 ? (
            <div className="empty-agenda-state">
              <Calendar size={48} className="empty-icon" />
              <h3>{activeTab === 'MY_SCHEDULE' ? 'Your Personal Timetable is Empty' : 'No Sessions Scheduled'}</h3>
              <p>
                {activeTab === 'MY_SCHEDULE' 
                  ? 'Browse the master agenda and click the bookmark button on any session to pin it to your timetable.'
                  : 'No agenda sessions match the selected track filter.'}
              </p>
            </div>
          ) : (
            displayList.map(session => (
              <div key={session.id} className={`session-card ${session.bookmarked ? 'is-bookmarked' : ''}`}>
                <div className="session-time-col">
                  <div className="time-badge">
                    <Clock size={14} />
                    <span>{formatTime(session.startTime)} - {formatTime(session.endTime)}</span>
                  </div>
                  <div className="date-sub">{formatDate(session.startTime)}</div>
                  {session.room && (
                    <div className="room-badge">
                      <MapPin size={12} />
                      <span>{session.room}</span>
                    </div>
                  )}
                </div>

                <div className="session-main-col">
                  <div className="session-tags-row">
                    <span className={`type-badge type-${session.sessionType?.toLowerCase()}`}>
                      {session.sessionType}
                    </span>
                    {session.track && (
                      <span className="track-badge">
                        <Tag size={11} />
                        <span>{session.track}</span>
                      </span>
                    )}
                  </div>

                  <h3 className="session-title">{session.title}</h3>
                  <p className="session-description">{session.description}</p>

                  {session.speaker && (
                    <div className="session-speaker-pill">
                      <img 
                        src={session.speaker.avatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80'} 
                        alt={session.speaker.name} 
                      />
                      <div className="speaker-pill-meta">
                        <span className="pill-name">{session.speaker.name}</span>
                        <span className="pill-title">{session.speaker.title} ({session.speaker.company})</span>
                      </div>
                    </div>
                  )}
                </div>

                <div className="session-action-col">
                  {/* Seating Capacity Meter */}
                  {session.maxCapacity && (
                    <div className="session-capacity-meter">
                      <div className="capacity-meta-row">
                        <span className="capacity-label">Room Capacity</span>
                        <span className={`capacity-count ${session.reservedSeats >= session.maxCapacity ? 'full' : ''}`}>
                          {session.reservedSeats || 0} / {session.maxCapacity}
                        </span>
                      </div>
                      <div className="capacity-progress-bar">
                        <div 
                          className={`capacity-progress-fill ${
                            ((session.reservedSeats || 0) / session.maxCapacity) >= 1.0 ? 'load-high' :
                            ((session.reservedSeats || 0) / session.maxCapacity) >= 0.8 ? 'load-medium' : 'load-low'
                          }`}
                          style={{ width: `${Math.min(100, Math.round(((session.reservedSeats || 0) / session.maxCapacity) * 100))}%` }}
                        />
                      </div>
                    </div>
                  )}

                  {/* Seat Booking Action */}
                  {(() => {
                    const userRsvp = myRsvps.find(r => r.sessionId === session.id);
                    if (userRsvp && userRsvp.status === 'CONFIRMED') {
                      return (
                        <div className="rsvp-actions-wrap">
                          <div className="btn-rsvp-action confirmed">
                            <CheckCircle2 size={13} />
                            <span>Seat Confirmed</span>
                          </div>
                          <span className="rsvp-pass-pill">{userRsvp.rsvpCode}</span>
                          <button 
                            className="btn-release-seat" 
                            onClick={() => handleCancelRsvp(session.id)}
                            disabled={bookingLoadingId === session.id}
                          >
                            {bookingLoadingId === session.id ? 'Releasing...' : 'Release Seat'}
                          </button>
                        </div>
                      );
                    }
                    if (userRsvp && userRsvp.status === 'WAITLISTED') {
                      return (
                        <div className="rsvp-actions-wrap">
                          <div className="btn-rsvp-action waitlist">
                            <Clock size={13} />
                            <span>Waitlist #{userRsvp.waitlistPosition || 1}</span>
                          </div>
                          <button 
                            className="btn-release-seat" 
                            onClick={() => handleCancelRsvp(session.id)}
                            disabled={bookingLoadingId === session.id}
                          >
                            {bookingLoadingId === session.id ? 'Leaving...' : 'Leave Waitlist'}
                          </button>
                        </div>
                      );
                    }
                    const isFull = (session.reservedSeats || 0) >= (session.maxCapacity || 100);
                    return (
                      <div className="rsvp-actions-wrap">
                        <button 
                          className={`btn-rsvp-action ${isFull ? 'waitlist' : 'reserve'}`}
                          onClick={() => handleReserveSeat(session)}
                          disabled={bookingLoadingId === session.id}
                        >
                          <Armchair size={13} />
                          <span>
                            {bookingLoadingId === session.id ? 'Booking...' : (isFull ? 'Join Waitlist' : 'Reserve Seat')}
                          </span>
                        </button>
                      </div>
                    );
                  })()}

                  <button 
                    className={`btn-bookmark ${session.bookmarked ? 'bookmarked' : ''}`}
                    onClick={() => handleToggleBookmark(session)}
                    title={session.bookmarked ? 'Remove from personal schedule' : 'Save to my personal timetable'}
                  >
                    {session.bookmarked ? <BookmarkCheck size={18} /> : <Bookmark size={18} />}
                    <span>{session.bookmarked ? 'Saved' : 'Save'}</span>
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* Organizer Modal: Add Speaker */}
      {isSpeakerModalOpen && (
        <div className="modal-backdrop" onClick={() => setIsSpeakerModalOpen(false)}>
          <div className="agenda-modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Register Keynote Speaker</h3>
              <button className="btn-close" onClick={() => setIsSpeakerModalOpen(false)}><X size={18} /></button>
            </div>
            <form onSubmit={handleCreateSpeaker} className="agenda-form">
              {formError && <div className="form-error">{formError}</div>}
              <div className="form-group">
                <label>Speaker Name *</label>
                <input type="text" value={spkName} onChange={e => setSpkName(e.target.value)} required placeholder="e.g. Dr. Jennifer Park" />
              </div>
              <div className="form-row-2">
                <div className="form-group">
                  <label>Title / Role</label>
                  <input type="text" value={spkTitle} onChange={e => setSpkTitle(e.target.value)} placeholder="VP of Cloud Systems" />
                </div>
                <div className="form-group">
                  <label>Company / Organization</label>
                  <input type="text" value={spkCompany} onChange={e => setSpkCompany(e.target.value)} placeholder="Nexus Architecture" />
                </div>
              </div>
              <div className="form-group">
                <label>Bio Summary</label>
                <textarea rows={3} value={spkBio} onChange={e => setSpkBio(e.target.value)} placeholder="Author of distributed consensus algorithms and conference keynote speaker..."></textarea>
              </div>
              <div className="form-row-2">
                <div className="form-group">
                  <label>Avatar Photo URL</label>
                  <input type="url" value={spkAvatar} onChange={e => setSpkAvatar(e.target.value)} placeholder="https://..." />
                </div>
                <div className="form-group">
                  <label>Social / LinkedIn Profile URL</label>
                  <input type="url" value={spkSocial} onChange={e => setSpkSocial(e.target.value)} placeholder="https://linkedin.com/in/..." />
                </div>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-cancel" onClick={() => setIsSpeakerModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn-submit" disabled={isSubmitting}>
                  {isSubmitting ? 'Saving...' : 'Register Speaker'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Organizer Modal: Add Session */}
      {isSessionModalOpen && (
        <div className="modal-backdrop" onClick={() => setIsSessionModalOpen(false)}>
          <div className="agenda-modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Add Conference Agenda Session</h3>
              <button className="btn-close" onClick={() => setIsSessionModalOpen(false)}><X size={18} /></button>
            </div>
            <form onSubmit={handleCreateSession} className="agenda-form">
              {formError && <div className="form-error">{formError}</div>}
              <div className="form-group">
                <label>Session Title *</label>
                <input type="text" value={sessionTitle} onChange={e => setSessionTitle(e.target.value)} required placeholder="e.g. Architecting Distributed Message Buses" />
              </div>
              <div className="form-row-2">
                <div className="form-group">
                  <label>Session Type *</label>
                  <select value={sessionType} onChange={e => setSessionType(e.target.value)}>
                    <option value="KEYNOTE">KEYNOTE</option>
                    <option value="PANEL">PANEL DISCUSSION</option>
                    <option value="WORKSHOP">HANDS-ON WORKSHOP</option>
                    <option value="BREAKOUT">TECHNICAL BREAKOUT</option>
                    <option value="NETWORKING">NETWORKING RECEPTION</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Assign Speaker</label>
                  <select value={sessionSpeakerId} onChange={e => setSessionSpeakerId(e.target.value)}>
                    <option value="">-- No Speaker / Panel --</option>
                    {speakers.map(spk => (
                      <option key={spk.id} value={spk.id}>{spk.name} ({spk.company})</option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="form-row-2">
                <div className="form-group">
                  <label>Room / Stage Location</label>
                  <input type="text" value={sessionRoom} onChange={e => setSessionRoom(e.target.value)} placeholder="Grand Ballroom A" />
                </div>
                <div className="form-group">
                  <label>Track Name</label>
                  <input type="text" value={sessionTrack} onChange={e => setSessionTrack(e.target.value)} placeholder="AI / Systems / Cloud" />
                </div>
              </div>
              <div className="form-row-2">
                <div className="form-group">
                  <label>Room Seating Capacity (Seats) *</label>
                  <input 
                    type="number" 
                    min="1" 
                    max="10000" 
                    value={sessionMaxCapacity} 
                    onChange={e => setSessionMaxCapacity(Math.max(1, parseInt(e.target.value) || 1))} 
                    required 
                    placeholder="100" 
                  />
                </div>
                <div className="form-group">
                  <label style={{ opacity: 0.75 }}>Capacity Policy</label>
                  <div style={{ fontSize: '0.78rem', color: '#94a3b8', background: '#090d16', border: '1px solid #1e293b', borderRadius: '8px', padding: '9px 12px', lineHeight: '1.3' }}>
                    Enforces live attendee reservation cap, schedule conflict checks & waitlist promotion.
                  </div>
                </div>
              </div>
              <div className="form-row-2">
                <div className="form-group">
                  <label>Start Date & Time *</label>
                  <input type="datetime-local" value={sessionStart} onChange={e => setSessionStart(e.target.value)} required />
                </div>
                <div className="form-group">
                  <label>End Date & Time *</label>
                  <input type="datetime-local" value={sessionEnd} onChange={e => setSessionEnd(e.target.value)} required />
                </div>
              </div>
              <div className="form-group">
                <label>Session Abstract & Takeaways</label>
                <textarea rows={3} value={sessionDesc} onChange={e => setSessionDesc(e.target.value)} placeholder="Detailed description of topics covered, prerequisites, and learning objectives..."></textarea>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-cancel" onClick={() => setIsSessionModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn-submit" disabled={isSubmitting}>
                  {isSubmitting ? 'Publishing...' : 'Add Session'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
