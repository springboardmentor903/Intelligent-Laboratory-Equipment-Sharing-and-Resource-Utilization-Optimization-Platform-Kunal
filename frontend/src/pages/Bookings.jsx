
import { useCallback, useEffect, useState } from 'react';
import {
  CalendarDays,
  Clock3,
  RefreshCw,
  CheckCircle2,
  XCircle,
  Play,
  CheckCheck,
  UserRound,
  AlertCircle,
  LogIn,
  LogOut,
} from 'lucide-react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import WaitlistRecurringPanel from './WaitlistRecurringPanel';

function getRoleKey(user) {
  const raw =
      user?.role?.name ??
      user?.role?.roleName ??
      user?.roleName ??
      (typeof user?.role === 'string' ? user.role : null) ??
      user?.authorities?.[0]?.authority ??
      '';

  return String(raw).replace(/^ROLE_/, '').trim().toUpperCase();
}

function getList(data) {
  if (Array.isArray(data)) return data;
  return data?.content ?? data?.items ?? data?.data ?? [];
}

function formatDate(value) {
  if (!value) return '—';

  const date = new Date(value);

  return Number.isNaN(date.getTime())
      ? '—'
      : date.toLocaleString();
}

const STAFF_ROLES = [
  'LAB_TECHNICIAN',
  'LAB_MANAGER',
  'SYSTEM_ADMIN',
  'INSTITUTION_ADMIN',
];

