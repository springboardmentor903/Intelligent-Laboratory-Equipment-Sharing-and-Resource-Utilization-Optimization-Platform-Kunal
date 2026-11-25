
import '../styles/MaintenanceHistory.css'
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
    AlertCircle,
    BarChart3,
    CheckCircle2,
    Clock,
    RefreshCw,
    Wrench,
} from 'lucide-react'
import api from '../api/client'
import { useAuth } from '../context/AuthContext'


function getItems(response) {
    const data = response?.data

    if (Array.isArray(data)) return data
    if (Array.isArray(data?.content)) return data.content
    if (Array.isArray(data?.items)) return data.items
    if (Array.isArray(data?.data)) return data.data

    return []
}

function getErrorMessage(error, fallback) {
    return (
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        error?.response?.data?.detail ||
        fallback
    )
}

function formatDate(value) {
    if (!value) return '—'

    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value

    return date.toLocaleString('en-IN', {
        dateStyle: 'medium',
        timeStyle: 'short',
    })
}

function formatCurrency(value) {
    if (value === null || value === undefined || value === '') {
        return '—'
    }

    return Number(value).toLocaleString('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2,
    })
}

function formatDuration(minutes) {
    const total = Math.max(0, Number(minutes) || 0)
    const days = Math.floor(total / 1440)
    const hours = Math.floor((total % 1440) / 60)
    const mins = total % 60

    if (days > 0) return `${days}d ${hours}h ${mins}m`
    if (hours > 0) return `${hours}h ${mins}m`
    return `${mins}m`
}

function toApiDate(value) {
    if (!value) return undefined
    return value.length === 16 ? `${value}:00` : value
}

