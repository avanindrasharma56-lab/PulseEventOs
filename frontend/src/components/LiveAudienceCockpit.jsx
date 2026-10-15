import React, { useState, useEffect, useRef } from 'react';
import { liveEngagementService, eventService, tokenStorage } from '../services/api';
import './LiveAudienceCockpit.css';

export default function LiveAudienceCockpit({ eventId: propEventId }) {
  const [events, setEvents] = useState([]);
  const [selectedEventId, setSelectedEventId] = useState(propEventId || null);
  const [activeTab, setActiveTab] = useState('qa'); // 'qa' | 'polls'

  // Q&A State
  const [questions, setQuestions] = useState([]);
  const [newQuestionText, setNewQuestionText] = useState('');
  const [isAnonymous, setIsAnonymous] = useState(false);
  const [submittingQ, setSubmittingQ] = useState(false);

  // Polls State
  const [polls, setPolls] = useState([]);
  const [newPollQuestion, setNewPollQuestion] = useState('');
  const [newPollOptions, setNewPollOptions] = useState(['', '']);
  const [creatingPoll, setCreatingPoll] = useState(false);
  const [showPollBuilder, setShowPollBuilder] = useState(false);

  // Connection & Auth
  const [socketConnected, setSocketConnected] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);
  const wsRef = useRef(null);

  const currentUser = tokenStorage.getUser();
  const isOrganizerOrAdmin = currentUser && currentUser.roles && (
    currentUser.roles.includes('ROLE_ORGANIZER') ||
    currentUser.roles.includes('ROLE_ADMIN') ||
    currentUser.roles.includes('ORGANIZER') ||
    currentUser.roles.includes('ADMIN')
  );

  // 1. Load available events if none supplied
  useEffect(() => {
    async function loadEvents() {
      try {
        const res = await eventService.searchEvents({ size: 20 });
        const list = res.content || [];
        setEvents(list);
        if (!selectedEventId && list.length > 0) {
          setSelectedEventId(list[0].id);
        }
      } catch (err) {
        console.error('Failed to load events:', err);
      }
    }
    loadEvents();
  }, []);

  // 2. Fetch Initial Q&A and Polls Data
  useEffect(() => {
    if (!selectedEventId) return;

    let isMounted = true;
    async function fetchData() {
      try {
        setErrorMsg(null);
        const [qaData, pollsData] = await Promise.all([
          liveEngagementService.getQuestions(selectedEventId),
          liveEngagementService.getPolls(selectedEventId),
        ]);
        if (isMounted) {
          setQuestions(qaData);
          setPolls(pollsData);
        }
      } catch (err) {
        if (isMounted) {
          setErrorMsg('Failed to load live audience engagement data.');
        }
      }
    }

    fetchData();
    return () => { isMounted = false; };
  }, [selectedEventId]);

  // 3. Raw STOMP over WebSocket connection
  useEffect(() => {
    if (!selectedEventId) return;

    let ws;
    try {
      const wsUrl = (window.location.protocol === 'https:' ? 'wss://' : 'ws://') +
        (window.location.hostname || 'localhost') + ':8085/ws-eventflow/websocket';

      ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        ws.send('CONNECT\naccept-version:1.2,1.1,1.0\nheart-beat:10000,10000\n\n\0');
      };

      ws.onmessage = (event) => {
        const msg = event.data;
        if (msg.includes('CONNECTED')) {
          setSocketConnected(true);
          // Subscribe to QA and Polls topics for this event
          ws.send(`SUBSCRIBE\nid:sub-qa-${selectedEventId}\ndestination:/topic/events/${selectedEventId}/qa\n\n\0`);
          ws.send(`SUBSCRIBE\nid:sub-polls-${selectedEventId}\ndestination:/topic/events/${selectedEventId}/polls\n\n\0`);
        } else if (msg.includes('MESSAGE')) {
          const bodyIndex = msg.indexOf('\n\n');
          if (bodyIndex !== -1) {
            const rawJson = msg.substring(bodyIndex + 2).replace(/\0$/, '').trim();
            try {
              const payload = JSON.parse(rawJson);

              // Check if payload is a QuestionDto
              if (payload.questionText !== undefined && payload.upvotes !== undefined) {
                setQuestions((prev) => {
                  if (payload.status === 'DISMISSED') {
                    return prev.filter((q) => q.id !== payload.id);
                  }
                  const exists = prev.some((q) => q.id === payload.id);
                  let updated = exists
                    ? prev.map((q) => (q.id === payload.id ? { ...payload, userHasUpvoted: q.userHasUpvoted } : q))
                    : [payload, ...prev];

                  // Sort: PINNED first, then by upvotes desc
                  return updated.sort((a, b) => {
                    if (a.status === 'PINNED' && b.status !== 'PINNED') return -1;
                    if (b.status === 'PINNED' && a.status !== 'PINNED') return 1;
                    return (b.upvotes || 0) - (a.upvotes || 0);
                  });
                });
              }
              // Check if payload is a PollDto
              else if (payload.options !== undefined && payload.totalVotes !== undefined) {
                setPolls((prev) => {
                  const exists = prev.some((p) => p.id === payload.id);
                  if (exists) {
                    return prev.map((p) => (p.id === payload.id ? { ...payload, userVotedOptionId: p.userVotedOptionId, hasVoted: p.hasVoted } : p));
                  }
                  return [payload, ...prev];
                });
              }
            } catch {
              // Ignore non-json frames
            }
          }
        }
      };

      ws.onclose = () => setSocketConnected(false);
      ws.onerror = () => setSocketConnected(false);
    } catch (err) {
      console.warn('WebSocket init warning:', err);
    }

    return () => {
      if (ws) ws.close();
    };
  }, [selectedEventId]);

  // --- Q&A Handlers ---

  const handleAskQuestion = async (e) => {
    e.preventDefault();
    if (!newQuestionText.trim() || !selectedEventId) return;

    if (!currentUser) {
      alert('Please log in to submit a question to the live stage.');
      return;
    }

    setSubmittingQ(true);
    try {
      const created = await liveEngagementService.submitQuestion(selectedEventId, {
        questionText: newQuestionText.trim(),
        anonymous: isAnonymous,
      });
      setNewQuestionText('');
      setQuestions((prev) => [created, ...prev.filter((q) => q.id !== created.id)]);
    } catch (err) {
      alert(err.message || 'Failed to submit question.');
    } finally {
      setSubmittingQ(false);
    }
  };

  const handleToggleUpvote = async (questionId) => {
    if (!currentUser) {
      alert('Please log in to upvote questions.');
      return;
    }

    try {
      // Optimistic update
      setQuestions((prev) =>
        prev.map((q) => {
          if (q.id === questionId) {
            const nextUpvoted = !q.userHasUpvoted;
            return {
              ...q,
              userHasUpvoted: nextUpvoted,
              upvotes: nextUpvoted ? q.upvotes + 1 : Math.max(0, q.upvotes - 1),
            };
          }
          return q;
        })
      );

      const updated = await liveEngagementService.toggleUpvote(questionId);
      setQuestions((prev) =>
        prev.map((q) => (q.id === questionId ? updated : q))
      );
    } catch (err) {
      console.error('Failed to toggle upvote:', err);
    }
  };

  const handleUpdateQuestionStatus = async (questionId, status) => {
    try {
      const updated = await liveEngagementService.updateQuestionStatus(questionId, status);
      setQuestions((prev) => {
        if (status === 'DISMISSED') {
          return prev.filter((q) => q.id !== questionId);
        }
        return prev.map((q) => (q.id === questionId ? updated : q));
      });
    } catch (err) {
      alert(err.message || 'Failed to update question status.');
    }
  };

  // --- Polls Handlers ---

  const handleAddPollOption = () => {
    if (newPollOptions.length < 6) {
      setNewPollOptions([...newPollOptions, '']);
    }
  };

  const handleRemovePollOption = (idx) => {
    if (newPollOptions.length > 2) {
      setNewPollOptions(newPollOptions.filter((_, i) => i !== idx));
    }
  };

  const handlePollOptionChange = (idx, val) => {
    const updated = [...newPollOptions];
    updated[idx] = val;
    setNewPollOptions(updated);
  };

  const handleCreatePoll = async (e) => {
    e.preventDefault();
    const validOptions = newPollOptions.map((o) => o.trim()).filter(Boolean);
    if (!newPollQuestion.trim() || validOptions.length < 2) {
      alert('Please provide a question and at least 2 options.');
      return;
    }

    setCreatingPoll(true);
    try {
      const created = await liveEngagementService.createPoll(selectedEventId, {
        question: newPollQuestion.trim(),
        options: validOptions,
      });
      setPolls([created, ...polls]);
      setNewPollQuestion('');
      setNewPollOptions(['', '']);
      setShowPollBuilder(false);
    } catch (err) {
      alert(err.message || 'Failed to create poll.');
    } finally {
      setCreatingPoll(false);
    }
  };

  const handleVoteInPoll = async (pollId, optionId) => {
    if (!currentUser) {
      alert('Please log in to vote in live polls.');
      return;
    }

    try {
      const updated = await liveEngagementService.voteInPoll(pollId, optionId);
      setPolls((prev) => prev.map((p) => (p.id === pollId ? updated : p)));
    } catch (err) {
      alert(err.message || 'Failed to cast vote.');
    }
  };

  const handleTogglePollStatus = async (pollId, currentStatus) => {
    const nextStatus = currentStatus === 'ACTIVE' ? 'CLOSED' : 'ACTIVE';
    try {
      const updated = await liveEngagementService.updatePollStatus(pollId, nextStatus);
      setPolls((prev) => prev.map((p) => (p.id === pollId ? updated : p)));
    } catch (err) {
      alert(err.message || 'Failed to change poll status.');
    }
  };

  const pinnedQuestion = questions.find((q) => q.status === 'PINNED');

  return (
    <div className="live-audience-cockpit">
      {/* Top Header */}
      <div className="cockpit-header">
        <div className="cockpit-title-area">
          <h1>Live Stage Interactive Cockpit</h1>
          <p className="cockpit-subtitle">
            Real-time audience Q&A with upvoting and live synchronized polling engine
          </p>
        </div>

        <div className="cockpit-controls">
          {events.length > 0 && (
            <select
              className="event-picker-select"
              value={selectedEventId || ''}
              onChange={(e) => setSelectedEventId(Number(e.target.value))}
            >
              {events.map((evt) => (
                <option key={evt.id} value={evt.id}>
                  {evt.title}
                </option>
              ))}
            </select>
          )}

          <div className={`ws-status-badge ${socketConnected ? 'connected' : 'disconnected'}`}>
            <span className="ws-dot" />
            <span>{socketConnected ? 'Live Real-time Synced' : 'Connecting Broker...'}</span>
          </div>
        </div>
      </div>

      {errorMsg && <div className="error-alert">{errorMsg}</div>}

      {/* Tabs */}
      <div className="cockpit-nav-tabs">
        <button
          className={`tab-btn ${activeTab === 'qa' ? 'active' : ''}`}
          onClick={() => setActiveTab('qa')}
        >
          <span>Live Q&A Stream</span>
          <span className="tab-badge">{questions.length}</span>
        </button>

        <button
          className={`tab-btn ${activeTab === 'polls' ? 'active' : ''}`}
          onClick={() => setActiveTab('polls')}
        >
          <span>Audience Polls</span>
          <span className="tab-badge">{polls.length}</span>
        </button>
      </div>

      {/* TAB 1: Q&A */}
      {activeTab === 'qa' && (
        <div className="qa-container">
          {/* Ask Question Form */}
          <div className="ask-question-box">
            <h3>Ask the Speakers & Stage</h3>
            <form onSubmit={handleAskQuestion}>
              <textarea
                className="ask-textarea"
                placeholder="What would you like to ask the speaker or panel? (Markdown supported)"
                value={newQuestionText}
                onChange={(e) => setNewQuestionText(e.target.value)}
                maxLength={500}
                required
              />
              <div className="ask-footer">
                <label className="anon-checkbox-label">
                  <input
                    type="checkbox"
                    checked={isAnonymous}
                    onChange={(e) => setIsAnonymous(e.target.checked)}
                  />
                  <span>Post Anonymously</span>
                </label>

                <button
                  type="submit"
                  className="btn-ask-submit"
                  disabled={submittingQ || !newQuestionText.trim()}
                >
                  {submittingQ ? 'Broadcasting...' : 'Ask Question'}
                </button>
              </div>
            </form>
          </div>

          {/* Pinned Question Highlight Banner */}
          {pinnedQuestion && (
            <div className="pinned-banner">
              <div className="pinned-header">
                <span>Currently Answering On Stage</span>
              </div>
              <p>"{pinnedQuestion.questionText}"</p>
              <div className="pinned-author">
                Asked by {pinnedQuestion.authorName} - {pinnedQuestion.upvotes} upvotes
              </div>
            </div>
          )}

          {/* Questions Stream */}
          <div className="questions-stream">
            {questions.length === 0 ? (
              <div className="empty-state-card">
                <h3>No questions asked yet</h3>
                <p>Be the first to ask a question to the speakers on stage!</p>
              </div>
            ) : (
              questions.map((q) => (
                <div
                  key={q.id}
                  className={`question-card status-${q.status}`}
                >
                  {/* Upvote Column */}
                  <div className="upvote-btn-col">
                    <button
                      className={`upvote-btn ${q.userHasUpvoted ? 'upvoted' : ''}`}
                      onClick={() => handleToggleUpvote(q.id)}
                      title={q.userHasUpvoted ? 'Remove upvote' : 'Upvote question'}
                    >
                      <span className="upvote-icon">▲</span>
                      <span>{q.upvotes || 0}</span>
                    </button>
                  </div>

                  {/* Question Content */}
                  <div className="question-content">
                    <div className="question-meta">
                      <span className="question-author">{q.authorName}</span>
                      <span className="question-badge badge-${q.status}">
                        {q.status}
                      </span>
                      {q.createdAt && (
                        <span className="question-time">
                          {new Date(q.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      )}
                    </div>

                    <p className="question-text">{q.questionText}</p>

                    {/* Organizer / Admin Moderation Controls */}
                    {isOrganizerOrAdmin && (
                      <div className="moderator-actions">
                        {q.status !== 'PINNED' && (
                          <button
                            className="mod-btn pin-btn"
                            onClick={() => handleUpdateQuestionStatus(q.id, 'PINNED')}
                          >
                            Pin on Stage
                          </button>
                        )}
                        {q.status === 'PINNED' && (
                          <button
                            className="mod-btn"
                            onClick={() => handleUpdateQuestionStatus(q.id, 'OPEN')}
                          >
                            Unpin
                          </button>
                        )}
                        {q.status !== 'ANSWERED' && (
                          <button
                            className="mod-btn ans-btn"
                            onClick={() => handleUpdateQuestionStatus(q.id, 'ANSWERED')}
                          >
                            Mark Answered
                          </button>
                        )}
                        <button
                          className="mod-btn dismiss-btn"
                          onClick={() => handleUpdateQuestionStatus(q.id, 'DISMISSED')}
                        >
                          Dismiss
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* TAB 2: AUDIENCE POLLS */}
      {activeTab === 'polls' && (
        <div className="polls-container">
          {/* Organizer Poll Builder */}
          {isOrganizerOrAdmin && (
            <div className="create-poll-card">
              {!showPollBuilder ? (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <h3 style={{ margin: 0 }}>Live Audience Poll Engine</h3>
                    <p style={{ margin: '0.4rem 0 0 0', color: '#94a3b8', fontSize: '0.9rem' }}>
                      Engage the audience with real-time synchronized voting and bar charts.
                    </p>
                  </div>
                  <button
                    className="btn-broadcast-poll"
                    onClick={() => setShowPollBuilder(true)}
                  >
                    + Create New Poll
                  </button>
                </div>
              ) : (
                <form onSubmit={handleCreatePoll}>
                  <h3>Create & Broadcast Live Audience Poll</h3>
                  <input
                    type="text"
                    className="poll-input"
                    placeholder="Enter poll question (e.g., Which topic should we deep-dive into next?)"
                    value={newPollQuestion}
                    onChange={(e) => setNewPollQuestion(e.target.value)}
                    required
                  />

                  <label className="options-builder-label">Poll Options (2 - 6)</label>
                  {newPollOptions.map((opt, idx) => (
                    <div key={idx} className="option-builder-row">
                      <input
                        type="text"
                        className="poll-input"
                        style={{ marginBottom: 0 }}
                        placeholder={`Option ${idx + 1}`}
                        value={opt}
                        onChange={(e) => handlePollOptionChange(idx, e.target.value)}
                        required
                      />
                      {newPollOptions.length > 2 && (
                        <button
                          type="button"
                          className="btn-remove-opt"
                          onClick={() => handleRemovePollOption(idx)}
                          title="Remove option"
                        >
                          ✕
                        </button>
                      )}
                    </div>
                  ))}

                  <div className="poll-builder-actions">
                    <button
                      type="button"
                      className="btn-add-opt"
                      onClick={handleAddPollOption}
                      disabled={newPollOptions.length >= 6}
                    >
                      + Add Option
                    </button>

                    <div style={{ display: 'flex', gap: '0.8rem' }}>
                      <button
                        type="button"
                        className="btn-add-opt"
                        onClick={() => setShowPollBuilder(false)}
                      >
                        Cancel
                      </button>
                      <button
                        type="submit"
                        className="btn-broadcast-poll"
                        disabled={creatingPoll || !newPollQuestion.trim()}
                      >
                        {creatingPoll ? 'Launching...' : 'Broadcast Poll to Audience'}
                      </button>
                    </div>
                  </div>
                </form>
              )}
            </div>
          )}

          {/* Polls Feed */}
          {polls.length === 0 ? (
            <div className="empty-state-card">
              <h3>No polls active</h3>
              <p>Check back when the speakers start an interactive audience vote!</p>
            </div>
          ) : (
            polls.map((poll) => {
              const isClosed = poll.status === 'CLOSED';
              return (
                <div key={poll.id} className="poll-card">
                  <div className="poll-card-header">
                    <h3 className="poll-question">{poll.question}</h3>
                    <span className={`poll-status-pill poll-status-${poll.status}`}>
                      {poll.status}
                    </span>
                  </div>

                  {/* Options with Animated Bars */}
                  <div className="poll-options-list">
                    {poll.options.map((option) => {
                      const isSelected = poll.userVotedOptionId === option.id;
                      const pct = Math.round(option.percentage || 0);

                      return (
                        <div
                          key={option.id}
                          className={`poll-option-row ${isSelected ? 'voted-by-me' : ''} ${isClosed ? 'poll-closed' : ''}`}
                          onClick={() => !isClosed && handleVoteInPoll(poll.id, option.id)}
                          title={isClosed ? 'Poll is closed' : 'Click to vote'}
                        >
                          {/* Animated Progress Bar Fill */}
                          <div
                            className="poll-bar-fill"
                            style={{ width: `${pct}%` }}
                          />

                          <div className="poll-option-content">
                            <div className="option-text-wrap">
                              {isSelected && <span className="vote-check-mark">✓</span>}
                              <span>{option.optionText}</span>
                            </div>

                            <div className="option-stats-wrap">
                              <span className="option-vote-count">
                                {option.voteCount} {option.voteCount === 1 ? 'vote' : 'votes'}
                              </span>
                              <span className="option-percentage">{pct}%</span>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  <div className="poll-card-footer">
                    <span className="poll-total-votes">
                      Total Audience Votes: {poll.totalVotes || 0}
                    </span>

                    {/* Organizer Status Toggle */}
                    {isOrganizerOrAdmin && (
                      <div className="poll-organizer-actions">
                        <button
                          className="mod-btn"
                          onClick={() => handleTogglePollStatus(poll.id, poll.status)}
                        >
                          {isClosed ? 'Reopen Poll' : 'Close Poll'}
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}
    </div>
  );
}
