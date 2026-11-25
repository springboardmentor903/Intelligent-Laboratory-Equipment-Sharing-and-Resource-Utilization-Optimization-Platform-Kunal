
import { useEffect, useRef, useState } from 'react';
import { Bell, CheckCheck, ExternalLink } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import api from '../../api/client';

const getList = (data) => {
    if (Array.isArray(data)) return data;
    return data?.content || data?.items || data?.notifications || data?.data || [];
};

const getUnread = (item) =>
    item?.read === false || item?.isRead === false || item?.status === 'UNREAD';

export default function NotificationBell({ unreadCount = 0, onUnreadCountChange }) {
    const [open, setOpen] = useState(false);
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const wrapperRef = useRef(null);
    const navigate = useNavigate();

    const loadLatest = async () => {
        setLoading(true);
        setError('');

        try {
            const response = await api.get('/notifications/latest', {
                params: { limit: 5, size: 5, page: 0 },
            });

            const list = getList(response.data).slice(0, 5);
            setItems(list);
        } catch (err) {
            try {
                const response = await api.get('/notifications', {
                    params: { page: 0, size: 5 },
                });
                setItems(getList(response.data).slice(0, 5));
            } catch {
                setError('Notifications could not be loaded.');
            }
        } finally {
            setLoading(false);
        }
    };

    const refreshCount = async () => {
        try {
            const response = await api.get('/notifications/unread-count');
            const value = response.data?.unreadCount ?? response.data?.count ?? 0;
            onUnreadCountChange?.(Number(value) || 0);
        } catch {
            // Keep the last known unread count if the request fails.
        }
    };

    useEffect(() => {
        if (open) loadLatest();
    }, [open]);

    useEffect(() => {
        const handleOutside = (event) => {
            if (wrapperRef.current && !wrapperRef.current.contains(event.target)) {
                setOpen(false);
            }
        };

        document.addEventListener('mousedown', handleOutside);
        return () => document.removeEventListener('mousedown', handleOutside);
    }, []);

    const markRead = async (item) => {
        if (!getUnread(item)) return;

        try {
            await api.patch(`/notifications/${item.id}/read`);
            setItems((current) =>
                current.map((notification) =>
                    notification.id === item.id
                        ? { ...notification, read: true, isRead: true }
                        : notification
                )
            );
            await refreshCount();
        } catch {
            setError('Could not mark notification as read.');
        }
    };

    const openNotification = async (item) => {
        await markRead(item);

        setOpen(false);

        const type = String(item.referenceType || '').toUpperCase();
        const id = item.referenceId;

        const routes = {
            BOOKING: '/bookings',
            RESOURCE: '/equipment',
            EQUIPMENT: '/equipment',
            WORK_ORDER: '/work-orders',
            MAINTENANCE_REQUEST: '/maintenance-requests',
            CALIBRATION: '/calibration-overview',
            SHARE: '/resource-sharing',
            RESOURCE_SHARE: '/resource-sharing',
            INVOICE: '/invoices',
            BUDGET: '/budgets',
        };

        const route = routes[type] || '/notifications';
        navigate(id ? `${route}?referenceId=${encodeURIComponent(id)}` : route);
    };

    const count = Number(unreadCount) || 0;

    return (
        <div className="notification-bell-wrap" ref={wrapperRef}>
            <button
                type="button"
                className="notification-bell-button"
                aria-label={`Notifications${count ? `, ${count} unread` : ''}`}
                aria-expanded={open}
                onClick={() => setOpen((value) => !value)}
            >
                <Bell size={20} />
                {count > 0 && (
                    <span className="notification-badge">
            {count > 99 ? '99+' : count}
          </span>
                )}
            </button>

            {open && (
                <div className="notification-dropdown">
                    <div className="notification-dropdown-header">
                        <div>
                            <strong>Notifications</strong>
                            <span>{count} unread</span>
                        </div>
                        <button
                            type="button"
                            className="notification-text-button"
                            onClick={() => navigate('/notifications')}
                        >
                            View all <ExternalLink size={14} />
                        </button>
                    </div>

                    {loading ? (
                        <div className="notification-empty">Loading notifications...</div>
                    ) : error ? (
                        <div className="notification-error">{error}</div>
                    ) : items.length === 0 ? (
                        <div className="notification-empty">
                            <Bell size={25} />
                            <span>You are all caught up.</span>
                        </div>
                    ) : (
                        <div className="notification-dropdown-list">
                            {items.map((item) => (
                                <button
                                    type="button"
                                    key={item.id}
                                    className={`notification-dropdown-item ${
                                        getUnread(item) ? 'unread' : ''
                                    }`}
                                    onClick={() => openNotification(item)}
                                >
                                    <span className="notification-item-dot" />
                                    <span className="notification-item-content">
                    <strong>{item.title || 'Notification'}</strong>
                    <span>{item.message || item.body || ''}</span>
                    <small>
                      {item.createdAt
                          ? new Date(item.createdAt).toLocaleString()
                          : ''}
                    </small>
                  </span>
                                </button>
                            ))}
                        </div>
                    )}

                    <button
                        type="button"
                        className="notification-dropdown-footer"
                        onClick={() => {
                            setOpen(false);
                            navigate('/notifications');
                        }}
                    >
                        <CheckCheck size={16} />
                        Open notification center
                    </button>
                </div>
            )}
        </div>
    );
}
