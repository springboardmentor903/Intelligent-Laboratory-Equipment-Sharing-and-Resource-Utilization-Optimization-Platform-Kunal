
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
    Activity, AlertTriangle, ArrowUpRight, Building2, IndianRupee,
    RefreshCw, Users, Wallet, Share2, Package, Wrench, CheckCircle2,
    ChevronLeft, ChevronRight, Search, X
} from 'lucide-react';
import {
    ResponsiveContainer, BarChart, Bar, XAxis, YAxis,
    CartesianGrid, Tooltip
} from 'recharts';
import api from '../api/client';
import './InstitutionAdminDashboard.css';

const getRole = user => String(
    user?.role?.name ?? user?.role?.roleName ?? user?.roleName ??
    (typeof user?.role === 'string' ? user.role : '') ??
    user?.authorities?.[0]?.authority ?? ''
).replace(/^ROLE_/, '').toUpperCase();

const money = value => value == null
    ? '—'
    : `₹${Number(value).toLocaleString('en-IN', { maximumFractionDigits: 2 })}`;

const num = value => value == null
    ? '—'
    : Number(value).toLocaleString('en-IN');

const pct = value => value == null
    ? '—'
    : `${Number(value).toFixed(2)}%`;

const date = value => value
    ? new Date(value).toLocaleDateString('en-IN')
    : '—';

const asArray = value => Array.isArray(value)
    ? value
    : value?.content ?? value?.items ?? [];

function Metric({ label, value, note, icon: Icon, tone = 'blue' }) {
    return (
        <article className="ia-metric">
            <div className="ia-metric-heading">
                <span>{label}</span>
                <span className={`ia-icon ${tone}`}><Icon size={19} /></span>
            </div>
            <strong>{value}</strong>
            <small>{note}</small>
        </article>
    );
}

function Panel({ title, subtitle, action, children, className = '' }) {
    return (
        <section className={`ia-panel ${className}`}>
            <div className="ia-panel-heading">
                <div>
                    <h2>{title}</h2>
                    {subtitle && <p>{subtitle}</p>}
                </div>
                {action}
            </div>
            {children}
        </section>
    );
}

function Empty({ children = 'No records available.' }) {
    return <div className="ia-empty">{children}</div>;
}

