import React, { useState, useEffect, useCallback } from 'react';
import { 
  Search, 
  Filter, 
  MapPin, 
  Globe, 
  Sparkles, 
  Layers, 
  RefreshCw,
  X,
  Plus,
  Calendar,
  Clock
} from 'lucide-react';
import { eventService, categoryService } from '../services/api';
import EventCard from './EventCard';
import EventDetailModal from './EventDetailModal';
import TicketSelectorModal from './TicketSelectorModal';
import './EventDiscovery.css';

export default function EventDiscovery({ onNavigateToStudio, onSelectTab, onOpenAuth }) {
  const [events, setEvents] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [formatFilter, setFormatFilter] = useState('ALL'); // 'ALL' | 'IN_PERSON' | 'ONLINE'
  const [timelineFilter, setTimelineFilter] = useState('UPCOMING'); // 'ALL' | 'UPCOMING' | 'THIS_WEEK'
  const [selectedCity, setSelectedCity] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [ticketModalEvent, setTicketModalEvent] = useState(null);

  // Load active categories
  useEffect(() => {
    async function fetchCategories() {
      try {
        const cats = await categoryService.getActiveCategories();
        setCategories(cats);
      } catch (err) {
        console.error('Failed to load categories', err);
      }
    }
    fetchCategories();
  }, []);

  // Search published events
  const loadEvents = useCallback(async () => {
    setIsLoading(true);
    try {
      const isOnlineParam = formatFilter === 'ONLINE' ? true : formatFilter === 'IN_PERSON' ? false : null;
      const res = await eventService.searchEvents({
        categoryId: selectedCategory,
        search: searchTerm.trim() || null,
        isOnline: isOnlineParam,
        city: selectedCity.trim() || null,
        page: 0,
        size: 50,
      });

      let list = res?.content || [];
      const now = new Date();

      if (timelineFilter === 'UPCOMING') {
        list = list.filter(e => !e.endTime || new Date(e.endTime) >= now);
      } else if (timelineFilter === 'THIS_WEEK') {
        const weekAhead = new Date();
        weekAhead.setDate(weekAhead.getDate() + 7);
        list = list.filter(e => {
          if (!e.startTime) return false;
          const s = new Date(e.startTime);
          return s >= now && s <= weekAhead;
        });
      }

      setEvents(list);
    } catch (err) {
      console.error('Failed to load published events', err);
      setEvents([]);
    } finally {
      setIsLoading(false);
    }
  }, [selectedCategory, searchTerm, formatFilter, timelineFilter, selectedCity]);

  useEffect(() => {
    const timer = setTimeout(() => {
      loadEvents();
    }, 250);
    return () => clearTimeout(timer);
  }, [loadEvents]);

  const clearFilters = () => {
    setSelectedCategory(null);
    setSearchTerm('');
    setFormatFilter('ALL');
    setTimelineFilter('ALL');
    setSelectedCity('');
  };

  return (
    <div className="event-discovery">
      {/* Search & Hero Filter Bar */}
      <div className="discovery-header">
        <div className="discovery-headline">
          <h2>Discover Experiences & Summits</h2>
          <p>Explore verified conferences, masterclasses, and music festivals curated on PulseEvent OS.</p>
        </div>

        {/* Search Input Row */}
        <div className="search-control-bar">
          <div className="search-input-wrap">
            <Search size={18} className="search-icon" />
            <input
              type="text"
              placeholder="Search by event title, keynote topic, or technology..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="search-input"
            />
            {searchTerm && (
              <button className="search-clear-btn" onClick={() => setSearchTerm('')}>
                <X size={14} />
              </button>
            )}
          </div>

          {/* Timeline Switchers */}
          <div className="format-toggle-group timeline-toggle-group">
            <button
              className={`format-btn ${timelineFilter === 'UPCOMING' ? 'active' : ''}`}
              onClick={() => setTimelineFilter('UPCOMING')}
            >
              <Calendar size={13} />
              <span>Upcoming</span>
            </button>
            <button
              className={`format-btn ${timelineFilter === 'THIS_WEEK' ? 'active' : ''}`}
              onClick={() => setTimelineFilter('THIS_WEEK')}
            >
              <Clock size={13} />
              <span>This Week</span>
            </button>
            <button
              className={`format-btn ${timelineFilter === 'ALL' ? 'active' : ''}`}
              onClick={() => setTimelineFilter('ALL')}
            >
              <span>All Dates</span>
            </button>
          </div>

          {/* Mode Switchers */}
          <div className="format-toggle-group">
            <button
              className={`format-btn ${formatFilter === 'ALL' ? 'active' : ''}`}
              onClick={() => setFormatFilter('ALL')}
            >
              All Formats
            </button>
            <button
              className={`format-btn ${formatFilter === 'IN_PERSON' ? 'active' : ''}`}
              onClick={() => setFormatFilter('IN_PERSON')}
            >
              <MapPin size={13} />
              <span>In-Person</span>
            </button>
            <button
              className={`format-btn ${formatFilter === 'ONLINE' ? 'active' : ''}`}
              onClick={() => setFormatFilter('ONLINE')}
            >
              <Globe size={13} />
              <span>Virtual</span>
            </button>
          </div>
        </div>

        {/* Category Pills Bar */}
        <div className="category-scroll-bar">
          <button
            className={`category-pill ${selectedCategory === null ? 'active' : ''}`}
            onClick={() => setSelectedCategory(null)}
          >
            <span>All Categories</span>
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              className={`category-pill ${selectedCategory === cat.id ? 'active' : ''}`}
              onClick={() => setSelectedCategory(selectedCategory === cat.id ? null : cat.id)}
            >
              <span>{cat.name}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Events Grid */}
      <div className="events-grid-container">
        {isLoading ? (
          <div className="loading-grid">
            {[1, 2, 3, 4, 5, 6].map((n) => (
              <div key={n} className="event-skeleton-card">
                <div className="skeleton-media"></div>
                <div className="skeleton-body">
                  <div className="skeleton-line short"></div>
                  <div className="skeleton-line"></div>
                  <div className="skeleton-line meta"></div>
                </div>
              </div>
            ))}
          </div>
        ) : events.length > 0 ? (
          <div className="events-grid">
            {events.map((evt) => (
              <EventCard
                key={evt.id}
                event={evt}
                onClick={(item) => setSelectedEvent(item)}
              />
            ))}
          </div>
        ) : (
          <div className="empty-discovery">
            <div className="empty-icon-wrap">
              <Sparkles size={32} />
            </div>
            <h3>No Published Events Found</h3>
            <p>
              {searchTerm || selectedCategory || formatFilter !== 'ALL'
                ? 'Try broadening your search keywords or clearing active filters.'
                : 'There are currently no live published events in the public catalog. Organizers can create and publish events from the Studio.'}
            </p>
            <div className="empty-actions">
              {(searchTerm || selectedCategory || formatFilter !== 'ALL') && (
                <button className="btn-empty-clear" onClick={clearFilters}>
                  Clear All Filters
                </button>
              )}
              {onNavigateToStudio && (
                <button className="btn-empty-create" onClick={onNavigateToStudio}>
                  <Plus size={16} />
                  <span>Open Organizer Studio</span>
                </button>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Event Detail Modal */}
      {selectedEvent && (
        <EventDetailModal
          event={selectedEvent}
          onClose={() => setSelectedEvent(null)}
          onOpenAuth={onOpenAuth}
          onRegisterInterest={(evt) => {
            setSelectedEvent(null);
            setTicketModalEvent(evt);
          }}
        />
      )}

      {/* Ticket Tier Selector & Checkout Modal */}
      {ticketModalEvent && (
        <TicketSelectorModal
          event={ticketModalEvent}
          onClose={() => setTicketModalEvent(null)}
          onOpenAuth={onOpenAuth}
          onOrderSuccess={(order, navigateToPasses) => {
            if (navigateToPasses && onSelectTab) {
              onSelectTab('my-tickets');
            }
          }}
        />
      )}
    </div>
  );
}