export default function Bookings() {
  const { user } = useAuth();

  const role = getRoleKey(user);
  const isStudent = role === 'RESEARCHER_STUDENT';
  const isInstitutionAdmin = role === 'INSTITUTION_ADMIN';
  const canReview = STAFF_ROLES.includes(role);

  const [items, setItems] = useState([]);
  const [resources, setResources] = useState([]);

  const [form, setForm] = useState({
    resourceId: '',
    startTime: '',
    endTime: '',
  });

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [checkingAvailability, setCheckingAvailability] = useState(false);
  const [availability, setAvailability] = useState(null);

  const [actionId, setActionId] = useState(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');

    try {
      if (isInstitutionAdmin) {
        const response = await api.get('/bookings/institution');
        setItems(getList(response.data));
      } else if (canReview) {
        const response = await api.get('/bookings');
        setItems(getList(response.data));
      } else if (isStudent) {
        const response = await api.get('/bookings/my-bookings');
        setItems(getList(response.data));
      } else {
        setItems([]);
      }
    } catch (err) {
      setError(
          err.response?.data?.message ||
          `Bookings load nahi hui. HTTP ${err.response?.status ?? 'network error'}.`
      );
    }

    if (isStudent) {
      try {
        const response = await api.get('/resources');
        const list = getList(response.data);

        setResources(
            list.filter((resource) => {
              const status = String(
                  resource.status ?? resource.availabilityStatus ?? ''
              ).toUpperCase();

              const quantity = Number(
                  resource.availableQuantity ??
                  resource.available_quantity ??
                  resource.quantity ??
                  0
              );

              return (
                  ['AVAILABLE', 'ACTIVE'].includes(status) ||
                  quantity > 0
              );
            })
        );
      } catch {
        setResources([]);
      }
    }

    setLoading(false);
  }, [canReview, isStudent]);

  useEffect(() => {
    load();
  }, [load]);

  async function checkAvailability() {
    setError('');
    setMessage('');
    setAvailability(null);

    if (!form.resourceId || !form.startTime || !form.endTime) {
      setError(
          'Availability check se pehle equipment, start time aur end time select karo.'
      );
      return;
    }

    const start = new Date(form.startTime);
    const end = new Date(form.endTime);

    if (
        Number.isNaN(start.getTime()) ||
        Number.isNaN(end.getTime()) ||
        end <= start
    ) {
      setError('End time, start time ke baad hona chahiye.');
      return;
    }

    if (start <= new Date()) {
      setError('Past time ke liye availability check nahi kar sakte.');
      return;
    }

    setCheckingAvailability(true);

    try {
      const response = await api.get('/bookings/availability', {
        params: {
          resourceId: Number(form.resourceId),
          startTime: form.startTime,
          endTime: form.endTime,
        },
      });

      setAvailability(response.data);

      if (response.data?.available) {
        setMessage(
            `Slot available hai. Available quantity: ${response.data.availableQuantity ?? 0}. Ab booking request submit kar sakte ho.`
        );
      } else {
        setMessage(
            'Is time slot mein equipment available nahi hai. Dusra time select karo.'
        );
      }
    } catch (err) {
      setError(
          err.response?.data?.message ||
          `Availability check fail hua. HTTP ${err.response?.status ?? 'network error'}.`
      );
    } finally {
      setCheckingAvailability(false);
    }
  }

  async function submit(e) {
    e.preventDefault();

    setError('');
    setMessage('');

    if (!form.resourceId || !form.startTime || !form.endTime) {
      setError('Equipment, start time aur end time select karo.');
      return;
    }

    const start = new Date(form.startTime);
    const end = new Date(form.endTime);

    if (
        Number.isNaN(start.getTime()) ||
        Number.isNaN(end.getTime()) ||
        end <= start
    ) {
      setError('End time, start time ke baad hona chahiye.');
      return;
    }

    if (start <= new Date()) {
      setError('Past time ke liye booking request nahi kar sakte.');
      return;
    }

    if (!availability?.available) {
      setError('Pehle selected time slot ki availability check karo.');
      return;
    }

    setSaving(true);

    try {
      await api.post(
          '/bookings',
          {
            startTime: form.startTime,
            endTime: form.endTime,
          },
          {
            params: {
              resourceId: Number(form.resourceId),
            },
          }
      );

      setMessage('Booking request successfully submit ho gayi.');

      setForm({
        resourceId: '',
        startTime: '',
        endTime: '',
      });

      setAvailability(null);

      await load();
    } catch (err) {
      setError(
          err.response?.data?.message ||
          `Booking submit nahi hui. HTTP ${err.response?.status ?? 'network error'}.`
      );
    } finally {
      setSaving(false);
    }
  }

  async function changeStatus(booking, action, label) {
    const bookingId = booking.id ?? booking.bookingId;

    if (!bookingId) {
      setError('Booking ID response mein nahi mili.');
      return;
    }

    setError('');
    setMessage('');
    setActionId(bookingId);

    try {
      const actionPath = isInstitutionAdmin
          ? `/bookings/${bookingId}/institution-${action}`
          : `/bookings/${bookingId}/${action}`;

      await api.put(actionPath);

      setMessage(`Booking #${bookingId}: ${label} successfully.`);

      await load();
    } catch (err) {
      setError(
          err.response?.data?.message ||
          `${label} action fail hua. HTTP ${err.response?.status ?? 'network error'}.`
      );
    } finally {
      setActionId(null);
    }
  }

  async function handleSessionAction(booking, action) {
    const bookingId = booking.id ?? booking.bookingId;

    if (!bookingId) {
      setError('Booking ID response mein nahi mili.');
      return;
    }

    const isCheckIn = action === 'check-in';
    const label = isCheckIn ? 'Check-in' : 'Check-out';

    setError('');
    setMessage('');
    setActionId(bookingId);

    try {
      await api.put(`/bookings/${bookingId}/${action}`);

      setMessage(
          `Booking #${bookingId}: ${label} successful ho gaya.`
      );

      await load();
    } catch (err) {
      setError(
          err.response?.data?.message ||
          `${label} fail hua. HTTP ${err.response?.status ?? 'network error'}.`
      );
    } finally {
      setActionId(null);
    }
  }

  function renderActions(booking) {
    if (!canReview) return null;

    const status = String(booking.status ?? '').toUpperCase();
    const id = booking.id ?? booking.bookingId;
    const busy = actionId === id;

    // Institution Admin can only approve/reject bookings for their institution.
    if (isInstitutionAdmin) {
      return (
          <div className="booking-actions">
            {status === 'PENDING' && (
                <>
                  <button
                      type="button"
                      className="primary-button"
                      disabled={busy}
                      onClick={() => changeStatus(booking, 'approve', 'Approved')}
                  >
                    <CheckCircle2 size={15} />
                    Approve
                  </button>

                  <button
                      type="button"
                      className="secondary-button"
                      disabled={busy}
                      onClick={() => changeStatus(booking, 'reject', 'Rejected')}
                  >
                    <XCircle size={15} />
                    Reject
                  </button>
                </>
            )}

            {busy && <span className="field-hint">Updating…</span>}
          </div>
      );
    }

    // Existing operational actions for Lab Technician, Lab Manager and System Admin.
    return (
        <div className="booking-actions">
          {status === 'PENDING' && (
              <>
                <button
                    type="button"
                    className="primary-button"
                    disabled={busy}
                    onClick={() => changeStatus(booking, 'approve', 'Approved')}
                >
                  <CheckCircle2 size={15} />
                  Approve
                </button>

                <button
                    type="button"
                    className="secondary-button"
                    disabled={busy}
                    onClick={() => changeStatus(booking, 'reject', 'Rejected')}
                >
                  <XCircle size={15} />
                  Reject
                </button>
              </>
          )}

          {status === 'APPROVED' && (
              <>
                <button
                    type="button"
                    className="primary-button"
                    disabled={busy}
                    onClick={() => changeStatus(booking, 'start', 'Started')}
                >
                  <Play size={15} />
                  Start
                </button>

                <button
                    type="button"
                    className="secondary-button"
                    disabled={busy}
                    onClick={() => changeStatus(booking, 'no-show', 'Marked no-show')}
                >
                  No-show
                </button>
              </>
          )}

          {status === 'IN_USE' && (
              <button
                  type="button"
                  className="primary-button"
                  disabled={busy}
                  onClick={() => changeStatus(booking, 'complete', 'Completed')}
              >
                <CheckCheck size={15} />
                Complete
              </button>
          )}

          {busy && <span className="field-hint">Updating…</span>}
        </div>
    );
  }

  function renderStudentSessionActions(booking) {
    if (!isStudent) return null;

    const status = String(booking.status ?? '').toUpperCase();
    const id = booking.id ?? booking.bookingId;
    const busy = actionId === id;

    return (
        <div className="booking-actions">
          {status === 'APPROVED' && (
              <button
                  type="button"
                  className="primary-button"
                  disabled={busy || loading}
                  onClick={() => handleSessionAction(booking, 'check-in')}
              >
                <LogIn size={15} />
                Check-in
              </button>
          )}

          {status === 'IN_USE' && (
              <button
                  type="button"
                  className="primary-button"
                  disabled={busy || loading}
                  onClick={() => handleSessionAction(booking, 'check-out')}
              >
                <LogOut size={15} />
                Check-out
              </button>
          )}

          {busy && <span className="field-hint">Updating…</span>}
        </div>
    );
  }

  return (
      <div>
        <div className="page-heading-row">
          <div>
          <span className="eyebrow">
            {isInstitutionAdmin
                ? 'INSTITUTION BOOKING MANAGEMENT'
                : canReview
                    ? 'BOOKING OPERATIONS'
                    : 'SCHEDULING'}
          </span>

            <h1>
              {isInstitutionAdmin
                  ? 'Institution bookings'
                  : canReview
                      ? 'Booking approvals'
                      : 'Bookings & availability'}
            </h1>

            <p>
              {isInstitutionAdmin
                  ? 'Review and approve or reject booking requests for your institution.'
                  : canReview
                      ? 'Review requests and manage approved laboratory sessions.'
                      : 'Request equipment time and review your booking history.'}
            </p>
          </div>

          <button
              className="secondary-button"
              type="button"
              onClick={load}
              disabled={loading || actionId !== null}
          >
            <RefreshCw size={16} />
            Refresh
          </button>
        </div>

        {error && (
            <div className="notice error" role="alert">
              <AlertCircle size={18} />
              {error}
            </div>
        )}

        {message && (
            <div className="notice success" role="status">
              <CheckCircle2 size={18} />
              {message}
            </div>
        )}

        {isStudent && (
            <div className="booking-layout">
              <section className="panel booking-form-panel">
                <div className="panel-heading">
                  <div>
                    <h2>New booking request</h2>
                    <p>Choose a resource and schedule.</p>
                  </div>

                  <CalendarDays size={21} />
                </div>

                <form className="stack-form" onSubmit={submit}>
                  <div className="field">
                    <label htmlFor="resourceId">Equipment</label>

                    <select
                        id="resourceId"
                        className="standalone-input"
                        value={form.resourceId}
                        onChange={(e) => {
                          setForm((old) => ({
                            ...old,
                            resourceId: e.target.value,
                          }));

                          setAvailability(null);
                          setMessage('');
                        }}
                        required
                    >
                      <option value="">Select equipment</option>

                      {resources.map((resource) => (
                          <option key={resource.id} value={resource.id}>
                            {resource.name ?? resource.resourceName ?? 'Equipment'}
                            {' — '}
                            {resource.location ?? resource.labName ?? 'Lab'}
                          </option>
                      ))}
                    </select>

                    {!loading && resources.length === 0 && (
                        <small className="field-hint">
                          No available resources were returned by the API.
                        </small>
                    )}
                  </div>

                  <div className="field">
                    <label htmlFor="startTime">Start time</label>

                    <input
                        id="startTime"
                        className="standalone-input"
                        type="datetime-local"
                        min={new Date(Date.now() + 60000).toISOString().slice(0, 16)}
                        value={form.startTime}
                        onChange={(e) => {
                          setForm((old) => ({
                            ...old,
                            startTime: e.target.value,
                          }));

                          setAvailability(null);
                          setMessage('');
                        }}
                        required
                    />
                  </div>

                  <div className="field">
                    <label htmlFor="endTime">End time</label>

                    <input
                        id="endTime"
                        className="standalone-input"
                        type="datetime-local"
                        min={form.startTime || undefined}
                        value={form.endTime}
                        onChange={(e) => {
                          setForm((old) => ({
                            ...old,
                            endTime: e.target.value,
                          }));

                          setAvailability(null);
                          setMessage('');
                        }}
                        required
                    />
                  </div>

                  <button
                      className="secondary-button"
                      type="button"
                      onClick={checkAvailability}
                      disabled={
                          checkingAvailability ||
                          saving ||
                          loading ||
                          !form.resourceId ||
                          !form.startTime ||
                          !form.endTime
                      }
                  >
                    {checkingAvailability
                        ? 'Checking…'
                        : 'Check availability'}

                    <RefreshCw size={16} />
                  </button>

                  {availability && (
                      <div
                          className={`notice ${availability.available ? 'success' : 'error'}`}
                          role="status"
                      >
                        {availability.available ? (
                            <CheckCircle2 size={18} />
                        ) : (
                            <AlertCircle size={18} />
                        )}

                        {availability.available
                            ? `Available — ${availability.availableQuantity} unit(s) available for this slot.`
                            : 'Not available for this time slot. Please choose another time.'}
                      </div>
                  )}

                  <button
                      className="primary-button"
                      type="submit"
                      disabled={
                          saving ||
                          checkingAvailability ||
                          loading ||
                          resources.length === 0 ||
                          !availability?.available
                      }
                  >
                    {saving ? 'Submitting…' : 'Request booking'}
                    <CalendarDays size={17} />
                  </button>
                </form>
              </section>
            </div>
        )}

        {!isStudent && !canReview && (
            <div className="panel empty-state">
              <UserRound size={28} />

              <strong>
                Booking access is not configured for this role
              </strong>

              <p>
                Your account role does not currently have permission to view
                bookings through the available backend endpoints.
              </p>
            </div>
        )}

        <section className="panel booking-history">
          <div className="panel-heading">
            <div>
              <h2>
                {isInstitutionAdmin
                    ? 'Bookings for my institution'
                    : canReview
                        ? 'All bookings'
                        : 'Booking history'}
              </h2>

              <p>
                {isInstitutionAdmin
                    ? 'Review pending requests and manage approval decisions.'
                    : canReview
                        ? 'Review booking status and take permitted actions.'
                        : 'Your reservations, session times and request statuses.'}
              </p>
            </div>

            <Clock3 size={21} />
          </div>

          {loading ? (
              <div className="loading-panel">
                Loading bookings…
              </div>
          ) : items.length > 0 ? (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                  <tr>
                    <th>ID</th>

                    {canReview && <th>Requester</th>}

                    <th>Resource</th>
                    <th>Scheduled start</th>
                    <th>Scheduled end</th>
                    <th>Actual check-in</th>
                    <th>Actual check-out</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                  </thead>

                  <tbody>
                  {items.map((booking, index) => (
                      <tr
                          key={booking.id ?? booking.bookingId ?? index}
                      >
                        <td>
                          #{booking.id ?? booking.bookingId ?? '—'}
                        </td>

                        {canReview && (
                            <td>
                              {booking.user?.fullName ??
                                  booking.userName ??
                                  booking.user?.email ??
                                  '—'}
                            </td>
                        )}

                        <td>
                          {booking.resourceName ??
                              booking.resource?.name ??
                              booking.resourceId ??
                              '—'}
                        </td>

                        <td>
                          {formatDate(
                              booking.startTime ?? booking.start_time
                          )}
                        </td>

                        <td>
                          {formatDate(
                              booking.endTime ?? booking.end_time
                          )}
                        </td>

                        <td>
                          {formatDate(
                              booking.actualStartTime ??
                              booking.actual_start_time
                          )}
                        </td>

                        <td>
                          {formatDate(
                              booking.actualEndTime ??
                              booking.actual_end_time
                          )}
                        </td>

                        <td>
                      <span
                          className={`status-chip ${String(
                              booking.status ?? 'unknown'
                          )
                              .toLowerCase()
                              .replaceAll('_', '-')}`}
                      >
                        {String(
                            booking.status ?? 'Unknown'
                        ).replaceAll('_', ' ')}
                      </span>
                        </td>

                        <td>
                          {canReview
                              ? renderActions(booking)
                              : renderStudentSessionActions(booking)}
                        </td>
                      </tr>
                  ))}
                  </tbody>
                </table>
              </div>
          ) : (
              <div className="empty-state">
                <CalendarDays size={28} />

                <strong>
                  {canReview ? 'No bookings found' : 'No bookings yet'}
                </strong>

                <span>
              {isInstitutionAdmin
                  ? 'No booking records were returned for your institution.'
                  : canReview
                      ? 'No booking records were returned by the backend.'
                      : 'Your booking requests will appear here.'}
            </span>
              </div>
          )}
        </section>
        <WaitlistRecurringPanel />
      </div>
  );
}