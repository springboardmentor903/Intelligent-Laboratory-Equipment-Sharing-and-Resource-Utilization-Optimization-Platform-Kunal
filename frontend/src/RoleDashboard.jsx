
import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
    ResponsiveContainer,
    BarChart,
    Bar,
    XAxis,
    YAxis,
    Tooltip,
    CartesianGrid,
    LineChart,
    Line,
} from 'recharts';
import {
    Activity,
    AlertTriangle,
    ArrowUpRight,
    Bell,
    CalendarDays,
    CheckCircle2,
    Clock3,
    Cpu,
    DollarSign,
    Package,
    RefreshCw,
    Users,
    Wrench,
} from 'lucide-react';

import './RoleDashboard.css';
import api from './api/client';
import { useAuth } from './context/AuthContext';

const arr = value =>
    Array.isArray(value)
        ? value
        : value?.content || value?.items || value?.data || [];

const num = value => Number(value ?? 0);

const money = value =>
    value == null
        ? '—'
        : new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency: 'INR',
            maximumFractionDigits: 0,
        }).format(Number(value));

const pct = value =>
    value == null ? '—' : `${Number(value).toFixed(1)}%`;

const date = value =>
    value ? new Date(value).toLocaleDateString('en-IN') : '—';

function getDisplayName(user) {
    return (
        user?.fullName ||
        user?.name ||
        user?.firstName ||
        user?.email?.split('@')[0] ||
        'User'
    );
}

function Card({ label, value, note, icon: Icon }) {
    return (
        <article className="rd-stat-card">
            <div className="rd-stat-top">
                <span>{label}</span>
                <span className="rd-stat-icon">
          <Icon size={20} />
        </span>
            </div>
            <strong>{value ?? '—'}</strong>
            {note && <p>{note}</p>}
        </article>
    );
}

function Panel({ title, subtitle, children, link }) {
    return (
        <section className="rd-panel">
            <div className="rd-panel-heading">
                <div>
                    <h3>{title}</h3>
                    {subtitle && <p>{subtitle}</p>}
                </div>
                {link && (
                    <Link className="rd-link" to={link}>
                        View <ArrowUpRight size={15} />
                    </Link>
                )}
            </div>
            {children}
        </section>
    );
}

function Empty({ text = 'No records available.' }) {
    return <div className="rd-empty">{text}</div>;
}

function ErrorLine({ children }) {
    return children ? (
        <div className="rd-error">
            <AlertTriangle size={17} />
            <span>{children}</span>
        </div>
    ) : null;
}

function Loading() {
    return (
        <div className="rd-loading">
            <RefreshCw className="rd-spin" size={22} />
            <span>Loading dashboard...</span>
        </div>
    );
}

