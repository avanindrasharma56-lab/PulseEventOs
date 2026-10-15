import React, { useState, useEffect } from 'react';
import { 
  Plus, 
  Sparkles, 
  Send, 
  Globe, 
  Edit3, 
  AlertTriangle, 
  CheckCircle, 
  Clock, 
  Calendar,
  MapPin,
  X,
  Layers,
  ArrowRight,
  Tag,
  Percent,
  Ticket,
  Loader2,
  RotateCcw,
  Users,
  CheckCircle2,
  DollarSign,
  RefreshCw,
  Trash2,
  Bot
} from 'lucide-react';
import { eventService, categoryService, venueService, promoCodeService, refundService, waitlistService, ticketTierService, pricingService } from '../services/api';
import './OrganizerEventStudio.css';

const SAMPLE_PRESETS = [
  {
    title: 'Pulse Next: Autonomous AI Developers Conference',
    shortSummary: 'The definitive gathering for foundation models, AI agents, and developer tooling in 2026.',
    description: 'Join industry pioneers and AI research teams for 3 days of intensive deep dives into autonomous agent frameworks, localized small language models, multimodal inference systems, and real-time GPU orchestration.\n\nFeaturing keynote sessions, live coding architectures, and exclusive networking sessions.',
    bannerImageUrl: 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=1200&q=80',
    daysFromNow: 14,
    durationHours: 36,
  },
  {
    title: 'Obsidian Sound & Light Electronic Music Festival',
    shortSummary: 'A state-of-the-art audiovisual live performance featuring global electronic music artists.',
    description: 'Immerse yourself in laser mapping, 360-degree spatial audio, and back-to-back live sets from award-winning electronic musicians.\n\nOutdoor stages equipped with cutting-edge visual projection and gourmet culinary pavilions.',
    bannerImageUrl: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=1200&q=80',
    daysFromNow: 28,
    durationHours: 12,
  },
  {
    title: 'Global Tech Founder & Angel Investor Mixer',
    shortSummary: 'Connect with tier-1 venture capitalists, tech startup founders, and enterprise angels.',
    description: 'Curated 1-on-1 pitch sessions, fireside chats with unicorn founders, and private networking suites.\n\nCatered executive dining and investor deal rooms available throughout the day.',
    bannerImageUrl: 'https://images.unsplash.com/photo-1515187029135-18ee286d815b?auto=format&fit=crop&w=1200&q=80',
    daysFromNow: 21,
    durationHours: 6,
  }
];

