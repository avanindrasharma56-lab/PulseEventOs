import React, { useState, useEffect, useRef } from 'react';
import { 
  Bell, 
  CheckCheck, 
  Ticket, 
  CheckCircle2, 
  AlertTriangle, 
  Scan, 
  Info,
  X
} from 'lucide-react';
import { notificationService } from '../services/api';
import './NotificationBell.css';

export default function NotificationBell({ isAuthenticated }) {
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const popoverRef = useRef(null);

  // Poll or fetch unread count
  useEffect(() => {
    if (!isAuthenticated) {
      setUnreadCount(0);
      setNotifications([]);
      return;
    }

    async function fetchCount() {
      try {
        const count = await notificationService.getUnreadCount();
        setUnreadCount(count);
      } catch (err) {
        // silent fail on unauth
      }
    }

    fetchCount();
    const interval = setInterval(fetchCount, 20000); // 20s poll
    return () => clearInterval(interval);
  }, [isAuthenticated]);

  // Close popover when clicking outside
  useEffect(() => {
    function handleClickOutside(event) {
      if (popoverRef.current && !popoverRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  const handleToggle = async () => {
    if (!isOpen) {
      setIsLoading(true);
      try {
        const list = await notificationService.getNotifications();
        setNotifications(list);
      } catch (err) {
        console.error('Failed to load notifications', err);
      } finally {
        setIsLoading(false);
      }
    }
    setIsOpen(prev => !prev);
  };

  const handleMarkAsRead = async (notifId, isRead) => {
    if (isRead) return;
    try {
      await notificationService.markAsRead(notifId);
      setNotifications(prev =>
        prev.map(n => n.id === notifId ? { ...n, read: true } : n)
      );
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (err) {
      console.error('Failed to mark notification as read', err);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (err) {
      console.error('Failed to mark all as read', err);
    }
  };

  const getNotificationIcon = (type) => {
    switch (type) {
      case 'ORDER_CONFIRMED':
        return <Ticket size={16} className="icon-order" />;
      case 'CHECK_IN_SUCCESS':
        return <Scan size={16} className="icon-checkin" />;
      case 'EVENT_APPROVED':
        return <CheckCircle2 size={16} className="icon-approved" />;
      case 'EVENT_REJECTED':
        return <AlertTriangle size={16} className="icon-rejected" />;
      default:
        return <Info size={16} className="icon-system" />;
    }
  };

  const formatTime = (timeStr) => {
    if (!timeStr) return '';
    const d = new Date(timeStr);
    return d.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (!isAuthenticated) return null;

  return (
    <div className="notification-bell-container" ref={popoverRef}>
      <button 
        className={`bell-trigger-btn ${isOpen ? 'active' : ''}`}
        onClick={handleToggle}
        aria-label="View notifications"
        title="Notifications"
      >
        <Bell size={18} />
        {unreadCount > 0 && (
          <span className="bell-badge">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="notifications-popover">
          <div className="popover-header">
            <div className="popover-title-row">
              <h4>Activity & Updates</h4>
              {unreadCount > 0 && (
                <span className="unread-counter-tag">{unreadCount} unread</span>
              )}
            </div>

            <div className="header-actions">
              {unreadCount > 0 && (
                <button 
                  className="btn-mark-all"
                  onClick={handleMarkAllAsRead}
                  title="Mark all as read"
                >
                  <CheckCheck size={14} />
                  <span>Mark all read</span>
                </button>
              )}
              <button 
                className="btn-close-popover" 
                onClick={() => setIsOpen(false)}
                aria-label="Close"
              >
                <X size={15} />
              </button>
            </div>
          </div>

          <div className="popover-body">
            {isLoading ? (
              <div className="popover-loading">Loading updates...</div>
            ) : notifications.length === 0 ? (
              <div className="popover-empty">
                <Bell size={32} className="empty-bell-icon" />
                <p>No notifications yet</p>
                <span>Orders, check-ins, and event updates will appear here.</span>
              </div>
            ) : (
              <div className="notifications-list">
                {notifications.map(notif => {
                  const isRead = notif.read || notif.isRead;
                  return (
                    <div 
                      key={notif.id}
                      className={`notification-item ${isRead ? 'read' : 'unread'}`}
                      onClick={() => handleMarkAsRead(notif.id, isRead)}
                    >
                      <div className="item-icon-wrap">
                        {getNotificationIcon(notif.type)}
                      </div>

                      <div className="item-content">
                        <div className="item-top">
                          <span className="item-title">{notif.title}</span>
                          <span className="item-time">{formatTime(notif.createdAt)}</span>
                        </div>
                        <p className="item-msg">{notif.message}</p>
                      </div>

                      {!isRead && <span className="item-unread-dot"></span>}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