function StatusBadge({ status }) {
    const value = String(status || 'UNKNOWN').toUpperCase()

    const styles = {
        COMPLETED: 'bg-green-100 text-green-800',
        CANCELLED: 'bg-gray-100 text-gray-700',
        IN_PROGRESS: 'bg-blue-100 text-blue-800',
        OPEN: 'bg-amber-100 text-amber-800',
        ASSIGNED: 'bg-indigo-100 text-indigo-800',
    }

    return (
        <span
            className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${
    styles[value] || 'bg-gray-100 text-gray-700'
}`}
        >
      {value.replaceAll('_', ' ')}
    </span>
    )
}

function StatCard({ title, value, subtitle, icon, color = 'blue' }) {
    const colors = {
        blue: 'm3-tone-blue',
        green: 'm3-tone-green',
        amber: 'm3-tone-amber',
        purple: 'm3-tone-purple',
    }

    return (
        <div className="m3-maintenancehistory-001">
            <div className="m3-maintenancehistory-002">
                <p className="m3-maintenancehistory-003">{title}</p>
                <div className={`rounded-lg p-2 ${colors[color]}`}>
                    {icon}
                </div>
            </div>
            <p className="m3-maintenancehistory-004">{value}</p>
            {subtitle && (
                <p className="m3-maintenancehistory-005">{subtitle}</p>
            )}
        </div>
    )
}

export default function MaintenanceHistory() {
    const { user } = useAuth()

    const rawRole =
        user?.role?.name ??
        user?.role?.roleName ??
        user?.roleName ??
        (typeof user?.role === 'string' ? user.role : null) ??
        user?.authorities?.[0]?.authority ??
        user?.authorities?.[0] ??
        ''

    const role = String(rawRole).replace(/^ROLE_/, '').trim().toUpperCase()

    const isManager = role === 'LAB_MANAGER'
    const isTechnician = role === 'LAB_TECHNICIAN'
    const isInstitutionAdmin = role === 'INSTITUTION_ADMIN'
    const isDepartmentHead = role === 'DEPARTMENT_HEAD'
    const hasAccess =
        isManager || isTechnician || isInstitutionAdmin || isDepartmentHead

    const [workOrders, setWorkOrders] = useState([])
    const [downtimeRecords, setDowntimeRecords] = useState([])
    const [analytics, setAnalytics] = useState(null)

    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [refreshing, setRefreshing] = useState(false)

    const [fromDate, setFromDate] = useState('')
    const [toDate, setToDate] = useState('')

    const [historyError, setHistoryError] = useState('')
    const [downtimeError, setDowntimeError] = useState('')
    const [analyticsError, setAnalyticsError] = useState('')

    const loadData = useCallback(async () => {
        if (!hasAccess) {
            setLoading(false)
            return
        }

        setLoading(true)
        setRefreshing(true)
        setError('')
        setHistoryError('')
        setDowntimeError('')
        setAnalyticsError('')

        const params = {
            from: toApiDate(fromDate),
            to: toApiDate(toDate),
        }

        const tasks = []

        if (isManager) {
            tasks.push({
                key: 'history',
                request: api.get('/work-orders', {
                    params: {
                        status: 'COMPLETED',
                        page: 0,
                        size: 100,
                        sort: 'completedAt,desc',
                    },
                }),
            })

            tasks.push({
                key: 'downtime',
                request: api.get('/maintenance/downtime/institution', {
                    params,
                }),
            })

            tasks.push({
                key: 'analytics',
                request: api.get('/maintenance/analytics/downtime', {
                    params,
                }),
            })
        } else if (isTechnician) {
            tasks.push({
                key: 'history',
                request: api.get('/work-orders/my', {
                    params: {
                        page: 0,
                        size: 100,
                        sort: 'completedAt,desc',
                    },
                }),
            })

            tasks.push({
                key: 'downtime',
                request: api.get('/maintenance/downtime/my', {
                    params,
                }),
            })
        } else if (isInstitutionAdmin) {
            tasks.push({
                key: 'analytics',
                request: api.get('/maintenance/analytics/downtime', {
                    params,
                }),
            })
        }

        try {
            const results = await Promise.all(
                tasks.map(async (task) => {
                    try {
                        return {
                            key: task.key,
                            result: await task.request,
                            error: null,
                        }
                    } catch (err) {
                        return {
                            key: task.key,
                            result: null,
                            error: err,
                        }
                    }
                })
            )

            results.forEach(({ key, result, error: requestError }) => {
                if (requestError) {
                    const message = getErrorMessage(
                        requestError,
                        'This information could not be loaded.'
                    )

                    if (key === 'history') setHistoryError(message)
                    if (key === 'downtime') setDowntimeError(message)
                    if (key === 'analytics') setAnalyticsError(message)

                    if (key === 'history') setWorkOrders([])
                    if (key === 'downtime') setDowntimeRecords([])
                    if (key === 'analytics') setAnalytics(null)

                    return
                }

                if (key === 'history') {
                    const items = getItems(result).filter(
                        (item) => String(item.status || '').toUpperCase() === 'COMPLETED'
                    )
                    setWorkOrders(items)
                }

                if (key === 'downtime') {
                    setDowntimeRecords(getItems(result))
                }

                if (key === 'analytics') {
                    setAnalytics(result?.data || null)
                }
            })
        } catch (err) {
            setError(
                getErrorMessage(err, 'Maintenance history could not be loaded.')
            )
        } finally {
            setLoading(false)
            setRefreshing(false)
        }
    }, [
        hasAccess,
        isManager,
        isTechnician,
        isInstitutionAdmin,
        isDepartmentHead,
        fromDate,
        toDate,
    ])

    useEffect(() => {
        loadData()
    }, [loadData])

    const totalHistoryCost = useMemo(() => {
        return workOrders.reduce(
            (total, item) => total + Number(item.actualCost || 0),
            0
        )
    }, [workOrders])

    const completedCount = workOrders.length

    const totalRecordDowntime = downtimeRecords.reduce(
        (total, item) => total + Number(item.downtimeMinutes || 0),
        0
    )

    const validateAndRefresh = () => {
        if (fromDate && toDate && new Date(toDate) < new Date(fromDate)) {
            setError('End date must be after or equal to start date.')
            return
        }

        setError('')
        loadData()
    }

    const clearDates = () => {
        setFromDate('')
        setToDate('')
    }

    if (!hasAccess) {
        return (
            <div className="m3-maintenancehistory-006">
                <div className="m3-maintenancehistory-007">
                    <AlertCircle size={22} />
                    <h2 className="m3-maintenancehistory-008">Access Restricted</h2>
                </div>
                <p className="m3-maintenancehistory-009">
                    You are not authorized to view maintenance history and downtime.
                </p>
            </div>
        )
    }

    return (
        <div className="m3-maintenancehistory-010">
            <div className="m3-maintenancehistory-011">
                <div>
                    <div className="m3-maintenancehistory-012">
                        <div className="m3-maintenancehistory-013">
                            <Wrench size={23} />
                        </div>
                        <h1 className="m3-maintenancehistory-014">
                            Maintenance History & Downtime
                        </h1>
                    </div>

                    <p className="m3-maintenancehistory-015">
                        Review completed maintenance work, downtime records and performance.
                    </p>
                </div>

                <button
                    type="button"
                    onClick={validateAndRefresh}
                    disabled={refreshing}
                    className="m3-maintenancehistory-016"
                >
                    <RefreshCw
                        size={16}
                        className={refreshing ? 'm3-spin' : ''}
                    />
                    Refresh
                </button>
            </div>

            {(error || historyError || downtimeError || analyticsError) && (
                <div className="m3-maintenancehistory-017">
                    {error && <ErrorBanner message={error} />}
                    {historyError && <ErrorBanner message={`Work Order History: ${historyError}`} />}
                    {downtimeError && <ErrorBanner message={`Downtime Records: ${downtimeError}`} />}
                    {analyticsError && <ErrorBanner message={`Downtime Analytics: ${analyticsError}`} />}
                </div>
            )}

            {!isDepartmentHead && (
            <section className="m3-maintenancehistory-018">
                <div className="m3-maintenancehistory-019">
                    <h2 className="m3-maintenancehistory-020">Date Range Filter</h2>
                    <p className="m3-maintenancehistory-021">
                        Filter downtime and analytics by maintenance date.
                    </p>
                </div>

                <div className="m3-maintenancehistory-022">
                    <div>
                        <label className="m3-maintenancehistory-023">
                            From
                        </label>
                        <input
                            type="datetime-local"
                            value={fromDate}
                            onChange={(event) => setFromDate(event.target.value)}
                            className="m3-maintenancehistory-024"
                        />
                    </div>

                    <div>
                        <label className="m3-maintenancehistory-023">
                            To
                        </label>
                        <input
                            type="datetime-local"
                            value={toDate}
                            onChange={(event) => setToDate(event.target.value)}
                            className="m3-maintenancehistory-024"
                        />
                    </div>

                    <button
                        type="button"
                        onClick={validateAndRefresh}
                        disabled={refreshing}
                        className="m3-maintenancehistory-025"
                    >
                        Apply Filter
                    </button>

                    <button
                        type="button"
                        onClick={() => {
                            clearDates()
                        }}
                        className="m3-maintenancehistory-026"
                    >
                        Clear
                    </button>
                </div>
            </section>
            )}

            {loading ? (
                <div className="m3-maintenancehistory-027">
                    <RefreshCw size={28} className="m3-maintenancehistory-028" />
                    <p className="m3-maintenancehistory-029">Loading maintenance records...</p>
                </div>
            ) : isDepartmentHead ? (
                <section className="m3-maintenancehistory-018">
                    <EmptyState
                        title="Department Maintenance History Unavailable"
                        message="Department-wise completed work orders and downtime history are not currently provided by the backend. No institution-wide analytics are shown for this role."
                    />
                </section>
            ) : (
                <>
                    {(isManager || isTechnician) && (
                        <>
                            <div className="m3-maintenancehistory-030">
                                <StatCard
                                    title="Completed Work Orders"
                                    value={completedCount}
                                    subtitle="Loaded completed records"
                                    icon={<CheckCircle2 size={20} />}
                                    color="green"
                                />

                                <StatCard
                                    title="Downtime Records"
                                    value={downtimeRecords.length}
                                    subtitle="Records returned by backend"
                                    icon={<Clock size={20} />}
                                    color="amber"
                                />

                                <StatCard
                                    title="Recorded Downtime"
                                    value={formatDuration(totalRecordDowntime)}
                                    subtitle="From downtime records"
                                    icon={<Wrench size={20} />}
                                    color="purple"
                                />

                                <StatCard
                                    title="Actual Maintenance Cost"
                                    value={formatCurrency(totalHistoryCost)}
                                    subtitle="From completed work orders"
                                    icon={<BarChart3 size={20} />}
                                    color="blue"
                                />
                            </div>

                            <section className="m3-maintenancehistory-018">
                                <div className="m3-maintenancehistory-031">
                                    <h2 className="m3-maintenancehistory-032">
                                        Completed Work Order History
                                    </h2>
                                    <p className="m3-maintenancehistory-021">
                                        {isManager
                                            ? 'Completed work orders across your institution.'
                                            : 'Your assigned completed work orders.'}
                                    </p>
                                </div>

                                {workOrders.length === 0 ? (
                                    <EmptyState
                                        title="No completed work orders"
                                        message="Completed maintenance work orders will appear here."
                                    />
                                ) : (
                                    <div className="m3-maintenancehistory-033">
                                        <table className="m3-maintenancehistory-034">
                                            <thead>
                                            <tr className="m3-maintenancehistory-035">
                                                <th className="m3-maintenancehistory-036">Work Order</th>
                                                <th className="m3-maintenancehistory-036">Equipment</th>
                                                <th className="m3-maintenancehistory-036">Type</th>
                                                <th className="m3-maintenancehistory-036">Technician</th>
                                                <th className="m3-maintenancehistory-036">Started</th>
                                                <th className="m3-maintenancehistory-036">Completed</th>
                                                <th className="m3-maintenancehistory-036">Actual Cost</th>
                                                <th className="m3-maintenancehistory-036">Status</th>
                                            </tr>
                                            </thead>

                                            <tbody className="m3-maintenancehistory-037">
                                            {workOrders.map((item) => (
                                                <tr key={item.id} className="m3-maintenancehistory-038">
                                                    <td className="m3-maintenancehistory-039">
                                                        <p className="m3-maintenancehistory-040">
                                                            {item.workOrderNumber || `#${item.id}`}
                                                        </p>
                                                        <p className="m3-maintenancehistory-005">
                                                            ID: {item.id}
                                                        </p>
                                                    </td>

                                                    <td className="m3-maintenancehistory-039">
                                                        <p className="m3-maintenancehistory-041">
                                                            {item.resourceName || '—'}
                                                        </p>
                                                        <p className="m3-maintenancehistory-005">
                                                            Resource ID: {item.resourceId ?? '—'}
                                                        </p>
                                                    </td>

                                                    <td className="m3-maintenancehistory-042">
                                                        {item.type || '—'}
                                                    </td>

                                                    <td className="m3-maintenancehistory-042">
                                                        {item.assignedTechnicianName || 'Not assigned'}
                                                    </td>

                                                    <td className="m3-maintenancehistory-043">
                                                        {formatDate(item.actualStart)}
                                                    </td>

                                                    <td className="m3-maintenancehistory-043">
                                                        {formatDate(item.completedAt || item.actualEnd)}
                                                    </td>

                                                    <td className="m3-maintenancehistory-044">
                                                        {formatCurrency(item.actualCost)}
                                                    </td>

                                                    <td className="m3-maintenancehistory-039">
                                                        <StatusBadge status={item.status} />
                                                    </td>
                                                </tr>
                                            ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </section>

                            <section className="m3-maintenancehistory-018">
                                <div className="m3-maintenancehistory-031">
                                    <h2 className="m3-maintenancehistory-032">
                                        Downtime Records
                                    </h2>
                                    <p className="m3-maintenancehistory-021">
                                        Equipment downtime, maintenance technician and resolution details.
                                    </p>
                                </div>

                                {downtimeRecords.length === 0 ? (
                                    <EmptyState
                                        title="No downtime records"
                                        message="Downtime records will appear here when maintenance work has started."
                                    />
                                ) : (
                                    <div className="m3-maintenancehistory-033">
                                        <table className="m3-maintenancehistory-034">
                                            <thead>
                                            <tr className="m3-maintenancehistory-035">
                                                <th className="m3-maintenancehistory-036">Work Order</th>
                                                <th className="m3-maintenancehistory-036">Equipment</th>
                                                <th className="m3-maintenancehistory-036">Type</th>
                                                <th className="m3-maintenancehistory-036">Technician</th>
                                                <th className="m3-maintenancehistory-036">Downtime Start</th>
                                                <th className="m3-maintenancehistory-036">Downtime End</th>
                                                <th className="m3-maintenancehistory-036">Duration</th>
                                                <th className="m3-maintenancehistory-036">Cost</th>
                                            </tr>
                                            </thead>

                                            <tbody className="m3-maintenancehistory-037">
                                            {downtimeRecords.map((item) => (
                                                <tr key={item.workOrderId} className="m3-maintenancehistory-038">
                                                    <td className="m3-maintenancehistory-039">
                                                        <p className="m3-maintenancehistory-040">
                                                            {item.workOrderNumber || `#${item.workOrderId}`}
                                                        </p>
                                                        <p className="m3-maintenancehistory-005">
                                                            ID: {item.workOrderId}
                                                        </p>
                                                    </td>

                                                    <td className="m3-maintenancehistory-039">
                                                        <p className="m3-maintenancehistory-041">
                                                            {item.resourceName || '—'}
                                                        </p>
                                                        <p className="m3-maintenancehistory-005">
                                                            Resource ID: {item.resourceId ?? '—'}
                                                        </p>
                                                    </td>

                                                    <td className="m3-maintenancehistory-042">
                                                        {item.maintenanceType || '—'}
                                                    </td>

                                                    <td className="m3-maintenancehistory-042">
                                                        {item.technicianName || '—'}
                                                    </td>

                                                    <td className="m3-maintenancehistory-043">
                                                        {formatDate(item.downtimeStart)}
                                                    </td>

                                                    <td className="m3-maintenancehistory-043">
                                                        {formatDate(item.downtimeEnd)}
                                                    </td>

                                                    <td className="m3-maintenancehistory-045">
                                                        {formatDuration(item.downtimeMinutes)}
                                                    </td>

                                                    <td className="m3-maintenancehistory-046">
                                                        {formatCurrency(item.maintenanceCost)}
                                                    </td>
                                                </tr>
                                            ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </section>
                        </>
                    )}

                    {(isManager || isInstitutionAdmin) && (
                        <section className="m3-maintenancehistory-018">
                            <div className="m3-maintenancehistory-031">
                                <h2 className="m3-maintenancehistory-047">
                                    <BarChart3 size={20} className="m3-maintenancehistory-048" />
                                    Downtime Analytics
                                </h2>
                                <p className="m3-maintenancehistory-021">
                                    Downtime and equipment utilization loss for the selected period.
                                </p>
                            </div>

                            {!analytics ? (
                                <EmptyState
                                    title="Analytics unavailable"
                                    message="Downtime analytics could not be loaded for this period."
                                />
                            ) : (
                                <>
                                    <div className="m3-maintenancehistory-049">
                                        <StatCard
                                            title="Total Downtime"
                                            value={formatDuration(analytics.totalDowntimeMinutes)}
                                            icon={<Clock size={20} />}
                                            color="amber"
                                        />

                                        <StatCard
                                            title="Downtime During Open Hours"
                                            value={formatDuration(analytics.downtimeDuringOpenHoursMinutes)}
                                            icon={<Wrench size={20} />}
                                            color="purple"
                                        />

                                        <StatCard
                                            title="Open Hours Lost"
                                            value={`${Number(analytics.openHoursLostPercentage || 0).toFixed(2)}%`}
                                            subtitle={`${formatDuration(analytics.downtimeDuringOpenHoursMinutes)} lost out of ${formatDuration(analytics.totalOpenHoursMinutes)} available`}
                                            icon={<BarChart3 size={20} />}
                                            color="blue"
                                        />

                                        <StatCard
                                            title="Average Downtime"
                                            value={formatDuration(analytics.averageDowntimeMinutes)}
                                            subtitle="Average per work order"
                                            icon={<Clock size={20} />}
                                            color="amber"
                                        />

                                        <StatCard
                                            title="Work Order Count"
                                            value={analytics.workOrderCount ?? 0}
                                            icon={<CheckCircle2 size={20} />}
                                            color="green"
                                        />

                                        <StatCard
                                            title="Total Open Hours"
                                            value={formatDuration(analytics.totalOpenHoursMinutes)}
                                            icon={<BarChart3 size={20} />}
                                            color="blue"
                                        />
                                    </div>

                                    <div className="m3-maintenancehistory-050">
                                        <h3 className="m3-maintenancehistory-051">
                                            Equipment-wise Downtime
                                        </h3>

                                        {!Array.isArray(analytics.equipmentDetails) ||
                                        analytics.equipmentDetails.length === 0 ? (
                                            <EmptyState
                                                title="No equipment details"
                                                message="No equipment downtime details are available for this period."
                                            />
                                        ) : (
                                            <div className="m3-maintenancehistory-033">
                                                <table className="m3-maintenancehistory-052">
                                                    <thead>
                                                    <tr className="m3-maintenancehistory-035">
                                                        <th className="m3-maintenancehistory-036">Equipment</th>
                                                        <th className="m3-maintenancehistory-036">Resource ID</th>
                                                        <th className="m3-maintenancehistory-036">Total Downtime</th>
                                                        <th className="m3-maintenancehistory-036">Open Hours Downtime</th>
                                                        <th className="m3-maintenancehistory-036">Work Orders</th>
                                                    </tr>
                                                    </thead>

                                                    <tbody className="m3-maintenancehistory-037">
                                                    {analytics.equipmentDetails.map((item) => (
                                                        <tr key={item.resourceId} className="m3-maintenancehistory-038">
                                                            <td className="m3-maintenancehistory-053">
                                                                {item.resourceName || '—'}
                                                            </td>
                                                            <td className="m3-maintenancehistory-042">
                                                                {item.resourceId ?? '—'}
                                                            </td>
                                                            <td className="m3-maintenancehistory-045">
                                                                {formatDuration(item.downtimeMinutes)}
                                                            </td>
                                                            <td className="m3-maintenancehistory-042">
                                                                {formatDuration(item.downtimeDuringOpenHoursMinutes)}
                                                            </td>
                                                            <td className="m3-maintenancehistory-042">
                                                                {item.workOrderCount ?? 0}
                                                            </td>
                                                        </tr>
                                                    ))}
                                                    </tbody>
                                                </table>
                                            </div>
                                        )}
                                    </div>
                                </>
                            )}
                        </section>
                    )}
                </>
            )}
        </div>
    )
}

function ErrorBanner({ message }) {
    return (
        <div className="m3-maintenancehistory-054">
            <AlertCircle size={18} className="m3-maintenancehistory-055" />
            <span>{message}</span>
        </div>
    )
}

function EmptyState({ title, message }) {
    return (
        <div className="m3-maintenancehistory-056">
            <div className="m3-maintenancehistory-057">
                <Wrench size={25} />
            </div>
            <h3 className="m3-maintenancehistory-040">{title}</h3>
            <p className="m3-maintenancehistory-058">{message}</p>
        </div>
    )
}

