
import { useCallback, useEffect, useState } from 'react';
import {
    Bell, CheckCheck, ChevronLeft, ChevronRight, ClipboardList,
    ExternalLink, Filter, RefreshCw, Plus, X, Send
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import api from '../../api/client';
import '../../styles/notifications.css';

const TYPE_OPTIONS = [
    ['BOOKING_CONFIRMED', 'Booking confirmed'],
    ['BOOKING_REJECTED', 'Booking rejected'],
    ['BOOKING_CANCELLED', 'Booking cancelled'],
    ['BOOKING_REMINDER', 'Booking reminder'],
    ['WAITLIST_OFFER', 'Waitlist offer'],
    ['WORK_ORDER_CREATED', 'Work order created'],
    ['WORK_ORDER_ASSIGNED', 'Work order assigned'],
    ['WORK_ORDER_STARTED', 'Work order started'],
    ['WORK_ORDER_COMPLETED', 'Work order completed'],
    ['CALIBRATION_EXPIRED', 'Calibration expired'],
    ['IDLE_EQUIPMENT', 'Idle equipment'],
    ['SHARING_REQUEST_FORWARDED', 'Sharing request forwarded'],
    ['SHARING_ACCESS_REVOKED', 'Sharing access revoked'],
    ['GENERAL', 'General'],
];

function getReferencePath(item) {
    const ref = String(item.referenceType || '').toUpperCase();
    if (ref.includes('BOOKING') || ref === 'WAITLIST') return '/bookings';
    if (ref.includes('WORK_ORDER')) return '/work-orders';
    if (ref.includes('MAINTENANCE')) return '/maintenance-requests';
    if (ref.includes('CALIBRATION')) return '/calibration-overview';
    if (ref.includes('SHAR') || ref.includes('AGREEMENT')) return '/resource-sharing';
    if (ref.includes('RESOURCE') || ref.includes('EQUIPMENT')) return '/equipment';
    return '/notifications';
}

function formatDate(value) {
    if (!value) return 'Date unavailable';
    const date = new Date(value);
    return Number.isNaN(date.getTime())
        ? 'Date unavailable'
        : date.toLocaleString();
}

function iconFor(type) {
    const value = String(type || '').toUpperCase();
    if (value.includes('BOOKING') || value.includes('WAITLIST')) return '📅';
    if (value.includes('CALIBRATION')) return '🧪';
    if (value.includes('WORK_ORDER') || value.includes('MAINTENANCE')) return '🛠️';
    if (value.includes('SHARING') || value.includes('AGREEMENT')) return '🤝';
    if (value.includes('IDLE') || value.includes('EXPIRED')) return '⚠️';
    return '🔔';
}

export default function Notifications() {
    const navigate = useNavigate();

    const [items, setItems] = useState([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [unreadOnly, setUnreadOnly] = useState(false);
    const [type, setType] = useState('');
    const [loading, setLoading] = useState(true);
    const [busyId, setBusyId] = useState(null);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    // Create notification form
    const [showCreate, setShowCreate] = useState(false);
    const [creating, setCreating] = useState(false);
    const [newTitle, setNewTitle] = useState('');
    const [newMessage, setNewMessage] = useState('');
    const [newType, setNewType] = useState('GENERAL');

    const load = useCallback(async () => {
        setLoading(true);
        setError('');

        try {
            const response = await api.get('/notifications', {
                params: {
                    page,
                    size: 10,
                    unread: unreadOnly ? true : undefined,
                    type: type || undefined,
                },
            });

            const data = response.data || {};
            setItems(Array.isArray(data.content) ? data.content : []);
            setTotalPages(Number(data.totalPages || 0));
            setTotalElements(Number(data.totalElements || 0));
        } catch (err) {
            setError(
                err.response?.data?.message ||
                'Unable to load notifications. Please try again.'
            );
            setItems([]);
        } finally {
            setLoading(false);
        }
    }, [page, unreadOnly, type]);

    useEffect(() => {
        load();
    }, [load]);

    const applyFilters = (setter, value) => {
        setter(value);
        setPage(0);
        setSuccess('');
    };

    const createNotification = async (event) => {
        event.preventDefault();
        setError('');
        setSuccess('');

        if (!newTitle.trim() || !newMessage.trim()) {
            setError('Please enter both title and message.');
            return;
        }

        setCreating(true);

        try {
            await api.post('/notifications/test', {
                title: newTitle.trim(),
                message: newMessage.trim(),
                type: newType,
            });

            setNewTitle('');
            setNewMessage('');
            setNewType('GENERAL');
            setShowCreate(false);

            setType('');
            setUnreadOnly(false);
            setPage(0);

            setSuccess('Notification created successfully.');
        } catch (err) {
            setError(
                err.response?.data?.message ||
                'Unable to create notification. Please try again.'
            );
        } finally {
            setCreating(false);
        }
    };

    const markRead = async (item) => {
        if (item.isRead) return;

        setBusyId(item.id);
        setError('');

        try {
            await api.patch(`/notifications/${item.id}/read`);
            await load();
        } catch (err) {
            setError(err.response?.data?.message || 'Unable to update notification.');
        } finally {
            setBusyId(null);
        }
    };

    const markAllRead = async () => {
        setBusyId('all');
        setError('');

        try {
            await api.patch('/notifications/read-all');
            await load();
        } catch (err) {
            setError(err.response?.data?.message || 'Unable to mark all as read.');
        } finally {
            setBusyId(null);
        }
    };

    const openItem = async (item) => {
        if (!item.isRead) {
            await markRead(item);
        }

        const target = getReferencePath(item);

        navigate(
            item.referenceId
                ? `${target}?referenceId=${encodeURIComponent(item.referenceId)}`
                : target,
            { state: { notification: item } }
        );
    };

    return (
        <div className="notification-page">

            <section className="notification-page-toolbar">
                <div>
                    <div className="notification-page-kicker">
                        <Bell size={15} /> YOUR ACTIVITY
                    </div>
                    <h2>All notifications</h2>
                    <p>{totalElements} notifications in your account</p>
                </div>

                <div className="notification-toolbar-actions">
                    <button
                        className="notification-secondary-button"
                        onClick={load}
                        disabled={loading}
                    >
                        <RefreshCw size={16} /> Refresh
                    </button>

                    <button
                        className="notification-primary-button"
                        onClick={() => {
                            setShowCreate(value => !value);
                            setError('');
                            setSuccess('');
                        }}
                    >
                        {showCreate ? <X size={16} /> : <Plus size={16} />}
                        {showCreate ? 'Close form' : 'Create test notification'}
                    </button>

                    <button
                        className="notification-primary-button"
                        onClick={markAllRead}
                        disabled={loading || busyId === 'all'}
                    >
                        <CheckCheck size={16} /> Mark all read
                    </button>
                </div>
            </section>

            {showCreate && (
                <section className="notification-filter-card">
                    <div className="notification-filter-title">
                        <Send size={18} />
                        <strong>Create a test notification</strong>
                    </div>

                    <form
                        onSubmit={createNotification}
                        style={{
                            display: 'grid',
                            gap: '14px',
                            marginTop: '16px',
                            maxWidth: '650px',
                        }}
                    >
                        <label style={{ display: 'grid', gap: '7px' }}>
                            <strong>Notification title</strong>
                            <input
                                type="text"
                                value={newTitle}
                                onChange={e => setNewTitle(e.target.value)}
                                placeholder="e.g. Equipment booking confirmed"
                                maxLength={100}
                                required
                                style={{
                                    padding: '11px 13px',
                                    border: '1px solid #d1d5db',
                                    borderRadius: '8px',
                                    width: '100%',
                                }}
                            />
                        </label>

                        <label style={{ display: 'grid', gap: '7px' }}>
                            <strong>Message</strong>
                            <textarea
                                value={newMessage}
                                onChange={e => setNewMessage(e.target.value)}
                                placeholder="Enter notification message..."
                                maxLength={2000}
                                rows={4}
                                required
                                style={{
                                    padding: '11px 13px',
                                    border: '1px solid #d1d5db',
                                    borderRadius: '8px',
                                    width: '100%',
                                    resize: 'vertical',
                                }}
                            />
                        </label>

                        <label style={{ display: 'grid', gap: '7px' }}>
                            <strong>Notification type</strong>
                            <select
                                value={newType}
                                onChange={e => setNewType(e.target.value)}
                                style={{
                                    padding: '11px 13px',
                                    border: '1px solid #d1d5db',
                                    borderRadius: '8px',
                                    width: '100%',
                                }}
                            >
                                {TYPE_OPTIONS.map(([value, label]) => (
                                    <option key={value} value={value}>
                                        {label}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <div>
                            <button
                                type="submit"
                                className="notification-primary-button"
                                disabled={creating}
                            >
                                <Send size={16} />
                                {creating ? 'Creating...' : 'Create notification'}
                            </button>
                        </div>
                    </form>
                </section>
            )}

            {error && (
                <div className="notification-error-banner">{error}</div>
            )}

            {success && (
                <div className="notification-success-banner">{success}</div>
            )}

            <section className="notification-filter-card">
                <div className="notification-filter-title">
                    <Filter size={17} />
                    <strong>Filters</strong>
                </div>

                <label className="notification-filter-control">
                    <span>Notification type</span>
                    <select
                        value={type}
                        onChange={e => applyFilters(setType, e.target.value)}
                    >
                        <option value="">All types</option>
                        {TYPE_OPTIONS.map(([value, label]) => (
                            <option value={value} key={value}>{label}</option>
                        ))}
                    </select>
                </label>

                <label className="notification-unread-toggle">
                    <input
                        type="checkbox"
                        checked={unreadOnly}
                        onChange={e => applyFilters(setUnreadOnly, e.target.checked)}
                    />
                    Unread only
                </label>
            </section>

            <section className="notification-list-card">
                {loading ? (
                    <div className="notification-state">
                        <RefreshCw className="spin" size={22} />
                        Loading notifications…
                    </div>
                ) : items.length ? (
                    items.map(item => (
                        <article
                            className={`notification-row ${item.isRead ? 'is-read' : 'is-unread'}`}
                            key={item.id}
                        >
                            <div
                                className={`notification-type-icon ${
                                    String(item.type).includes('EXPIRED') ||
                                    String(item.type).includes('IDLE')
                                        ? 'warning'
                                        : ''
                                }`}
                            >
                                {iconFor(item.type)}
                            </div>

                            <div className="notification-row-content">
                                <div className="notification-row-title">
                                    <strong>{item.title}</strong>
                                    {!item.isRead && (
                                        <span className="notification-new-label">NEW</span>
                                    )}
                                </div>

                                <p>{item.message}</p>

                                <div className="notification-row-meta">
                                    <span>{String(item.type || 'GENERAL').replaceAll('_', ' ')}</span>
                                    <span>·</span>
                                    <time>{formatDate(item.createdAt)}</time>
                                </div>
                            </div>

                            <div className="notification-row-actions">
                                {!item.isRead && (
                                    <button
                                        className="notification-mark-button"
                                        onClick={() => markRead(item)}
                                        disabled={busyId === item.id}
                                        title="Mark as read"
                                    >
                                        <CheckCheck size={17} />
                                    </button>
                                )}

                                <button
                                    className="notification-open-button"
                                    onClick={() => openItem(item)}
                                    title="Open related item"
                                >
                                    <ExternalLink size={16} />
                                </button>
                            </div>
                        </article>
                    ))
                ) : (
                    <div className="notification-state">
                        <ClipboardList size={30} />
                        <strong>No notifications found</strong>
                        <span>Try changing your filters or create a test notification.</span>
                    </div>
                )}
            </section>

            <div className="notification-pagination">
        <span>
          Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
        </span>

                <div>
                    <button
                        onClick={() => setPage(p => Math.max(0, p - 1))}
                        disabled={page === 0 || loading}
                    >
                        <ChevronLeft size={17} /> Previous
                    </button>

                    <button
                        onClick={() => setPage(p => p + 1)}
                        disabled={loading || page + 1 >= totalPages}
                    >
                        <ChevronRight size={17} /> Next
                    </button>
                </div>
            </div>
        </div>
    );
}
