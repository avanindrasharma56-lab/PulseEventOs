import React, { useState, useEffect } from 'react';
import {
  Award,
  Crown,
  ExternalLink,
  MapPin,
  Plus,
  Send,
  Users,
  X,
  FileSpreadsheet,
  Star,
  CheckCircle2,
  Building,
} from 'lucide-react';
import { sponsorService, ticketPassService } from '../services/api';
import './SponsorFloorDirectory.css';

export default function SponsorFloorDirectory({
  eventId,
  eventTitle,
  isOrganizer = false,
  isAdmin = false,
  isAuthenticated = false,
  onOpenAuth,
}) {
  const [sponsors, setSponsors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [myTickets, setMyTickets] = useState([]);

  // Modals
  const [activeLeadSponsor, setActiveLeadSponsor] = useState(null); // Sponsor to manage leads for
  const [leadsList, setLeadsList] = useState([]);
  const [loadingLeads, setLoadingLeads] = useState(false);

  // Drop Digital Card Modal
  const [dropCardSponsor, setDropCardSponsor] = useState(null);
  const [selectedTicketCode, setSelectedTicketCode] = useState('');
  const [leadNotes, setLeadNotes] = useState('');
  const [leadRating, setLeadRating] = useState(5);
  const [submittingLead, setSubmittingLead] = useState(false);
  const [dropSuccess, setDropSuccess] = useState(false);

  // Add Sponsor Modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [newSponsor, setNewSponsor] = useState({
    name: '',
    tier: 'GOLD',
    boothNumber: '',
    websiteUrl: '',
    description: '',
  });

  const loadSponsors = async () => {
    if (!eventId) return;
    setLoading(true);
    try {
      const list = await sponsorService.getEventSponsors(eventId);
      setSponsors(list);
    } catch (err) {
      console.error('Failed to load event sponsors:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSponsors();
    if (isAuthenticated) {
      ticketPassService.getMyTickets()
        .then((res) => {
          const valid = (res || []).filter(t => t.eventId === eventId || true);
          setMyTickets(valid);
          if (valid.length > 0) {
            setSelectedTicketCode(valid[0].ticketCode);
          }
        })
        .catch(() => {});
    }
  }, [eventId, isAuthenticated]);

  // View Leads
  const handleOpenLeads = async (sponsor) => {
    setActiveLeadSponsor(sponsor);
    setLoadingLeads(true);
    try {
      const leads = await sponsorService.getSponsorLeads(sponsor.id);
      setLeadsList(leads);
    } catch (err) {
      alert('Failed to load exhibitor leads: ' + err.message);
    } finally {
      setLoadingLeads(false);
    }
  };

  // Export CSV
  const handleExportCsv = async (sponsorId) => {
    try {
      await sponsorService.exportLeadsCsv(sponsorId, `sponsor-${sponsorId}-leads.csv`);
    } catch (err) {
      alert('Failed to export leads CSV: ' + err.message);
    }
  };

  // Submit Digital Card / Lead
  const handleDropCard = async (e) => {
    e.preventDefault();
    if (!selectedTicketCode || submittingLead) return;

    setSubmittingLead(true);
    try {
      await sponsorService.scanLead(dropCardSponsor.id, {
        ticketCode: selectedTicketCode,
        notes: leadNotes || 'Digital business card exchanged at booth.',
        rating: leadRating,
      });
      setDropSuccess(true);
      setTimeout(() => {
        setDropSuccess(false);
        setDropCardSponsor(null);
        setLeadNotes('');
      }, 1500);
    } catch (err) {
      alert('Failed to share digital card: ' + err.message);
    } finally {
      setSubmittingLead(false);
    }
  };

  // Create Sponsor
  const handleCreateSponsor = async (e) => {
    e.preventDefault();
    try {
      await sponsorService.createSponsor(eventId, newSponsor);
      setShowAddModal(false);
      setNewSponsor({
        name: '',
        tier: 'GOLD',
        boothNumber: '',
        websiteUrl: '',
        description: '',
      });
      loadSponsors();
    } catch (err) {
      alert('Failed to create sponsor: ' + err.message);
    }
  };

  const titleSponsors = sponsors.filter(s => s.tier === 'TITLE');
  const platSponsors = sponsors.filter(s => s.tier === 'PLATINUM');
  const goldSponsors = sponsors.filter(s => s.tier === 'GOLD');
  const otherSponsors = sponsors.filter(s => s.tier === 'SILVER' || s.tier === 'BRONZE');

  return (
    <section className="expo-floor-section">
      {/* Header */}
      <div className="expo-header">
        <div className="expo-title-group">
          <div className="expo-icon-badge">
            <Award size={26} />
          </div>
          <div>
            <h2>Exhibitor Expo Floor & Sponsors</h2>
            <p>
              Explore partner technology booths at {eventTitle || 'the summit'}, connect with architects, and exchange digital business cards.
            </p>
          </div>
        </div>

        <div className="expo-actions-row">
          {(isOrganizer || isAdmin) && (
            <button className="btn-add-sponsor" onClick={() => setShowAddModal(true)}>
              <Plus size={16} />
              <span>Register Booth Partner</span>
            </button>
          )}
        </div>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: '#94a3b8' }}>
          Loading expo floor and partner booths...
        </div>
      ) : sponsors.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>
          No sponsors or exhibitor booths listed yet for this conference.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
          {/* TITLE SPONSORS */}
          {titleSponsors.length > 0 && (
            <div className="tier-group-container">
              <h3 className="tier-section-title title-tier">
                <Crown size={18} />
                Title Conference Partners
              </h3>
              <div className="sponsors-grid title-grid">
                {titleSponsors.map(sp => (
                  <SponsorCard
                    key={sp.id}
                    sponsor={sp}
                    isTitle
                    isAuthenticated={isAuthenticated}
                    isOrganizer={isOrganizer || isAdmin}
                    onDropCard={() => setDropCardSponsor(sp)}
                    onViewLeads={() => handleOpenLeads(sp)}
                    onOpenAuth={onOpenAuth}
                  />
                ))}
              </div>
            </div>
          )}

          {/* PLATINUM SPONSORS */}
          {platSponsors.length > 0 && (
            <div className="tier-group-container">
              <h3 className="tier-section-title platinum-tier">
                <Award size={18} />
                Platinum Exhibitors
              </h3>
              <div className="sponsors-grid">
                {platSponsors.map(sp => (
                  <SponsorCard
                    key={sp.id}
                    sponsor={sp}
                    isAuthenticated={isAuthenticated}
                    isOrganizer={isOrganizer || isAdmin}
                    onDropCard={() => setDropCardSponsor(sp)}
                    onViewLeads={() => handleOpenLeads(sp)}
                    onOpenAuth={onOpenAuth}
                  />
                ))}
              </div>
            </div>
          )}

          {/* GOLD & OTHER */}
          {(goldSponsors.length > 0 || otherSponsors.length > 0) && (
            <div className="tier-group-container">
              <h3 className="tier-section-title gold-tier">
                <Building size={18} />
                Gold & Supporting Exhibitors
              </h3>
              <div className="sponsors-grid">
                {[...goldSponsors, ...otherSponsors].map(sp => (
                  <SponsorCard
                    key={sp.id}
                    sponsor={sp}
                    isAuthenticated={isAuthenticated}
                    isOrganizer={isOrganizer || isAdmin}
                    onDropCard={() => setDropCardSponsor(sp)}
                    onViewLeads={() => handleOpenLeads(sp)}
                    onOpenAuth={onOpenAuth}
                  />
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Drop Digital Card Modal */}
      {dropCardSponsor && (
        <div className="leads-modal-backdrop" onClick={() => setDropCardSponsor(null)}>
          <div className="leads-modal-container" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '480px' }}>
            <div className="leads-modal-header">
              <h3 style={{ margin: 0, fontSize: '1.1rem', color: '#f8fafc' }}>
                Exchange Digital Business Card
              </h3>
              <button
                onClick={() => setDropCardSponsor(null)}
                style={{ background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer' }}
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleDropCard} style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <p style={{ margin: 0, fontSize: '0.85rem', color: '#94a3b8' }}>
                Share your verified attendee credentials with <strong>{dropCardSponsor.name}</strong> ({dropCardSponsor.boothNumber}).
              </p>

              {dropSuccess ? (
                <div style={{ padding: '1rem', background: 'rgba(16, 185, 129, 0.2)', border: '1px solid #10b981', borderRadius: '12px', color: '#34d399', textAlign: 'center', fontWeight: 600 }}>
                  <CheckCircle2 size={24} style={{ display: 'block', margin: '0 auto 0.5rem' }} />
                  Digital Card Exchanged Successfully!
                </div>
              ) : (
                <>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', marginBottom: '0.4rem' }}>
                      Select Admission Pass
                    </label>
                    {myTickets.length > 0 ? (
                      <select
                        value={selectedTicketCode}
                        onChange={(e) => setSelectedTicketCode(e.target.value)}
                        style={{ width: '100%', background: '#090d16', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: '10px', padding: '0.6rem', color: '#f8fafc', fontSize: '0.85rem' }}
                      >
                        {myTickets.map(t => (
                          <option key={t.ticketCode} value={t.ticketCode}>
                            {t.ticketCode} — {t.attendeeName} ({t.tierName})
                          </option>
                        ))}
                      </select>
                    ) : (
                      <input
                        type="text"
                        placeholder="Enter your ticket code (e.g. TCK-XXXX)..."
                        value={selectedTicketCode}
                        onChange={(e) => setSelectedTicketCode(e.target.value)}
                        style={{ width: '100%', background: '#090d16', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: '10px', padding: '0.6rem', color: '#f8fafc', fontSize: '0.85rem' }}
                      />
                    )}
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', marginBottom: '0.4rem' }}>
                      Intent / Project Inquiry Note
                    </label>
                    <textarea
                      rows={3}
                      placeholder="e.g., Interested in private cloud deployment, evaluate in Q1..."
                      value={leadNotes}
                      onChange={(e) => setLeadNotes(e.target.value)}
                      style={{ width: '100%', background: '#090d16', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: '10px', padding: '0.6rem', color: '#f8fafc', fontSize: '0.85rem' }}
                    />
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', marginBottom: '0.4rem' }}>
                      Buying Intent Level: {leadRating} / 5 Stars
                    </label>
                    <input
                      type="range"
                      min={1}
                      max={5}
                      value={leadRating}
                      onChange={(e) => setLeadRating(Number(e.target.value))}
                      style={{ width: '100%' }}
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={submittingLead || !selectedTicketCode}
                    style={{
                      background: 'linear-gradient(135deg, #10b981, #059669)',
                      border: 'none',
                      borderRadius: '12px',
                      padding: '0.75rem',
                      color: '#ffffff',
                      fontWeight: 700,
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '0.5rem',
                    }}
                  >
                    <Send size={16} />
                    <span>{submittingLead ? 'Exchanging...' : 'Submit Digital Card'}</span>
                  </button>
                </>
              )}
            </form>
          </div>
        </div>
      )}

      {/* Exhibitor Leads Modal */}
      {activeLeadSponsor && (
        <div className="leads-modal-backdrop" onClick={() => setActiveLeadSponsor(null)}>
          <div className="leads-modal-container" onClick={(e) => e.stopPropagation()}>
            <div className="leads-modal-header">
              <div>
                <h3 style={{ margin: 0, fontSize: '1.2rem', color: '#f8fafc' }}>
                  Captured Leads: {activeLeadSponsor.name}
                </h3>
                <span style={{ fontSize: '0.8rem', color: '#38bdf8' }}>
                  {activeLeadSponsor.boothNumber} • Total {leadsList.length} Qualified Contacts
                </span>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <button
                  className="btn-add-sponsor"
                  style={{ background: 'linear-gradient(135deg, #10b981, #059669)' }}
                  onClick={() => handleExportCsv(activeLeadSponsor.id)}
                >
                  <FileSpreadsheet size={16} />
                  <span>Export CSV</span>
                </button>
                <button
                  onClick={() => setActiveLeadSponsor(null)}
                  style={{ background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer' }}
                >
                  <X size={18} />
                </button>
              </div>
            </div>

            <div className="leads-modal-body">
              {loadingLeads ? (
                <div style={{ textAlign: 'center', padding: '2rem', color: '#94a3b8' }}>Loading leads...</div>
              ) : leadsList.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                  No leads captured yet at this booth. Attendees can scan or drop their cards.
                </div>
              ) : (
                <table className="leads-table">
                  <thead>
                    <tr>
                      <th>Attendee</th>
                      <th>Ticket Code</th>
                      <th>Rating</th>
                      <th>Notes</th>
                      <th>Scanned At</th>
                    </tr>
                  </thead>
                  <tbody>
                    {leadsList.map((lead) => (
                      <tr key={lead.id}>
                        <td>
                          <strong>{lead.attendeeName}</strong>
                          <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>{lead.attendeeEmail}</div>
                        </td>
                        <td><code>{lead.ticketCode}</code></td>
                        <td>
                          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.2rem', color: '#fbbf24', fontWeight: 700 }}>
                            <Star size={12} fill="#fbbf24" /> {lead.rating}/5
                          </span>
                        </td>
                        <td style={{ fontSize: '0.8rem', color: '#cbd5e1' }}>{lead.notes || '—'}</td>
                        <td style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                          {lead.scannedAt ? new Date(lead.scannedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Add Sponsor Modal */}
      {showAddModal && (
        <div className="leads-modal-backdrop" onClick={() => setShowAddModal(false)}>
          <div className="leads-modal-container" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '520px' }}>
            <div className="leads-modal-header">
              <h3 style={{ margin: 0, fontSize: '1.1rem', color: '#f8fafc' }}>
                Register Sponsor / Exhibitor Booth
              </h3>
              <button
                onClick={() => setShowAddModal(false)}
                style={{ background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer' }}
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleCreateSponsor} style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', marginBottom: '0.4rem' }}>
                  Company Name
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. HyperScale Cloud Inc"
                  value={newSponsor.name}
                  onChange={(e) => setNewSponsor({ ...newSponsor, name: e.target.value })}
                  style={{ width: '100%', background: '#090d16', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: '10px', padding: '0.6rem', color: '#f8fafc', fontSize: '0.85rem' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', marginBottom: '0.4rem' }}>
                  Sponsorship Tier
                </label>
                <select
                  value={newSponsor.tier}
                  onChange={(e) => setNewSponsor({ ...newSponsor, tier: e.target.value })}
                  style={{ width: '100%', background: '#090d16', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: '10px', padding: '0.6rem', color: '#f8fafc', fontSize: '0.85rem' }}
                >
                  <option value="TITLE">TITLE PARTNER</option>
                  <option value="PLATINUM">PLATINUM</option>
                  <option value="GOLD">GOLD</option>
                  <option value="SILVER">SILVER</option>
                  <option value="BRONZE">BRONZE</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', marginBottom: '0.4rem' }}>
                  Booth Number & Hall
                </label>
                <input
                  type="text"
                  placeholder="e.g. Booth #T-02 (Grand Hallway)"
                  value={newSponsor.boothNumber}
                  onChange={(e) => setNewSponsor({ ...newSponsor, boothNumber: e.target.value })}
                  style={{ width: '100%', background: '#090d16', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: '10px', padding: '0.6rem', color: '#f8fafc', fontSize: '0.85rem' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', marginBottom: '0.4rem' }}>
                  Website URL
                </label>
                <input
                  type="url"
                  placeholder="https://..."
                  value={newSponsor.websiteUrl}
                  onChange={(e) => setNewSponsor({ ...newSponsor, websiteUrl: e.target.value })}
                  style={{ width: '100%', background: '#090d16', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: '10px', padding: '0.6rem', color: '#f8fafc', fontSize: '0.85rem' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.75rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', marginBottom: '0.4rem' }}>
                  Overview & Tech Stack
                </label>
                <textarea
                  rows={2}
                  placeholder="Short description of offerings..."
                  value={newSponsor.description}
                  onChange={(e) => setNewSponsor({ ...newSponsor, description: e.target.value })}
                  style={{ width: '100%', background: '#090d16', border: '1px solid rgba(255, 255, 255, 0.15)', borderRadius: '10px', padding: '0.6rem', color: '#f8fafc', fontSize: '0.85rem' }}
                />
              </div>

              <button
                type="submit"
                style={{
                  background: 'linear-gradient(135deg, #6366f1, #4f46e5)',
                  border: 'none',
                  borderRadius: '12px',
                  padding: '0.75rem',
                  color: '#ffffff',
                  fontWeight: 700,
                  cursor: 'pointer',
                }}
              >
                Add Sponsor
              </button>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}

function SponsorCard({
  sponsor,
  isTitle,
  isAuthenticated,
  isOrganizer,
  onDropCard,
  onViewLeads,
  onOpenAuth,
}) {
  const getBadgeClass = (tier) => {
    switch (tier) {
      case 'TITLE': return 'badge-title';
      case 'PLATINUM': return 'badge-platinum';
      case 'GOLD': return 'badge-gold';
      default: return 'badge-silver';
    }
  };

  return (
    <div className={`sponsor-card ${isTitle ? 'is-title' : ''}`}>
      <div className="sponsor-card-top">
        <div className="sponsor-meta-col">
          <span className={`sponsor-tier-badge ${getBadgeClass(sponsor.tier)}`}>
            {sponsor.tier === 'TITLE' && <Crown size={12} />}
            {sponsor.tier} PARTNER
          </span>
          <h4 className="sponsor-name">{sponsor.name}</h4>
        </div>

        {sponsor.boothNumber && (
          <span className="sponsor-booth-pill">
            <MapPin size={13} />
            {sponsor.boothNumber}
          </span>
        )}
      </div>

      <p className="sponsor-desc">
        {sponsor.description || 'Global industry innovator and event partner.'}
      </p>

      <div className="sponsor-card-footer">
        {sponsor.websiteUrl ? (
          <a
            href={sponsor.websiteUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="btn-visit-site"
          >
            <span>Visit Booth Site</span>
            <ExternalLink size={12} />
          </a>
        ) : <span />}

        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          {isOrganizer && (
            <button className="btn-manage-leads" onClick={onViewLeads} title="View captured leads">
              <Users size={13} />
              <span>Leads</span>
            </button>
          )}

          <button
            className="btn-share-lead"
            onClick={() => {
              if (!isAuthenticated) {
                onOpenAuth && onOpenAuth('signin');
              } else {
                onDropCard();
              }
            }}
          >
            <Send size={12} />
            <span>Drop Card</span>
          </button>
        </div>
      </div>
    </div>
  );
}
