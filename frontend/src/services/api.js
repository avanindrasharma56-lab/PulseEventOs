// API Client with automatic JWT Bearer token handling

const TOKEN_KEY = 'eventflow_token';
const USER_KEY = 'eventflow_user';

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token) => localStorage.setItem(TOKEN_KEY, token),
  remove: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
  getUser: () => {
    try {
      const data = localStorage.getItem(USER_KEY);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  },
  setUser: (user) => localStorage.setItem(USER_KEY, JSON.stringify(user)),
};

export async function request(endpoint, options = {}) {
  const token = tokenStorage.get();

  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(endpoint, {
    ...options,
    headers,
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    if (response.status === 401) {
      tokenStorage.remove();
    }
    const message = data?.message || `Request failed with status ${response.status}`;
    const error = new Error(message);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

export const authService = {
  async register({ fullName, email, password, phone, role }) {
    const res = await request('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ fullName, email, password, phone, role }),
    });
    if (res.data?.accessToken) {
      tokenStorage.set(res.data.accessToken);
      tokenStorage.setUser(res.data.user);
    }
    return res.data;
  },

  async login({ email, password }) {
    const res = await request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    if (res.data?.accessToken) {
      tokenStorage.set(res.data.accessToken);
      tokenStorage.setUser(res.data.user);
    }
    return res.data;
  },

  async getCurrentUser() {
    const res = await request('/api/auth/me', {
      method: 'GET',
    });
    if (res.data) {
      tokenStorage.setUser(res.data);
    }
    return res.data;
  },

  async updateProfile({ fullName, phone }) {
    const res = await request('/api/auth/profile', {
      method: 'PUT',
      body: JSON.stringify({ fullName, phone }),
    });
    if (res.data) {
      tokenStorage.setUser(res.data);
    }
    return res.data;
  },

  logout() {
    tokenStorage.remove();
  },
};

export const categoryService = {
  async getActiveCategories() {
    const res = await request('/api/categories');
    return res.data || [];
  },

  async getAllCategories() {
    const res = await request('/api/categories/all');
    return res.data || [];
  },

  async createCategory(data) {
    const res = await request('/api/categories', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return res.data;
  },

  async toggleActive(id) {
    const res = await request(`/api/categories/${id}/toggle`, {
      method: 'PATCH',
    });
    return res.data;
  },
};

export const venueService = {
  async getAllVenues() {
    const res = await request('/api/venues');
    return res.data || [];
  },

  async createVenue(data) {
    const res = await request('/api/venues', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return res.data;
  },
};

export const eventService = {
  async searchEvents({ categoryId, search, isOnline, city, startDate, endDate, page = 0, size = 12 } = {}) {
    const params = new URLSearchParams();
    if (categoryId) params.append('categoryId', categoryId);
    if (search) params.append('search', search);
    if (isOnline !== undefined && isOnline !== null && isOnline !== '') params.append('isOnline', isOnline);
    if (city) params.append('city', city);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    params.append('page', page);
    params.append('size', size);

    const res = await request(`/api/events?${params.toString()}`);
    return res.data;
  },

  async getEventBySlug(slug) {
    const res = await request(`/api/events/${slug}`);
    return res.data;
  },

  async getEventById(id) {
    const res = await request(`/api/events/id/${id}`);
    return res.data;
  },

  async createDraft(data) {
    const res = await request('/api/events', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return res.data;
  },

  async updateEvent(id, data) {
    const res = await request(`/api/events/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
    return res.data;
  },

  async submitForApproval(id) {
    const res = await request(`/api/events/${id}/submit`, {
      method: 'POST',
    });
    return res.data;
  },

  async publishEvent(id) {
    const res = await request(`/api/events/${id}/publish`, {
      method: 'POST',
    });
    return res.data;
  },

  async cancelEvent(id) {
    const res = await request(`/api/events/${id}/cancel`, {
      method: 'POST',
    });
    return res.data;
  },

  async deleteEvent(id) {
    const res = await request(`/api/events/${id}`, {
      method: 'DELETE',
    });
    return res.data;
  },

  async getMyEvents() {
    const res = await request('/api/events/my-events');
    return res.data || [];
  },
};

export const adminEventService = {
  async getPendingEvents(page = 0, size = 20) {
    const res = await request(`/api/admin/events/pending?page=${page}&size=${size}`);
    return res.data;
  },

  async approveEvent(id) {
    const res = await request(`/api/admin/events/${id}/approve`, {
      method: 'POST',
    });
    return res.data;
  },

  async rejectEvent(id, reason) {
    const res = await request(`/api/admin/events/${id}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
    return res.data;
  },
};

export const ticketTierService = {
  async getTiersForEvent(eventId) {
    const res = await request(`/api/events/${eventId}/tiers`);
    return res.data || [];
  },

  async createTier(eventId, data) {
    const res = await request(`/api/events/${eventId}/tiers`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return res.data;
  },

  async updateTier(eventId, tierId, data) {
    const res = await request(`/api/events/${eventId}/tiers/${tierId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
    return res.data;
  },

  async deleteTier(eventId, tierId) {
    const res = await request(`/api/events/${eventId}/tiers/${tierId}`, {
      method: 'DELETE',
    });
    return res.data;
  },
};

export const orderService = {
  async createOrder({ eventId, items, promoCode }) {
    const res = await request('/api/orders', {
      method: 'POST',
      body: JSON.stringify({ eventId, items, promoCode }),
    });
    return res.data;
  },

  async getMyOrders() {
    const res = await request('/api/orders/my-orders');
    return res.data || [];
  },
};

export const ticketPassService = {
  async getMyTickets() {
    const res = await request('/api/tickets/my-tickets');
    return res.data || [];
  },

  async getTicketByCode(ticketCode) {
    const res = await request(`/api/tickets/${ticketCode}`);
    return res.data;
  },

  async checkInTicket(ticketCode) {
    const res = await request('/api/tickets/check-in', {
      method: 'POST',
      body: JSON.stringify({ ticketCode }),
    });
    return res.data;
  },

  async getEventAttendeeTickets(eventId) {
    const res = await request(`/api/tickets/event/${eventId}`);
    return res.data || [];
  },

  async getDoorStats(eventId) {
    const res = await request(`/api/tickets/event/${eventId}/door-stats`);
    return res.data;
  },

  async downloadTicketPdf(ticketCode, filename = `ticket-${ticketCode}.pdf`) {
    const token = tokenStorage.get();
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    const res = await fetch(`/api/tickets/${ticketCode}/pdf`, { headers });
    if (!res.ok) {
      throw new Error(`Failed to download ticket pass: ${res.statusText}`);
    }
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  },

  async downloadLanyardBadgePdf(ticketCode, filename = `badge-${ticketCode}.pdf`) {
    const token = tokenStorage.get();
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    const res = await fetch(`/api/tickets/${ticketCode}/badge.pdf`, { headers });
    if (!res.ok) {
      throw new Error(`Failed to download lanyard badge: ${res.statusText}`);
    }
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  },

  async downloadBulkBadgesPdf(eventId, filename = `event-${eventId}-badges.pdf`) {
    const token = tokenStorage.get();
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    const res = await fetch(`/api/events/${eventId}/badges.pdf`, { headers });
    if (!res.ok) {
      throw new Error(`Failed to download bulk badges: ${res.statusText}`);
    }
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  },
};

export const analyticsService = {
  async getOrganizerAnalytics() {
    const res = await request('/api/analytics/organizer');
    return res.data;
  },

  async getEventAnalytics(eventId) {
    const res = await request(`/api/analytics/events/${eventId}`);
    return res.data;
  },

  async getAdminAnalytics() {
    const res = await request('/api/analytics/admin');
    return res.data;
  },
};

export const notificationService = {
  async getNotifications() {
    const res = await request('/api/notifications');
    return res.data || [];
  },

  async getUnreadCount() {
    const res = await request('/api/notifications/unread-count');
    return res.data?.unreadCount || 0;
  },

  async markAsRead(id) {
    const res = await request(`/api/notifications/${id}/read`, { method: 'PUT' });
    return res.data;
  },

  async markAllAsRead() {
    const res = await request('/api/notifications/read-all', { method: 'PUT' });
    return res.data;
  },
};

export const exportService = {
  async downloadAttendeeCsv(eventId, eventSlug = 'event') {
    const token = tokenStorage.get();
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    const res = await fetch(`/api/events/${eventId}/export/attendees`, { headers });
    if (!res.ok) {
      throw new Error(`Export failed: ${res.statusText}`);
    }
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `attendees-${eventSlug}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  },
};

export const paymentService = {
  async createCheckoutSession({ orderId, provider = 'STRIPE', successUrl, cancelUrl }) {
    const res = await request('/api/payments/checkout-session', {
      method: 'POST',
      body: JSON.stringify({ orderId, provider, successUrl, cancelUrl }),
    });
    return res.data;
  },

  async confirmSimulatedPayment(paymentReference) {
    const res = await request(`/api/payments/confirm/${paymentReference}`, {
      method: 'POST',
    });
    return res.data;
  },

  async getPaymentStatus(paymentReference) {
    const res = await request(`/api/payments/${paymentReference}`);
    return res.data;
  },

  async getOrderPayments(orderId) {
    const res = await request(`/api/payments/order/${orderId}`);
    return res.data || [];
  },
};

export const promoCodeService = {
  async validatePromoCode({ code, eventId, orderTotal }) {
    const res = await request('/api/promo-codes/validate', {
      method: 'POST',
      body: JSON.stringify({ code, eventId, orderTotal }),
    });
    return res.data;
  },

  async getAllPromoCodes() {
    const res = await request('/api/promo-codes');
    return res.data || [];
  },

  async createPromoCode(data) {
    const res = await request('/api/promo-codes', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return res.data;
  },
};

export const reviewService = {
  async getEventReviews(eventId) {
    const res = await request(`/api/events/${eventId}/reviews`);
    return res.data;
  },

  async submitReview(eventId, { rating, title, comment }) {
    const res = await request(`/api/events/${eventId}/reviews`, {
      method: 'POST',
      body: JSON.stringify({ rating, title, comment }),
    });
    return res.data;
  },
};

export const waitlistService = {
  async joinWaitlist(ticketTierId) {
    const res = await request('/api/waitlist/join', {
      method: 'POST',
      body: JSON.stringify({ ticketTierId }),
    });
    return res.data;
  },

  async leaveWaitlist(waitlistEntryId) {
    const res = await request(`/api/waitlist/${waitlistEntryId}`, {
      method: 'DELETE',
    });
    return res;
  },

  async getMyWaitlistEntries() {
    const res = await request('/api/waitlist/my-entries');
    return res.data || [];
  },

  async getTierWaitlist(tierId) {
    const res = await request(`/api/waitlist/tier/${tierId}`);
    return res.data || [];
  },

  async claimOffer(waitlistEntryId, attendeeName = null, attendeeEmail = null) {
    let url = `/api/waitlist/${waitlistEntryId}/claim`;
    const params = new URLSearchParams();
    if (attendeeName) params.append('attendeeName', attendeeName);
    if (attendeeEmail) params.append('attendeeEmail', attendeeEmail);
    if (params.toString()) {
      url += `?${params.toString()}`;
    }
    const res = await request(url, {
      method: 'POST',
    });
    return res.data;
  },
};

export const refundService = {
  async requestRefund({ orderId, reason }) {
    const res = await request('/api/refunds/request', {
      method: 'POST',
      body: JSON.stringify({ orderId, reason }),
    });
    return res.data;
  },

  async getMyRefundRequests() {
    const res = await request('/api/refunds/my-requests');
    return res.data || [];
  },

  async getAllRefunds() {
    const res = await request('/api/refunds');
    return res.data || [];
  },

  async reviewRefund(refundRequestId, { approved, adminNotes }) {
    const res = await request(`/api/refunds/${refundRequestId}/review`, {
      method: 'POST',
      body: JSON.stringify({ approved, adminNotes }),
    });
    return res.data;
  },
};

export const agendaService = {
  async getSpeakers(eventId) {
    const res = await request(`/api/events/${eventId}/speakers`);
    return res.data || [];
  },

  async createSpeaker(eventId, data) {
    const res = await request(`/api/events/${eventId}/speakers`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return res.data;
  },

  async deleteSpeaker(speakerId) {
    const res = await request(`/api/events/speakers/${speakerId}`, {
      method: 'DELETE',
    });
    return res;
  },

  async getSessions(eventId) {
    const res = await request(`/api/events/${eventId}/sessions`);
    return res.data || [];
  },

  async createSession(eventId, data) {
    const res = await request(`/api/events/${eventId}/sessions`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return res.data;
  },

  async deleteSession(sessionId) {
    const res = await request(`/api/events/sessions/${sessionId}`, {
      method: 'DELETE',
    });
    return res;
  },

  async toggleBookmark(sessionId) {
    const res = await request(`/api/events/sessions/${sessionId}/bookmark`, {
      method: 'POST',
    });
    return res.data;
  },

  async getMySchedule(eventId) {
    const res = await request(`/api/events/${eventId}/my-schedule`);
    return res.data || [];
  },

  async downloadIcal(eventId, eventSlug = 'event') {
    const token = tokenStorage.get();
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    const res = await fetch(`/api/events/${eventId}/export/ical`, { headers });
    if (!res.ok) throw new Error('Failed to export iCal schedule');
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${eventSlug}-schedule.ics`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  },
};

export const seatMapService = {
  async getSeatMap(eventId) {
    const res = await request(`/api/events/${eventId}/seat-map`);
    return res.data;
  },

  async holdSeat(eventId, seatId) {
    const res = await request(`/api/events/${eventId}/seats/${seatId}/hold`, {
      method: 'POST',
    });
    return res.data;
  },

  async releaseHold(eventId, seatId) {
    const res = await request(`/api/events/${eventId}/seats/${seatId}/hold`, {
      method: 'DELETE',
    });
    return res;
  },
};

export const pricingService = {
  async getCurrencies() {
    const res = await request('/api/pricing/currencies');
    return res.data || [];
  },

  async convertCurrency(amount, from = 'USD', to = 'EUR') {
    const res = await request('/api/pricing/convert', {
      method: 'POST',
      body: JSON.stringify({ amount, from, to }),
    });
    return res.data;
  },

  async getTierQuote(tierId, currency = 'USD') {
    const res = await request(`/api/pricing/tiers/${tierId}/quote?currency=${currency}`);
    return res.data;
  },

  async getEventRules(eventId) {
    const res = await request(`/api/events/${eventId}/pricing-rules`);
    return res.data || [];
  },

  async createPricingRule(eventId, { ruleType, thresholdPercent, multiplier }) {
    const params = new URLSearchParams({
      ruleType,
      multiplier,
    });
    if (thresholdPercent !== undefined && thresholdPercent !== null) {
      params.append('thresholdPercent', thresholdPercent);
    }
    const res = await request(`/api/events/${eventId}/pricing-rules?${params.toString()}`, {
      method: 'POST',
    });
    return res.data;
  },
};

export const sponsorService = {
  async getEventSponsors(eventId) {
    const res = await request(`/api/events/${eventId}/sponsors`);
    return res.data || [];
  },

  async createSponsor(eventId, data) {
    const res = await request(`/api/events/${eventId}/sponsors`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return res.data;
  },

  async deleteSponsor(eventId, sponsorId) {
    const res = await request(`/api/events/${eventId}/sponsors/${sponsorId}`, {
      method: 'DELETE',
    });
    return res;
  },

  async scanLead(sponsorId, { ticketCode, notes, rating }) {
    const res = await request(`/api/sponsors/${sponsorId}/leads/scan`, {
      method: 'POST',
      body: JSON.stringify({ ticketCode, notes, rating }),
    });
    return res.data;
  },

  async getSponsorLeads(sponsorId) {
    const res = await request(`/api/sponsors/${sponsorId}/leads`);
    return res.data || [];
  },

  async exportLeadsCsv(sponsorId, filename = `sponsor-${sponsorId}-leads.csv`) {
    const token = tokenStorage.get();
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    const res = await fetch(`/api/sponsors/${sponsorId}/leads/export.csv`, { headers });
    if (!res.ok) {
      throw new Error(`Failed to export leads: ${res.statusText}`);
    }
    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  },
};

export const aiService = {
  async generateEventCopy({ topic, category, targetAudience, tone, keyHighlights }) {
    const res = await request('/api/ai/generate-event-copy', {
      method: 'POST',
      body: JSON.stringify({ topic, category, targetAudience, tone, keyHighlights }),
    });
    return res.data;
  },
};

export const certificateService = {
  async claimCertificate(eventId, data = { certificateType: 'COMPLETION' }) {
    const res = await request(`/api/events/${eventId}/certificates/claim`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return res.data;
  },

  async verifyCertificate(certificateCode) {
    const res = await request(`/api/certificates/verify/${certificateCode}`);
    return res.data;
  },

  async getMyCertificates() {
    const res = await request('/api/certificates/my-certificates');
    return res.data || [];
  },

  async getEventCertificates(eventId) {
    const res = await request(`/api/events/${eventId}/certificates`);
    return res.data || [];
  },

  getCertificatePdfUrl(certificateCode) {
    return `/api/certificates/${certificateCode}/pdf`;
  },
};

export const liveEngagementService = {
  async getQuestions(eventId) {
    const res = await request(`/api/events/${eventId}/qa`);
    return res.data || [];
  },

  async submitQuestion(eventId, { questionText, anonymous = false, sessionId = null }) {
    const res = await request(`/api/events/${eventId}/qa`, {
      method: 'POST',
      body: JSON.stringify({ questionText, anonymous, sessionId }),
    });
    return res.data;
  },

  async toggleUpvote(questionId) {
    const res = await request(`/api/qa/${questionId}/upvote`, {
      method: 'POST',
    });
    return res.data;
  },

  async updateQuestionStatus(questionId, status) {
    const res = await request(`/api/qa/${questionId}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    });
    return res.data;
  },

  async getPolls(eventId) {
    const res = await request(`/api/events/${eventId}/polls`);
    return res.data || [];
  },

  async createPoll(eventId, { question, options, sessionId = null }) {
    const res = await request(`/api/events/${eventId}/polls`, {
      method: 'POST',
      body: JSON.stringify({ question, options, sessionId }),
    });
    return res.data;
  },

  async voteInPoll(pollId, optionId) {
    const res = await request(`/api/polls/${pollId}/vote`, {
      method: 'POST',
      body: JSON.stringify({ optionId }),
    });
    return res.data;
  },

  async updatePollStatus(pollId, status) {
    const res = await request(`/api/polls/${pollId}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    });
    return res.data;
  },
};

export const sessionBookingService = {
  async reserveSeat(sessionId) {
    const res = await request(`/api/sessions/${sessionId}/rsvp`, {
      method: 'POST',
    });
    return res.data;
  },

  async cancelRsvp(sessionId) {
    const res = await request(`/api/sessions/${sessionId}/rsvp`, {
      method: 'DELETE',
    });
    return res.data;
  },

  async getMyRsvps(eventId) {
    const res = await request(`/api/events/${eventId}/my-rsvps`);
    return res.data || [];
  },

  async getSessionCapacity(sessionId) {
    const res = await request(`/api/sessions/${sessionId}/capacity`);
    return res.data;
  },

  async getRoomCapacityHeatmap(eventId) {
    const res = await request(`/api/events/${eventId}/sessions/heatmap`);
    return res.data || [];
  },
};

export const networkingService = {
  async getMyProfile(eventId) {
    const res = await request(`/api/events/${eventId}/networking/profile`);
    return res.data;
  },

  async upsertProfile(eventId, data) {
    const res = await request(`/api/events/${eventId}/networking/profile`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return res.data;
  },

  async searchAttendees(eventId, { query = '', interest = '' } = {}) {
    const params = new URLSearchParams();
    if (query) params.append('query', query);
    if (interest) params.append('interest', interest);
    const qs = params.toString() ? `?${params.toString()}` : '';
    const res = await request(`/api/events/${eventId}/networking/attendees${qs}`);
    return res.data || [];
  },

  async requestMeeting(eventId, data) {
    const res = await request(`/api/events/${eventId}/networking/meetings`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return res.data;
  },

  async getMyMeetings(eventId) {
    const res = await request(`/api/events/${eventId}/networking/meetings`);
    return res.data || [];
  },

  async respondMeeting(meetingId, accept) {
    const res = await request(`/api/networking/meetings/${meetingId}/respond?accept=${accept}`, {
      method: 'PUT',
    });
    return res.data;
  },

  async cancelMeeting(meetingId) {
    const res = await request(`/api/networking/meetings/${meetingId}`, {
      method: 'DELETE',
    });
    return res.data;
  },
};

export const gamificationService = {
  async getProfile(eventId) {
    const res = await request(`/api/events/${eventId}/gamification/profile`);
    return res.data;
  },

  async getLeaderboard(eventId, limit = 20) {
    const res = await request(`/api/events/${eventId}/gamification/leaderboard?limit=${limit}`);
    return res.data || [];
  },

  async scanBooth(eventId, sponsorId) {
    const res = await request(`/api/events/${eventId}/gamification/scan-booth/${sponsorId}`, {
      method: 'POST',
    });
    return res.data;
  },

  async checkInSession(eventId, sessionId) {
    const res = await request(`/api/events/${eventId}/gamification/session-checkin/${sessionId}`, {
      method: 'POST',
    });
    return res.data;
  },

  async awardXp(eventId, data) {
    const res = await request(`/api/events/${eventId}/gamification/award-xp`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return res.data;
  },

  async claimSwag(eventId) {
    const res = await request(`/api/events/${eventId}/gamification/claim-swag`, {
      method: 'POST',
    });
    return res.data;
  },
};