export default function RoleDashboardContent() {
    const { user } = useAuth();
    const displayName = getDisplayName(user);

    const role = String(
        user?.role?.name ??
        user?.role?.roleName ??
        user?.roleName ??
        (typeof user?.role === 'string' ? user.role : '') ??
        user?.authorities?.[0]?.authority ??
        ''
    )
        .replace(/^ROLE_/, '')
        .toUpperCase();

    const isResearcher = role === 'RESEARCHER_STUDENT';
    const isManager = ['LAB_MANAGER', 'DEPARTMENT_HEAD'].includes(role);
    const isInstitution = ['INSTITUTION_ADMIN', 'SYSTEM_ADMIN'].includes(role);
    const isSystem = role === 'SYSTEM_ADMIN';

    const [data, setData] = useState(null);
    const [procurement, setProcurement] = useState(null);
    const [lifecycle, setLifecycle] = useState(null);
    const [system, setSystem] = useState(null);

    const [errors, setErrors] = useState({});
    const [loading, setLoading] = useState(false);
    const [period, setPeriod] = useState(30);
    const [page, setPage] = useState(0);

    const [institutionId, setInstitutionId] = useState(
        user?.institution?.id || user?.institutionId || ''
    );

    const load = useCallback(async () => {
        setLoading(true);
        setErrors({});

        const fail = {};

        try {
            if (isResearcher) {
                const response = await api.get('/dashboard/researcher');
                setData(response.data);
            } else if (isManager) {
                const to = new Date();
                const from = new Date();
                from.setDate(to.getDate() - period);

                const formatDate = value => value.toISOString().slice(0, 10);

                const response = await api.get('/dashboard/manager', {
                    params: {
                        fromDate: formatDate(from),
                        toDate: formatDate(to),
                    },
                });

                setData(response.data);
            } else if (isInstitution) {
                let id = institutionId;

                const params = {};

                if (isSystem) {
                    if (!id) {
                        throw new Error('System Admin ke liye Institution ID enter karo.');
                    }
                    params.institutionId = id;
                }

                const response = await api.get('/dashboard/institution', {
                    params,
                });

                setData(response.data);

                id = response.data?.institutionId || id;

                if (id) {
                    setInstitutionId(String(id));

                    const jobs = await Promise.allSettled([
                        api.get('/dashboard/institution/procurement', {
                            params: { institutionId: id },
                        }),
                        api.get('/dashboard/institution/lifecycle', {
                            params: {
                                institutionId: id,
                                page,
                                size: 8,
                            },
                        }),
                    ]);

                    if (jobs[0].status === 'fulfilled') {
                        setProcurement(jobs[0].value.data);
                    } else {
                        fail.procurement =
                            jobs[0].reason?.response?.data?.message ||
                            'Procurement data unavailable.';
                    }

                    if (jobs[1].status === 'fulfilled') {
                        setLifecycle(jobs[1].value.data);
                    } else {
                        fail.lifecycle =
                            jobs[1].reason?.response?.data?.message ||
                            'Lifecycle data unavailable.';
                    }
                }

                try {
                    const result = await api.get('/dashboard/institution/system', {
                        params: isSystem ? { institutionId: id } : {},
                    });
                    setSystem(result.data);
                } catch (error) {
                    fail.system =
                        error.response?.data?.message ||
                        'System monitoring unavailable.';
                }
            } else {
                throw new Error('Dashboard is not configured for this role.');
            }
        } catch (error) {
            fail.main =
                error.response?.data?.message ||
                error.response?.data?.error ||
                error.message ||
                'Dashboard could not be loaded.';
        } finally {
            setErrors(fail);
            setLoading(false);
        }
    }, [
        isResearcher,
        isManager,
        isInstitution,
        isSystem,
        institutionId,
        period,
        page,
    ]);

    useEffect(() => {
        load();
    }, [load]);

    const refresh = (
        <button
            className="rd-refresh"
            onClick={load}
            disabled={loading}
            type="button"
        >
            <RefreshCw size={16} />
            {loading ? 'Refreshing...' : 'Refresh'}
        </button>
    );

    if (loading && !data) {
        return (
            <div className="rd-dashboard">
                <Loading />
            </div>
        );
    }

    // RESEARCHER / STUDENT DASHBOARD
    if (isResearcher) {
        const d = data || {};
        const usage = d.usageSummary || {};
        const wait = d.waitlist || {};
        const notices = d.notifications || {};

        return (
            <div className="rd-dashboard">
                <div className="rd-page-heading">
                    <div>
                        <span className="rd-eyebrow">PERSONAL OVERVIEW</span>
                        <h1>Welcome back, {displayName}!</h1>
                        <p className="rd-role-subtitle">Researcher / Student Dashboard</p>
                        <p>Your bookings, usage, waitlist and equipment suggestions.</p>
                    </div>
                    {refresh}
                </div>

                <ErrorLine>{errors.main}</ErrorLine>

                <div className="rd-stats-grid">
                    <Card
                        label="Upcoming bookings"
                        value={d.upcomingBookings?.length ?? '—'}
                        icon={CalendarDays}
                    />
                    <Card
                        label="Actual usage (90 days)"
                        value={`${num(usage.actualUsageHours).toFixed(1)} hrs`}
                        icon={Clock3}
                    />
                    <Card
                        label="No-shows (90 days)"
                        value={usage.noShowCount ?? '—'}
                        icon={AlertTriangle}
                    />
                    <Card
                        label="Usage cost (90 days)"
                        value={money(usage.usageCost)}
                        icon={DollarSign}
                    />
                    <Card
                        label="Unread notifications"
                        value={notices.unreadCount ?? '—'}
                        icon={Bell}
                    />
                </div>

                {wait.offeredCount > 0 && (
                    <div className="rd-offer-banner">
                        <Bell size={21} />
                        <div>
                            <strong>You have a waitlist offer!</strong>
                            <p>
                                {wait.offeredCount} equipment offer(s) are waiting for your
                                attention.
                            </p>
                        </div>
                        <Link to="/waitlist">View offers</Link>
                    </div>
                )}

                <div className="rd-content-grid">
                    <Panel
                        title="Upcoming bookings"
                        subtitle="Next five reservations"
                        link="/bookings"
                    >
                        {d.upcomingBookings?.length ? (
                            <div className="rd-table-wrap">
                                <table>
                                    <thead>
                                    <tr>
                                        <th>Equipment</th>
                                        <th>Start</th>
                                        <th>End</th>
                                        <th>Status</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {d.upcomingBookings.map(booking => (
                                        <tr key={booking.bookingId}>
                                            <td>{booking.equipmentName}</td>
                                            <td>{date(booking.startTime)}</td>
                                            <td>{date(booking.endTime)}</td>
                                            <td>
                                                <span className="rd-status">{booking.status}</span>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <Empty text="No upcoming bookings." />
                        )}
                    </Panel>

                    <Panel
                        title="Equipment availability"
                        subtitle="Equipment grouped by status"
                        link="/equipment"
                    >
                        {d.equipmentAvailabilityByStatus &&
                        Object.keys(d.equipmentAvailabilityByStatus).length ? (
                            Object.entries(d.equipmentAvailabilityByStatus).map(
                                ([status, count]) => (
                                    <div className="rd-data-row" key={status}>
                                        <strong>{status.replaceAll('_', ' ')}</strong>
                                        <span className="rd-count">{count}</span>
                                    </div>
                                )
                            )
                        ) : (
                            <Empty />
                        )}
                        <Link className="rd-bottom-link" to="/equipment">
                            Browse equipment <ArrowUpRight size={15} />
                        </Link>
                    </Panel>

                    <Panel title="Usage summary" subtitle="Bookings by status">
                        {usage.bookingsByStatus &&
                        Object.keys(usage.bookingsByStatus).length ? (
                            Object.entries(usage.bookingsByStatus).map(([status, count]) => (
                                <div className="rd-data-row" key={status}>
                                    <span>{status.replaceAll('_', ' ')}</span>
                                    <strong>{count}</strong>
                                </div>
                            ))
                        ) : (
                            <Empty />
                        )}
                    </Panel>

                    <Panel
                        title="Waitlist"
                        subtitle={`${wait.waitingCount ?? 0} waiting · ${
                            wait.offeredCount ?? 0
                        } offered`}
                    >
                        {wait.offers?.length ? (
                            wait.offers.map(offer => (
                                <div className="rd-data-row" key={offer.waitlistEntryId}>
                                    <div>
                                        <strong>{offer.equipmentName}</strong>
                                        <small>
                                            {offer.status} · Expires {date(offer.offerExpiresAt)}
                                        </small>
                                    </div>
                                    <span className="rd-status">{offer.status}</span>
                                </div>
                            ))
                        ) : (
                            <Empty text="No active waitlist offers." />
                        )}
                    </Panel>

                    <Panel title="Latest notifications" link="/notifications">
                        {notices.latest?.length ? (
                            notices.latest.map(notification => (
                                <div className="rd-data-row" key={notification.id}>
                                    <div>
                                        <strong>{notification.title}</strong>
                                        <small>{notification.message}</small>
                                    </div>
                                    <small>{date(notification.createdAt)}</small>
                                </div>
                            ))
                        ) : (
                            <Empty text="No recent notifications." />
                        )}
                    </Panel>

                    <Panel
                        title="Recommended equipment"
                        subtitle="Recommendations returned by the platform"
                        link="/equipment"
                    >
                        {d.recommendations?.length ? (
                            <div className="rd-table-wrap">
                                <table>
                                    <thead>
                                    <tr>
                                        <th>Equipment</th>
                                        <th>Category</th>
                                        <th>Available</th>
                                        <th>Utilization</th>
                                        <th>Action</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {d.recommendations.map(item => (
                                        <tr key={item.equipmentId}>
                                            <td>{item.equipmentName}</td>
                                            <td>{item.category}</td>
                                            <td>{item.availableQuantity ?? 0}</td>
                                            <td>{pct(item.recentUtilizationPercentage)}</td>
                                            <td>
                                                <Link className="rd-book-link" to="/equipment">
                                                    Book
                                                </Link>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <Empty text="No recommendations available." />
                        )}
                    </Panel>
                </div>
            </div>
        );
    }

    // LAB MANAGER / DEPARTMENT HEAD DASHBOARD
    if (isManager) {
        const d = data || {};
        const utilization = d.utilization || {};
        const calibration = d.calibrationOverview || {};
        const heatmap = arr(d.utilizationHeatmap);
        const workOrders = d.openWorkOrdersByStatus || {};
        const maintenance = arr(d.equipmentUnderMaintenance);
        const schedulesSoon = arr(d.preventiveSchedulesDueSoon);
        const schedulesOverdue = arr(d.preventiveSchedulesOverdue);
        const highDemand = arr(d.highDemandEquipment);

        const heatmapChart = heatmap.map(item => ({
            label: `${date(item.date)} ${item.hour ?? ''}`,
            hours: num(item.usageHours),
        }));

        return (
            <div className="rd-dashboard">
                <div className="rd-page-heading">
                    <div>
            <span className="rd-eyebrow">
              {role === 'DEPARTMENT_HEAD' ? 'DEPARTMENT' : 'INSTITUTION'} OVERVIEW
            </span>
                        <h1>Welcome back, {displayName}!</h1>
                        <p className="rd-role-subtitle">
                            {role === 'DEPARTMENT_HEAD' ? 'Department Head Dashboard' : 'Lab Manager Dashboard'}
                        </p>
                        <p>Utilization, adoption, maintenance and calibration overview.</p>
                    </div>

                    <div className="rd-heading-actions">
                        <select
                            className="rd-select"
                            value={period}
                            onChange={event => setPeriod(Number(event.target.value))}
                        >
                            <option value={7}>Last 7 days</option>
                            <option value={30}>Last 30 days</option>
                            <option value={90}>Last 90 days</option>
                        </select>
                        {refresh}
                    </div>
                </div>

                <ErrorLine>{errors.main}</ErrorLine>

                <div className="rd-stats-grid">
                    <Card
                        label="Actual utilization"
                        value={pct(utilization.actualUtilizationPercentage)}
                        note={`Target ${pct(
                            utilization.targetUtilizationPercentage
                        )} · Gap ${pct(utilization.gapPercentage)}`}
                        icon={Activity}
                    />
                    <Card
                        label="Booking adoption"
                        value={pct(d.bookingAdoptionPercentage)}
                        icon={Users}
                    />
                    <Card
                        label="No-show rate"
                        value={pct(d.noShowRatePercentage)}
                        icon={AlertTriangle}
                    />
                    <Card
                        label="Open work orders"
                        value={Object.values(workOrders).reduce(
                            (total, value) => total + num(value),
                            0
                        )}
                        icon={Wrench}
                    />
                    <Card
                        label="Equipment under maintenance"
                        value={maintenance.length}
                        icon={Package}
                    />
                    <Card
                        label="Calibration valid"
                        value={calibration.validCount ?? '—'}
                        note={`Due soon ${calibration.dueSoonCount ?? 0} · Expired ${
                            calibration.expiredCount ?? 0
                        } · None ${calibration.noneCount ?? 0}`}
                        icon={CheckCircle2}
                    />
                </div>

                <div className="rd-content-grid">
                    <Panel
                        title="Utilization heatmap"
                        subtitle={`${date(d.fromDate)} – ${date(d.toDate)}`}
                    >
                        {heatmapChart.length ? (
                            <ResponsiveContainer width="100%" height={280}>
                                <BarChart data={heatmapChart}>
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="label" hide />
                                    <YAxis />
                                    <Tooltip />
                                    <Bar
                                        dataKey="hours"
                                        name="Usage hours"
                                        fill="#2563eb"
                                        radius={[5, 5, 0, 0]}
                                    />
                                </BarChart>
                            </ResponsiveContainer>
                        ) : (
                            <Empty text="No utilization heatmap data." />
                        )}
                    </Panel>

                    <Panel title="Work orders by status" link="/work-orders">
                        {Object.keys(workOrders).length ? (
                            Object.entries(workOrders).map(([status, count]) => (
                                <div className="rd-data-row" key={status}>
                                    <span>{status.replaceAll('_', ' ')}</span>
                                    <strong>{count}</strong>
                                </div>
                            ))
                        ) : (
                            <Empty />
                        )}
                    </Panel>

                    <Panel title="High-demand equipment" link="/equipment">
                        {highDemand.length ? (
                            highDemand.map(item => (
                                <div className="rd-data-row" key={item.equipmentId}>
                                    <div>
                                        <strong>{item.equipmentName}</strong>
                                        <small>{item.category}</small>
                                    </div>
                                    <span className="rd-count">
                    {pct(
                        item.bookingRatePercentage ??
                        item.utilizationPercentage
                    )}
                  </span>
                                </div>
                            ))
                        ) : (
                            <Empty />
                        )}
                    </Panel>

                    <Panel
                        title="Equipment under maintenance"
                        link="/work-orders"
                    >
                        {maintenance.length ? (
                            maintenance.map(item => (
                                <div className="rd-data-row" key={item.equipmentId}>
                                    <div>
                                        <strong>{item.equipmentName}</strong>
                                        <small>
                                            {item.currentStatus} · WO {item.activeWorkOrderId ?? '—'}
                                        </small>
                                    </div>
                                    <span className="rd-status">{item.workOrderStatus}</span>
                                </div>
                            ))
                        ) : (
                            <Empty />
                        )}
                    </Panel>

                    <Panel
                        title="Preventive schedules due soon"
                        link="/maintenance-schedules"
                    >
                        {schedulesSoon.length ? (
                            schedulesSoon.map(item => (
                                <div className="rd-data-row" key={item.scheduleId}>
                                    <div>
                                        <strong>{item.equipmentName}</strong>
                                        <small>Due {date(item.dueDate)}</small>
                                    </div>
                                    <span className="rd-status">{item.status}</span>
                                </div>
                            ))
                        ) : (
                            <Empty />
                        )}
                    </Panel>

                    <Panel
                        title="Preventive schedules overdue"
                        link="/maintenance-schedules"
                    >
                        {schedulesOverdue.length ? (
                            schedulesOverdue.map(item => (
                                <div className="rd-data-row" key={item.scheduleId}>
                                    <div>
                                        <strong>{item.equipmentName}</strong>
                                        <small>Due {date(item.dueDate)}</small>
                                    </div>
                                    <span className="rd-status rd-status-danger">
                    {item.status}
                  </span>
                                </div>
                            ))
                        ) : (
                            <Empty text="No overdue schedules." />
                        )}
                    </Panel>

                    {role === 'LAB_MANAGER' ? (
                        <Panel
                            title="Pending booking approvals"
                            link="/booking-approvals"
                        >
                            {arr(d.pendingBookingApprovals).length ? (
                                arr(d.pendingBookingApprovals).map(item => (
                                    <div className="rd-data-row" key={item.bookingId}>
                                        <div>
                                            <strong>{item.equipmentName}</strong>
                                            <small>{item.requesterName}</small>
                                        </div>
                                        <span>{date(item.startTime)}</span>
                                    </div>
                                ))
                            ) : (
                                <Empty text="No pending booking approvals." />
                            )}
                        </Panel>
                    ) : (
                        <Panel
                            title="Department sharing requests"
                            link="/resource-sharing"
                        >
                            {arr(d.departmentSharingRequests).length ? (
                                arr(d.departmentSharingRequests).map(item => (
                                    <div className="rd-data-row" key={item.requestId}>
                                        <div>
                                            <strong>{item.equipmentName}</strong>
                                            <small>{item.status}</small>
                                        </div>
                                        <span>{date(item.requestedAt)}</span>
                                    </div>
                                ))
                            ) : (
                                <Empty text="No department sharing requests." />
                            )}
                        </Panel>
                    )}
                </div>
            </div>
        );
    }

    // INSTITUTION ADMIN / SYSTEM ADMIN DASHBOARD
    if (isInstitution) {
        const d = data || {};
        const cost = d.costBudgetSummary || {};
        const s = system || {};
        const counts = s.systemCounts || {};

        const recommendations = arr(procurement?.recommendations);
        const lifecycleItems = arr(
            lifecycle?.items || lifecycle?.content || lifecycle?.data
        );

        return (
            <div className="rd-dashboard">
                <div className="rd-page-heading">
                    <div>
                        <span className="rd-eyebrow">INSTITUTION INTELLIGENCE</span>
                        <h1>Welcome back, {displayName}!</h1>
                        <p className="rd-role-subtitle">
                            {isSystem ? 'System Administrator Dashboard' : 'Institution Administrator Dashboard'}
                        </p>
                        <p>
                            {d.institutionName ||
                                'Institution-wide utilization, sharing, cost and lifecycle.'}
                            {d.readOnly ? ' · Read-only view' : ''}
                        </p>
                    </div>

                    <div className="rd-heading-actions">
                        {isSystem && (
                            <input
                                className="rd-select"
                                placeholder="Institution ID"
                                value={institutionId}
                                onChange={event => setInstitutionId(event.target.value)}
                            />
                        )}
                        {refresh}
                    </div>
                </div>

                <ErrorLine>{errors.main}</ErrorLine>
                <ErrorLine>{errors.system}</ErrorLine>

                <div className="rd-stats-grid">
                    <Card
                        label="Budget utilization"
                        value={pct(cost.budgetUtilizationPercentage)}
                        note={`${money(cost.consumedBudget)} consumed`}
                        icon={DollarSign}
                    />
                    <Card
                        label="Allocated budget"
                        value={money(cost.allocatedBudget)}
                        icon={Package}
                    />
                    <Card
                        label="Remaining budget"
                        value={money(cost.remainingBudget)}
                        icon={CheckCircle2}
                    />
                    <Card
                        label="Usage charges"
                        value={money(cost.totalUsageCharges)}
                        icon={Activity}
                    />
                    <Card
                        label="Maintenance cost"
                        value={money(cost.totalMaintenanceCost)}
                        icon={Wrench}
                    />
                    <Card
                        label="Failed notifications (7 days)"
                        value={s.failedNotificationDeliveriesLast7Days ?? '—'}
                        icon={Bell}
                    />
                </div>

                <div className="rd-content-grid">
                    <Panel
                        title="Department utilization"
                        subtitle="Actual utilization against target"
                    >
                        {d.departmentUtilization?.length ? (
                            <ResponsiveContainer width="100%" height={280}>
                                <BarChart data={d.departmentUtilization}>
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="departmentName" />
                                    <YAxis />
                                    <Tooltip />
                                    <Bar
                                        dataKey="utilizationPercentage"
                                        name="Actual %"
                                        fill="#2563eb"
                                        radius={[5, 5, 0, 0]}
                                    />
                                    <Bar
                                        dataKey="targetPercentage"
                                        name="Target %"
                                        fill="#16a34a"
                                        radius={[5, 5, 0, 0]}
                                    />
                                </BarChart>
                            </ResponsiveContainer>
                        ) : (
                            <Empty />
                        )}
                    </Panel>

                    <Panel
                        title="Monthly utilization trend"
                        subtitle="Snapshot-based trend"
                    >
                        {d.monthlyUtilizationTrends?.length ? (
                            <ResponsiveContainer width="100%" height={280}>
                                <LineChart data={d.monthlyUtilizationTrends}>
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis dataKey="month" />
                                    <YAxis />
                                    <Tooltip />
                                    <Line
                                        type="monotone"
                                        dataKey="utilizationPercentage"
                                        name="Utilization"
                                        stroke="#2563eb"
                                        strokeWidth={3}
                                        dot={{ r: 4 }}
                                    />
                                </LineChart>
                            </ResponsiveContainer>
                        ) : (
                            <Empty />
                        )}
                    </Panel>

                    <Panel title="Inter-institution sharing">
                        <div className="rd-mini-grid">
                            <Card
                                label="Outgoing"
                                value={d.interInstitutionSharing?.outgoingBookings ?? 0}
                                icon={ArrowUpRight}
                            />
                            <Card
                                label="Incoming"
                                value={d.interInstitutionSharing?.incomingBookings ?? 0}
                                icon={ArrowUpRight}
                            />
                            <Card
                                label="Hours shared"
                                value={num(
                                    d.interInstitutionSharing?.hoursShared
                                ).toFixed(1)}
                                icon={Clock3}
                            />
                            <Card
                                label="Fees earned"
                                value={money(d.interInstitutionSharing?.feesEarned)}
                                icon={DollarSign}
                            />
                        </div>
                    </Panel>

                    <Panel
                        title="Cross-department sharing"
                        subtitle={`${num(
                            d.crossDepartmentSharing?.totalHours
                        ).toFixed(1)} total hours`}
                    >
                        {d.crossDepartmentSharing?.largestDepartmentPairs?.length ? (
                            d.crossDepartmentSharing.largestDepartmentPairs.map(
                                (item, index) => (
                                    <div className="rd-data-row" key={index}>
                                        <div>
                                            <strong>
                                                {item.ownerDepartmentName} → {item.userDepartmentName}
                                            </strong>
                                            <small>
                                                {num(item.sharedHours).toFixed(1)} hours
                                            </small>
                                        </div>
                                    </div>
                                )
                            )
                        ) : (
                            <Empty />
                        )}
                    </Panel>

                    <Panel title="Idle equipment alerts">
                        {d.idleEquipmentAlerts?.length ? (
                            d.idleEquipmentAlerts.map(item => (
                                <div className="rd-data-row" key={item.equipmentId}>
                                    <div>
                                        <strong>{item.equipmentName}</strong>
                                        <small>{item.idleDays} idle days</small>
                                    </div>
                                    <span className="rd-status">
                    {pct(item.utilizationPercentage)}
                  </span>
                                </div>
                            ))
                        ) : (
                            <Empty text="No idle equipment alerts." />
                        )}
                    </Panel>

                    <Panel
                        title="Procurement recommendations"
                        subtitle="Backend rule-based recommendations"
                    >
                        <ErrorLine>{errors.procurement}</ErrorLine>

                        {recommendations.length ? (
                            <div className="rd-table-wrap">
                                <table>
                                    <thead>
                                    <tr>
                                        <th>Reason</th>
                                        <th>Equipment / Category</th>
                                        <th>Evidence</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {recommendations.map((item, index) => (
                                        <tr key={`${item.equipmentId}-${index}`}>
                                            <td>
                          <span className="rd-status">
                            {item.reasonCode}
                          </span>
                                            </td>
                                            <td>{item.equipmentName || item.category}</td>
                                            <td>
                                                {item.reasonCode === 'HIGH_DEMAND'
                                                    ? `Booking rate ${pct(
                                                        item.averageBookingRatePercentage
                                                    )}, waitlist ${
                                                        item.waitlistEntriesLast90Days ?? '—'
                                                    }`
                                                    : item.reasonCode === 'LOW_UTILIZATION'
                                                        ? `Utilization ${pct(
                                                            item.utilizationLast90DaysPercentage
                                                        )}`
                                                        : `Maintenance ${money(
                                                            item.maintenanceCostLast12Months
                                                        )} / Purchase ${money(item.purchaseCost)}`}
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <Empty text="No procurement recommendations available." />
                        )}
                    </Panel>

                    <Panel
                        title="Equipment lifecycle & ROI"
                        subtitle="Equipment age, costs and return on investment"
                    >
                        <ErrorLine>{errors.lifecycle}</ErrorLine>

                        {lifecycleItems.length ? (
                            <>
                                <div className="rd-table-wrap">
                                    <table>
                                        <thead>
                                        <tr>
                                            <th>Equipment</th>
                                            <th>Age</th>
                                            <th>Calibration</th>
                                            <th>Work orders</th>
                                            <th>Maintenance</th>
                                            <th>Usage charges</th>
                                            <th>ROI</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {lifecycleItems.map(item => (
                                            <tr key={item.equipmentId}>
                                                <td>{item.equipmentName}</td>
                                                <td>
                                                    {item.ageInDays == null
                                                        ? '—'
                                                        : `${item.ageInDays} days`}
                                                </td>
                                                <td>{item.latestCalibrationState || '—'}</td>
                                                <td>{item.workOrderCount ?? '—'}</td>
                                                <td>{money(item.maintenanceCost)}</td>
                                                <td>{money(item.usageCharges)}</td>
                                                <td>{pct(item.roiPercentage)}</td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>

                                <div className="rd-pagination">
                                    <button
                                        type="button"
                                        disabled={page === 0}
                                        onClick={() => setPage(value => Math.max(0, value - 1))}
                                    >
                                        Previous
                                    </button>
                                    <span>Page {page + 1}</span>
                                    <button
                                        type="button"
                                        disabled={
                                            lifecycle?.last === true ||
                                            lifecycle?.lastPage === true
                                        }
                                        onClick={() => setPage(value => value + 1)}
                                    >
                                        Next
                                    </button>
                                </div>
                            </>
                        ) : (
                            <Empty text="No lifecycle records available." />
                        )}
                    </Panel>

                    <Panel
                        title="System monitoring"
                        subtitle={`Last utilization snapshot: ${date(
                            s.lastUtilizationSnapshotAt
                        )}`}
                    >
                        <div className="rd-data-row">
                            <span>Total users</span>
                            <strong>{counts.totalUsers ?? '—'}</strong>
                        </div>
                        <div className="rd-data-row">
                            <span>Active / Inactive users</span>
                            <strong>
                                {counts.activeUsers ?? '—'} / {counts.inactiveUsers ?? '—'}
                            </strong>
                        </div>
                        <div className="rd-data-row">
                            <span>Total equipment</span>
                            <strong>{counts.totalEquipment ?? '—'}</strong>
                        </div>
                        <div className="rd-data-row">
                            <span>Under maintenance</span>
                            <strong>{s.equipmentUnderMaintenance ?? '—'}</strong>
                        </div>
                        <div className="rd-data-row">
                            <span>Pending Department Head approvals</span>
                            <strong>{s.pendingDepartmentHeadApprovals ?? '—'}</strong>
                        </div>
                    </Panel>

                    <Panel title="Institution users" subtitle="User roles and account status">
                        {s.users?.length ? (
                            <div className="rd-table-wrap">
                                <table>
                                    <thead>
                                    <tr>
                                        <th>Name</th>
                                        <th>Role</th>
                                        <th>Department</th>
                                        <th>Status</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {s.users.map(item => (
                                        <tr key={item.userId}>
                                            <td>
                                                <strong>{item.fullName}</strong>
                                                <small>{item.email}</small>
                                            </td>
                                            <td>{item.role}</td>
                                            <td>{item.departmentName || '—'}</td>
                                            <td>
                                                <span className="rd-status">{item.status}</span>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <Empty text="No institution users available." />
                        )}
                    </Panel>
                </div>
            </div>
        );
    }

    return (
        <div className="rd-dashboard">
            <div className="rd-page-heading">
                <div>
                    <h1>Welcome back, {displayName}!</h1>
                    <p>This role does not have a dashboard configured.</p>
                </div>
                {refresh}
            </div>
        </div>
    );
}
