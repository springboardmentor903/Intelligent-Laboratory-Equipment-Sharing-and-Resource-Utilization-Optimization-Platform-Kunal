import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import {
    Activity,
    RefreshCw,
    Search,
    Clock3,
    Package,
    MapPin,
    ChevronLeft,
    ChevronRight,
    AlertCircle,
    Wifi,
    WifiOff,
} from 'lucide-react';
import './LiveEquipmentStatus.css';
import api from '../api/client';

const PAGE_SIZE = 10;
const WS_URL = (import.meta.env.VITE_WS_BASE_URL || 'http://localhost:8080')
    .replace(/^http:/, 'ws:')
    .replace(/^https:/, 'wss:') + '/ws';

function formatDate(value) {
    if (!value) return '—';
    return new Date(value).toLocaleString();
}

function formatStatus(value) {
    return String(value || 'UNKNOWN')
        .replaceAll('_', ' ')
        .toLowerCase()
        .replace(/\b\w/g, (char) => char.toUpperCase());
}

function StatusBadge({ status }) {
    const normalized = String(status || 'UNKNOWN').toLowerCase();

    return (
        <span className={`live-status-badge ${normalized}`}>
            <span />
            {formatStatus(status)}
        </span>
    );
}

export default function LiveEquipmentStatus() {
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState('');
    const [isConnected, setIsConnected] = useState(false);

    const [search, setSearch] = useState('');
    const [category, setCategory] = useState('');
    const [location, setLocation] = useState('');
    const [status, setStatus] = useState('');

    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);

    const loadLiveStatusRef = useRef(null);

    const loadLiveStatus = useCallback(async (showLoader = true) => {
        try {
            if (showLoader) {
                setLoading(true);
            } else {
                setRefreshing(true);
            }

            setError('');

            const response = await api.get('/resources/live-status', {
                params: {
                    page,
                    size: PAGE_SIZE,
                    search: search.trim() || undefined,
                    category: category || undefined,
                    location: location.trim() || undefined,
                    status: status || undefined,
                },
            });

            const data = response.data;
            setItems(data.content || []);
            setTotalPages(data.totalPages || 0);
            setTotalElements(data.totalElements || 0);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                err.response?.data?.error ||
                'Unable to load live equipment status.'
            );
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }, [page, search, category, location, status]);

    useEffect(() => {
        loadLiveStatusRef.current = loadLiveStatus;
    }, [loadLiveStatus]);

    // Load the selected page whenever filters or pagination change.
    useEffect(() => {
        loadLiveStatus();
    }, [loadLiveStatus]);

    // Authenticated STOMP connection. Backend endpoint is a native WebSocket
    // endpoint (/ws), so use brokerURL rather than SockJS.
    useEffect(() => {
        let active = true;
        const token = localStorage.getItem('token');

        if (!token) {
            setIsConnected(false);
            return undefined;
        }

        const client = new Client({
            brokerURL: WS_URL,
            connectHeaders: {
                Authorization: `Bearer ${token}`,
            },
            reconnectDelay: 5000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            onConnect: () => {
                if (!active) return;

                setIsConnected(true);

                client.subscribe('/topic/resources/live-status', () => {
                    // Any resource update can affect the current filtered view.
                    loadLiveStatusRef.current?.(false);
                });
            },
            onDisconnect: () => {
                if (active) setIsConnected(false);
            },
            onWebSocketClose: () => {
                if (active) setIsConnected(false);
            },
            onStompError: (frame) => {
                if (active) {
                    setIsConnected(false);
                    console.error(
                        'Live monitoring STOMP error:',
                        frame.headers?.message || frame.body
                    );
                }
            },
            onWebSocketError: (event) => {
                if (active) {
                    setIsConnected(false);
                    console.error('Live monitoring WebSocket error:', event);
                }
            },
        });

        client.activate();

        return () => {
            active = false;
            setIsConnected(false);
            client.deactivate();
        };
    }, []);

    // Poll only while WebSocket is disconnected; this is the fallback.
    useEffect(() => {
        if (isConnected) return undefined;

        const interval = setInterval(() => {
            loadLiveStatusRef.current?.(false);
        }, 15000);

        return () => clearInterval(interval);
    }, [isConnected]);

    function handleFilterChange(setter, value) {
        setter(value);
        setPage(0);
    }

    return (
        <div className="live-status-page">
            <section className="live-status-summary">
                <div className="live-summary-icon">
                    <Activity size={25} />
                </div>

                <div>
                    <h2>Live Equipment Monitoring</h2>
                    <p>
                        Monitor equipment availability, active sessions
                        and upcoming bookings.
                    </p>
                    <span className={`live-connection-state ${isConnected ? 'connected' : 'disconnected'}`}>
                        {isConnected ? <Wifi size={14} /> : <WifiOff size={14} />}
                        {isConnected ? 'Live connection' : 'Polling fallback'}
                    </span>
                </div>

                <button
                    className="live-refresh-button"
                    onClick={() => loadLiveStatus(false)}
                    disabled={refreshing}
                >
                    <RefreshCw
                        size={16}
                        className={refreshing ? 'live-spinning' : ''}
                    />
                    Refresh
                </button>
            </section>

            <section className="live-stat-grid">
                <article className="live-stat-card">
                    <Package size={20} />
                    <span>Total Equipment</span>
                    <strong>{totalElements}</strong>
                </article>

                <article className="live-stat-card">
                    <Activity size={20} />
                    <span>Currently In Use</span>
                    <strong>
                        {items.reduce(
                            (sum, item) => sum + (item.activeSessionCount || 0),
                            0
                        )}
                    </strong>
                </article>

                <article className="live-stat-card">
                    <Clock3 size={20} />
                    <span>Current Page</span>
                    <strong>{totalPages ? page + 1 : 0} / {totalPages}</strong>
                </article>
            </section>

            <section className="live-filter-panel">
                <div className="live-search-box">
                    <Search size={17} />
                    <input
                        type="text"
                        placeholder="Search equipment..."
                        value={search}
                        onChange={(e) =>
                            handleFilterChange(setSearch, e.target.value)
                        }
                    />
                </div>

                <input
                    className="live-filter-input"
                    type="text"
                    placeholder="Filter by location"
                    value={location}
                    onChange={(e) =>
                        handleFilterChange(setLocation, e.target.value)
                    }
                />

                <input
                    className="live-filter-input"
                    type="text"
                    placeholder="Filter by category"
                    value={category}
                    onChange={(e) =>
                        handleFilterChange(setCategory, e.target.value)
                    }
                />

                <select
                    className="live-filter-input"
                    value={status}
                    onChange={(e) =>
                        handleFilterChange(setStatus, e.target.value)
                    }
                >
                    <option value="">All Statuses</option>
                    <option value="AVAILABLE">Available</option>
                    <option value="BOOKED">Booked</option>
                    <option value="UNDER_MAINTENANCE">Under Maintenance</option>
                    <option value="OUT_OF_SERVICE">Out of Service</option>
                </select>
            </section>

            {error && (
                <div className="live-error-panel">
                    <AlertCircle size={20} />
                    <span>{error}</span>
                    <button onClick={() => loadLiveStatus()}>
                        Retry
                    </button>
                </div>
            )}

            <section className="live-table-panel">
                <div className="live-table-heading">
                    <div>
                        <h3>Equipment Status</h3>
                        <p>
                            {isConnected
                                ? 'Updates automatically when equipment events occur.'
                                : 'WebSocket unavailable. Refreshes every 15 seconds.'}
                        </p>
                    </div>

                    <span>{totalElements} records</span>
                </div>

                {loading ? (
                    <div className="live-empty-state">
                        Loading equipment status...
                    </div>
                ) : items.length === 0 ? (
                    <div className="live-empty-state">
                        <Package size={30} />
                        <strong>No equipment found</strong>
                        <span>Try changing your filters.</span>
                    </div>
                ) : (
                    <div className="live-table-scroll">
                        <table className="live-status-table">
                            <thead>
                            <tr>
                                <th>Equipment</th>
                                <th>Location</th>
                                <th>Status</th>
                                <th>Quantity</th>
                                <th>Active Sessions</th>
                                <th>Session Started</th>
                                <th>Elapsed</th>
                                <th>Scheduled End</th>
                                <th>Next Booking</th>
                            </tr>
                            </thead>

                            <tbody>
                            {items.map((item) => (
                                <tr key={item.resourceId}>
                                    <td>
                                        <strong>{item.resourceName}</strong>
                                        <small>{item.category || '—'}</small>
                                    </td>

                                    <td>
                                            <span className="live-location">
                                                <MapPin size={13} />
                                                {item.location || '—'}
                                            </span>
                                    </td>

                                    <td>
                                        <StatusBadge status={item.status} />
                                    </td>

                                    <td>
                                        {item.availableQuantity ?? 0}
                                        {' / '}
                                        {item.totalQuantity ?? 0}
                                    </td>

                                    <td>{item.activeSessionCount ?? 0}</td>
                                    <td>{formatDate(item.currentSessionStart)}</td>

                                    <td>
                                        {item.elapsedMinutes == null
                                            ? '—'
                                            : `${item.elapsedMinutes} min`}
                                    </td>

                                    <td>{formatDate(item.scheduledBookingEnd)}</td>
                                    <td>{formatDate(item.nextBookingStart)}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}

                <div className="live-pagination">
                    <span>
                        Page {totalPages ? page + 1 : 0} of {totalPages}
                    </span>

                    <div>
                        <button
                            disabled={page === 0 || loading}
                            onClick={() => setPage((current) => current - 1)}
                        >
                            <ChevronLeft size={17} />
                            Previous
                        </button>

                        <button
                            disabled={page + 1 >= totalPages || loading}
                            onClick={() => setPage((current) => current + 1)}
                        >
                            Next
                            <ChevronRight size={17} />
                        </button>
                    </div>
                </div>
            </section>
        </div>
    );
}
