import React, { useState, useEffect } from 'react';
import { gamificationService, agendaService, sponsorService } from '../services/api';
import './GamifiedLeaderboardView.css';

export const GamifiedLeaderboardView = ({ eventId, eventTitle, user, onLoginRequired }) => {
  const [profile, setProfile] = useState(null);
  const [leaderboard, setLeaderboard] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [activeTab, setActiveTab] = useState('leaderboard'); // 'leaderboard' | 'quests' | 'badges' | 'history'
  const [sponsors, setSponsors] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [message, setMessage] = useState(null);
  const [scanning, setScanning] = useState(false);

  useEffect(() => {
    if (eventId) {
      loadData();
    }
  }, [eventId, user]);

  const loadData = async (isSilent = false) => {
    if (!isSilent) setLoading(true);
    try {
      const lb = await gamificationService.getLeaderboard(eventId, 50);
      setLeaderboard(lb || []);

      if (user) {
        try {
          const prof = await gamificationService.getProfile(eventId);
          setProfile(prof);
        } catch (e) {
          console.warn('Could not load attendee gamification profile:', e);
        }
      }

      // Fetch sponsors & sessions for quest triggers
      try {
        const spList = await sponsorService.getSponsorsByEvent(eventId);
        setSponsors(spList || []);
      } catch (e) {
        // optional
      }
      try {
        const sessList = await agendaService.getSessions(eventId);
        setSessions(sessList || []);
      } catch (e) {
        // optional
      }
    } catch (err) {
      console.error('Failed to load gamification data:', err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const handleClaimSwag = async () => {
    if (!user) return onLoginRequired?.();
    try {
      const updated = await gamificationService.claimSwag(eventId);
      setProfile(updated);
      setMessage({ type: 'success', text: '🎉 VIP Swag Pass successfully claimed! Show this at the Registration Desk.' });
    } catch (err) {
      setMessage({ type: 'error', text: err.message || 'Failed to claim swag pass' });
    }
  };

  const handleScanBooth = async (sponsorId, sponsorName) => {
    if (!user) return onLoginRequired?.();
    setScanning(true);
    try {
      const updated = await gamificationService.scanBooth(eventId, sponsorId);
      setProfile(updated);
      setMessage({ type: 'success', text: `🚀 Scanned booth "${sponsorName}"! Earned +75 XP!` });
      loadData(true);
    } catch (err) {
      setMessage({ type: 'error', text: err.message || 'Failed to scan booth' });
    } finally {
      setScanning(false);
    }
  };

  const handleCheckInSession = async (sessionId, sessionTitle) => {
    if (!user) return onLoginRequired?.();
    setScanning(true);
    try {
      const updated = await gamificationService.checkInSession(eventId, sessionId);
      setProfile(updated);
      setMessage({ type: 'success', text: `🎓 Checked in to "${sessionTitle}"! Earned +150 XP!` });
      loadData(true);
    } catch (err) {
      setMessage({ type: 'error', text: err.message || 'Failed to check in session' });
    } finally {
      setScanning(false);
    }
  };

  const handleSimulateAction = async (activityType, description, customXp) => {
    if (!user) return onLoginRequired?.();
    try {
      const refId = `SIM_${activityType}_${Date.now()}`;
      const updated = await gamificationService.awardXp(eventId, {
        activityType,
        referenceId: refId,
        description,
        customXp
      });
      setProfile(updated);
      setMessage({ type: 'success', text: `⚡ Quest Completed: ${description} (+${customXp} XP)!` });
      loadData(true);
    } catch (err) {
      setMessage({ type: 'error', text: err.message || 'Failed to award XP' });
    }
  };

  const getTierColor = (tier) => {
    switch (tier) {
      case 'PLATINUM': return 'var(--tier-platinum)';
      case 'GOLD': return 'var(--tier-gold)';
      case 'SILVER': return 'var(--tier-silver)';
      case 'BRONZE': return 'var(--tier-bronze)';
      default: return 'var(--tier-novice)';
    }
  };

  const allMilestoneBadges = [
    { key: 'first_checkin', name: 'Turnstile Pioneer', icon: '🚀', desc: 'Scan turnstile admission gate' },
    { key: 'booth_scout', name: 'Expo Explorer', icon: '🔍', desc: 'Visit and scan a sponsor booth QR' },
    { key: 'session_master', name: 'Knowledge Seeker', icon: '🎓', desc: 'Check in to a breakout session' },
    { key: 'networking_pro', name: 'Super Connector', icon: '🤝', desc: 'Complete a 1-on-1 meeting' },
    { key: 'voice_of_crowd', name: 'Active Voice', icon: '🗳️', desc: 'Submit a Q&A question or poll vote' },
    { key: 'tier_gold', name: 'Gold Elite', icon: '🏆', desc: 'Ascend to Gold Tier (500+ XP)' },
    { key: 'tier_platinum', name: 'Eventflow Legend', icon: '👑', desc: 'Ascend to Platinum Tier (1000+ XP)' },
  ];

  const earnedBadgeKeys = new Set(profile?.badges?.map(b => b.badgeKey) || []);

  const xpProgressPercent = profile ? Math.min(100, Math.round((profile.totalXp / (profile.nextTierMinXp || 1000)) * 100)) : 0;

  return (
    <div className="gamified-container">
      {/* Hero Header */}
      <div className="gamified-hero">
        <div className="hero-content">
          <div className="hero-badge">
            <span className="live-pulse"></span> LIVE ENGAGEMENT & SCAVENGER HUNT
          </div>
          <h1 className="hero-title">Attendee Quest & Leaderboard</h1>
          <p className="hero-subtitle">
            Explore {eventTitle || 'the summit'}, check into sessions, scan sponsor booths, and unlock exclusive VIP Swag Passes!
          </p>
        </div>

        <div className="hero-actions">
          <button
            className="btn-refresh"
            onClick={() => { setRefreshing(true); loadData(); }}
            disabled={refreshing}
          >
            {refreshing ? 'Refreshing...' : '🔄 Live Sync'}
          </button>
        </div>
      </div>

      {message && (
        <div className={`gamified-alert alert-${message.type}`}>
          <span>{message.text}</span>
          <button className="alert-close" onClick={() => setMessage(null)}>✕</button>
        </div>
      )}

      {/* User Passport & Status Card */}
      {user ? (
        <div className="attendee-passport-card">
          <div className="passport-header">
            <div className="attendee-avatar-cluster">
              <div className="avatar-ring" style={{ borderColor: getTierColor(profile?.currentTier) }}>
                <span className="avatar-letter">{profile?.userName ? profile.userName[0].toUpperCase() : 'A'}</span>
              </div>
              <div className="attendee-meta">
                <div className="attendee-name">{profile?.userName || user.email}</div>
                <div className="attendee-tier-tag" style={{ backgroundColor: `${getTierColor(profile?.currentTier)}22`, color: getTierColor(profile?.currentTier) }}>
                  ✦ {profile?.currentTier || 'NOVICE'} TIER
                </div>
              </div>
            </div>

            <div className="rank-podium-stat">
              <div className="rank-label">EVENT RANK</div>
              <div className="rank-value">#{profile?.rankPosition || '-'}</div>
            </div>
          </div>

          <div className="passport-progress-zone">
            <div className="progress-labels">
              <span className="xp-curr"><strong>{profile?.totalXp || 0}</strong> XP Points</span>
              <span className="xp-next">Next Tier at {profile?.nextTierMinXp || 1000} XP</span>
            </div>
            <div className="xp-progress-bar">
              <div
                className="xp-progress-fill"
                style={{
                  width: `${xpProgressPercent}%`,
                  background: `linear-gradient(90deg, #6366f1, ${getTierColor(profile?.currentTier)})`
                }}
              ></div>
            </div>
          </div>

          {/* Swag Pass Vault */}
          <div className={`swag-vault-box ${profile?.swagCode ? 'unlocked' : 'locked'}`}>
            <div className="swag-icon">🎁</div>
            <div className="swag-details">
              <div className="swag-title">
                {profile?.swagCode ? 'VIP Swag Pass Unlocked!' : 'VIP Swag Pass (Requires Gold Tier - 500 XP)'}
              </div>
              <div className="swag-desc">
                {profile?.swagCode ? (
                  <span>Pass Code: <strong className="code-badge">{profile.swagCode}</strong></span>
                ) : (
                  <span>Earn {(500 - (profile?.totalXp || 0)) > 0 ? (500 - (profile?.totalXp || 0)) : 0} more XP to claim your official convention swag bag!</span>
                )}
              </div>
            </div>

            {profile?.swagCode && (
              <div className="swag-action">
                {profile.isSwagClaimed ? (
                  <span className="swag-claimed-badge">✓ Claimed at Desk</span>
                ) : (
                  <button className="btn-claim-swag" onClick={handleClaimSwag}>
                    Claim Swag
                  </button>
                )}
              </div>
            )}
          </div>
        </div>
      ) : (
        <div className="attendee-login-prompt">
          <div className="prompt-text">
            <h3>Join the Scavenger Hunt</h3>
            <p>Sign in to track your personal XP, unlock badges, and claim exclusive event swag.</p>
          </div>
          <button className="btn-login-cta" onClick={onLoginRequired}>
            Sign In to Play
          </button>
        </div>
      )}

      {/* Navigation Tabs */}
      <div className="gamified-tabs">
        <button
          className={`tab-btn ${activeTab === 'leaderboard' ? 'active' : ''}`}
          onClick={() => setActiveTab('leaderboard')}
        >
          🏆 Live Leaderboard ({leaderboard.length})
        </button>
        <button
          className={`tab-btn ${activeTab === 'quests' ? 'active' : ''}`}
          onClick={() => setActiveTab('quests')}
        >
          ⚡ Quests & Scavenger Hunt
        </button>
        <button
          className={`tab-btn ${activeTab === 'badges' ? 'active' : ''}`}
          onClick={() => setActiveTab('badges')}
        >
          🎖️ Badges ({profile?.badges?.length || 0}/7)
        </button>
        {user && (
          <button
            className={`tab-btn ${activeTab === 'history' ? 'active' : ''}`}
            onClick={() => setActiveTab('history')}
          >
            📜 XP Activity History
          </button>
        )}
      </div>

      {/* Tab 1: Live Leaderboard */}
      {activeTab === 'leaderboard' && (
        <div className="tab-pane leaderboard-pane">
          {leaderboard.length === 0 ? (
            <div className="empty-state">
              <span className="empty-icon">🏆</span>
              <h4>No players on the leaderboard yet</h4>
              <p>Be the first to check in and earn XP!</p>
            </div>
          ) : (
            <div className="leaderboard-table-card">
              <div className="table-header-row">
                <span className="col-rank">RANK</span>
                <span className="col-attendee">ATTENDEE</span>
                <span className="col-tier">TIER</span>
                <span className="col-badges">BADGES</span>
                <span className="col-xp">TOTAL XP</span>
              </div>
              <div className="table-body">
                {leaderboard.map((entry) => {
                  const isCurrentUser = user && entry.userId === user.id;
                  const rankIcon = entry.rank === 1 ? '🥇' : entry.rank === 2 ? '🥈' : entry.rank === 3 ? '🥉' : `#${entry.rank}`;
                  return (
                    <div
                      key={entry.userId}
                      className={`table-row ${isCurrentUser ? 'current-user-row' : ''} ${entry.rank <= 3 ? 'podium-row' : ''}`}
                    >
                      <span className="col-rank">
                        <span className={`rank-chip rank-${entry.rank <= 3 ? entry.rank : 'other'}`}>
                          {rankIcon}
                        </span>
                      </span>
                      <div className="col-attendee attendee-info-cell">
                        <div className="attendee-avatar-sm">
                          {entry.userName ? entry.userName[0].toUpperCase() : 'U'}
                        </div>
                        <div className="attendee-text">
                          <span className="name-bold">
                            {entry.userName} {isCurrentUser && <span className="you-badge">(You)</span>}
                          </span>
                          <span className="email-sub">{entry.company || 'Attendee'}</span>
                        </div>
                      </div>
                      <span className="col-tier">
                        <span className="tier-pill" style={{ color: getTierColor(entry.currentTier), borderColor: `${getTierColor(entry.currentTier)}44` }}>
                          {entry.currentTier}
                        </span>
                      </span>
                      <span className="col-badges">
                        <span className="badge-counter">🎖️ {entry.badgesCount || 0}</span>
                      </span>
                      <span className="col-xp">
                        <span className="xp-highlight">{entry.totalXp} XP</span>
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Tab 2: Quests & Scavenger Hunt */}
      {activeTab === 'quests' && (
        <div className="tab-pane quests-pane">
          <div className="quests-grid">
            {/* Booth Scavenger Hunt */}
            <div className="quest-card">
              <div className="quest-header">
                <span className="quest-type-badge">EXPO FLOOR</span>
                <span className="quest-xp">+75 XP each</span>
              </div>
              <h3 className="quest-title">🔍 Sponsor Booth Passport</h3>
              <p className="quest-desc">
                Visit exhibition booths across the expo hall. Scan their digital passport QR to earn points and network.
              </p>

              <div className="quest-targets-list">
                {sponsors.length === 0 ? (
                  <div className="no-targets">No booths registered yet for this event.</div>
                ) : (
                  sponsors.map((sp) => (
                    <div key={sp.id} className="target-item">
                      <div className="target-info">
                        <strong>{sp.name}</strong>
                        <span className="target-sub">{sp.tier} Booth {sp.boothNumber ? `(${sp.boothNumber})` : ''}</span>
                      </div>
                      <button
                        className="btn-scan"
                        onClick={() => handleScanBooth(sp.id, sp.name)}
                        disabled={scanning}
                      >
                        Scan Booth
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Session Check-ins */}
            <div className="quest-card">
              <div className="quest-header">
                <span className="quest-type-badge">AGENDA</span>
                <span className="quest-xp">+150 XP each</span>
              </div>
              <h3 className="quest-title">🎓 Breakout Session Check-in</h3>
              <p className="quest-desc">
                Attend keynote talks, workshops, and panels. Check in at the room entrance to log credits.
              </p>

              <div className="quest-targets-list">
                {sessions.length === 0 ? (
                  <div className="no-targets">No active breakout sessions found.</div>
                ) : (
                  sessions.slice(0, 5).map((s) => (
                    <div key={s.id} className="target-item">
                      <div className="target-info">
                        <strong>{s.title}</strong>
                        <span className="target-sub">{s.room || 'Main Hall'}</span>
                      </div>
                      <button
                        className="btn-scan"
                        onClick={() => handleCheckInSession(s.id, s.title)}
                        disabled={scanning}
                      >
                        Check In
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Interactive Community Actions */}
            <div className="quest-card">
              <div className="quest-header">
                <span className="quest-type-badge">ENGAGEMENT</span>
                <span className="quest-xp">Variable XP</span>
              </div>
              <h3 className="quest-title">💬 Live Interaction Missions</h3>
              <p className="quest-desc">
                Participate in Q&A sessions, live audience polls, and schedule 1-on-1 networking meetings.
              </p>

              <div className="quest-targets-list">
                <div className="target-item">
                  <div className="target-info">
                    <strong>Ask a Q&A Question</strong>
                    <span className="target-sub">+30 XP in Session Chat</span>
                  </div>
                  <button
                    className="btn-scan secondary"
                    onClick={() => handleSimulateAction('QA_QUESTION', 'Submitted question in session Q&A', 30)}
                  >
                    Simulate
                  </button>
                </div>

                <div className="target-item">
                  <div className="target-info">
                    <strong>Cast Live Poll Vote</strong>
                    <span className="target-sub">+25 XP in Audience Cockpit</span>
                  </div>
                  <button
                    className="btn-scan secondary"
                    onClick={() => handleSimulateAction('POLL_VOTE', 'Voted in live audience poll', 25)}
                  >
                    Simulate
                  </button>
                </div>

                <div className="target-item">
                  <div className="target-info">
                    <strong>Complete 1-on-1 Meeting</strong>
                    <span className="target-sub">+120 XP in B2B Hub</span>
                  </div>
                  <button
                    className="btn-scan secondary"
                    onClick={() => handleSimulateAction('NETWORKING_MEETING', 'Completed 1-on-1 networking meeting', 120)}
                  >
                    Simulate
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tab 3: Badges */}
      {activeTab === 'badges' && (
        <div className="tab-pane badges-pane">
          <div className="badges-grid">
            {allMilestoneBadges.map((badge) => {
              const isEarned = earnedBadgeKeys.has(badge.key);
              return (
                <div key={badge.key} className={`badge-card ${isEarned ? 'earned' : 'locked'}`}>
                  <div className="badge-icon-box">{badge.icon}</div>
                  <h4 className="badge-name">{badge.name}</h4>
                  <p className="badge-desc">{badge.desc}</p>
                  <div className="badge-status">
                    {isEarned ? (
                      <span className="status-earned">✓ UNLOCKED</span>
                    ) : (
                      <span className="status-locked">🔒 LOCKED</span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Tab 4: History */}
      {activeTab === 'history' && (
        <div className="tab-pane history-pane">
          {!profile?.recentActivities || profile.recentActivities.length === 0 ? (
            <div className="empty-state">
              <span className="empty-icon">📜</span>
              <h4>No activity recorded yet</h4>
              <p>Complete quests or check in to start building your event timeline.</p>
            </div>
          ) : (
            <div className="activity-timeline">
              {profile.recentActivities.map((act) => (
                <div key={act.id} className="timeline-item">
                  <div className="timeline-dot"></div>
                  <div className="timeline-card">
                    <div className="timeline-header">
                      <span className="act-type">{act.activityType}</span>
                      <span className="act-xp">+{act.xpEarned} XP</span>
                    </div>
                    <div className="act-desc">{act.description}</div>
                    <div className="act-time">
                      {act.createdAt ? new Date(act.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Recently'}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