export default function InstitutionAdminDashboard({ user }) {
    const role = getRole(user);
    const isSystemAdmin = role === 'SYSTEM_ADMIN';

    const [institutionId, setInstitutionId] = useState(
        user?.institution?.id ?? user?.institutionId ?? ''
    );
    const [institutions, setInstitutions] = useState([]);
    const [dashboard, setDashboard] = useState(null);
    const [system, setSystem] = useState(null);
    const [procurement, setProcurement] = useState(null);
    const [lifecycle, setLifecycle] = useState(null);

    const [page, setPage] = useState(0);
    const [sortKey, setSortKey] = useState('equipmentName');
    const [sortDirection, setSortDirection] = useState('asc');
    const [search, setSearch] = useState('');

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [actionError, setActionError] = useState('');
    const [deactivating, setDeactivating] = useState(null);

    const loadInstitutions = useCallback(async () => {
        if (!isSystemAdmin) return;
        try {
            const response = await api.get('/institutions');
            const list = asArray(response.data);
            setInstitutions(list);

            if (!institutionId && list.length) {
                setInstitutionId(String(list[0].id));
            }
        } catch (err) {
            setError(err.response?.data?.message || 'Institutions load nahi ho paaye.');
        }
    }, [isSystemAdmin, institutionId]);

    useEffect(() => {
        loadInstitutions();
    }, [loadInstitutions]);

    const loadDashboard = useCallback(async () => {
        if (isSystemAdmin && !institutionId) {
            setLoading(false);
            return;
        }

        setLoading(true);
        setError('');
        setActionError('');

        try {
            // Institution Admin is scoped from the authenticated user by the backend.
            // System Admin must explicitly select an institution.
            const params = isSystemAdmin ? { institutionId } : {};

            const [dashboardResponse, systemResponse] = await Promise.all([
                api.get('/dashboard/institution', { params }),
                api.get('/dashboard/institution/system', { params }),
            ]);

            const dashboardData = dashboardResponse.data;
            const systemData = systemResponse.data;
            setDashboard(dashboardData);
            setSystem(systemData);

            const resolvedInstitutionId =
                dashboardData?.institutionId ??
                (isSystemAdmin ? institutionId : (user?.institution?.id ?? user?.institutionId));

            if (resolvedInstitutionId && String(resolvedInstitutionId) !== String(institutionId)) {
                setInstitutionId(String(resolvedInstitutionId));
            }

            if (!resolvedInstitutionId) {
                setProcurement(null);
                setLifecycle(null);
                setActionError('Institution ID dashboard response mein nahi mila; procurement aur lifecycle load nahi ho sake.');
                return;
            }

            const scopedParams = { institutionId: resolvedInstitutionId };
            const results = await Promise.allSettled([
                api.get('/dashboard/institution/procurement', { params: scopedParams }),
                api.get('/dashboard/institution/lifecycle', {
                    params: { ...scopedParams, page, size: 10 },
                }),
            ]);

            if (results[0].status === 'fulfilled') {
                setProcurement(results[0].value.data);
            } else {
                setProcurement(null);
                setActionError(
                    results[0].reason?.response?.data?.message ||
                    'Procurement recommendations load nahi ho paayi.'
                );
            }

            if (results[1].status === 'fulfilled') {
                setLifecycle(results[1].value.data);
            } else {
                setLifecycle(null);
                setActionError(previous => [
                    previous,
                    results[1].reason?.response?.data?.message ||
                    'Equipment lifecycle data load nahi hua.'
                ].filter(Boolean).join(' | '));
            }
        } catch (err) {
            setError(
                err.response?.data?.message ||
                err.response?.data?.error ||
                'Institution dashboard load nahi ho paya.'
            );
        } finally {
            setLoading(false);
        }
    }, [institutionId, isSystemAdmin, page, user?.institution?.id, user?.institutionId]);

    useEffect(() => {
        loadDashboard();
    }, [loadDashboard]);

    const departments = dashboard?.departmentUtilization ?? [];
    const trends = useMemo(
        () => (dashboard?.monthlyUtilizationTrends ?? []).map(item => ({
            ...item,
            monthLabel: new Date(item.year, item.month - 1, 1)
                .toLocaleDateString('en-IN', { month: 'short', year: '2-digit' })
        })),
        [dashboard]
    );

    const sharing = dashboard?.interInstitutionSharing ?? {};
    const crossSharing = dashboard?.crossDepartmentSharing ?? {};
    const costs = dashboard?.costBudgetSummary ?? {};
    const systemCounts = system?.systemCounts ?? {};

    const averageUtilization = departments.length
        ? departments.reduce(
        (total, item) => total + Number(item.utilizationPercentage ?? 0), 0
    ) / departments.length
        : null;

    const alerts = [
        ...(dashboard?.idleEquipmentAlerts ?? []).map(item => ({
            id: `idle-${item.equipmentId}`,
            title: item.equipmentName,
            description: `${num(item.idleDays)} idle days`,
            category: 'Idle equipment'
        })),
        ...(Number(system?.failedNotificationDeliveriesLast7Days ?? 0) > 0
            ? [{
                id: 'notification-failures',
                title: 'Failed notification deliveries',
                description: `${num(system.failedNotificationDeliveriesLast7Days)} failures in last 7 days`,
                category: 'System'
            }]
            : [])
    ];

    const recommendations = procurement?.recommendations ?? [];
    const lifecycleItems = lifecycle?.items ?? [];

    const sortedLifecycle = useMemo(() => {
        const filtered = lifecycleItems.filter(item =>
            `${item.equipmentName ?? ''} ${item.category ?? ''} ${item.currentStatus ?? ''}`
                .toLowerCase().includes(search.toLowerCase())
        );

        return [...filtered].sort((a, b) => {
            const left = a[sortKey] ?? '';
            const right = b[sortKey] ?? '';
            const result = typeof left === 'number' && typeof right === 'number'
                ? left - right
                : String(left).localeCompare(String(right));
            return sortDirection === 'asc' ? result : -result;
        });
    }, [lifecycleItems, search, sortKey, sortDirection]);

    const roiRows = lifecycleItems
        .filter(item => item.roiPercentage != null)
        .slice(0, 8)
        .map(item => ({
            name: item.equipmentName,
            roi: Number(item.roiPercentage)
        }));

    function sortBy(key) {
        if (sortKey === key) {
            setSortDirection(current => current === 'asc' ? 'desc' : 'asc');
        } else {
            setSortKey(key);
            setSortDirection('asc');
        }
    }

    async function deactivateUser(userId, fullName) {
        const confirmed = window.confirm(
            `Are you sure you want to deactivate ${fullName}?`
        );
        if (!confirmed) return;

        setDeactivating(userId);
        setActionError('');

        try {
            await api.patch(
                `/dashboard/institution/system/users/${userId}/deactivate`,
                null,
                { params: isSystemAdmin ? { institutionId } : {} }
            );
            await loadDashboard();
        } catch (err) {
            setActionError(
                err.response?.data?.message ||
                err.response?.data?.error ||
                'User deactivate nahi ho paya.'
            );
        } finally {
            setDeactivating(null);
        }
    }

    const changeInstitution = value => {
        setInstitutionId(value);
        setPage(0);
        setDashboard(null);
        setSystem(null);
        setProcurement(null);
        setLifecycle(null);
    };

    return (
        <div className="ia-dashboard">
            <div className="ia-topline">
                <div>
                    <span className="ia-eyebrow">INSTITUTION ADMINISTRATION</span>
                    <h1>{dashboard?.institutionName || 'Institution Dashboard'}</h1>
                    <p>Institution-wide utilization, sharing, procurement and lifecycle overview.</p>
                </div>
                <div className="ia-top-actions">
                    {isSystemAdmin && (
                        <select
                            value={institutionId}
                            onChange={event => changeInstitution(event.target.value)}
                            aria-label="Select institution"
                        >
                            <option value="">Select institution</option>
                            {institutions.map(item => (
                                <option key={item.id} value={item.id}>{item.name}</option>
                            ))}
                        </select>
                    )}
                    <button className="ia-button" onClick={loadDashboard} disabled={loading}>
                        <RefreshCw size={16} />
                        Refresh
                    </button>
                </div>
            </div>

            {error && (
                <div className="ia-error">
                    <AlertTriangle size={18} />
                    <span>{error}</span>
                    <button onClick={loadDashboard}>Retry</button>
                </div>
            )}

            {actionError && (
                <div className="ia-error">
                    <AlertTriangle size={18} />
                    <span>{actionError}</span>
                    <button onClick={() => setActionError('')}><X size={16} /></button>
                </div>
            )}

            {loading && !dashboard ? (
                <div className="ia-loading">Loading institution dashboard…</div>
            ) : dashboard ? (
                <>
                    <div className="ia-metrics-grid">
                        <Metric
                            label="Average utilization"
                            value={pct(averageUtilization)}
                            note="Across institution departments"
                            icon={Activity}
                            tone="blue"
                        />
                        <Metric
                            label="Sharing hours"
                            value={num(sharing.hoursShared)}
                            note={`${num(sharing.partnerInstitutionCount)} partner institutions`}
                            icon={Share2}
                            tone="purple"
                        />
                        <Metric
                            label="Sharing fees earned"
                            value={money(sharing.feesEarned)}
                            note={`${num(sharing.outgoingBookings)} outgoing bookings`}
                            icon={IndianRupee}
                            tone="green"
                        />
                        <Metric
                            label="Budget consumed"
                            value={money(costs.consumedBudget)}
                            note={`${pct(costs.budgetUtilizationPercentage)} of allocated budget`}
                            icon={Wallet}
                            tone="orange"
                        />
                        <Metric
                            label="System alerts"
                            value={num(alerts.length)}
                            note="Current institution alerts"
                            icon={AlertTriangle}
                            tone="red"
                        />
                        <Metric
                            label="Active users"
                            value={num(systemCounts.activeUsers)}
                            note={`${num(systemCounts.totalUsers)} total users`}
                            icon={Users}
                            tone="blue"
                        />
                    </div>

                    <div className="ia-grid-two">
                        <Panel
                            title="Department utilization"
                            subtitle="Actual utilization compared with target"
                        >
                            {departments.length ? (
                                <div className="ia-table-scroll">
                                    <table className="ia-table">
                                        <thead>
                                        <tr>
                                            <th>Department</th>
                                            <th>Actual</th>
                                            <th>Target</th>
                                            <th>Gap</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {departments.map(item => (
                                            <tr key={item.departmentId}>
                                                <td>{item.departmentName}</td>
                                                <td>{pct(item.utilizationPercentage)}</td>
                                                <td>{pct(item.targetPercentage)}</td>
                                                <td className={Number(item.gapPercentage) < 0 ? 'ia-negative' : 'ia-positive'}>
                                                    {pct(item.gapPercentage)}
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            ) : <Empty>No department utilization records found.</Empty>}
                        </Panel>

                        <Panel
                            title="Monthly utilization trend"
                            subtitle="Monthly actual utilization from backend"
                        >
                            {trends.length ? (
                                <div className="ia-chart">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <BarChart data={trends}>
                                            <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                            <XAxis dataKey="monthLabel" />
                                            <YAxis unit="%" />
                                            <Tooltip formatter={value => pct(value)} />
                                            <Bar
                                                dataKey="utilizationPercentage"
                                                name="Utilization"
                                                fill="#4776e6"
                                                radius={[5, 5, 0, 0]}
                                            />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </div>
                            ) : <Empty>Monthly utilization data is not available.</Empty>}
                        </Panel>
                    </div>

                    <div className="ia-grid-two">
                        <Panel
                            title="Institution sharing overview"
                            subtitle="Incoming, outgoing and cross-department sharing"
                            action={
                                <Link to="/sharing-analytics" className="ia-link">
                                    View analytics <ArrowUpRight size={15} />
                                </Link>
                            }
                        >
                            <div className="ia-stat-list">
                                <div><span>Incoming bookings</span><strong>{num(sharing.incomingBookings)}</strong></div>
                                <div><span>Outgoing bookings</span><strong>{num(sharing.outgoingBookings)}</strong></div>
                                <div><span>Hours shared</span><strong>{num(sharing.hoursShared)}</strong></div>
                                <div><span>Fees earned</span><strong>{money(sharing.feesEarned)}</strong></div>
                                <div><span>Cross-department hours</span><strong>{num(crossSharing.totalHours)}</strong></div>
                            </div>

                            <h3 className="ia-subheading">Largest department pairs</h3>
                            {crossSharing.largestDepartmentPairs?.length ? (
                                <div className="ia-pair-list">
                                    {crossSharing.largestDepartmentPairs.map((pair, index) => (
                                        <div key={`${pair.ownerDepartmentId}-${pair.userDepartmentId}-${index}`}>
                                            <span>{pair.ownerDepartmentName} → {pair.userDepartmentName}</span>
                                            <strong>{num(pair.sharedHours)} hrs</strong>
                                        </div>
                                    ))}
                                </div>
                            ) : <Empty>No cross-department sharing records.</Empty>}
                        </Panel>

                        <Panel
                            title="Cost and budget status"
                            subtitle="Financial figures supplied by backend"
                            action={
                                <Link to="/cost-overview" className="ia-link">
                                    Cost overview <ArrowUpRight size={15} />
                                </Link>
                            }
                        >
                            <div className="ia-stat-list">
                                <div><span>Total usage charges</span><strong>{money(costs.totalUsageCharges)}</strong></div>
                                <div><span>Maintenance cost</span><strong>{money(costs.totalMaintenanceCost)}</strong></div>
                                <div><span>Allocated budget</span><strong>{money(costs.allocatedBudget)}</strong></div>
                                <div><span>Consumed budget</span><strong>{money(costs.consumedBudget)}</strong></div>
                                <div><span>Remaining budget</span><strong>{money(costs.remainingBudget)}</strong></div>
                            </div>
                            <div className="ia-quick-links">
                                <Link to="/budgets">Budgets <ArrowUpRight size={14} /></Link>
                                <Link to="/invoices">Invoices <ArrowUpRight size={14} /></Link>
                                <Link to="/financial-summary">Financial summary <ArrowUpRight size={14} /></Link>
                            </div>
                        </Panel>
                    </div>

                    <Panel
                        title="Procurement recommendations"
                        subtitle="Backend-generated equipment demand and cost recommendations"
                    >
                        {recommendations.length ? (
                            <div className="ia-table-scroll">
                                <table className="ia-table">
                                    <thead>
                                    <tr>
                                        <th>Equipment</th>
                                        <th>Recommendation</th>
                                        <th>Booking rate</th>
                                        <th>Waitlist (90d)</th>
                                        <th>Utilization (90d)</th>
                                        <th>Maintenance (12m)</th>
                                        <th>Purchase cost</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {recommendations.map((item, index) => (
                                        <tr key={`${item.equipmentId}-${item.reasonCode}-${index}`}>
                                            <td>
                                                <strong>{item.equipmentName}</strong>
                                                <small>{item.category}</small>
                                            </td>
                                            <td>
                          <span className={`ia-reason ${String(item.reasonCode).toLowerCase()}`}>
                            {item.reasonCode?.replaceAll('_', ' ')}
                          </span>
                                                <small>{item.description}</small>
                                            </td>
                                            <td>{pct(item.averageBookingRatePercentage)}</td>
                                            <td>{num(item.waitlistEntriesLast90Days)}</td>
                                            <td>{pct(item.utilizationLast90DaysPercentage)}</td>
                                            <td>{money(item.maintenanceCostLast12Months)}</td>
                                            <td>{money(item.purchaseCost)}</td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : <Empty>No procurement recommendations available.</Empty>}
                    </Panel>

                    <Panel
                        title="Equipment lifecycle and ROI"
                        subtitle="Purchase, calibration, maintenance and return-on-investment figures"
                    >
                        <div className="ia-lifecycle-toolbar">
                            <div className="ia-search">
                                <Search size={16} />
                                <input
                                    value={search}
                                    onChange={event => setSearch(event.target.value)}
                                    placeholder="Search equipment, category or status"
                                />
                            </div>
                            <span>{num(lifecycle?.totalElements)} equipment records</span>
                        </div>

                        {sortedLifecycle.length ? (
                            <div className="ia-table-scroll">
                                <table className="ia-table">
                                    <thead>
                                    <tr>
                                        {[
                                            ['equipmentName', 'Equipment'],
                                            ['purchaseDate', 'Purchase date'],
                                            ['currentStatus', 'Status'],
                                            ['latestCalibrationState', 'Calibration'],
                                            ['workOrderCount', 'Work orders'],
                                            ['downtimeHours', 'Downtime (hrs)'],
                                            ['purchaseCost', 'Purchase cost'],
                                            ['maintenanceCost', 'Maintenance'],
                                            ['usageCharges', 'Usage charges'],
                                            ['roiPercentage', 'ROI']
                                        ].map(([key, label]) => (
                                            <th key={key}>
                                                <button className="ia-sort" onClick={() => sortBy(key)}>
                                                    {label}
                                                    {sortKey === key ? (sortDirection === 'asc' ? ' ↑' : ' ↓') : ''}
                                                </button>
                                            </th>
                                        ))}
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {sortedLifecycle.map(item => (
                                        <tr key={item.equipmentId}>
                                            <td>
                                                <strong>{item.equipmentName}</strong>
                                                <small>{item.category}</small>
                                            </td>
                                            <td>{date(item.purchaseDate)}</td>
                                            <td><span className="ia-status">{item.currentStatus || '—'}</span></td>
                                            <td>{item.latestCalibrationState || '—'}</td>
                                            <td>{num(item.workOrderCount)}</td>
                                            <td>{num(item.downtimeHours)}</td>
                                            <td>{money(item.purchaseCost)}</td>
                                            <td>{money(item.maintenanceCost)}</td>
                                            <td>{money(item.usageCharges)}</td>
                                            <td>{item.roiPercentage == null ? '—' : pct(item.roiPercentage)}</td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : <Empty>No lifecycle records match this search.</Empty>}

                        <div className="ia-pagination">
                            <span>Page {Number(lifecycle?.page ?? page) + 1} of {Math.max(1, lifecycle?.totalPages ?? 1)}</span>
                            <div>
                                <button
                                    onClick={() => setPage(value => Math.max(0, value - 1))}
                                    disabled={page <= 0 || loading}
                                    aria-label="Previous page"
                                ><ChevronLeft size={17} /></button>
                                <button
                                    onClick={() => setPage(value => value + 1)}
                                    disabled={loading || page + 1 >= (lifecycle?.totalPages ?? 1)}
                                    aria-label="Next page"
                                ><ChevronRight size={17} /></button>
                            </div>
                        </div>

                        <h3 className="ia-subheading">ROI comparison — current page</h3>
                        {roiRows.length ? (
                            <div className="ia-chart ia-roi-chart">
                                <ResponsiveContainer width="100%" height="100%">
                                    <BarChart data={roiRows} layout="vertical" margin={{ left: 20, right: 25 }}>
                                        <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                                        <XAxis type="number" unit="%" />
                                        <YAxis type="category" dataKey="name" width={125} />
                                        <Tooltip formatter={value => pct(value)} />
                                        <Bar dataKey="roi" name="ROI" fill="#29976a" radius={[0, 5, 5, 0]} />
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                        ) : <Empty>ROI values are not available for this page.</Empty>}
                    </Panel>

                    <div className="ia-grid-two">
                        <Panel title="System monitoring" subtitle="Institution health snapshot">
                            <div className="ia-system-grid">
                                <div><Building2 size={18} /><span>Departments</span><strong>{num(systemCounts.totalDepartments)}</strong></div>
                                <div><Users size={18} /><span>Inactive users</span><strong>{num(systemCounts.inactiveUsers)}</strong></div>
                                <div><Package size={18} /><span>Active equipment</span><strong>{num(systemCounts.activeEquipment)}</strong></div>
                                <div><Wrench size={18} /><span>Under maintenance</span><strong>{num(system?.equipmentUnderMaintenance)}</strong></div>
                                <div><AlertTriangle size={18} /><span>Failed notifications (7d)</span><strong>{num(system?.failedNotificationDeliveriesLast7Days)}</strong></div>
                                <div><CheckCircle2 size={18} /><span>Pending approvals</span><strong>{num(system?.pendingDepartmentHeadApprovals)}</strong></div>
                            </div>
                            <p className="ia-last-snapshot">
                                Last utilization snapshot: {system?.lastUtilizationSnapshotAt
                                ? new Date(system.lastUtilizationSnapshotAt).toLocaleString('en-IN')
                                : 'Not available'}
                            </p>
                        </Panel>

                        <Panel
                            title="Department Head approvals"
                            subtitle="Pending institution approval requests"
                        >
                            {system?.pendingApprovals?.length ? (
                                <div className="ia-approval-list">
                                    {system.pendingApprovals.map(item => (
                                        <div key={item.userId}>
                                            <strong>{item.fullName}</strong>
                                            <span>{item.departmentName || 'Department not assigned'}</span>
                                            <small>{item.email}</small>
                                            <span className="ia-pending-label">Pending</span>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <Empty>
                                    No pending approval records returned by the backend.
                                </Empty>
                            )}
                            <p className="ia-note">
                                Approval/rejection actions are not enabled because the current backend
                                does not expose approve/reject endpoints for these requests.
                            </p>
                        </Panel>
                    </div>

                    <Panel
                        title="Institution users"
                        subtitle="View users and deactivate accounts when required"
                    >
                        {system?.users?.length ? (
                            <div className="ia-table-scroll">
                                <table className="ia-table">
                                    <thead>
                                    <tr>
                                        <th>User</th>
                                        <th>Role</th>
                                        <th>Department</th>
                                        <th>Status</th>
                                        <th>Created</th>
                                        <th>Last login</th>
                                        <th>Action</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {system.users.map(item => (
                                        <tr key={item.userId}>
                                            <td>
                                                <strong>{item.fullName}</strong>
                                                <small>{item.email}</small>
                                            </td>
                                            <td>{item.role}</td>
                                            <td>{item.departmentName || '—'}</td>
                                            <td>
                          <span className={`ia-status ${String(item.status).toLowerCase()}`}>
                            {item.status}
                          </span>
                                            </td>
                                            <td>{date(item.createdAt)}</td>
                                            <td>{item.lastLoginAt ? new Date(item.lastLoginAt).toLocaleString('en-IN') : '—'}</td>
                                            <td>
                                                {String(item.status).toUpperCase() === 'ACTIVE' ? (
                                                    <button
                                                        className="ia-deactivate"
                                                        onClick={() => deactivateUser(item.userId, item.fullName)}
                                                        disabled={deactivating === item.userId}
                                                    >
                                                        {deactivating === item.userId ? 'Working…' : 'Deactivate'}
                                                    </button>
                                                ) : <span className="ia-muted">Inactive</span>}
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : <Empty>No institution users returned.</Empty>}
                    </Panel>

                    <p className="ia-generated">
                        Generated: {dashboard.generatedAt
                        ? new Date(dashboard.generatedAt).toLocaleString('en-IN')
                        : '—'}
                    </p>
                </>
            ) : (
                <div className="ia-empty">
                    {isSystemAdmin && !institutionId
                        ? 'Select an institution to view its dashboard.'
                        : 'Dashboard data is not available.'}
                </div>
            )}
        </div>
    );
}