export default function OrganizerEventStudio({ onEventPublished }) {
  const [events, setEvents] = useState([]);
  const [categories, setCategories] = useState([]);
  const [venues, setVenues] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState(null);
  const [formError, setFormError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Form Fields
  const [title, setTitle] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [venueId, setVenueId] = useState('');
  const [shortSummary, setShortSummary] = useState('');
  const [description, setDescription] = useState('');
  const [bannerImageUrl, setBannerImageUrl] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [featured, setFeatured] = useState(false);

  // Promo Code Management State
  const [isPromoModalOpen, setIsPromoModalOpen] = useState(false);
  const [promoCodes, setPromoCodes] = useState([]);
  const [isLoadingPromos, setIsLoadingPromos] = useState(false);
  const [promoFormCode, setPromoFormCode] = useState('');
  const [promoFormType, setPromoFormType] = useState('PERCENTAGE');
  const [promoFormValue, setPromoFormValue] = useState('');
  const [promoFormLimit, setPromoFormLimit] = useState('100');
  const [promoFormEventId, setPromoFormEventId] = useState('');
  const [promoFormMinTotal, setPromoFormMinTotal] = useState('');
  const [promoCreateError, setPromoCreateError] = useState('');
  const [promoCreateSuccess, setPromoCreateSuccess] = useState('');
  const [isCreatingPromo, setIsCreatingPromo] = useState(false);

  // Custom Location / Venue State
  const [isVenueModalOpen, setIsVenueModalOpen] = useState(false);
  const [newVenueName, setNewVenueName] = useState('');
  const [newVenueCity, setNewVenueCity] = useState('');
  const [newVenueAddress, setNewVenueAddress] = useState('');
  const [newVenueCapacity, setNewVenueCapacity] = useState('500');
  const [newVenueIsOnline, setNewVenueIsOnline] = useState(false);
  const [isSavingVenue, setIsSavingVenue] = useState(false);
  const [venueError, setVenueError] = useState('');

  const handleSaveVenue = async (e) => {
    e.preventDefault();
    if (!newVenueName.trim()) {
      setVenueError('Location name is required');
      return;
    }
    setIsSavingVenue(true);
    setVenueError('');
    try {
      const created = await venueService.createVenue({
        name: newVenueName.trim(),
        city: newVenueCity.trim() || null,
        address: newVenueAddress.trim() || null,
        capacity: newVenueCapacity ? parseInt(newVenueCapacity, 10) : 500,
        online: newVenueIsOnline,
      });
      const freshVenues = await venueService.getAllVenues();
      setVenues(freshVenues);
      const targetId = created?.id ? created.id.toString() : (freshVenues[freshVenues.length - 1]?.id?.toString() || '');
      setVenueId(targetId);
      setIsVenueModalOpen(false);
      setNewVenueName('');
      setNewVenueCity('');
      setNewVenueAddress('');
      setNewVenueCapacity('500');
      setNewVenueIsOnline(false);
    } catch (err) {
      setVenueError(err.message || 'Failed to create venue');
    } finally {
      setIsSavingVenue(false);
    }
  };

  // Ticket Tier & Pricing Management State
  const [isTierModalOpen, setIsTierModalOpen] = useState(false);
  const [tierEvent, setTierEvent] = useState(null);
  const [tiersList, setTiersList] = useState([]);
  const [isLoadingTiers, setIsLoadingTiers] = useState(false);
  const [tierName, setTierName] = useState('General Admission');
  const [tierPrice, setTierPrice] = useState('25.00');
  const [tierCapacity, setTierCapacity] = useState('100');
  const [tierDescription, setTierDescription] = useState('');
  const [isSavingTier, setIsSavingTier] = useState(false);
  const [tierError, setTierError] = useState('');
  const [tierSuccess, setTierSuccess] = useState('');

  // Currency adaptation state
  const [currency, setCurrency] = useState(localStorage.getItem('preferredCurrency') || 'USD');
  const [currenciesList, setCurrenciesList] = useState([]);
  const [currencyRate, setCurrencyRate] = useState({ symbol: '$', rateToUsd: 1.0 });
  const [promoCurrency, setPromoCurrency] = useState('USD');

  useEffect(() => {
    pricingService.getCurrencies().then(currencies => {
      if (Array.isArray(currencies) && currencies.length > 0) {
        setCurrenciesList(currencies);
      }
      const match = currencies.find(c => c.currencyCode === currency);
      if (match) {
        setCurrencyRate({ symbol: match.symbol || '$', rateToUsd: match.rateToUsd || 1.0 });
      }
    }).catch(() => {});
  }, [currency]);

  useEffect(() => {
    setPromoCurrency(currency);
  }, [currency]);

  useEffect(() => {
    const handleCurrChange = (e) => {
      if (e.detail) {
        setCurrency(e.detail);
        setPromoCurrency(e.detail);
      }
    };
    window.addEventListener('currencyChanged', handleCurrChange);
    return () => window.removeEventListener('currencyChanged', handleCurrChange);
  }, []);

  const openTierModal = async (event) => {
    setTierEvent(event);
    setTierName('General Admission');
    setTierPrice('25.00');
    setTierCapacity('100');
    setTierDescription('');
    setTierError('');
    setTierSuccess('');
    setIsTierModalOpen(true);
    await loadTiers(event.id);
  };

  const loadTiers = async (eventId) => {
    setIsLoadingTiers(true);
    try {
      const data = await ticketTierService.getTiersForEvent(eventId);
      setTiersList(data || []);
    } catch (err) {
      console.error('Failed to load ticket tiers', err);
    } finally {
      setIsLoadingTiers(false);
    }
  };

  const handleSaveTier = async (e) => {
    e.preventDefault();
    if (!tierName.trim()) {
      setTierError('Ticket tier name is required (e.g. General Admission, VIP Pass)');
      return;
    }
    if (tierPrice === '' || isNaN(tierPrice) || parseFloat(tierPrice) < 0) {
      setTierError('Please enter a valid price (0 or higher for free events)');
      return;
    }
    if (!tierCapacity || isNaN(tierCapacity) || parseInt(tierCapacity, 10) < 1) {
      setTierError('Please enter a valid capacity (minimum 1 ticket)');
      return;
    }

    setIsSavingTier(true);
    setTierError('');
    setTierSuccess('');
    try {
      const inputVal = parseFloat(tierPrice);
      const rateToUsd = currencyRate.rateToUsd || 1.0;
      // Convert to base USD for database storage: baseUsd = inputVal / rateToUsd
      const priceInUsd = inputVal > 0 ? parseFloat((inputVal / rateToUsd).toFixed(2)) : 0;

      await ticketTierService.createTier(tierEvent.id, {
        name: tierName.trim(),
        price: priceInUsd,
        capacity: parseInt(tierCapacity, 10),
        description: tierDescription.trim() || null,
      });
      setTierSuccess(`Ticket tier "${tierName.trim()}" added at ${currencyRate.symbol || '$'} ${inputVal.toFixed(2)}!`);
      setTierName('');
      setTierPrice('0.00');
      setTierCapacity('50');
      setTierDescription('');
      await loadTiers(tierEvent.id);
    } catch (err) {
      setTierError(err.message || 'Failed to create ticket tier');
    } finally {
      setIsSavingTier(false);
    }
  };

  const handleDeleteTier = async (tierId, tierTitle) => {
    if (!window.confirm(`Are you sure you want to delete ticket tier "${tierTitle}"?`)) return;
    try {
      await ticketTierService.deleteTier(tierEvent.id, tierId);
      await loadTiers(tierEvent.id);
    } catch (err) {
      alert(err.message || 'Failed to delete ticket tier');
    }
  };

  // Refunds & Waitlists Studio State
  const [isRefundModalOpen, setIsRefundModalOpen] = useState(false);
  const [refundTab, setRefundTab] = useState('REFUNDS'); // 'REFUNDS' | 'WAITLISTS'
  const [refundsList, setRefundsList] = useState([]);
  const [refundsFilter, setRefundsFilter] = useState('ALL'); // 'ALL' | 'PENDING' | 'PROCESSED'
  const [isLoadingRefunds, setIsLoadingRefunds] = useState(false);
  const [reviewNotes, setReviewNotes] = useState({});
  const [isReviewingRefund, setIsReviewingRefund] = useState({});
  const [activeWaitlistEventId, setActiveWaitlistEventId] = useState('');
  const [eventTiers, setEventTiers] = useState([]);
  const [tierWaitlists, setTierWaitlists] = useState({});
  const [isLoadingWaitlists, setIsLoadingWaitlists] = useState(false);

  const loadRefunds = async () => {
    setIsLoadingRefunds(true);
    try {
      const data = await refundService.getAllRefunds();
      setRefundsList(data);
    } catch (err) {
      console.error('Failed to load refunds', err);
    } finally {
      setIsLoadingRefunds(false);
    }
  };

  const handleReviewRefund = async (refundId, approved) => {
    setIsReviewingRefund(prev => ({ ...prev, [refundId]: true }));
    try {
      await refundService.reviewRefund(refundId, {
        approved,
        adminNotes: reviewNotes[refundId] || (approved ? 'Approved by organizer' : 'Rejected per policy')
      });
      await loadRefunds();
      await loadData();
      if (activeWaitlistEventId) {
        await loadEventTiersAndWaitlist(activeWaitlistEventId);
      }
    } catch (err) {
      alert(err.message || 'Failed to review refund request');
    } finally {
      setIsReviewingRefund(prev => ({ ...prev, [refundId]: false }));
    }
  };

  const loadEventTiersAndWaitlist = async (evId) => {
    if (!evId) return;
    setActiveWaitlistEventId(evId);
    setIsLoadingWaitlists(true);
    try {
      const tiers = await ticketTierService.getTiersForEvent(evId);
      setEventTiers(tiers);
      const waitlistMap = {};
      await Promise.all(
        tiers.map(async (t) => {
          try {
            const list = await waitlistService.getTierWaitlist(t.id);
            waitlistMap[t.id] = list;
          } catch {
            waitlistMap[t.id] = [];
          }
        })
      );
      setTierWaitlists(waitlistMap);
    } catch (err) {
      console.error('Failed to load event tiers waitlist', err);
    } finally {
      setIsLoadingWaitlists(false);
    }
  };

  const loadPromoCodes = async () => {
    setIsLoadingPromos(true);
    try {
      const data = await promoCodeService.getAllPromoCodes();
      setPromoCodes(data);
    } catch (err) {
      console.error('Failed to load promo codes', err);
    } finally {
      setIsLoadingPromos(false);
    }
  };

  const handleCreatePromoCode = async (e) => {
    e.preventDefault();
    if (!promoFormCode.trim() || !promoFormValue) {
      setPromoCreateError('Promo code and discount value are required');
      return;
    }
    setIsCreatingPromo(true);
    setPromoCreateError('');
    setPromoCreateSuccess('');
    try {
      // Find the conversion rate for the selected currency
      const selectedCurrObj = currenciesList.find(c => c.currencyCode === promoCurrency);
      const rateToUsd = selectedCurrObj ? (selectedCurrObj.rateToUsd || 1.0) : 1.0;
      const currSymbol = selectedCurrObj?.symbol || '$';

      const rawVal = parseFloat(promoFormValue);
      // If FIXED_AMOUNT, normalize back to base USD so it works across all currencies
      const normalizedDiscountValue = promoFormType === 'FIXED_AMOUNT' ? (rawVal / rateToUsd) : rawVal;
      const normalizedMinTotal = promoFormMinTotal ? (parseFloat(promoFormMinTotal) / rateToUsd) : null;

      await promoCodeService.createPromoCode({
        code: promoFormCode.trim().toUpperCase(),
        discountType: promoFormType,
        discountValue: normalizedDiscountValue,
        usageLimit: promoFormLimit ? parseInt(promoFormLimit) : null,
        eventId: promoFormEventId ? parseInt(promoFormEventId) : null,
        minOrderAmount: normalizedMinTotal,
        active: true,
      });

      const displayDesc = promoFormType === 'PERCENTAGE'
        ? `${rawVal}% OFF`
        : `${currSymbol} ${rawVal.toFixed(2)} OFF`;

      setPromoCreateSuccess(`Promo code ${promoFormCode.trim().toUpperCase()} (${displayDesc}) created successfully!`);
      setPromoFormCode('');
      setPromoFormValue('');
      await loadPromoCodes();
    } catch (err) {
      setPromoCreateError(err.message || 'Failed to create promo code');
    } finally {
      setIsCreatingPromo(false);
    }
  };

  const loadData = async () => {
    setIsLoading(true);
    try {
      const [catsRes, vensRes] = await Promise.allSettled([
        categoryService.getActiveCategories(),
        venueService.getAllVenues(),
      ]);
      const cats = (catsRes.status === 'fulfilled' && Array.isArray(catsRes.value)) ? catsRes.value : [];
      const vens = (vensRes.status === 'fulfilled' && Array.isArray(vensRes.value)) ? vensRes.value : [];
      setCategories(cats);
      setVenues(vens);
      if (cats.length > 0 && !categoryId) setCategoryId(cats[0].id.toString());
      if (vens.length > 0 && !venueId) setVenueId(vens[0].id.toString());

      try {
        const evts = await eventService.getMyEvents();
        setEvents(Array.isArray(evts) ? evts : []);
      } catch (err) {
        console.warn('Could not load organizer events', err);
        setEvents([]);
      }
    } catch (err) {
      console.error('Failed to load studio data', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const openCreateModal = () => {
    setEditingEvent(null);
    setTitle('');
    setShortSummary('');
    setDescription('');
    setBannerImageUrl('');
    setFeatured(false);
    setFormError('');

    const now = new Date();
    const start = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);
    const end = new Date(start.getTime() + 8 * 60 * 60 * 1000);
    setStartTime(start.toISOString().slice(0, 16));
    setEndTime(end.toISOString().slice(0, 16));

    if (categories.length > 0) setCategoryId(categories[0].id.toString());
    if (venues.length > 0) setVenueId(venues[0].id.toString());

    setIsModalOpen(true);
  };

  const openEditModal = (event) => {
    setEditingEvent(event);
    setTitle(event.title || '');
    setCategoryId(event.category?.id?.toString() || (categories[0]?.id?.toString() || ''));
    setVenueId(event.venue?.id?.toString() || (venues[0]?.id?.toString() || ''));
    setShortSummary(event.shortSummary || '');
    setDescription(event.description || '');
    setBannerImageUrl(event.bannerImageUrl || '');
    setStartTime(event.startTime ? event.startTime.slice(0, 16) : '');
    setEndTime(event.endTime ? event.endTime.slice(0, 16) : '');
    setFeatured(!!event.featured);
    setFormError('');
    setIsModalOpen(true);
  };

  const applyPreset = (preset) => {
    setTitle(preset.title);
    setShortSummary(preset.shortSummary);
    setDescription(preset.description);
    setBannerImageUrl(preset.bannerImageUrl);

    const now = new Date();
    const start = new Date(now.getTime() + preset.daysFromNow * 24 * 60 * 60 * 1000);
    const end = new Date(start.getTime() + preset.durationHours * 60 * 60 * 1000);
    setStartTime(start.toISOString().slice(0, 16));
    setEndTime(end.toISOString().slice(0, 16));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');

    if (!title.trim()) {
      setFormError('Event title is required');
      return;
    }
    if (!categoryId) {
      setFormError('Please select a category');
      return;
    }
    if (!startTime || !endTime) {
      setFormError('Start time and End time must be set');
      return;
    }
    if (new Date(startTime) >= new Date(endTime)) {
      setFormError('Start time must be strictly before End time');
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        title: title.trim(),
        categoryId: parseInt(categoryId),
        venueId: venueId ? parseInt(venueId) : null,
        shortSummary: shortSummary.trim(),
        description: description.trim(),
        bannerImageUrl: bannerImageUrl.trim() || null,
        startTime: new Date(startTime).toISOString(),
        endTime: new Date(endTime).toISOString(),
        featured,
      };

      if (editingEvent) {
        await eventService.updateEvent(editingEvent.id, payload);
      } else {
        await eventService.createDraft(payload);
      }

      setIsModalOpen(false);
      await loadData();
    } catch (err) {
      setFormError(err.message || 'Failed to save event');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSubmitForApproval = async (eventId) => {
    try {
      await eventService.submitForApproval(eventId);
      await loadData();
    } catch (err) {
      alert(err.message || 'Failed to submit event for approval');
    }
  };

  const handlePublish = async (eventId) => {
    try {
      await eventService.publishEvent(eventId);
      await loadData();
      if (onEventPublished) onEventPublished();
    } catch (err) {
      alert(err.message || 'Failed to publish event');
    }
  };

  const handleCancel = async (eventId) => {
    if (!window.confirm('Are you sure you want to cancel this event?')) return;
    try {
      await eventService.cancelEvent(eventId);
      await loadData();
    } catch (err) {
      alert(err.message || 'Failed to cancel event');
    }
  };

  const handleDeleteEvent = async (eventId, title) => {
    if (!window.confirm(`Are you sure you want to permanently delete "${title || 'this event'}" and all its records? This cannot be undone.`)) return;
    try {
      await eventService.deleteEvent(eventId);
      await loadData();
    } catch (err) {
      alert(err.message || 'Failed to delete event');
    }
  };

  return (
    <div className="organizer-studio">
      {/* Studio Header */}
      <div className="studio-header">
        <div>
          <h2>Event Organizer Studio</h2>
          <p>Create drafts, manage schedules, submit events for moderation, and publish approved summits.</p>
        </div>
        <div className="header-actions-row">
          <button
            className="btn-manage-refunds"
            onClick={() => {
              setIsRefundModalOpen(true);
              loadRefunds();
              if (events.length > 0 && !activeWaitlistEventId) {
                loadEventTiersAndWaitlist(events[0].id);
              }
            }}
          >
            <RotateCcw size={16} />
            <span>Refunds & Waitlists</span>
            {refundsList.filter(r => r.status === 'PENDING').length > 0 && (
              <span className="pending-badge-counter">
                {refundsList.filter(r => r.status === 'PENDING').length}
              </span>
            )}
          </button>
          <button
            className="btn-manage-promos"
            onClick={() => {
              setIsPromoModalOpen(true);
              loadPromoCodes();
            }}
          >
            <Tag size={16} />
            <span>Promo Codes Engine</span>
          </button>
          <button className="btn-create-event" onClick={openCreateModal}>
            <Plus size={16} />
            <span>Create New Event</span>
          </button>
        </div>
      </div>

      {/* Events Table / Card Roster */}
      <div className="studio-content">
        {isLoading ? (
          <div className="studio-loading">Loading organizer workspace...</div>
        ) : events.length === 0 ? (
          <div className="studio-empty">
            <div className="empty-sparkle">
              <Sparkles size={32} />
            </div>
            <h3>No Events Created Yet</h3>
            <p>You haven't initiated any event drafts. Click below to create your first event draft with preset templates.</p>
            <button className="btn-create-event" onClick={openCreateModal}>
              <Plus size={16} />
              <span>Create First Event</span>
            </button>
          </div>
        ) : (
          <div className="studio-event-list">
            {events.map((evt) => (
              <div key={evt.id} className="studio-event-card">
                <div className="event-card-left">
                  {evt.bannerImageUrl ? (
                    <img src={evt.bannerImageUrl} alt={evt.title} className="thumb-img" />
                  ) : (
                    <div className="thumb-placeholder">Event</div>
                  )}

                  <div className="event-info">
                    <div className="info-badge-row">
                      <span className={`status-pill status-${(evt.status || 'draft').toLowerCase()}`}>
                        {evt.status}
                      </span>
                      {evt.category && <span className="cat-pill">{evt.category.name}</span>}
                    </div>

                    <h4 className="event-title-text">{evt.title}</h4>

                    <div className="event-meta-line">
                      <span><Calendar size={13} /> {new Date(evt.startTime).toLocaleDateString()}</span>
                      <span><MapPin size={13} /> {evt.venue?.name || 'Online'}</span>
                    </div>

                    {/* Rejection Notice Banner */}
                    {evt.status === 'REJECTED' && evt.rejectionReason && (
                      <div className="rejection-box">
                        <AlertTriangle size={15} />
                        <span><strong>Rejection Reason:</strong> {evt.rejectionReason}</span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Action Trigger Buttons */}
                <div className="event-actions-col">
                  {/* Tickets & Pricing Button - hidden for cancelled events */}
                  {evt.status !== 'CANCELLED' && (
                    <button
                      className="btn-action tiers-action-btn"
                      onClick={() => openTierModal(evt)}
                      title="Configure Ticket Tiers and Prices"
                    >
                      <Ticket size={14} />
                      <span>Tickets & Pricing</span>
                    </button>
                  )}

                  {(evt.status === 'DRAFT' || evt.status === 'REJECTED') && (
                    <>
                      <button className="btn-action edit" onClick={() => openEditModal(evt)}>
                        <Edit3 size={14} />
                        <span>Edit Draft</span>
                      </button>
                      <button className="btn-action submit" onClick={() => handleSubmitForApproval(evt.id)}>
                        <Send size={14} />
                        <span>Submit for Approval</span>
                      </button>
                      <button className="btn-action delete-draft" onClick={() => handleDeleteEvent(evt.id, evt.title)} title="Delete Event Draft">
                        <Trash2 size={14} />
                        <span>Delete Draft</span>
                      </button>
                    </>
                  )}

                  {evt.status === 'PENDING_APPROVAL' && (
                    <div className="pending-status-notice">
                      <Clock size={16} />
                      <span>In Admin Review Queue</span>
                    </div>
                  )}

                  {evt.status === 'APPROVED' && (
                    <button className="btn-action publish" onClick={() => handlePublish(evt.id)}>
                      <Globe size={14} />
                      <span>Publish Live Now</span>
                    </button>
                  )}

                  {evt.status === 'PUBLISHED' && (
                    <div className="live-actions">
                      <span className="live-tag">
                        <CheckCircle size={14} /> Live on Catalog
                      </span>
                      <button className="btn-action cancel" onClick={() => handleCancel(evt.id)}>
                        Cancel Event
                      </button>
                    </div>
                  )}

                  {evt.status === 'CANCELLED' && (
                    <div className="cancelled-actions">
                      <button
                        className="btn-action delete-cancelled"
                        onClick={() => handleDeleteEvent(evt.id, evt.title)}
                        title="Permanently Delete this Cancelled Event"
                      >
                        <Trash2 size={14} />
                        <span>Delete from History</span>
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Create / Edit Event Modal */}
      {isModalOpen && (
        <div className="modal-backdrop" onClick={() => setIsModalOpen(false)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editingEvent ? 'Edit Event Draft' : 'Create New Event Draft'}</h3>
              <button className="modal-close" onClick={() => setIsModalOpen(false)}>
                <X size={18} />
              </button>
            </div>

            {/* Quick Presets for Demo / Testing */}
            <div className="presets-banner">
              <span className="presets-label">
                <Sparkles size={13} /> Quick Presets:
              </span>
              <div className="presets-buttons">
                {!editingEvent && SAMPLE_PRESETS.map((p, idx) => (
                  <button
                    key={idx}
                    type="button"
                    className="btn-preset"
                    onClick={() => applyPreset(p)}
                  >
                    {p.title.split(':')[0]}
                  </button>
                ))}
              </div>
            </div>

            <form onSubmit={handleSubmit} className="modal-form">
              {formError && <div className="form-error-box">{formError}</div>}

              <div className="form-group">
                <label>Event Title *</label>
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g. Next-Gen Cloud Architecture Summit"
                  required
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Category *</label>
                  <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)} required>
                    {categories.length === 0 && <option value="" disabled>Loading categories from database...</option>}
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                    <label style={{ margin: 0 }}>Venue / Location *</label>
                    <button
                      type="button"
                      onClick={() => setIsVenueModalOpen(true)}
                      style={{
                        background: 'rgba(99, 102, 241, 0.15)',
                        border: '1px solid rgba(99, 102, 241, 0.4)',
                        color: '#a5b4fc',
                        borderRadius: '6px',
                        padding: '3px 9px',
                        fontSize: '0.78rem',
                        fontWeight: '600',
                        cursor: 'pointer',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '4px',
                        transition: 'all 0.15s ease'
                      }}
                      title="Add a custom physical or virtual location"
                    >
                      <Plus size={12} /> + Add Location
                    </button>
                  </div>
                  <select 
                    value={venueId} 
                    onChange={(e) => {
                      if (e.target.value === '__NEW_VENUE__') {
                        setIsVenueModalOpen(true);
                      } else {
                        setVenueId(e.target.value);
                      }
                    }} 
                    required
                  >
                    {venues.length === 0 && <option value="" disabled>Loading venues from database...</option>}
                    {venues.map((v) => (
                      <option key={v.id} value={v.id}>
                        {v.name} {v.city ? `(${v.city})` : (v.online ? '(Virtual)' : '')}
                      </option>
                    ))}
                    <option value="__NEW_VENUE__">➕ Enter a New Location...</option>
                  </select>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Start Date & Time *</label>
                  <input
                    type="datetime-local"
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>End Date & Time *</label>
                  <input
                    type="datetime-local"
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Banner Image URL</label>
                <input
                  type="url"
                  value={bannerImageUrl}
                  onChange={(e) => setBannerImageUrl(e.target.value)}
                  placeholder="https://images.unsplash.com/..."
                />
              </div>

              <div className="form-group">
                <label>Short Summary</label>
                <input
                  type="text"
                  value={shortSummary}
                  onChange={(e) => setShortSummary(e.target.value)}
                  placeholder="A concise, 1-2 sentence hook for discovery cards"
                />
              </div>

              <div className="form-group">
                <label>Full Description</label>
                <textarea
                  rows={4}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Detailed schedule, speaker highlights, agenda, and guidelines..."
                ></textarea>
              </div>

              <div className="form-checkbox">
                <label>
                  <input
                    type="checkbox"
                    checked={featured}
                    onChange={(e) => setFeatured(e.target.checked)}
                  />
                  <span>Mark as Featured Event</span>
                </label>
              </div>

              <div className="pricing-tip-box">
                <Ticket size={16} className="pricing-tip-icon" />
                <div className="pricing-tip-content">
                  <strong>Ticket Pricing & Capacities</strong>
                  <p>Ticket prices and seat limits are configured as <em>Ticket Tiers</em> (e.g. Free, General Admission, VIP). After saving, click the <strong>"Tickets & Pricing"</strong> button on your event card to set ticket prices!</p>
                </div>
              </div>

              <div className="modal-actions">
                <button
                  type="button"
                  className="btn-cancel"
                  onClick={() => setIsModalOpen(false)}
                >
                  Cancel
                </button>
                <button type="submit" className="btn-save" disabled={isSubmitting}>
                  {isSubmitting ? 'Saving...' : editingEvent ? 'Save Changes' : 'Create Draft'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Promo Codes Engine Modal */}
      {isPromoModalOpen && (
        <div className="studio-modal-overlay" onClick={() => setIsPromoModalOpen(false)}>
          <div className="promo-mgmt-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="studio-modal-header">
              <div className="header-info-with-icon">
                <Tag size={20} className="header-title-icon" />
                <div>
                  <h3>Promotional Discount Codes Engine</h3>
                  <p className="header-subtitle">Configure discount codes, percentages, fixed savings, and redemption limits</p>
                </div>
              </div>
              <button className="btn-close-studio-modal" onClick={() => setIsPromoModalOpen(false)}>
                <X size={18} />
              </button>
            </div>

            <div className="promo-mgmt-body">
              {/* Active Promo Codes List */}
              <div className="promos-roster-section">
                <h4 className="promo-roster-heading">Active Discount Codes ({promoCodes.length})</h4>
                {isLoadingPromos ? (
                  <div className="promos-loading">Loading promotional codes...</div>
                ) : promoCodes.length === 0 ? (
                  <div className="promos-empty">No promo codes configured yet.</div>
                ) : (
                  <div className="promo-codes-grid">
                    {promoCodes.map((pc) => (
                      <div key={pc.id} className="promo-card">
                        <div className="promo-card-top">
                          <code className="promo-pill-code">{pc.code}</code>
                          <span className={`promo-status-pill ${pc.active ? 'active' : 'inactive'}`}>
                            {pc.active ? 'Active' : 'Expired'}
                          </span>
                        </div>
                        <div className="promo-card-value">
                          {pc.discountType === 'PERCENTAGE'
                            ? `${pc.discountValue}% OFF`
                            : `${currencyRate.symbol || '$'}${Number(Number(pc.discountValue) * (currencyRate.rateToUsd || 1.0)).toFixed(2)} OFF`}
                        </div>
                        <div className="promo-card-meta">
                          <span>Usage: <strong>{pc.currentUsageCount || 0}</strong> / {pc.usageLimit || '∞'}</span>
                          {pc.minOrderAmount && (
                            <span>Min Order: <strong>{currencyRate.symbol || '$'}{Number(Number(pc.minOrderAmount) * (currencyRate.rateToUsd || 1.0)).toFixed(2)}</strong></span>
                          )}
                          <span>Scope: <strong>{pc.eventId ? `Event #${pc.eventId}` : 'Global (All Events)'}</strong></span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Create Promo Code Form */}
              <div className="promo-create-card">
                <h4 className="promo-create-heading">
                  <Plus size={16} />
                  <span>Create New Promo Code</span>
                </h4>

                {promoCreateSuccess && (
                  <div className="promo-success-alert">
                    <CheckCircle size={15} />
                    <span>{promoCreateSuccess}</span>
                  </div>
                )}

                {promoCreateError && (
                  <div className="promo-error-alert">
                    <AlertTriangle size={15} />
                    <span>{promoCreateError}</span>
                  </div>
                )}

                <form onSubmit={handleCreatePromoCode} className="promo-new-form">
                  <div className="promo-inputs-grid">
                    <div className="form-group">
                      <label>Promo Code *</label>
                      <input
                        type="text"
                        placeholder="e.g. SUMMER25"
                        value={promoFormCode}
                        onChange={(e) => setPromoFormCode(e.target.value.toUpperCase())}
                        required
                        className="mono-code-input"
                      />
                    </div>

                    <div className="form-group">
                      <label>Discount Type *</label>
                      <select
                        value={promoFormType}
                        onChange={(e) => setPromoFormType(e.target.value)}
                      >
                        <option value="PERCENTAGE">Percentage (%)</option>
                        <option value="FIXED_AMOUNT">Fixed Currency ({currenciesList.find(c => c.currencyCode === promoCurrency)?.symbol || currencyRate.symbol || '$'})</option>
                      </select>
                    </div>

                    {promoFormType === 'FIXED_AMOUNT' && (
                      <div className="form-group">
                        <label>Currency *</label>
                        <select
                          value={promoCurrency}
                          onChange={(e) => setPromoCurrency(e.target.value)}
                        >
                          {currenciesList.length > 0 ? (
                            currenciesList.map(curr => (
                              <option key={curr.currencyCode} value={curr.currencyCode}>
                                {curr.symbol} {curr.currencyCode} - {curr.currencyName}
                              </option>
                            ))
                          ) : (
                            <>
                              <option value="USD">$ USD - US Dollar</option>
                              <option value="INR">₹ INR - Indian Rupee</option>
                              <option value="EUR">€ EUR - Euro</option>
                              <option value="GBP">£ GBP - British Pound</option>
                            </>
                          )}
                        </select>
                      </div>
                    )}

                    <div className="form-group">
                      <label>
                        Discount Value * {promoFormType === 'FIXED_AMOUNT' && `(${currenciesList.find(c => c.currencyCode === promoCurrency)?.symbol || currencyRate.symbol || '$'} ${promoCurrency})`}
                      </label>
                      <input
                        type="number"
                        step="0.01"
                        placeholder={
                          promoFormType === 'PERCENTAGE' 
                            ? 'e.g. 25 (%)' 
                            : `e.g. ${(50 * (currenciesList.find(c => c.currencyCode === promoCurrency)?.rateToUsd || currencyRate.rateToUsd || 1.0)).toFixed(0)} (${currenciesList.find(c => c.currencyCode === promoCurrency)?.symbol || currencyRate.symbol || '$'})`
                        }
                        value={promoFormValue}
                        onChange={(e) => setPromoFormValue(e.target.value)}
                        required
                      />
                    </div>

                    <div className="form-group">
                      <label>Redemption Limit</label>
                      <input
                        type="number"
                        placeholder="e.g. 100 uses"
                        value={promoFormLimit}
                        onChange={(e) => setPromoFormLimit(e.target.value)}
                      />
                    </div>

                    <div className="form-group">
                      <label>
                        Min Order Amount ({promoFormType === 'FIXED_AMOUNT' ? (currenciesList.find(c => c.currencyCode === promoCurrency)?.symbol || '$') : (currencyRate.symbol || '$')})
                      </label>
                      <input
                        type="number"
                        step="0.01"
                        placeholder={
                          promoFormType === 'FIXED_AMOUNT'
                            ? `e.g. ${(100 * (currenciesList.find(c => c.currencyCode === promoCurrency)?.rateToUsd || currencyRate.rateToUsd || 1.0)).toFixed(0)}`
                            : 'e.g. 50.00'
                        }
                        value={promoFormMinTotal}
                        onChange={(e) => setPromoFormMinTotal(e.target.value)}
                      />
                    </div>

                    <div className="form-group">
                      <label>Event Scope</label>
                      <select
                        value={promoFormEventId}
                        onChange={(e) => setPromoFormEventId(e.target.value)}
                      >
                        <option value="">Global (Valid for Any Event)</option>
                        {events.map((ev) => (
                          <option key={ev.id} value={ev.id}>
                            #{ev.id} - {ev.title}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div className="promo-form-actions">
                    <button
                      type="submit"
                      className="btn-create-promo-submit"
                      disabled={isCreatingPromo || !promoFormCode.trim() || !promoFormValue}
                    >
                      {isCreatingPromo ? (
                        <>
                          <Loader2 size={15} className="spin-icon" />
                          <span>Creating...</span>
                        </>
                      ) : (
                        <>
                          <Tag size={15} />
                          <span>Publish Promo Code</span>
                        </>
                      )}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Refunds & Waitlists Management Modal */}
      {isRefundModalOpen && (
        <div className="studio-modal-backdrop" onClick={() => setIsRefundModalOpen(false)}>
          <div className="refund-studio-modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="studio-modal-header">
              <div className="modal-title-with-icon">
                <RotateCcw size={20} className="text-primary" />
                <div>
                  <h3>Refund Workflow & Tier Waitlists Monitor</h3>
                  <p>Approve attendee refund requests with automatic pessimistic restock and inspect FIFO waitlist queues.</p>
                </div>
              </div>
              <button className="btn-modal-close" onClick={() => setIsRefundModalOpen(false)}>
                <X size={20} />
              </button>
            </div>

            {/* Sub-Tabs: Refunds vs Waitlists */}
            <div className="studio-subtab-bar">
              <button
                className={`studio-subtab-btn ${refundTab === 'REFUNDS' ? 'active' : ''}`}
                onClick={() => setRefundTab('REFUNDS')}
              >
                <RotateCcw size={15} />
                <span>Attendee Refund Requests ({refundsList.length})</span>
              </button>
              <button
                className={`studio-subtab-btn ${refundTab === 'WAITLISTS' ? 'active' : ''}`}
                onClick={() => setRefundTab('WAITLISTS')}
              >
                <Users size={15} />
                <span>Tier FIFO Waitlists</span>
              </button>
            </div>

            <div className="refund-studio-modal-body">
              {refundTab === 'REFUNDS' ? (
                <div className="refunds-management-view">
                  {/* Filter pills */}
                  <div className="refund-filter-chips">
                    <button
                      className={`chip ${refundsFilter === 'ALL' ? 'active' : ''}`}
                      onClick={() => setRefundsFilter('ALL')}
                    >
                      All ({refundsList.length})
                    </button>
                    <button
                      className={`chip ${refundsFilter === 'PENDING' ? 'active' : ''}`}
                      onClick={() => setRefundsFilter('PENDING')}
                    >
                      Pending Review ({refundsList.filter(r => r.status === 'PENDING').length})
                    </button>
                    <button
                      className={`chip ${refundsFilter === 'PROCESSED' ? 'active' : ''}`}
                      onClick={() => setRefundsFilter('PROCESSED')}
                    >
                      Processed ({refundsList.filter(r => r.status === 'PROCESSED' || r.status === 'APPROVED').length})
                    </button>
                  </div>

                  {isLoadingRefunds ? (
                    <div className="refunds-loading">Loading refund records...</div>
                  ) : refundsList.length === 0 ? (
                    <div className="refunds-empty">No refund requests found across your events.</div>
                  ) : (
                    <div className="refunds-review-list">
                      {refundsList
                        .filter(r => {
                          if (refundsFilter === 'PENDING') return r.status === 'PENDING';
                          if (refundsFilter === 'PROCESSED') return r.status === 'PROCESSED' || r.status === 'APPROVED';
                          return true;
                        })
                        .map(r => {
                          const isPending = r.status === 'PENDING';

                          return (
                            <div key={r.id} className="refund-review-card">
                              <div className="review-card-top">
                                <div>
                                  <span className="order-tag">Order #{r.orderNumber}</span>
                                  <h4 className="refund-event-title">{r.eventTitle}</h4>
                                  <span className="attendee-line">Attendee ID #{r.userId} • Requested {new Date(r.createdAt).toLocaleDateString()}</span>
                                </div>
                                <div className="amount-status-block">
                                  <span className="refund-amount-tag">${Number(r.amount).toFixed(2)}</span>
                                  <span className={`refund-status-tag status-${r.status.toLowerCase()}`}>
                                    {r.status}
                                  </span>
                                </div>
                              </div>

                              <div className="review-card-reason">
                                <span className="label">Attendee Reason:</span>
                                <p className="reason-text">"{r.reason}"</p>
                              </div>

                              {r.adminNotes && (
                                <div className="review-card-notes">
                                  <span className="label">Reviewer Notes:</span>
                                  <p>{r.adminNotes}</p>
                                </div>
                              )}

                              {isPending && (
                                <div className="review-card-action-box">
                                  <input
                                    type="text"
                                    placeholder="Optional note for attendee (e.g. Approved per 48h policy)..."
                                    className="input-review-notes"
                                    value={reviewNotes[r.id] || ''}
                                    onChange={(e) => setReviewNotes({ ...reviewNotes, [r.id]: e.target.value })}
                                  />
                                  <div className="review-btns">
                                    <button
                                      type="button"
                                      className="btn-reject-refund"
                                      onClick={() => handleReviewRefund(r.id, false)}
                                      disabled={isReviewingRefund[r.id]}
                                    >
                                      Reject
                                    </button>
                                    <button
                                      type="button"
                                      className="btn-approve-refund"
                                      onClick={() => handleReviewRefund(r.id, true)}
                                      disabled={isReviewingRefund[r.id]}
                                    >
                                      {isReviewingRefund[r.id] ? <Loader2 size={13} className="spin" /> : <CheckCircle size={13} />}
                                      <span>Approve & Restock Inventory</span>
                                    </button>
                                  </div>
                                </div>
                              )}
                            </div>
                          );
                        })}
                    </div>
                  )}
                </div>
              ) : (
                /* TIER WAITLISTS TAB */
                <div className="waitlists-monitor-view">
                  <div className="event-selector-row">
                    <label>Select Event to Monitor:</label>
                    <select
                      value={activeWaitlistEventId}
                      onChange={(e) => loadEventTiersAndWaitlist(e.target.value)}
                    >
                      {events.map((ev) => (
                        <option key={ev.id} value={ev.id}>
                          #{ev.id} - {ev.title}
                        </option>
                      ))}
                    </select>
                  </div>

                  {isLoadingWaitlists ? (
                    <div className="waitlists-loading">Loading live tier queues...</div>
                  ) : eventTiers.length === 0 ? (
                    <div className="waitlists-empty">No ticket tiers found for this event.</div>
                  ) : (
                    <div className="tiers-waitlist-list">
                      {eventTiers.map(tier => {
                        const queue = tierWaitlists[tier.id] || [];
                        const isSoldOut = tier.availableQuantity <= 0;

                        return (
                          <div key={tier.id} className="tier-waitlist-group">
                            <div className="tier-waitlist-header">
                              <div>
                                <span className="tier-name">{tier.name}</span>
                                <span className={`tier-inventory-badge ${isSoldOut ? 'sold-out' : 'available'}`}>
                                  {isSoldOut ? 'Sold Out' : `${tier.availableQuantity} available`} (Capacity: {tier.capacity})
                                </span>
                              </div>
                              <span className="queue-count-tag">
                                <Users size={13} /> {queue.length} in FIFO Waitlist
                              </span>
                            </div>

                            {queue.length === 0 ? (
                              <div className="tier-queue-empty">No attendees currently in waitlist for this tier.</div>
                            ) : (
                              <div className="queue-entries-table">
                                <div className="queue-row header">
                                  <span>Position</span>
                                  <span>Attendee</span>
                                  <span>Status</span>
                                  <span>Offer Details</span>
                                </div>
                                {queue.map(entry => (
                                  <div key={entry.id} className="queue-row">
                                    <span className="pos">#{entry.queuePosition}</span>
                                    <span className="att">{entry.userName} ({entry.userEmail})</span>
                                    <span className={`status status-${entry.status.toLowerCase()}`}>{entry.status}</span>
                                    <span className="expiry">
                                      {entry.offerExpiresAt ? `Hold expires: ${new Date(entry.offerExpiresAt).toLocaleTimeString()}` : '-'}
                                    </span>
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Add New Venue / Location Modal */}
      {isVenueModalOpen && (
        <div className="modal-backdrop" onClick={() => setIsVenueModalOpen(false)} style={{ zIndex: 1200 }}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <h3>Add New Location / Venue</h3>
              <button className="modal-close" onClick={() => setIsVenueModalOpen(false)}>
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleSaveVenue} className="modal-form" style={{ padding: '20px' }}>
              {venueError && <div className="form-error-box">{venueError}</div>}

              <div className="form-group">
                <label>Venue / Location Name *</label>
                <input
                  type="text"
                  value={newVenueName}
                  onChange={(e) => setNewVenueName(e.target.value)}
                  placeholder="e.g. Pragati Maidan Exhibition Center"
                  required
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>City</label>
                  <input
                    type="text"
                    value={newVenueCity}
                    onChange={(e) => setNewVenueCity(e.target.value)}
                    placeholder="e.g. New Delhi"
                  />
                </div>
                <div className="form-group">
                  <label>Capacity</label>
                  <input
                    type="number"
                    value={newVenueCapacity}
                    onChange={(e) => setNewVenueCapacity(e.target.value)}
                    min="1"
                    placeholder="500"
                  />
                </div>
              </div>

              <div className="form-group">
                <label>Street Address</label>
                <input
                  type="text"
                  value={newVenueAddress}
                  onChange={(e) => setNewVenueAddress(e.target.value)}
                  placeholder="e.g. Mathura Road, Railway Colony"
                />
              </div>

              <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '10px' }}>
                <input
                  type="checkbox"
                  id="newVenueIsOnline"
                  checked={newVenueIsOnline}
                  onChange={(e) => setNewVenueIsOnline(e.target.checked)}
                  style={{ width: 'auto', cursor: 'pointer' }}
                />
                <label htmlFor="newVenueIsOnline" style={{ margin: 0, cursor: 'pointer', fontSize: '0.88rem', color: 'var(--text-medium, #cbd5e1)' }}>
                  This is a Virtual / Online Livestream Event (Zoom, Teams, YouTube)
                </label>
              </div>

              <div className="modal-actions" style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <button
                  type="button"
                  className="btn-cancel"
                  onClick={() => setIsVenueModalOpen(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-submit"
                  disabled={isSavingVenue}
                >
                  {isSavingVenue ? <Loader2 size={16} className="spin-animate" /> : 'Save & Select Location'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Ticket Tiers & Pricing Studio Modal */}
      {isTierModalOpen && tierEvent && (
        <div className="studio-modal-overlay" onClick={() => setIsTierModalOpen(false)}>
          <div className="tier-mgmt-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="studio-modal-header">
              <div className="header-info-with-icon">
                <Ticket size={22} className="header-title-icon" />
                <div>
                  <h3>Tickets & Pricing Studio</h3>
                  <p>Set ticket tiers, prices, and attendee capacities for <strong>"{tierEvent.title}"</strong></p>
                </div>
              </div>
              <button className="modal-close" onClick={() => setIsTierModalOpen(false)}>
                <X size={18} />
              </button>
            </div>

            <div className="tier-mgmt-body">
              {/* Existing Configured Tiers Section */}
              <div className="tier-roster-section">
                <h4 className="tier-roster-heading">
                  <Layers size={16} />
                  <span>Configured Ticket Tiers ({tiersList.length})</span>
                </h4>

                {isLoadingTiers ? (
                  <div className="tier-loading">Loading ticket tiers from database...</div>
                ) : tiersList.length === 0 ? (
                  <div className="tier-empty-box">
                    <Ticket size={24} style={{ opacity: 0.5, marginBottom: '6px' }} />
                    <p>No ticket tiers configured yet for this event.</p>
                    <small>Add a ticket tier below to set ticket prices (e.g. Free Entry, General Admission, VIP Pass).</small>
                  </div>
                ) : (
                  <div className="tiers-table-container">
                    <table className="tiers-table">
                      <thead>
                        <tr>
                          <th>Tier Name</th>
                          <th>Price</th>
                          <th>Capacity</th>
                          <th>Available</th>
                          <th>Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {tiersList.map((tier) => (
                          <tr key={tier.id}>
                            <td className="tier-name-cell">
                              <strong>{tier.name}</strong>
                              {tier.description && <span className="tier-desc-text">{tier.description}</span>}
                            </td>
                            <td className="tier-price-cell">
                              {Number(tier.price) === 0 ? (
                                <span className="tier-free-pill">FREE</span>
                              ) : (
                                <span className="tier-price-pill">
                                  {currencyRate.symbol || '$'} {(Number(tier.price) * (currencyRate.rateToUsd || 1.0)).toFixed(2)}
                                </span>
                              )}
                            </td>
                            <td>{tier.capacity} seats</td>
                            <td>{tier.availableTickets ?? tier.capacity} remaining</td>
                            <td>
                              <button
                                className="btn-tier-delete"
                                onClick={() => handleDeleteTier(tier.id, tier.name)}
                                title="Delete this tier"
                              >
                                <Trash2 size={14} />
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              {/* Add New Tier Form Card */}
              <div className="tier-create-card">
                <h4 className="tier-create-heading">
                  <Plus size={16} />
                  <span>Add Ticket Tier & Price</span>
                </h4>

                {tierSuccess && (
                  <div className="promo-success-alert">
                    <CheckCircle size={15} />
                    <span>{tierSuccess}</span>
                  </div>
                )}

                {tierError && (
                  <div className="promo-error-alert">
                    <AlertTriangle size={15} />
                    <span>{tierError}</span>
                  </div>
                )}

                <form onSubmit={handleSaveTier} className="tier-new-form">
                  <div className="tier-inputs-grid">
                    <div className="form-group">
                      <label>Ticket Tier Name *</label>
                      <input
                        type="text"
                        placeholder="e.g. General Admission, VIP Pass, Early Bird"
                        value={tierName}
                        onChange={(e) => setTierName(e.target.value)}
                        required
                      />
                    </div>

                    <div className="form-group">
                      <label>Ticket Price ({currencyRate.symbol || '$'} {currency}) *</label>
                      <div className="input-with-prefix">
                        <span className="input-prefix">{currencyRate.symbol || '$'}</span>
                        <input
                          type="number"
                          step="0.01"
                          min="0"
                          placeholder={`0.00 for free or ${(25.0 * (currencyRate.rateToUsd || 1.0)).toFixed(2)}`}
                          value={tierPrice}
                          onChange={(e) => setTierPrice(e.target.value)}
                          required
                        />
                      </div>
                    </div>

                    <div className="form-group">
                      <label>Seat Capacity (Tickets) *</label>
                      <input
                        type="number"
                        min="1"
                        placeholder="e.g. 100"
                        value={tierCapacity}
                        onChange={(e) => setTierCapacity(e.target.value)}
                        required
                      />
                    </div>

                    <div className="form-group full-width">
                      <label>Perks / Description (Optional)</label>
                      <input
                        type="text"
                        placeholder="e.g. Stage front row, complimentary drinks, access to live Q&A"
                        value={tierDescription}
                        onChange={(e) => setTierDescription(e.target.value)}
                      />
                    </div>
                  </div>

                  <div className="tier-form-actions">
                    <button
                      type="submit"
                      disabled={isSavingTier}
                      className="btn-save-tier"
                    >
                      {isSavingTier ? (
                        <>
                          <Loader2 size={15} className="spin-animate" />
                          <span>Saving Tier...</span>
                        </>
                      ) : (
                        <>
                          <Plus size={15} />
                          <span>Save Ticket Tier</span>
                        </>
                      )}
                    </button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
