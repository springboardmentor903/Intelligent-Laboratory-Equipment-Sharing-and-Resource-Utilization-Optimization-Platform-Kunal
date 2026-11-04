import { useCallback, useEffect, useMemo, useState } from 'react';
import {
    Building2,
    CalendarClock,
    Check,
    RefreshCw,
    Search,
    Send,
    X,
} from 'lucide-react';
import api from '../api/client';
import './ResourceSharing.css';

const listOf = (data) =>
    Array.isArray(data)
        ? data
        : data?.content ?? data?.items ?? data?.data ?? [];

const roleName = (share) =>
    share?.resource?.name ?? share?.resourceName ?? 'Shared resource';

const resourceIdOf = (share) =>
    share?.resource?.id ?? share?.resourceId;

const shareIdOf = (share) =>
    share?.id ?? share?.shareId;

const errorText = (err) =>
    err.response?.data?.message ||
    err.response?.data?.error ||
    err.message ||
    'Request failed.';

export default function ResourceSharing() {
    const [tab, setTab] = useState('discover');

    const [discover, setDiscover] = useState([]);
    const [sent, setSent] = useState([]);
    const [received, setReceived] = useState([]);

    const [myBookings, setMyBookings] = useState([]);
    const [incomingBookings, setIncomingBookings] = useState([]);
    const [outgoingBookings, setOutgoingBookings] = useState([]);

    const [resources, setResources] = useState([]);

    const [resourceId, setResourceId] = useState('');
    const [targetInstitutionId, setTargetInstitutionId] = useState('');
    const [shareStart, setShareStart] = useState('');
    const [shareEnd, setShareEnd] = useState('');
    const [shareRemarks, setShareRemarks] = useState('');
    const [feePerHour, setFeePerHour] = useState('');

    const [selectedShareId, setSelectedShareId] = useState('');
    const [bookingStart, setBookingStart] = useState('');
    const [bookingEnd, setBookingEnd] = useState('');
    const [bookingRemarks, setBookingRemarks] = useState('');

    const [decisionRemarks, setDecisionRemarks] = useState('');
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');

    const loadAll = useCallback(async () => {
        setError('');

        const requests = [
            api.get('/shares/discover'),
            api.get('/shares/sent'),
            api.get('/shares/received'),
            api.get('/external-bookings/my'),
            api.get('/external-bookings/incoming'),
            api.get('/external-bookings/outgoing'),
            api.get('/resources'),
        ];

        const results = await Promise.allSettled(requests);

        const setters = [
            setDiscover,
            setSent,
            setReceived,
            setMyBookings,
            setIncomingBookings,
            setOutgoingBookings,
            setResources,
        ];

        const labels = [
            'Shared resources',
            'Sent sharing requests',
            'Received sharing requests',
            'My external bookings',
            'Incoming external bookings',
            'Outgoing external bookings',
            'Resources',
        ];

        results.forEach((result, index) => {
            if (result.status === 'fulfilled') {
                setters[index](listOf(result.value.data));
            }
        });

        const failures = results
            .map((result, index) =>
                result.status === 'rejected'
                    ? `${labels[index]}: ${errorText(result.reason)}`
                    : null
            )
            .filter(Boolean);

        if (failures.length === results.length) {
            setError(failures[0]);
        }
    }, []);

    useEffect(() => {
        loadAll();
    }, [loadAll]);

    const approvedShares = useMemo(
        () =>
            discover.filter(
                (share) =>
                    String(share.status || '').toUpperCase() === 'APPROVED'
            ),
        [discover]
    );

    const run = async (action, successMessage) => {
        setBusy(true);
        setError('');
        setMessage('');

        try {
            await action();
            await loadAll();
            setMessage(successMessage);
        } catch (err) {
            setError(errorText(err));
        } finally {
            setBusy(false);
        }
    };

    const createShare = () =>
        run(async () => {
            await api.post('/shares', {
                resourceId: Number(resourceId),
                targetInstitutionId: Number(targetInstitutionId),
                startTime: shareStart || null,
                endTime: shareEnd || null,
                feePerHour: Number(feePerHour || 0),
                remarks: shareRemarks,
            });
        }, 'Resource sharing request submitted successfully.');

    const createExternalBooking = () =>
        run(async () => {
            await api.post('/external-bookings', {
                shareId: Number(selectedShareId),
                startTime: bookingStart,
                endTime: bookingEnd,
                remarks: bookingRemarks,
            });
        }, 'External booking request submitted successfully.');

    const decideShare = (id, status) =>
        run(async () => {
            await api.put(`/shares/${id}/${status.toLowerCase()}`, {
                remarks: decisionRemarks,
            });
        }, `Sharing request ${status.toLowerCase()} successfully.`);

    const decideBooking = (id, status) =>
        run(async () => {
            await api.put(
                `/external-bookings/${id}/${status.toLowerCase()}`,
                { remarks: decisionRemarks }
            );
        }, `External booking ${status.toLowerCase()} successfully.`);

    const cancelBooking = (id) =>
        run(async () => {
            await api.put(`/external-bookings/${id}/cancel`);
        }, 'External booking request cancelled successfully.');

    const statusPill = (status) => (
        <span
            className={`sharing-status status-${String(
                status || 'unknown'
            ).toLowerCase()}`}
        >
            {String(status || 'UNKNOWN').replaceAll('_', ' ')}
        </span>
    );

    const resourceLabel = (item) =>
        item.resourceName ||
        item.resource?.name ||
        item.name ||
        'Resource';

    const dateLabel = (value) =>
        value ? new Date(value).toLocaleString() : 'Not specified';

    return (
        <section className="sharing-page">
            <div className="sharing-toolbar">
                <div className="sharing-tabs">
                    <button
                        className={tab === 'discover' ? 'active' : ''}
                        onClick={() => setTab('discover')}
                    >
                        <Search size={16} />
                        Discover & Share
                    </button>

                    <button
                        className={tab === 'requests' ? 'active' : ''}
                        onClick={() => setTab('requests')}
                    >
                        <Building2 size={16} />
                        Sharing Requests
                    </button>

                    <button
                        className={tab === 'bookings' ? 'active' : ''}
                        onClick={() => setTab('bookings')}
                    >
                        <CalendarClock size={16} />
                        External Bookings
                    </button>
                </div>

                <button
                    className="sharing-refresh"
                    onClick={() =>
                        run(loadAll, 'Data refreshed successfully.')
                    }
                    disabled={busy}
                >
                    <RefreshCw size={15} />
                    Refresh
                </button>
            </div>

            {error && (
                <div className="sharing-alert error">{error}</div>
            )}

            {message && (
                <div className="sharing-alert success">{message}</div>
            )}

            {tab === 'discover' && (
                <>
                    <div className="sharing-section-head">
                        <div>
                            <h2>Discover shared resources</h2>
                            <p>
                                Browse resources approved for your institution
                                and request an external booking.
                            </p>
                        </div>
                    </div>

                    {approvedShares.length ? (
                        <div className="sharing-grid">
                            {approvedShares.map((share) => (
                                <article
                                    className="sharing-card"
                                    key={shareIdOf(share)}
                                >
                                    <div className="sharing-card-top">
                                        <span className="sharing-icon">
                                            <Building2 size={20} />
                                        </span>
                                        {statusPill(share.status)}
                                    </div>

                                    <h3>{roleName(share)}</h3>

                                    <p>
                                        Resource ID: {resourceIdOf(share) ?? '—'}
                                        {' · '}
                                        Share ID: {shareIdOf(share)}
                                    </p>

                                    <div className="sharing-meta">
                                        <span>Available from</span>
                                        <strong>
                                            {dateLabel(share.startTime)}
                                        </strong>
                                    </div>

                                    <div className="sharing-meta">
                                        <span>Available until</span>
                                        <strong>
                                            {dateLabel(share.endTime)}
                                        </strong>
                                    </div>
                                </article>
                            ))}
                        </div>
                    ) : (
                        <p className="sharing-muted">
                            No approved shared resources are currently
                            available to your institution.
                        </p>
                    )}

                    <div className="sharing-section-head sharing-create-head">
                        <div>
                            <h2>Share your institution's resource</h2>
                            <p>
                                Send a sharing request to another institution.
                            </p>
                        </div>
                    </div>

                    <div className="sharing-form-card">
                        <div className="sharing-form-grid">
                            <label>
                                Resource
                                <select
                                    value={resourceId}
                                    onChange={(e) =>
                                        setResourceId(e.target.value)
                                    }
                                >
                                    <option value="">Select resource</option>
                                    {resources.map((resource) => (
                                        <option
                                            key={resource.id}
                                            value={resource.id}
                                        >
                                            {resource.name ||
                                                resource.resourceName ||
                                                `Resource ${resource.id}`}
                                        </option>
                                    ))}
                                </select>
                            </label>

                            <label>
                                Target institution ID
                                <input
                                    type="number"
                                    min="1"
                                    value={targetInstitutionId}
                                    onChange={(e) =>
                                        setTargetInstitutionId(e.target.value)
                                    }
                                    placeholder="Enter institution ID"
                                />
                            </label>

                            <label>
                                Sharing starts
                                <input
                                    type="datetime-local"
                                    value={shareStart}
                                    onChange={(e) =>
                                        setShareStart(e.target.value)
                                    }
                                />
                            </label>

                            <label>
                                Sharing ends
                                <input
                                    type="datetime-local"
                                    value={shareEnd}
                                    onChange={(e) =>
                                        setShareEnd(e.target.value)
                                    }
                                />
                            </label>

                            <label>
                                Sharing Fee Per Hour (₹)
                                <input
                                    type="number"
                                    min="0"
                                    step="0.01"
                                    value={feePerHour}
                                    onChange={(e) =>
                                        setFeePerHour(e.target.value)
                                    }
                                    placeholder="Enter hourly fee"
                                />
                            </label>

                            <label className="sharing-wide">
                                Remarks
                                <input
                                    value={shareRemarks}
                                    onChange={(e) =>
                                        setShareRemarks(e.target.value)
                                    }
                                    placeholder="Add a note for the receiving institution"
                                />
                            </label>
                        </div>

                        <button
                            className="sharing-primary"
                            disabled={
                                busy || !resourceId || !targetInstitutionId
                            }
                            onClick={createShare}
                        >
                            <Send size={15} />
                            Submit sharing request
                        </button>
                    </div>
                </>
            )}

            {tab === 'requests' && (
                <div className="sharing-request-columns">
                    <section className="sharing-list-panel">
                        <h2>Sent Sharing Requests</h2>

                        {sent.length ? sent.map((share) => (
                            <article
                                className="sharing-request-card"
                                key={shareIdOf(share)}
                            >
                                <div className="sharing-request-heading">
                                    <h3>{roleName(share)}</h3>
                                    {statusPill(share.status)}
                                </div>

                                <p>Share ID: {shareIdOf(share)}</p>
                                <p>Resource ID: {resourceIdOf(share)}</p>
                                <p>
                                    Fee per hour: {currency(share.feePerHour)}
                                </p>
                                <p>
                                    Period: {dateLabel(share.startTime)}
                                    {' — '}
                                    {dateLabel(share.endTime)}
                                </p>
                            </article>
                        )) : (
                            <p className="sharing-muted">
                                No sent sharing requests.
                            </p>
                        )}
                    </section>

                    <section className="sharing-list-panel">
                        <h2>Incoming Sharing Requests</h2>

                        {received.length ? received.map((share) => (
                            <article
                                className="sharing-request-card"
                                key={shareIdOf(share)}
                            >
                                <div className="sharing-request-heading">
                                    <h3>{roleName(share)}</h3>
                                    {statusPill(share.status)}
                                </div>

                                <p>Share ID: {shareIdOf(share)}</p>
                                <p>Resource ID: {resourceIdOf(share)}</p>
                                <p>
                                    Period: {dateLabel(share.startTime)}
                                    {' — '}
                                    {dateLabel(share.endTime)}
                                </p>

                                {String(share.status).toUpperCase() ===
                                    'PENDING' && (
                                        <div className="sharing-actions">
                                            <button
                                                className="sharing-primary"
                                                disabled={busy}
                                                onClick={() =>
                                                    decideShare(
                                                        shareIdOf(share),
                                                        'approve'
                                                    )
                                                }
                                            >
                                                <Check size={15} />
                                                Approve
                                            </button>

                                            <button
                                                className="sharing-danger"
                                                disabled={busy}
                                                onClick={() =>
                                                    decideShare(
                                                        shareIdOf(share),
                                                        'reject'
                                                    )
                                                }
                                            >
                                                <X size={15} />
                                                Reject
                                            </button>
                                        </div>
                                    )}
                            </article>
                        )) : (
                            <p className="sharing-muted">
                                No incoming sharing requests.
                            </p>
                        )}
                    </section>
                </div>
            )}

            {tab === 'bookings' && (
                <>
                    <div className="sharing-section-head">
                        <div>
                            <h2>Request an external booking</h2>
                            <p>
                                Select an approved shared resource and
                                request a booking slot.
                            </p>
                        </div>
                    </div>

                    <div className="sharing-form-card">
                        <div className="sharing-form-grid">
                            <label className="sharing-wide">
                                Approved shared resource
                                <select
                                    value={selectedShareId}
                                    onChange={(e) =>
                                        setSelectedShareId(e.target.value)
                                    }
                                >
                                    <option value="">
                                        Select shared resource
                                    </option>

                                    {approvedShares.map((share) => (
                                        <option
                                            key={shareIdOf(share)}
                                            value={shareIdOf(share)}
                                        >
                                            {roleName(share)} — Share #
                                            {shareIdOf(share)}
                                        </option>
                                    ))}
                                </select>
                            </label>

                            <label>
                                Booking starts
                                <input
                                    type="datetime-local"
                                    value={bookingStart}
                                    onChange={(e) =>
                                        setBookingStart(e.target.value)
                                    }
                                />
                            </label>

                            <label>
                                Booking ends
                                <input
                                    type="datetime-local"
                                    value={bookingEnd}
                                    onChange={(e) =>
                                        setBookingEnd(e.target.value)
                                    }
                                />
                            </label>

                            <label className="sharing-wide">
                                Remarks
                                <input
                                    value={bookingRemarks}
                                    onChange={(e) =>
                                        setBookingRemarks(e.target.value)
                                    }
                                    placeholder="Purpose of booking"
                                />
                            </label>
                        </div>

                        <button
                            className="sharing-primary"
                            disabled={
                                busy ||
                                !selectedShareId ||
                                !bookingStart ||
                                !bookingEnd
                            }
                            onClick={createExternalBooking}
                        >
                            <Send size={15} />
                            Submit booking request
                        </button>
                    </div>

                    <div className="sharing-request-columns">
                        <section className="sharing-list-panel">
                            <h2>My Booking Requests</h2>

                            {myBookings.length ? myBookings.map((booking) => (
                                <article
                                    className="sharing-request-card"
                                    key={booking.id}
                                >
                                    <div className="sharing-request-heading">
                                        <h3>
                                            {resourceLabel(booking)}
                                        </h3>
                                        {statusPill(booking.status)}
                                    </div>

                                    <p>Booking ID: {booking.id}</p>
                                    <p>
                                        Start: {dateLabel(booking.startTime)}
                                    </p>
                                    <p>
                                        End: {dateLabel(booking.endTime)}
                                    </p>

                                    {String(booking.status).toUpperCase() ===
                                        'PENDING' && (
                                            <button
                                                className="sharing-danger"
                                                disabled={busy}
                                                onClick={() =>
                                                    cancelBooking(booking.id)
                                                }
                                            >
                                                <X size={15} />
                                                Cancel request
                                            </button>
                                        )}
                                </article>
                            )) : (
                                <p className="sharing-muted">
                                    No booking requests found.
                                </p>
                            )}
                        </section>

                        <section className="sharing-list-panel">
                            <h2>Outgoing Booking Requests</h2>

                            {outgoingBookings.length ?
                                outgoingBookings.map((booking) => (
                                    <article
                                        className="sharing-request-card"
                                        key={booking.id}
                                    >
                                        <div className="sharing-request-heading">
                                            <h3>
                                                {resourceLabel(booking)}
                                            </h3>
                                            {statusPill(booking.status)}
                                        </div>

                                        <p>Booking ID: {booking.id}</p>
                                        <p>
                                            Start: {dateLabel(booking.startTime)}
                                        </p>
                                        <p>
                                            End: {dateLabel(booking.endTime)}
                                        </p>
                                    </article>
                                )) : (
                                    <p className="sharing-muted">
                                        No outgoing booking requests.
                                    </p>
                                )
                            }
                        </section>
                    </div>

                    <section className="sharing-list-panel sharing-incoming-panel">
                        <h2>Incoming Booking Requests</h2>

                        {incomingBookings.length ?
                            incomingBookings.map((booking) => (
                                <article
                                    className="sharing-request-card"
                                    key={booking.id}
                                >
                                    <div className="sharing-request-heading">
                                        <h3>{resourceLabel(booking)}</h3>
                                        {statusPill(booking.status)}
                                    </div>

                                    <p>Booking ID: {booking.id}</p>
                                    <p>
                                        Start: {dateLabel(booking.startTime)}
                                    </p>
                                    <p>
                                        End: {dateLabel(booking.endTime)}
                                    </p>

                                    {String(booking.status).toUpperCase() ===
                                        'PENDING' && (
                                            <div className="sharing-actions">
                                                <button
                                                    className="sharing-primary"
                                                    disabled={busy}
                                                    onClick={() =>
                                                        decideBooking(
                                                            booking.id,
                                                            'approve'
                                                        )
                                                    }
                                                >
                                                    <Check size={15} />
                                                    Approve
                                                </button>

                                                <button
                                                    className="sharing-danger"
                                                    disabled={busy}
                                                    onClick={() =>
                                                        decideBooking(
                                                            booking.id,
                                                            'reject'
                                                        )
                                                    }
                                                >
                                                    <X size={15} />
                                                    Reject
                                                </button>
                                            </div>
                                        )}
                                </article>
                            )) : (
                                <p className="sharing-muted">
                                    No incoming booking requests.
                                </p>
                            )}
                    </section>
                </>
            )}
        </section>
    );
}

function currency(value) {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2,
    }).format(Number(value || 0));
}
