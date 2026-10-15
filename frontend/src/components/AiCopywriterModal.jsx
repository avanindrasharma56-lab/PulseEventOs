import React, { useState } from 'react';
import {
  Sparkles,
  Bot,
  Copy,
  Check,
  Send,
  FileText,
  Mail,
  Share2,
  Hash,
  ArrowRight,
  X,
  RefreshCw,
  Loader2,
  ChevronRight,
  Layers,
  Flame,
  CheckCircle2
} from 'lucide-react';
import { aiService } from '../services/api';
import './AiCopywriterModal.css';

const TONE_OPTIONS = [
  { value: 'Visionary & Innovative', label: 'Visionary & Inspiring', desc: 'Forward-looking, cutting-edge, tech pioneers' },
  { value: 'Professional & Corporate', label: 'Executive & Corporate', desc: 'Authoritative, enterprise B2B, strategic' },
  { value: 'High-Energy & Exciting', label: 'High-Energy & Buzzworthy', desc: 'Festivals, summits, dynamic excitement' },
  { value: 'Deeply Technical & Hands-On', label: 'Technical & Practical', desc: 'Engineers, architects, code-level workshops' }
];

export default function AiCopywriterModal({
  isOpen,
  onClose,
  initialTopic = '',
  initialCategory = 'Technology & AI',
  onApplyCopy
}) {
  const [topic, setTopic] = useState(initialTopic);
  const [category, setCategory] = useState(initialCategory || 'Technology & AI');
  const [targetAudience, setTargetAudience] = useState('');
  const [tone, setTone] = useState('Visionary & Innovative');
  const [keyHighlights, setKeyHighlights] = useState('');

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  const [activeTab, setActiveTab] = useState('TITLES'); // 'TITLES' | 'SUMMARY' | 'EMAIL' | 'SOCIAL'
  const [selectedTitleIdx, setSelectedTitleIdx] = useState(0);
  const [copiedField, setCopiedField] = useState(null);

  if (!isOpen) return null;

  const handleGenerate = async (e) => {
    if (e) e.preventDefault();
    if (!topic.trim()) {
      setError('Please provide an event topic or core theme.');
      return;
    }

    setIsLoading(true);
    setError('');
    try {
      const highlightsArray = keyHighlights
        ? keyHighlights.split(',').map(s => s.trim()).filter(Boolean)
        : [];

      const response = await aiService.generateEventCopy({
        topic: topic.trim(),
        category: category || 'Technology',
        targetAudience: targetAudience.trim() || 'Industry Professionals & Developers',
        tone: tone,
        keyHighlights: highlightsArray
      });

      setResult(response);
      setSelectedTitleIdx(0);
      setActiveTab('TITLES');
    } catch (err) {
      console.error('AI generation error:', err);
      setError(err.message || 'Failed to generate copy. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const copyToClipboard = (text, fieldName) => {
    if (!text) return;
    navigator.clipboard.writeText(text);
    setCopiedField(fieldName);
    setTimeout(() => {
      setCopiedField(null);
    }, 2000);
  };

  const handleApplyToDraft = () => {
    if (!result) return;
    const chosenTitle = result.titleSuggestions?.[selectedTitleIdx] || topic;
    onApplyCopy({
      title: chosenTitle,
      shortSummary: result.shortSummary || '',
      description: result.description || ''
    });
    onClose();
  };

  return (
    <div className="ai-copywriter-backdrop" onClick={onClose}>
      <div className="ai-copywriter-dialog" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="ai-modal-header">
          <div className="ai-header-brand">
            <div className="ai-icon-pulse">
              <Bot size={22} className="ai-bot-icon" />
              <Sparkles size={12} className="ai-sparkle-sub" />
            </div>
            <div>
              <div className="ai-title-row">
                <h3>Pulse AI Copywriter & Marketing Assistant</h3>
                <span className="ai-pill-engine">Powered by Gemini</span>
              </div>
              <p className="ai-subtitle">
                Synthesize conference branding, search-engine slugs, multi-section descriptions, email blasts, and viral social campaigns in seconds.
              </p>
            </div>
          </div>
          <button className="btn-ai-close" onClick={onClose} aria-label="Close modal">
            <X size={18} />
          </button>
        </div>

        <div className="ai-modal-body">
          {/* Left Column: Input Form */}
          <div className="ai-input-column">
            <form onSubmit={handleGenerate} className="ai-form">
              <div className="form-group">
                <label>
                  Event Topic / Core Concept <span className="req">*</span>
                </label>
                <input
                  type="text"
                  placeholder="e.g. Autonomous AI Agents & Cloud Robotics Summit 2026"
                  value={topic}
                  onChange={(e) => setTopic(e.target.value)}
                  required
                />
              </div>

              <div className="form-row-2">
                <div className="form-group">
                  <label>Event Category</label>
                  <input
                    type="text"
                    placeholder="e.g. Technology, AI, Web3, Music"
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                  />
                </div>

                <div className="form-group">
                  <label>Tone of Voice</label>
                  <select value={tone} onChange={(e) => setTone(e.target.value)}>
                    {TONE_OPTIONS.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="form-group">
                <label>Target Audience</label>
                <input
                  type="text"
                  placeholder="e.g. Robotics Engineers, AI Researchers, Cloud Architects"
                  value={targetAudience}
                  onChange={(e) => setTargetAudience(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label>Key Highlights & Hooks (comma-separated)</label>
                <input
                  type="text"
                  placeholder="e.g. Spatial intelligence, Real-time edge compute, VIP founder dinner"
                  value={keyHighlights}
                  onChange={(e) => setKeyHighlights(e.target.value)}
                />
              </div>

              {error && (
                <div className="ai-error-box">
                  <span>{error}</span>
                </div>
              )}

              <button
                type="submit"
                className="btn-ai-generate"
                disabled={isLoading}
              >
                {isLoading ? (
                  <>
                    <Loader2 size={16} className="spin" />
                    <span>Synthesizing Copy...</span>
                  </>
                ) : (
                  <>
                    <Sparkles size={16} />
                    <span>{result ? 'Regenerate Marketing Suite' : 'Generate Marketing Suite'}</span>
                  </>
                )}
              </button>
            </form>
          </div>

          {/* Right Column: Results Showcase */}
          <div className="ai-results-column">
            {!result && !isLoading && (
              <div className="ai-results-placeholder">
                <div className="ai-placeholder-art">
                  <Sparkles size={36} className="placeholder-icon" />
                </div>
                <h4>Zero-Latency Marketing Generator</h4>
                <p>
                  Fill out the parameters on the left and click generate. Gemini AI will create headline alternatives, search slugs, short teasers, full agendas, email copy, and social announcement posts.
                </p>
              </div>
            )}

            {isLoading && (
              <div className="ai-loading-state">
                <div className="ai-loading-spinner-box">
                  <Loader2 size={36} className="spin ai-main-spin" />
                  <Bot size={20} className="ai-center-icon" />
                </div>
                <h4>Synthesizing Marketing Intelligence</h4>
                <p>Gemini is orchestrating high-converting headlines, email copy, and engagement hooks...</p>
              </div>
            )}

            {result && !isLoading && (
              <div className="ai-results-wrapper">
                {/* Tabs Header */}
                <div className="ai-tabs-nav">
                  <button
                    className={`ai-tab-btn ${activeTab === 'TITLES' ? 'active' : ''}`}
                    onClick={() => setActiveTab('TITLES')}
                  >
                    <Flame size={14} />
                    <span>Headlines ({result.titleSuggestions?.length || 0})</span>
                  </button>
                  <button
                    className={`ai-tab-btn ${activeTab === 'SUMMARY' ? 'active' : ''}`}
                    onClick={() => setActiveTab('SUMMARY')}
                  >
                    <FileText size={14} />
                    <span>Summary & Details</span>
                  </button>
                  <button
                    className={`ai-tab-btn ${activeTab === 'EMAIL' ? 'active' : ''}`}
                    onClick={() => setActiveTab('EMAIL')}
                  >
                    <Mail size={14} />
                    <span>Email Blast</span>
                  </button>
                  <button
                    className={`ai-tab-btn ${activeTab === 'SOCIAL' ? 'active' : ''}`}
                    onClick={() => setActiveTab('SOCIAL')}
                  >
                    <Share2 size={14} />
                    <span>Social Media</span>
                  </button>
                </div>

                {/* Tab 1: Titles & Slug */}
                {activeTab === 'TITLES' && (
                  <div className="ai-tab-pane">
                    <div className="pane-section">
                      <label className="pane-label">Recommended Headline Options (Select One to Apply):</label>
                      <div className="titles-options-list">
                        {result.titleSuggestions?.map((t, idx) => (
                          <div
                            key={idx}
                            className={`title-option-card ${selectedTitleIdx === idx ? 'selected' : ''}`}
                            onClick={() => setSelectedTitleIdx(idx)}
                          >
                            <div className="title-radio-circle">
                              {selectedTitleIdx === idx && <div className="inner-dot" />}
                            </div>
                            <div className="title-text-content">
                              <span className="title-variant-tag">Option #{idx + 1}</span>
                              <p className="title-string">{t}</p>
                            </div>
                            <button
                              type="button"
                              className="btn-card-copy"
                              onClick={(e) => {
                                e.stopPropagation();
                                copyToClipboard(t, `title-${idx}`);
                              }}
                              title="Copy title"
                            >
                              {copiedField === `title-${idx}` ? <Check size={14} className="copied" /> : <Copy size={14} />}
                            </button>
                          </div>
                        ))}
                      </div>
                    </div>

                    {result.suggestedSlug && (
                      <div className="slug-preview-box">
                        <label className="pane-label">Recommended URL Slug</label>
                        <div className="slug-line">
                          <code>eventflow.io/events/{result.suggestedSlug}</code>
                          <button
                            type="button"
                            className="btn-card-copy"
                            onClick={() => copyToClipboard(result.suggestedSlug, 'slug')}
                          >
                            {copiedField === 'slug' ? <Check size={14} className="copied" /> : <Copy size={14} />}
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                )}

                {/* Tab 2: Summary & Description */}
                {activeTab === 'SUMMARY' && (
                  <div className="ai-tab-pane">
                    <div className="pane-section">
                      <div className="section-header-row">
                        <label className="pane-label">Card Hook / Short Summary</label>
                        <button
                          type="button"
                          className="btn-copy-mini"
                          onClick={() => copyToClipboard(result.shortSummary, 'shortSummary')}
                        >
                          {copiedField === 'shortSummary' ? <Check size={13} /> : <Copy size={13} />}
                          <span>{copiedField === 'shortSummary' ? 'Copied' : 'Copy'}</span>
                        </button>
                      </div>
                      <div className="preview-block-summary">
                        <p>{result.shortSummary}</p>
                      </div>
                    </div>

                    <div className="pane-section">
                      <div className="section-header-row">
                        <label className="pane-label">Full Multi-Section Event Description</label>
                        <button
                          type="button"
                          className="btn-copy-mini"
                          onClick={() => copyToClipboard(result.description, 'description')}
                        >
                          {copiedField === 'description' ? <Check size={13} /> : <Copy size={13} />}
                          <span>{copiedField === 'description' ? 'Copied' : 'Copy'}</span>
                        </button>
                      </div>
                      <div className="preview-block-desc">
                        <pre>{result.description}</pre>
                      </div>
                    </div>
                  </div>
                )}

                {/* Tab 3: Email Campaign */}
                {activeTab === 'EMAIL' && (
                  <div className="ai-tab-pane">
                    <div className="pane-section">
                      <div className="section-header-row">
                        <label className="pane-label">Email Invitation Blast (Ready to Send)</label>
                        <button
                          type="button"
                          className="btn-copy-mini"
                          onClick={() => copyToClipboard(result.emailMarketingCopy, 'email')}
                        >
                          {copiedField === 'email' ? <Check size={13} /> : <Copy size={13} />}
                          <span>{copiedField === 'email' ? 'Copied to Clipboard' : 'Copy Email Copy'}</span>
                        </button>
                      </div>
                      <div className="email-preview-container">
                        <pre className="email-body-text">{result.emailMarketingCopy}</pre>
                      </div>
                    </div>
                  </div>
                )}

                {/* Tab 4: Social Media */}
                {activeTab === 'SOCIAL' && (
                  <div className="ai-tab-pane">
                    <div className="pane-section">
                      <div className="section-header-row">
                        <label className="pane-label">Social Media Announcement Post</label>
                        <button
                          type="button"
                          className="btn-copy-mini"
                          onClick={() => copyToClipboard(result.socialMediaPost, 'social')}
                        >
                          {copiedField === 'social' ? <Check size={13} /> : <Copy size={13} />}
                          <span>{copiedField === 'social' ? 'Copied' : 'Copy Post'}</span>
                        </button>
                      </div>
                      <div className="social-preview-container">
                        <pre className="social-post-text">{result.socialMediaPost}</pre>
                      </div>
                    </div>

                    {result.hashtags && result.hashtags.length > 0 && (
                      <div className="pane-section">
                        <label className="pane-label">Generated Hashtags</label>
                        <div className="hashtags-list">
                          {result.hashtags.map((tag, idx) => (
                            <span key={idx} className="hashtag-chip">
                              <Hash size={11} /> {tag.replace(/^#/, '')}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}

                {/* Footer Action Bar */}
                <div className="ai-results-footer">
                  <div className="footer-info">
                    <CheckCircle2 size={15} className="success-icon" />
                    <span>Selected: <strong>Option #{selectedTitleIdx + 1}</strong></span>
                  </div>
                  <div className="footer-actions">
                    <button
                      type="button"
                      className="btn-apply-draft"
                      onClick={handleApplyToDraft}
                    >
                      <Sparkles size={15} />
                      <span>Apply to Event Draft Form</span>
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
