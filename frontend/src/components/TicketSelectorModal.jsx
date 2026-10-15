import React, { useState, useEffect } from 'react';
import { 
  X, 
  Ticket, 
  CheckCircle2, 
  AlertCircle, 
  CreditCard, 
  ShieldCheck, 
  Plus, 
  Minus,
  Sparkles,
  ArrowRight,
  Zap,
  Lock,
  Loader2,
  Tag,
  Check,
  Clock,
  Users,
  Hourglass,
  Armchair
} from 'lucide-react';
import { ticketTierService, orderService, paymentService, promoCodeService, waitlistService, pricingService } from '../services/api';
import { useAuth } from '../context/AuthContext';
import VisualSeatSelectorModal from './VisualSeatSelectorModal';
import './TicketSelectorModal.css';

export default function TicketSelectorModal({ event, onClose, onOrderSuccess, onOpenAuth }) {
  const { user, isAuthenticated } = useAuth();
  const [tiers, setTiers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedQuantities, setSelectedQuantities] = useState({});
  const [attendeeName, setAttendeeName] = useState(user?.fullName || '');
  const [attendeeEmail, setAttendeeEmail] = useState(user?.email || '');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [completedOrder, setCompletedOrder] = useState(null);
  const [paymentProvider, setPaymentProvider] = useState('STRIPE');
  const [paymentDetails, setPaymentDetails] = useState(null);
  const [processingStatus, setProcessingStatus] = useState('');

  // Promo Code State
  const [promoCodeInput, setPromoCodeInput] = useState('');
  const [appliedPromo, setAppliedPromo] = useState(null);
  const [promoError, setPromoError] = useState('');
  const [isValidatingPromo, setIsValidatingPromo] = useState(false);

  // Phase 6 Waitlist State
  const [myWaitlists, setMyWaitlists] = useState([]);
  const [waitlistActionLoading, setWaitlistActionLoading] = useState({});
  const [waitlistMessage, setWaitlistMessage] = useState({});

  // Phase 8 Interactive Seat Map State
  const [showSeatMap, setShowSeatMap] = useState(false);
  const [selectedSeatHold, setSelectedSeatHold] = useState(null);

  // Phase 9 Multi-Currency & Surge Pricing State
  const [currency, setCurrency] = useState(localStorage.getItem('preferredCurrency') || 'USD');
  const [tierQuotes, setTierQuotes] = useState({});
  const [currencyRate, setCurrencyRate] = useState({ symbol: '$', rateToUsd: 1.0 });

  useEffect(() => {
    pricingService.getCurrencies().then(currencies => {
      const match = currencies.find(c => c.currencyCode === currency);
      if (match) {
        setCurrencyRate({ symbol: match.symbol || '$', rateToUsd: match.rateToUsd || 1.0 });
      }
    }).catch(() => {});
  }, [currency]);

  useEffect(() => {
    const handleCurrChange = (e) => {
      if (e.detail) setCurrency(e.detail);
    };
    window.addEventListener('currencyChanged', handleCurrChange);
    return () => window.removeEventListener('currencyChanged', handleCurrChange);
  }, []);

  useEffect(() => {
    if (tiers.length > 0) {
      tiers.forEach(t => {
        pricingService.getTierQuote(t.id, currency).then(quote => {
          setTierQuotes(prev => ({ ...prev, [t.id]: quote }));
        }).catch(() => {});
      });
    }
  }, [tiers, currency]);

  useEffect(() => {
    if (user) {
      if (!attendeeName) setAttendeeName(user.fullName || '');
      if (!attendeeEmail) setAttendeeEmail(user.email || '');
    }
  }, [user]);

  useEffect(() => {
    async function loadTiers() {
      setIsLoading(true);
      try {
        const data = await ticketTierService.getTiersForEvent(event.id);
        setTiers(data);
        const initial = {};
        data.forEach(t => {
          initial[t.id] = 0;
        });
        setSelectedQuantities(initial);
      } catch (err) {
        setErrorMessage(err.message || 'Failed to load ticket tiers');
      } finally {
        setIsLoading(false);
      }
    }
    if (event?.id) loadTiers();
  }, [event]);

  const fetchMyWaitlists = async () => {
    if (!isAuthenticated) return;
    try {
      const entries = await waitlistService.getMyWaitlistEntries();
      setMyWaitlists(entries);
    } catch (err) {
      console.warn('Failed to fetch waitlist entries', err);
    }
  };

  useEffect(() => {
    fetchMyWaitlists();
  }, [isAuthenticated, event?.id]);

  const handleJoinWaitlist = async (tierId) => {
    if (!isAuthenticated) {
      if (onOpenAuth) onOpenAuth();
      else alert('Please sign in or create an account to join the waitlist queue.');
      return;
    }
    setWaitlistActionLoading(prev => ({ ...prev, [tierId]: true }));
    try {
      const entry = await waitlistService.joinWaitlist(tierId);
      setMyWaitlists(prev => [...prev.filter(e => e.ticketTierId !== tierId), entry]);
      setWaitlistMessage(prev => ({ ...prev, [tierId]: `Queued at position #${entry.queuePosition}!` }));
    } catch (err) {
      alert(err.message || 'Failed to join waitlist');
    } finally {
      setWaitlistActionLoading(prev => ({ ...prev, [tierId]: false }));
    }
  };

  const handleLeaveWaitlist = async (entryId, tierId) => {
    setWaitlistActionLoading(prev => ({ ...prev, [tierId]: true }));
    try {
      await waitlistService.leaveWaitlist(entryId);
      setMyWaitlists(prev => prev.filter(e => e.id !== entryId));
      setWaitlistMessage(prev => ({ ...prev, [tierId]: 'Left waitlist queue' }));
    } catch (err) {
      alert(err.message || 'Failed to leave waitlist');
    } finally {
      setWaitlistActionLoading(prev => ({ ...prev, [tierId]: false }));
    }
  };

  const handleClaimWaitlistOffer = async (entryId, tierId) => {
    setWaitlistActionLoading(prev => ({ ...prev, [tierId]: true }));
    try {
      const order = await waitlistService.claimOffer(entryId, attendeeName, attendeeEmail);
      setCompletedOrder(order);
      if (onOrderSuccess) onOrderSuccess(order);
    } catch (err) {
      alert(err.message || 'Failed to claim offered reservation');
    } finally {
      setWaitlistActionLoading(prev => ({ ...prev, [tierId]: false }));
    }
  };

  const updateQuantity = (tierId, delta, maxAvailable) => {
    setSelectedQuantities(prev => {
      const current = prev[tierId] || 0;
      const next = Math.max(0, Math.min(Math.min(10, maxAvailable), current + delta));
      return { ...prev, [tierId]: next };
    });
  };

  const selectedItems = tiers
    .filter(t => (selectedQuantities[t.id] || 0) > 0)
    .map(t => ({
      tier: t,
      quantity: selectedQuantities[t.id],
      subtotal: t.price * selectedQuantities[t.id],
    }));

  const totalTickets = selectedItems.reduce((sum, item) => sum + item.quantity, 0);
  const grandTotal = selectedItems.reduce((sum, item) => sum + item.subtotal, 0);
  const discountAmount = appliedPromo ? Number(appliedPromo.discountAmount || 0) : 0;
  const payableTotal = Math.max(0, grandTotal - discountAmount);

  // Converted totals for selected currency (e.g. INR, EUR, GBP, USD)
  const currSymbol = currencyRate.symbol || '$';
  const rate = currencyRate.rateToUsd || 1.0;
  const convertedGrandTotal = grandTotal * rate;
  const convertedDiscountAmount = discountAmount * rate;
  const convertedPayableTotal = payableTotal * rate;

  const handleApplyPromo = async (e) => {
    if (e) e.preventDefault();
    if (!promoCodeInput.trim()) return;
    setPromoError('');
    setIsValidatingPromo(true);
    try {
      const codeClean = promoCodeInput.trim().toUpperCase();
      const res = await promoCodeService.validatePromoCode({
        code: codeClean,
        eventId: event.id,
        orderTotal: grandTotal,
      });
      if (res && res.valid) {
        setAppliedPromo(res);
        setPromoError('');
      } else {
        setAppliedPromo(null);
        setPromoError(res?.description || 'Promo code is not applicable');
      }
    } catch (err) {
      setAppliedPromo(null);
      setPromoError(err.message || 'Failed to validate promo code');
    } finally {
      setIsValidatingPromo(false);
    }
  };

  const handleRemovePromo = () => {
    setAppliedPromo(null);
    setPromoCodeInput('');
    setPromoError('');
  };

  const handleCheckout = async (e) => {
    e.preventDefault();
    if (!isAuthenticated) {
      onOpenAuth('signin');
      return;
    }

    if (totalTickets <= 0) {
      setErrorMessage('Please select at least 1 ticket to proceed');
      return;
    }

    if (!attendeeName.trim() || !attendeeEmail.trim()) {
      setErrorMessage('Attendee name and email are required');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');
    setProcessingStatus('Securing inventory with pessimistic locks...');

    try {
      const itemsPayload = selectedItems.map(item => ({
        ticketTierId: item.tier.id,
        quantity: item.quantity,
        attendeeName: attendeeName.trim(),
        attendeeEmail: attendeeEmail.trim(),
      }));

      // 1. Create order with promoCode
      const orderResult = await orderService.createOrder({
        eventId: event.id,
        items: itemsPayload,
        promoCode: appliedPromo?.code || null,
      });

      // 2. If payable total > 0, trigger Payment Gateway session
      if (payableTotal > 0) {
        setProcessingStatus(`Initiating ${paymentProvider} payment gateway session...`);
        const session = await paymentService.createCheckoutSession({
          orderId: orderResult.id,
          provider: paymentProvider,
          successUrl: window.location.origin,
          cancelUrl: window.location.origin,
        });

        setProcessingStatus('Verifying payment authorization and transaction webhook...');
        // Confirm payment
        const confirmed = await paymentService.confirmSimulatedPayment(session.paymentReference);
        setPaymentDetails(confirmed);
      }

      setProcessingStatus('');
      setCompletedOrder(orderResult);
      if (onOrderSuccess) onOrderSuccess(orderResult);
    } catch (err) {
      setErrorMessage(err.message || 'Checkout failed. Please check tier availability.');
      setProcessingStatus('');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="ticket-modal-overlay" onClick={onClose}>
      <div className="ticket-modal-dialog" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="ticket-modal-header">
          <div className="header-info">
            <div className="header-icon">
              <Ticket size={20} />
            </div>
            <div>
              <h3>Reserve Tickets & Passes</h3>
              <p className="header-event-title">{event.title}</p>
            </div>
          </div>
          <button className="btn-close-modal" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        {completedOrder ? (
          /* Order Confirmation View */
          <div className="order-success-view">
            <div className="success-badge-wrap">
              <CheckCircle2 size={48} className="success-check-icon" />
            </div>
            <h3>Registration Confirmed!</h3>
            <p className="success-order-num">
              Order Reference: <strong>{completedOrder.orderNumber}</strong>
            </p>
            <p className="success-desc">
              Your {completedOrder.tickets?.length} digital ticket pass{completedOrder.tickets?.length === 1 ? '' : 'es'} with unique QR codes have been generated and linked to your account.
            </p>

            <div className="confirmation-summary-card">
              <div className="summary-row">
                <span>Event</span>
                <strong>{completedOrder.eventTitle}</strong>
              </div>
              <div className="summary-row">
                <span>Total Amount</span>
                <strong className="summary-amount">
                  {currSymbol}{(Number(completedOrder.totalAmount) * rate).toFixed(2)}
                </strong>
              </div>
              {Number(completedOrder.discountAmount) > 0 && (
                <div className="summary-row discount-row">
                  <span>Discount Applied ({completedOrder.promoCode})</span>
                  <span className="summary-discount-badge">
                    -{currSymbol}{(Number(completedOrder.discountAmount) * rate).toFixed(2)} OFF
                  </span>
                </div>
              )}
              {paymentDetails && (
                <>
                  <div className="summary-row">
                    <span>Payment Gateway</span>
                    <strong className="badge-provider">{paymentDetails.provider}</strong>
                  </div>
                  <div className="summary-row">
                    <span>Payment Ref</span>
                    <span className="mono-ref">{paymentDetails.paymentReference}</span>
                  </div>
                </>
              )}
              <div className="summary-row">
                <span>Pass Status</span>
                <span className="status-badge-confirmed">CONFIRMED</span>
              </div>
            </div>

            <div className="success-actions">
              <button 
                className="btn-view-tickets"
                onClick={() => {
                  onClose();
                  if (onOrderSuccess) onOrderSuccess(completedOrder, true);
                }}
              >
                <span>View My Digital Tickets & QR Pass</span>
                <ArrowRight size={16} />
              </button>
            </div>
          </div>
        ) : (
          /* Selection & Checkout Form */
          <form onSubmit={handleCheckout} className="ticket-form-body">
            {errorMessage && (
              <div className="ticket-error-banner">
                <AlertCircle size={16} />
                <span>{errorMessage}</span>
              </div>
            )}

            {/* Phase 8 Reserved Seating Prompt */}
            <div className="seatmap-prompt-card">
              <div className="seatmap-prompt-left">
                <Armchair className="seatmap-prompt-icon" size={22} />
                <div>
                  <div className="seatmap-prompt-title">
                    {selectedSeatHold
                      ? `Locked Seat: ${selectedSeatHold.seatCode} (${selectedSeatHold.sectionName})`
                      : 'Auditorium Reserved Seating Map'}
                  </div>
                  <div className="seatmap-prompt-desc">
                    {selectedSeatHold
                      ? `5-minute lock active for tier ${selectedSeatHold.tierName || 'Standard'}.`
                      : 'Pick your exact seat with visual layout and 5-minute hold lock.'}
                  </div>
                </div>
              </div>
              <button
                type="button"
                className="btn-open-seatmap"
                onClick={() => setShowSeatMap(true)}
              >
                <Armchair size={15} />
                <span>{selectedSeatHold ? 'Change Seat' : 'Pick Seat'}</span>
              </button>
            </div>

            {/* Tiers List */}
            <div className="tiers-section">
              <label className="section-label">Select Ticket Tier & Quantity</label>

              {isLoading ? (
                <div className="tiers-loading">Loading live ticket inventory...</div>
              ) : tiers.length === 0 ? (
                <div className="tiers-empty">
                  No ticket tiers currently active for this event.
                </div>
              ) : (
                <div className="tiers-list">
                  {tiers.map(tier => {
                    const isSoldOut = tier.isSoldOut || tier.availableQuantity <= 0;
                    const qty = selectedQuantities[tier.id] || 0;
                    const waitlistEntry = myWaitlists.find(e => e.ticketTierId === tier.id && (e.status === 'WAITING' || e.status === 'OFFERED'));
                    const isWaitlistOffered = waitlistEntry?.status === 'OFFERED';
                    const isWaitlistWaiting = waitlistEntry?.status === 'WAITING';

                    return (
                      <div key={tier.id} className={`tier-card ${isSoldOut ? 'sold-out' : ''} ${isWaitlistOffered ? 'offer-active' : ''}`}>
                        <div className="tier-card-left">
                          <div className="tier-title-row">
                            <span className="tier-name">{tier.name}</span>
                            {isWaitlistOffered ? (
                              <span className="badge-offer-banner">⚡ Reserved For You</span>
                            ) : isSoldOut ? (
                              <span className="badge-sold-out">Sold Out</span>
                            ) : tier.availableQuantity <= 10 ? (
                              <span className="badge-limited">Only {tier.availableQuantity} left!</span>
                            ) : (
                              <span className="badge-available">{tier.availableQuantity} available</span>
                            )}
                          </div>
                          {tier.description && (
                            <p className="tier-desc">{tier.description}</p>
                          )}
                          <div className="tier-price">
                            {tierQuotes[tier.id] ? (
                              <span>{tierQuotes[tier.id].formattedPrice}</span>
                            ) : Number(tier.price) === 0 ? 'FREE' : `$${Number(tier.price).toFixed(2)}`}
                            <span className="price-suffix">/ pass</span>
                            {tierQuotes[tier.id]?.surgeActive && (
                              <span className="badge-surge-pricing" title={tierQuotes[tier.id]?.appliedRules?.join(', ')}>
                                ⚡ Surge ({tierQuotes[tier.id]?.surgeMultiplier}x)
                              </span>
                            )}
                          </div>
                        </div>

                        {/* Quantity / Waitlist Controls */}
                        <div className="tier-card-right">
                          {isSoldOut ? (
                            <div className="waitlist-card-controls">
                              {isWaitlistOffered ? (
                                <div className="waitlist-offered-cluster">
                                  <span className="badge-offer-active">
                                    <Clock size={12} /> 15-Min Hold Active
                                  </span>
                                  <button
                                    type="button"
                                    className="btn-claim-pass"
                                    onClick={() => handleClaimWaitlistOffer(waitlistEntry.id, tier.id)}
                                    disabled={waitlistActionLoading[tier.id]}
                                  >
                                    {waitlistActionLoading[tier.id] ? <Loader2 size={13} className="spin" /> : <Sparkles size={13} />}
                                    <span>Claim Pass</span>
                                  </button>
                                </div>
                              ) : isWaitlistWaiting ? (
                                <div className="waitlist-waiting-cluster">
                                  <span className="badge-in-queue">
                                    <Users size={12} /> Queue #{waitlistEntry.queuePosition}
                                  </span>
                                  <button
                                    type="button"
                                    className="btn-leave-queue"
                                    onClick={() => handleLeaveWaitlist(waitlistEntry.id, tier.id)}
                                    disabled={waitlistActionLoading[tier.id]}
                                    title="Leave waitlist queue"
                                  >
                                    Leave
                                  </button>
                                </div>
                              ) : (
                                <button
                                  type="button"
                                  className="btn-join-queue"
                                  onClick={() => handleJoinWaitlist(tier.id)}
                                  disabled={waitlistActionLoading[tier.id]}
                                >
                                  {waitlistActionLoading[tier.id] ? <Loader2 size={13} className="spin" /> : <Users size={13} />}
                                  <span>Join Waitlist</span>
                                </button>
                              )}
                              {waitlistMessage[tier.id] && (
                                <span className="waitlist-status-subtext">{waitlistMessage[tier.id]}</span>
                              )}
                            </div>
                          ) : (
                            <div className="qty-controls">
                              <button
                                type="button"
                                className="btn-qty"
                                onClick={() => updateQuantity(tier.id, -1, tier.availableQuantity)}
                                disabled={qty <= 0}
                              >
                                <Minus size={14} />
                              </button>
                              <span className="qty-count">{qty}</span>
                              <button
                                type="button"
                                className="btn-qty"
                                onClick={() => updateQuantity(tier.id, 1, tier.availableQuantity)}
                                disabled={qty >= Math.min(10, tier.availableQuantity)}
                              >
                                <Plus size={14} />
                              </button>
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Attendee Details */}
            <div className="attendee-section">
              <label className="section-label">Attendee Registration Info</label>
              <div className="attendee-inputs-row">
                <div className="input-group">
                  <label>Full Name *</label>
                  <input
                    type="text"
                    value={attendeeName}
                    onChange={(e) => setAttendeeName(e.target.value)}
                    placeholder="Full name for ticket pass"
                    required
                  />
                </div>
                <div className="input-group">
                  <label>Email Address *</label>
                  <input
                    type="email"
                    value={attendeeEmail}
                    onChange={(e) => setAttendeeEmail(e.target.value)}
                    placeholder="Pass will be linked to this email"
                    required
                  />
                </div>
              </div>
            </div>

            {/* Promo / Discount Code Section */}
            {grandTotal > 0 && (
              <div className="promo-code-section">
                <div className="section-label-row">
                  <label className="section-label">
                    <Tag size={13} />
                    <span>Have a Promo Code?</span>
                  </label>
                  {!appliedPromo && (
                    <div className="promo-hints">
                      <span className="hint-pill" onClick={() => setPromoCodeInput('PULSE20')}>PULSE20 (20% OFF)</span>
                      <span className="hint-pill" onClick={() => setPromoCodeInput('SAVE50')}>SAVE50 ($50 OFF)</span>
                    </div>
                  )}
                </div>

                {appliedPromo ? (
                  <div className="promo-applied-badge">
                    <div className="promo-applied-info">
                      <Check size={16} className="promo-check-icon" />
                      <div>
                        <strong className="applied-code-title">{appliedPromo.code} Applied</strong>
                        <span className="promo-applied-desc">
                          {appliedPromo.discountType === 'PERCENTAGE'
                            ? `${appliedPromo.discountValue}% discount (-${currSymbol}${convertedDiscountAmount.toFixed(2)})`
                            : `${currSymbol}${convertedDiscountAmount.toFixed(2)} discount applied`}
                        </span>
                      </div>
                    </div>
                    <button type="button" className="btn-remove-promo" onClick={handleRemovePromo} title="Remove promo code">
                      <X size={14} />
                      <span>Remove</span>
                    </button>
                  </div>
                ) : (
                  <div className="promo-input-row">
                    <input
                      type="text"
                      className="promo-input"
                      placeholder="Enter code (e.g. PULSE20, SAVE50)"
                      value={promoCodeInput}
                      onChange={(e) => setPromoCodeInput(e.target.value.toUpperCase())}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          e.preventDefault();
                          handleApplyPromo();
                        }
                      }}
                    />
                    <button
                      type="button"
                      className="btn-apply-promo"
                      disabled={!promoCodeInput.trim() || isValidatingPromo}
                      onClick={handleApplyPromo}
                    >
                      {isValidatingPromo ? <Loader2 size={14} className="spin-icon" /> : 'Apply'}
                    </button>
                  </div>
                )}

                {promoError && (
                  <div className="promo-error-msg">
                    <AlertCircle size={13} />
                    <span>{promoError}</span>
                  </div>
                )}
              </div>
            )}

            {/* Payment Gateway Bridge Selector (when payable > 0) */}
            {payableTotal > 0 && (
              <div className="payment-gateway-section">
                <div className="section-label-row">
                  <label className="section-label">Payment Gateway Bridge</label>
                  <span className="encrypted-badge">
                    <Lock size={12} /> 256-Bit Encrypted
                  </span>
                </div>
                <div className="gateway-selector-grid">
                  <button
                    type="button"
                    className={`gateway-option ${paymentProvider === 'STRIPE' ? 'selected' : ''}`}
                    onClick={() => setPaymentProvider('STRIPE')}
                  >
                    <div className="gateway-header">
                      <CreditCard size={18} />
                      <span className="gateway-name">Stripe</span>
                    </div>
                    <span className="gateway-sub">Cards & Wallets (Test Sandbox 4242)</span>
                  </button>

                  <button
                    type="button"
                    className={`gateway-option ${paymentProvider === 'RAZORPAY' ? 'selected' : ''}`}
                    onClick={() => setPaymentProvider('RAZORPAY')}
                  >
                    <div className="gateway-header">
                      <Zap size={18} />
                      <span className="gateway-name">Razorpay</span>
                    </div>
                    <span className="gateway-sub">UPI & Cards (Sandbox Ready)</span>
                  </button>

                  <button
                    type="button"
                    className={`gateway-option ${paymentProvider === 'SIMULATED' ? 'selected' : ''}`}
                    onClick={() => setPaymentProvider('SIMULATED')}
                  >
                    <div className="gateway-header">
                      <Sparkles size={18} />
                      <span className="gateway-name">FastPay Instant</span>
                    </div>
                    <span className="gateway-sub">One-Click Auto-Approve</span>
                  </button>
                </div>
              </div>
            )}

            {/* Processing banner */}
            {isSubmitting && processingStatus && (
              <div className="payment-processing-banner">
                <Loader2 size={16} className="spin-icon" />
                <span>{processingStatus}</span>
              </div>
            )}

            {/* Order Total & Checkout */}
            <div className="ticket-modal-footer">
              <div className="footer-price-breakdown">
                <span className="total-tickets-count">
                  {totalTickets} ticket{totalTickets === 1 ? '' : 's'} selected
                </span>
                {appliedPromo && discountAmount > 0 ? (
                  <div className="total-amount-discount-wrap">
                    <span className="original-strikethrough">{currSymbol}{convertedGrandTotal.toFixed(2)}</span>
                    <span className="discount-tag-amount">-{currSymbol}{convertedDiscountAmount.toFixed(2)}</span>
                    <span className="total-amount-display">
                      Total: <strong>{currSymbol}{convertedPayableTotal.toFixed(2)}</strong>
                    </span>
                  </div>
                ) : (
                  <span className="total-amount-display">
                    Total: <strong>{currSymbol}{convertedGrandTotal.toFixed(2)}</strong>
                  </span>
                )}
              </div>

              <button
                type="submit"
                className="btn-confirm-checkout"
                disabled={isSubmitting || totalTickets <= 0}
              >
                <ShieldCheck size={16} />
                <span>
                  {isSubmitting
                    ? 'Authorizing...'
                    : payableTotal > 0
                    ? `Pay ${currSymbol}${convertedPayableTotal.toFixed(2)} & Reserve`
                    : 'Confirm & Reserve Pass'}
                </span>
              </button>
            </div>
          </form>
        )}
      </div>

      {showSeatMap && (
        <VisualSeatSelectorModal
          event={event}
          onClose={() => setShowSeatMap(false)}
          onSelectSeat={(seat) => {
            setSelectedSeatHold(seat);
            if (seat.tierId) {
              setSelectedQuantities({ [seat.tierId]: 1 });
            } else if (tiers.length > 0) {
              setSelectedQuantities({ [tiers[0].id]: 1 });
            }
          }}
          onOpenAuth={onOpenAuth}
          isAuthenticated={isAuthenticated}
        />
      )}
    </div>
  );
}
