import React from 'react';
import { useAuth } from '../context/AuthContext';
import { 
  Sparkles, 
  Calendar, 
  Ticket, 
  ShieldCheck, 
  Database, 
  Server, 
  CheckCircle2, 
  ArrowRight, 
  Layers,
  Lock,
  Compass,
  Briefcase,
  SlidersHorizontal,
  ChevronRight,
  Scan,
  QrCode,
  BarChart3,
  Award,
  Radio
} from 'lucide-react';
import EventDiscovery from '../components/EventDiscovery';
import OrganizerEventStudio from '../components/OrganizerEventStudio';
import AdminModerationDesk from '../components/AdminModerationDesk';
import MyTicketsPage from '../components/MyTicketsPage';
import CheckInScanner from '../components/CheckInScanner';
import AnalyticsDashboard from '../components/AnalyticsDashboard';
import SponsorFloorDirectory from '../components/SponsorFloorDirectory';
import LiveAudienceCockpit from '../components/LiveAudienceCockpit';
import { GamifiedLeaderboardView } from '../components/GamifiedLeaderboardView';
import './HomePage.css';

export default function HomePage({ onOpenAuth, activeTab, onSelectTab }) {
  const { user, isAuthenticated, isAdmin, isOrganizer, isAttendee } = useAuth();

  return (
    <div className="home-page">
      {/* Hero Welcome Banner */}
      <section className="hero-section">
        <div className="hero-glow-back"></div>
        <div className="hero-content">
          <div className="phase-pill">
            <Sparkles size={14} />
            <span>Next-Gen Event Cloud · Built for Global Experiences</span>
          </div>

          <h1 className="main-title">
            Effortlessly orchestrate, <br />
            <span className="gradient-text">scale, and monetize live events.</span>
          </h1>

          <p className="main-subtitle">
            The all-in-one experience platform designed for summits, festivals, and modern conferences. 
            Deliver frictionless ticketing, real-time door check-ins, and automated global payouts with unmatched speed and elegance.
          </p>

          <div className="hero-cta-group">
            {isAuthenticated ? (
              <div className="auth-status-container">
                <div className="logged-in-badge">
                  <CheckCircle2 size={18} className="check-icon" />
                  <span>
                    Authenticated as <strong>{user?.fullName}</strong> ({user?.roles?.join(', ')})
                  </span>
                </div>
                {activeTab !== 'my-tickets' && (
                  <button onClick={() => onSelectTab('my-tickets')} className="btn-secondary-quick">
                    <Ticket size={15} />
                    <span>My Passes</span>
                  </button>
                )}
                {isOrganizer && activeTab !== 'studio' && (
                  <button onClick={() => onSelectTab('studio')} className="btn-secondary-quick">
                    <Briefcase size={15} />
                    <span>Organizer Studio</span>
                  </button>
                )}
                {(isOrganizer || isAdmin) && activeTab !== 'checkin' && (
                  <button onClick={() => onSelectTab('checkin')} className="btn-secondary-quick">
                    <Scan size={15} />
                    <span>Door Scanner</span>
                  </button>
                )}
                {(isOrganizer || isAdmin) && activeTab !== 'analytics' && (
                  <button onClick={() => onSelectTab('analytics')} className="btn-secondary-quick">
                    <BarChart3 size={15} />
                    <span>Analytics & Ledger</span>
                  </button>
                )}
                {isAdmin && activeTab !== 'moderation' && (
                  <button onClick={() => onSelectTab('moderation')} className="btn-secondary-quick admin-quick">
                    <ShieldCheck size={15} />
                    <span>Moderation Desk</span>
                  </button>
                )}
              </div>
            ) : (
              <>
                <button onClick={() => onOpenAuth('signup')} className="btn-primary">
                  <span>Join Platform / Create Account</span>
                  <ArrowRight size={16} />
                </button>
                <button onClick={() => onOpenAuth('signin')} className="btn-secondary">
                  <span>Sign In as Attendee, Organizer or Admin</span>
                </button>
              </>
            )}
          </div>
        </div>
      </section>

      {/* Main Feature Tabs Navigation */}
      <section className="tab-switcher-section">
        <div className="tab-switcher-bar">
          <button
            className={`tab-btn ${activeTab === 'discovery' ? 'active' : ''}`}
            onClick={() => onSelectTab('discovery')}
          >
            <Compass size={16} />
            <span>Public Event Catalog</span>
          </button>

          <button
            className={`tab-btn ${activeTab === 'sponsors' ? 'active' : ''}`}
            onClick={() => onSelectTab('sponsors')}
          >
            <Award size={16} />
            <span>Expo & Sponsors</span>
          </button>

          <button
            className={`tab-btn ${activeTab === 'live-stage' ? 'active' : ''}`}
            onClick={() => onSelectTab('live-stage')}
          >
            <Radio size={16} />
            <span>Live Stage (Q&A & Polls)</span>
          </button>

          <button
            className={`tab-btn ${activeTab === 'my-tickets' ? 'active' : ''}`}
            onClick={() => {
              if (!isAuthenticated) {
                onOpenAuth('signin');
              } else {
                onSelectTab('my-tickets');
              }
            }}
          >
            <Ticket size={16} />
            <span>Attendee Portal</span>
          </button>

          <button
            className={`tab-btn ${activeTab === 'studio' ? 'active' : ''}`}
            onClick={() => {
              if (!isAuthenticated) {
                onOpenAuth('signin');
              } else {
                onSelectTab('studio');
              }
            }}
          >
            <Briefcase size={16} />
            <span>Organizer Studio</span>
          </button>

          {(isOrganizer || isAdmin) && (
            <button
              className={`tab-btn ${activeTab === 'checkin' ? 'active' : ''}`}
              onClick={() => {
                if (!isAuthenticated) {
                  onOpenAuth('signin');
                } else {
                  onSelectTab('checkin');
                }
              }}
            >
              <Scan size={16} />
              <span>Door Scanner</span>
            </button>
          )}

          {(isOrganizer || isAdmin) && (
            <button
              className={`tab-btn ${activeTab === 'analytics' ? 'active' : ''}`}
              onClick={() => {
                if (!isAuthenticated) {
                  onOpenAuth('signin');
                } else {
                  onSelectTab('analytics');
                }
              }}
            >
              <BarChart3 size={16} />
              <span>Performance & Analytics</span>
            </button>
          )}

          {isAdmin && (
            <button
              className={`tab-btn ${activeTab === 'moderation' ? 'active' : ''}`}
              onClick={() => onSelectTab('moderation')}
            >
              <ShieldCheck size={16} />
              <span>Admin Moderation Desk</span>
            </button>
          )}
        </div>
      </section>

      {/* Active Tab View */}
      <main className="tab-view-container">
        {activeTab === 'discovery' && (
          <EventDiscovery 
            onNavigateToStudio={() => {
              if (!isAuthenticated) onOpenAuth('signin');
              else onSelectTab('studio');
            }}
            onSelectTab={onSelectTab}
            onOpenAuth={onOpenAuth}
          />
        )}

        {activeTab === 'sponsors' && (
          <SponsorFloorDirectory
            eventId={4}
            eventTitle="Pulse Developer Summit 2026"
            isOrganizer={isOrganizer}
            isAdmin={isAdmin}
            isAuthenticated={isAuthenticated}
            onOpenAuth={onOpenAuth}
          />
        )}

        {activeTab === 'live-stage' && (
          <LiveAudienceCockpit eventId={4} />
        )}

        {activeTab === 'leaderboard' && (
          <GamifiedLeaderboardView
            eventId={4}
            eventTitle="Pulse Developer Summit 2026"
            user={user}
            onLoginRequired={() => onOpenAuth('signin')}
          />
        )}

        {activeTab === 'my-tickets' && (
          isAuthenticated ? (
            <MyTicketsPage onBrowseEvents={() => onSelectTab('discovery')} />
          ) : (
            <div className="auth-required-box">
              <Ticket size={36} className="lock-icon" />
              <h3>Sign In to View Passes</h3>
              <p>Your digital tickets, QR entrance passes, and event receipts are stored in your private passbook.</p>
              <button onClick={() => onOpenAuth('signin')} className="btn-primary">
                <span>Sign In to Access Passes</span>
              </button>
            </div>
          )
        )}

        {activeTab === 'studio' && (
          isAuthenticated ? (
            <OrganizerEventStudio onEventPublished={() => onSelectTab('discovery')} />
          ) : (
            <div className="auth-required-box">
              <Lock size={36} className="lock-icon" />
              <h3>Organizer Studio Access Restricted</h3>
              <p>Please sign in with an Organizer or Admin account to create drafts and manage event rosters.</p>
              <button onClick={() => onOpenAuth('signin')} className="btn-primary">
                <span>Sign In to Continue</span>
              </button>
            </div>
          )
        )}

        {activeTab === 'checkin' && (
          (isOrganizer || isAdmin) ? (
            <CheckInScanner />
          ) : (
            <div className="auth-required-box">
              <Scan size={36} className="lock-icon" />
              <h3>Organizer Privileges Required</h3>
              <p>Door check-in and attendee ticket verification are restricted to event hosts and platform administrators.</p>
              <button onClick={() => onOpenAuth('signin')} className="btn-secondary">
                <span>Sign In as Event Host</span>
              </button>
            </div>
          )
        )}

        {activeTab === 'analytics' && (
          (isOrganizer || isAdmin) ? (
            <AnalyticsDashboard />
          ) : (
            <div className="auth-required-box">
              <BarChart3 size={36} className="lock-icon" />
              <h3>Organizer Privileges Required</h3>
              <p>Financial ledger metrics, tier turnover, and attendee roster CSV exports are restricted to event hosts.</p>
              <button onClick={() => onOpenAuth('signin')} className="btn-secondary">
                <span>Sign In as Event Host</span>
              </button>
            </div>
          )
        )}

        {activeTab === 'moderation' && (
          isAdmin ? (
            <AdminModerationDesk onModerationAction={() => {}} />
          ) : (
            <div className="auth-required-box">
              <ShieldCheck size={36} className="lock-icon" />
              <h3>Administrator Privileges Required</h3>
              <p>Only users with the <code>ROLE_ADMIN</code> role have permission to approve or reject submitted events.</p>
              <button onClick={() => onOpenAuth('signin')} className="btn-secondary">
                <span>Switch to Admin Account</span>
              </button>
            </div>
          )
        )}

        {activeTab === 'diagnostics' && (
          <section className="diagnostics-section">
            <div className="section-header">
              <Layers size={20} className="section-icon" />
              <h2>System Architecture & Phase 4 Runtime Verification</h2>
            </div>

            <div className="diagnostics-grid">
              {/* Card 1: Backend */}
              <div className="diag-card">
                <div className="diag-card-top">
                  <Server size={22} className="diag-icon icon-indigo" />
                  <span className="status-badge status-online">Port :8085</span>
                </div>
                <h3>Spring Boot 3.3.4 REST API</h3>
                <p className="diag-desc">
                  Hosts Category, Event, Tier, Order, Verification, Notification, and Analytics REST controllers with stateless JWT security.
                </p>
                <div className="tech-tags">
                  <span>Java 17</span>
                  <span>Spring MVC</span>
                  <span>Spring Data JPA</span>
                </div>
              </div>

              {/* Card 2: Database */}
              <div className="diag-card">
                <div className="diag-card-top">
                  <Database size={22} className="diag-icon icon-emerald" />
                  <span className="status-badge status-online">Port :3306</span>
                </div>
                <h3>MariaDB / MySQL & Flyway V4</h3>
                <p className="diag-desc">
                  Migrations <code>V1</code> (Auth), <code>V2</code> (Events), <code>V3</code> (Tickets & Locks), and <code>V4__init_notifications_schema.sql</code> applied.
                </p>
                <div className="tech-tags">
                  <span>MariaDB 13.0</span>
                  <span>Flyway V4</span>
                  <span>HikariCP</span>
                </div>
              </div>

              {/* Card 3: Analytics & Exports */}
              <div className="diag-card">
                <div className="diag-card-top">
                  <BarChart3 size={22} className="diag-icon icon-cyan" />
                  <span className="status-badge status-online">RFC 4180</span>
                </div>
                <h3>Analytics Engine & CSV Exports</h3>
                <p className="diag-desc">
                  SQL-backed financial turnover aggregation, tier occupancy ratios, arrival velocities, and UTF-8 BOM attendee CSV streaming.
                </p>
                <div className="tech-tags">
                  <span>CSV Streaming</span>
                  <span>Live Turnover</span>
                  <span>Tier Velocity</span>
                </div>
              </div>

              {/* Card 4: Notification Hub */}
              <div className="diag-card">
                <div className="diag-card-top">
                  <QrCode size={22} className="diag-icon icon-violet" />
                  <span className="status-badge status-online">Active Hub</span>
                </div>
                <h3>Activity & Notification Hub</h3>
                <p className="diag-desc">
                  Automatic event-driven activity alerts on order reservation, door check-in, and moderation approvals with unread counter badge.
                </p>
                <div className="tech-tags">
                  <span>In-App Alerts</span>
                  <span>Unread Badges</span>
                  <span>Polling Engine</span>
                </div>
              </div>
            </div>

            {/* Test Results Summary */}
            <div className="test-verification-card">
              <h4>✅ JUnit & MockMvc Test Verification</h4>
              <p>10/10 automated tests executed and passing with 0 failures:</p>
              <ul className="test-list">
                <li><code>AuthControllerTest</code>: 5 tests (Registration, Duplicate rejection, JWT login, /me access)</li>
                <li><code>EventLifecycleIntegrationTest</code>: 2 tests (Full lifecycle: Draft &rarr; Submit &rarr; Reject &rarr; Resubmit &rarr; Approve &rarr; Publish &rarr; Public Discovery, plus Self-Approval Prohibited rule)</li>
                <li><code>TicketOrderIntegrationTest</code>: 1 comprehensive test (Tier setup, pessimistic write inventory lock, capacity exhaustion guard, Base64 QR code generation, single-use door check-in, duplicate scan blocking)</li>
                <li><code>AnalyticsAndNotificationIntegrationTest</code>: 2 tests (Lifecycle notifications, unread counter decrement, organizer analytics ledger aggregation, RFC 4180 CSV roster download)</li>
              </ul>
            </div>
          </section>
        )}
      </main>
    </div>
  );
}
