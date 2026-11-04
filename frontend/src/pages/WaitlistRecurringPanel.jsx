
import { useCallback, useEffect, useState } from 'react';
import {
    Clock3,
    ListOrdered,
    Repeat2,
    Sparkles,
    RefreshCw,
    XCircle
} from 'lucide-react';

import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import './WaitlistRecurringPanel.css';

const toLocalInput = (date) => {
    const d = new Date(
        date.getTime() - date.getTimezoneOffset() * 60000
    );

    return d.toISOString().slice(0, 16);
};

const getItems = (data) =>
    Array.isArray(data)
        ? data
        : data?.content ?? data?.items ?? data?.data ?? [];

const roleOf = (user) =>
    String(
        user?.role?.name ??
        user?.roleName ??
        user?.role ??
        user?.authorities?.[0]?.authority ??
        ''
    )
        .replace(/^ROLE_/, '')
        .toUpperCase();

export default function WaitlistRecurringPanel() {
    const { user } = useAuth();

    const role = roleOf(user);

    // Only roles permitted by the backend can load/manage the waitlist queue.
    // Institution Admin is intentionally excluded to prevent 403 API calls.
    const isManager = [
        'LAB_MANAGER',
        'SYSTEM_ADMIN'
    ].includes(role);

    const isStudent = role === 'RESEARCHER_STUDENT';

    const [resources, setResources] = useState([]);
    const [waitlist, setWaitlist] = useState([]);
    const [queue, setQueue] = useState([]);

    const [resourceId, setResourceId] = useState('');

    const [startTime, setStartTime] = useState(
        toLocalInput(new Date(Date.now() + 86400000))
    );

    const [endTime, setEndTime] = useState(
        toLocalInput(new Date(Date.now() + 90000000))
    );

    const [frequency, setFrequency] = useState('WEEKLY');
    const [interval, setInterval] = useState(1);
    const [occurrences, setOccurrences] = useState(4);

    const [suggestions, setSuggestions] = useState([]);

    const [busy, setBusy] = useState(false);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');

    // Load resources and role-specific data
    const load = useCallback(async () => {
        try {
            setError('');

            const resourceRes = await api.get('/resources');
            const resourceList = getItems(resourceRes.data);

            setResources(resourceList);

            if (!resourceId && resourceList.length) {
                setResourceId(String(resourceList[0].id));
            }

            // Only students can access their personal waitlist
            if (isStudent) {
                const waitlistRes = await api.get(
                    '/bookings/waitlist/my'
                );

                setWaitlist(getItems(waitlistRes.data));
            } else {
                setWaitlist([]);
            }
        } catch (err) {
            setError(
                err.response?.data?.message ||
                err.response?.data?.error ||
                'Data load nahi ho paya.'
            );
        }
    }, [resourceId, isStudent]);

    useEffect(() => {
        load();
    }, [load]);

    // Manager: Load selected equipment queue
    const loadQueue = useCallback(async () => {
        if (!isManager || !resourceId) {
            setQueue([]);
            return;
        }

        try {
            setError('');

            const response = await api.get(
                `/bookings/waitlist/resource/${resourceId}`
            );

            setQueue(getItems(response.data));
        } catch (err) {
            setQueue([]);

            setError(
                err.response?.data?.message ||
                err.response?.data?.error ||
                'Waitlist queue load nahi hui.'
            );
        }
    }, [isManager, resourceId]);

    useEffect(() => {
        if (isManager && resourceId) {
            loadQueue();
        }
    }, [isManager, resourceId, loadQueue]);

    // Common action handler
    const runAction = async (action) => {
        setBusy(true);
        setError('');
        setMessage('');

        try {
            await action();

            await load();

            if (isManager) {
                await loadQueue();
            }
        } catch (err) {
            setError(
                err.response?.data?.message ||
                err.response?.data?.error ||
                err.message ||
                'Request failed.'
            );
        } finally {
            setBusy(false);
        }
    };

    // STUDENT: Join waitlist
    const joinWaitlist = () =>
        runAction(async () => {
            await api.post(
                '/bookings/waitlist',
                null,
                {
                    params: {
                        resourceId: Number(resourceId),
                        startTime,
                        endTime
                    }
                }
            );

            setMessage('Waitlist request submit ho gayi.');
        });

    // STUDENT: Create recurring bookings
    const createRecurring = () =>
        runAction(async () => {
            const response = await api.post(
                '/bookings/recurring',
                {
                    resourceId: Number(resourceId),
                    startTime,
                    endTime,
                    frequency,
                    interval: Number(interval),
                    occurrences: Number(occurrences)
                }
            );

            setMessage(
                `${response.data?.createdCount ?? 0} recurring bookings create hui.`
            );
        });

    // STUDENT: Find alternative slots
    const findSuggestions = () =>
        runAction(async () => {
            const durationMinutes = Math.max(
                15,
                Math.round(
                    (
                        new Date(endTime) -
                        new Date(startTime)
                    ) / 60000
                )
            );

            const response = await api.get(
                '/bookings/optimization/suggestions',
                {
                    params: {
                        resourceId: Number(resourceId),
                        preferredStart: startTime,
                        durationMinutes
                    }
                }
            );

            setSuggestions(getItems(response.data));

            setMessage(
                `${getItems(response.data).length} alternative slots mile.`
            );
        });

    // STUDENT: Cancel own waitlist request
    const cancelWaitlist = (id) =>
        runAction(async () => {
            await api.delete(`/bookings/waitlist/${id}`);

            setMessage('Waitlist entry cancel ho gayi.');
        });

    // MANAGER: Offer next oldest waiting request
    // Backend automatically takes date/time from the queue entry
    const offerNext = () =>
        runAction(async () => {
            const response = await api.post(
                '/bookings/waitlist/offer-next',
                null,
                {
                    params: {
                        resourceId: Number(resourceId)
                    }
                }
            );

            setMessage(
                `${response.data?.userName || response.data?.userEmail || 'Next student'} ko slot offer kar diya gaya.`
            );
        });

    return (
        <section className="t10-panel">

            {/* Page heading */}

            <div className="t10-heading">
                <div>
                    <span className="t10-eyebrow">
                        MILESTONE 2 · T10
                    </span>

                    <h2>Waitlist & Recurring Bookings</h2>

                    <p>
                        {isManager
                            ? 'Manage equipment waitlists and offer available slots.'
                            : 'Join high-demand equipment queues, schedule repeating sessions and find alternative slots.'}
                    </p>
                </div>

                <button
                    className="t10-secondary"
                    onClick={() => runAction(load)}
                    disabled={busy}
                >
                    <RefreshCw size={15} />
                    Refresh
                </button>
            </div>

            {/* Alerts */}

            {error && (
                <div className="t10-alert error">
                    {error}
                </div>
            )}

            {message && (
                <div className="t10-alert success">
                    {message}
                </div>
            )}

            {/* Resource and time selection */}

            <div className="t10-card">

                <h3>
                    <Clock3 size={18} />
                    {isManager
                        ? 'Manage equipment queue'
                        : 'Booking slot'}
                </h3>

                <div className="t10-grid">

                    <label>
                        Equipment

                        <select
                            value={resourceId}
                            onChange={(e) => {
                                setResourceId(e.target.value);
                                setQueue([]);
                            }}
                        >
                            <option value="">Select equipment</option>

                            {resources.map((r) => (
                                <option key={r.id} value={r.id}>
                                    {r.name}
                                </option>
                            ))}
                        </select>
                    </label>

                    <label>
                        Start time

                        <input
                            type="datetime-local"
                            value={startTime}
                            onChange={(e) =>
                                setStartTime(e.target.value)
                            }
                        />
                    </label>

                    <label>
                        End time

                        <input
                            type="datetime-local"
                            value={endTime}
                            onChange={(e) =>
                                setEndTime(e.target.value)
                            }
                        />
                    </label>

                </div>

                {/* Student actions */}

                {isStudent && (
                    <div className="t10-actions">

                        <button
                            onClick={joinWaitlist}
                            disabled={busy || !resourceId}
                        >
                            Join Waitlist
                        </button>

                        <button
                            className="t10-outline"
                            onClick={findSuggestions}
                            disabled={busy || !resourceId}
                        >
                            <Sparkles size={15} />
                            Find Available Slots
                        </button>

                    </div>
                )}

                {/* Manager actions */}

                {isManager && (
                    <div className="t10-actions">

                        <button
                            className="t10-outline"
                            onClick={loadQueue}
                            disabled={busy || !resourceId}
                        >
                            Load selected equipment queue
                        </button>

                        <button
                            onClick={offerNext}
                            disabled={busy || !resourceId}
                        >
                            Offer slot to next user
                        </button>

                    </div>
                )}

            </div>

            {/* STUDENT: Recurring booking */}

            {isStudent && (
                <div className="t10-card">

                    <h3>
                        <Repeat2 size={18} />
                        Recurring booking
                    </h3>

                    <div className="t10-grid recurring-grid">

                        <label>
                            Repeat

                            <select
                                value={frequency}
                                onChange={(e) =>
                                    setFrequency(e.target.value)
                                }
                            >
                                <option value="WEEKLY">Weekly</option>
                                <option value="DAILY">Daily</option>
                            </select>
                        </label>

                        <label>
                            Every

                            <input
                                type="number"
                                min="1"
                                max="4"
                                value={interval}
                                onChange={(e) =>
                                    setInterval(e.target.value)
                                }
                            />
                        </label>

                        <label>
                            Occurrences

                            <input
                                type="number"
                                min="2"
                                max="12"
                                value={occurrences}
                                onChange={(e) =>
                                    setOccurrences(e.target.value)
                                }
                            />
                        </label>

                    </div>

                    <p className="t10-hint">
                        Maximum 12 sessions per request.
                        All sessions must pass the normal booking availability
                        and approval rules.
                    </p>

                    <button
                        onClick={createRecurring}
                        disabled={busy || !resourceId}
                    >
                        <Repeat2 size={15} />
                        Create recurring bookings
                    </button>

                </div>
            )}

            {/* STUDENT: Suggested slots */}

            {isStudent && suggestions.length > 0 && (
                <div className="t10-card">

                    <h3>
                        <Sparkles size={18} />
                        Suggested slots
                    </h3>

                    <div className="t10-list">

                        {suggestions.map((s, i) => (
                            <div
                                className="t10-list-row"
                                key={`${s.startTime}-${i}`}
                            >
                                <span>
                                    {new Date(
                                        s.startTime
                                    ).toLocaleString()}

                                    {' – '}

                                    {new Date(
                                        s.endTime
                                    ).toLocaleTimeString()}
                                </span>

                                <small>
                                    {s.availableQuantity} available · {s.reason}
                                </small>
                            </div>
                        ))}

                    </div>

                </div>
            )}

            {/* STUDENT: My waitlist */}

            {isStudent && (
                <div className="t10-card">

                    <h3>
                        <ListOrdered size={18} />
                        My waitlist requests
                    </h3>

                    {waitlist.length > 0 ? (

                        <div className="t10-list">

                            {waitlist.map((item) => (
                                <div
                                    className="t10-list-row"
                                    key={item.id}
                                >

                                    <div>
                                        <strong>
                                            {item.resourceName}
                                        </strong>

                                        <small>
                                            {new Date(
                                                item.startTime
                                            ).toLocaleString()}

                                            {' – '}

                                            {new Date(
                                                item.endTime
                                            ).toLocaleTimeString()}
                                        </small>
                                    </div>

                                    <div className="t10-row-actions">

                                        <span
                                            className={`t10-status ${String(item.status).toLowerCase()}`}
                                        >
                                            {item.status}

                                            {item.position
                                                ? ` · #${item.position}`
                                                : ''}
                                        </span>

                                        {item.status === 'WAITING' && (
                                            <button
                                                className="t10-danger"
                                                onClick={() =>
                                                    cancelWaitlist(item.id)
                                                }
                                                disabled={busy}
                                            >
                                                <XCircle size={14} />
                                                Cancel
                                            </button>
                                        )}

                                    </div>

                                </div>
                            ))}

                        </div>

                    ) : (
                        <p className="t10-empty">
                            You have no waitlist requests.
                        </p>
                    )}

                </div>
            )}

            {/* MANAGER: Waitlist queue */}

            {isManager && (
                <div className="t10-card">

                    <h3>
                        <ListOrdered size={18} />
                        Manager waitlist queue
                    </h3>

                    {queue.length > 0 ? (

                        <div className="t10-list">

                            {queue.map((item, i) => (
                                <div
                                    className="t10-list-row"
                                    key={item.id}
                                >

                                    <span>
                                        #{i + 1} · {item.userName || item.userEmail}
                                    </span>

                                    <small>
                                        {new Date(
                                            item.startTime
                                        ).toLocaleString()}

                                        {' – '}

                                        {new Date(
                                            item.endTime
                                        ).toLocaleTimeString()}
                                    </small>

                                </div>
                            ))}

                        </div>

                    ) : (
                        <p className="t10-empty">
                            No waiting requests found. Load a resource queue to refresh.
                        </p>
                    )}

                </div>
            )}

            {/* Unsupported role */}

            {!isManager && !isStudent && (
                <p className="t10-empty">
                    Waitlist management is available to students and authorized lab staff.
                </p>
            )}

        </section>
    );
}