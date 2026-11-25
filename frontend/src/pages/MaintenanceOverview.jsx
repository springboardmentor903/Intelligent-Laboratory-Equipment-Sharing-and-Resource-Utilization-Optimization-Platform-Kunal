import '../styles/MaintenanceOverview.css'
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
    AlertCircle, BarChart3, CalendarDays, CheckCircle2,
    Clock, RefreshCw, TrendingDown, Wrench
} from 'lucide-react'
import api from '../api/client'
import { useAuth } from '../context/AuthContext'


function formatDuration(minutes) {
    const total = Math.max(0, Number(minutes) || 0)
    const days = Math.floor(total / 1440)
    const hours = Math.floor((total % 1440) / 60)
    const mins = Math.floor(total % 60)

    if (days > 0) return `${days}d ${hours}h ${mins}m`
    if (hours > 0) return `${hours}h ${mins}m`
    return `${mins}m`
}

function formatPercentage(value) {
    return `${Number(value || 0).toFixed(2)}%`
}

function toApiDate(value) {
    if (!value) return undefined
    return value.length === 16 ? `${value}:00` : value
}

function getErrorMessage(error) {
    return (
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        error?.response?.data?.detail ||
        'Unable to load maintenance analytics. Please try again.'
    )
}

function StatCard({ title, value, description, icon, color }) {
    const colors = {
        blue: 'm3-tone-blue',
        green: 'm3-tone-green',
        amber: 'm3-tone-amber',
        purple: 'm3-tone-purple',
        red: 'm3-tone-red',
    }

    return (
        <div className="m3-maintenanceoverview-001">
            <div className="m3-maintenanceoverview-002">
                <div>
                    <p className="m3-maintenanceoverview-003">{title}</p>
                    <p className="m3-maintenanceoverview-004">{value}</p>
                    <p className="m3-maintenanceoverview-005">{description}</p>
                </div>
                <div className={`rounded-xl p-3 ${colors[color]}`}>
                    {icon}
                </div>
            </div>
        </div>
    )
}

function ErrorBanner({ message }) {
    return (
        <div className="m3-maintenanceoverview-006">
            <AlertCircle size={19} className="m3-maintenanceoverview-007" />
            <span>{message}</span>
        </div>
    )
}

function EmptyState() {
    return (
        <div className="m3-maintenanceoverview-008">
            <div className="m3-maintenanceoverview-009">
                <BarChart3 size={27} />
            </div>
            <h3 className="m3-maintenanceoverview-010">
                No equipment downtime data
            </h3>
            <p className="m3-maintenanceoverview-011">
                No equipment-wise downtime details are available for the selected period.
            </p>
        </div>
    )
}

export default function MaintenanceOverview() {
    const { user } = useAuth()

    const rawRole =
        user?.role?.name ||
        user?.role ||
        user?.authorities?.[0]?.authority ||
        user?.authorities?.[0] ||
        ''

    const role = String(rawRole).replace('ROLE_', '').toUpperCase()

    const hasAccess = ['LAB_MANAGER', 'INSTITUTION_ADMIN'].includes(role)

    const [analytics, setAnalytics] = useState(null)
    const [loading, setLoading] = useState(true)
    const [refreshing, setRefreshing] = useState(false)
    const [error, setError] = useState('')
    const [fromDate, setFromDate] = useState('')
    const [toDate, setToDate] = useState('')

    const loadAnalytics = useCallback(async () => {
        if (!hasAccess) {
            setLoading(false)
            return
        }

        if (fromDate && toDate && new Date(toDate) < new Date(fromDate)) {
            setError('End date must be after or equal to start date.')
            setLoading(false)
            setRefreshing(false)
            return
        }

        setLoading(true)
        setRefreshing(true)
        setError('')

        try {
            const response = await api.get('/maintenance/analytics/downtime', {
                params: {
                    from: toApiDate(fromDate),
                    to: toApiDate(toDate),
                },
            })

            setAnalytics(response.data || null)
        } catch (err) {
            setAnalytics(null)
            setError(getErrorMessage(err))
        } finally {
            setLoading(false)
            setRefreshing(false)
        }
    }, [hasAccess, fromDate, toDate])

    useEffect(() => {
        loadAnalytics()
    }, [loadAnalytics])

    const equipmentDetails = useMemo(() => {
        if (!Array.isArray(analytics?.equipmentDetails)) return []

        return [...analytics.equipmentDetails].sort(
            (a, b) =>
                Number(b.downtimeMinutes || 0) -
                Number(a.downtimeMinutes || 0)
        )
    }, [analytics])

    const totalDowntime = Number(analytics?.totalDowntimeMinutes || 0)
    const openHoursDowntime = Number(
        analytics?.downtimeDuringOpenHoursMinutes || 0
    )
    const totalOpenHours = Number(analytics?.totalOpenHoursMinutes || 0)
    const averageDowntime = Number(analytics?.averageDowntimeMinutes || 0)
    const workOrderCount = Number(analytics?.workOrderCount || 0)
    const lostPercentage = Number(analytics?.openHoursLostPercentage || 0)

    const maximumDowntime = Math.max(
        1,
        ...equipmentDetails.map(item => Number(item.downtimeMinutes || 0))
    )

    const handleApplyFilter = () => {
        if (fromDate && toDate && new Date(toDate) < new Date(fromDate)) {
            setError('End date must be after or equal to start date.')
            return
        }
        loadAnalytics()
    }

    const handleClearFilter = () => {
        setFromDate('')
        setToDate('')
    }

    if (!hasAccess) {
        return (
            <div className="m3-maintenanceoverview-012">
                <div className="m3-maintenanceoverview-013">
                    <AlertCircle size={22} />
                    <h2 className="m3-maintenanceoverview-014">Access Restricted</h2>
                </div>
                <p className="m3-maintenanceoverview-015">
                    Only Lab Managers and Institution Admins can access Maintenance Overview.
                </p>
            </div>
        )
    }

    return (
        <div className="m3-maintenanceoverview-016">
            <div className="m3-maintenanceoverview-017">
                <div>
                    <div className="m3-maintenanceoverview-018">
                        <div className="m3-maintenanceoverview-019">
                            <BarChart3 size={24} />
                        </div>
                        <h1 className="m3-maintenanceoverview-020">
                            Maintenance Overview
                        </h1>
                    </div>
                    <p className="m3-maintenanceoverview-021">
                        Institution-level equipment maintenance and downtime analytics.
                    </p>
                </div>

                <button
                    type="button"
                    onClick={loadAnalytics}
                    disabled={refreshing}
                    className="m3-maintenanceoverview-022"
                >
                    <RefreshCw size={16} className={refreshing ? 'm3-spin' : ''} />
                    Refresh
                </button>
            </div>

            {error && <ErrorBanner message={error} />}

            <section className="m3-maintenanceoverview-001">
                <div className="m3-maintenanceoverview-023">
                    <CalendarDays size={19} className="m3-maintenanceoverview-024" />
                    <div>
                        <h2 className="m3-maintenanceoverview-025">Analytics Date Range</h2>
                        <p className="m3-maintenanceoverview-026">
                            Select a period to calculate downtime performance.
                        </p>
                    </div>
                </div>

                <div className="m3-maintenanceoverview-027">
                    <div>
                        <label className="m3-maintenanceoverview-028">
                            From Date
                        </label>
                        <input
                            type="datetime-local"
                            value={fromDate}
                            onChange={event => setFromDate(event.target.value)}
                            className="m3-maintenanceoverview-029"
                        />
                    </div>

                    <div>
                        <label className="m3-maintenanceoverview-028">
                            To Date
                        </label>
                        <input
                            type="datetime-local"
                            value={toDate}
                            onChange={event => setToDate(event.target.value)}
                            className="m3-maintenanceoverview-029"
                        />
                    </div>

                    <button
                        type="button"
                        onClick={handleApplyFilter}
                        disabled={refreshing}
                        className="m3-maintenanceoverview-030"
                    >
                        Apply Filter
                    </button>

                    <button
                        type="button"
                        onClick={handleClearFilter}
                        className="m3-maintenanceoverview-031"
                    >
                        Clear
                    </button>
                </div>
            </section>

            {loading ? (
                <div className="m3-maintenanceoverview-032">
                    <RefreshCw size={28} className="m3-maintenanceoverview-033" />
                    <p className="m3-maintenanceoverview-034">Loading maintenance analytics...</p>
                </div>
            ) : analytics ? (
                <>
                    <div className="m3-maintenanceoverview-035">
                        <StatCard
                            title="Total Downtime"
                            value={formatDuration(totalDowntime)}
                            description="Total equipment downtime"
                            icon={<Clock size={22} />}
                            color="amber"
                        />
                        <StatCard
                            title="Open-Hours Downtime"
                            value={formatDuration(openHoursDowntime)}
                            description="Downtime during institution working hours"
                            icon={<Wrench size={22} />}
                            color="purple"
                        />
                        <StatCard
                            title="Open Hours Lost"
                            value={formatPercentage(lostPercentage)}
                            description="Percentage of available open hours lost"
                            icon={<TrendingDown size={22} />}
                            color="red"
                        />
                        <StatCard
                            title="Average Downtime"
                            value={formatDuration(averageDowntime)}
                            description="Average downtime per work order"
                            icon={<Clock size={22} />}
                            color="blue"
                        />
                        <StatCard
                            title="Work Order Count"
                            value={workOrderCount.toLocaleString('en-IN')}
                            description="Work orders included in analytics"
                            icon={<CheckCircle2 size={22} />}
                            color="green"
                        />
                        <StatCard
                            title="Total Open Hours"
                            value={formatDuration(totalOpenHours)}
                            description="Available institution operating time"
                            icon={<CalendarDays size={22} />}
                            color="blue"
                        />
                    </div>

                    <section className="m3-maintenanceoverview-001">
                        <div className="m3-maintenanceoverview-036">
                            <div>
                                <h2 className="m3-maintenanceoverview-037">
                                    Equipment-wise Downtime
                                </h2>
                                <p className="m3-maintenanceoverview-038">
                                    Comparison of equipment downtime in the selected period.
                                </p>
                            </div>
                            <span className="m3-maintenanceoverview-039">
                {equipmentDetails.length} equipment item(s)
              </span>
                        </div>

                        {equipmentDetails.length === 0 ? (
                            <EmptyState />
                        ) : (
                            <div className="m3-maintenanceoverview-040">
                                {equipmentDetails.map(item => {
                                    const downtime = Number(item.downtimeMinutes || 0)
                                    const openDowntime = Number(
                                        item.downtimeDuringOpenHoursMinutes || 0
                                    )
                                    const percentage = Math.min(
                                        100,
                                        (downtime / maximumDowntime) * 100
                                    )

                                    return (
                                        <div key={item.resourceId}>
                                            <div className="m3-maintenanceoverview-041">
                                                <div>
                                                    <p className="m3-maintenanceoverview-010">
                                                        {item.resourceName || `Resource #${item.resourceId}`}
                                                    </p>
                                                    <p className="m3-maintenanceoverview-026">
                                                        Resource ID: {item.resourceId}
                                                    </p>
                                                </div>
                                                <div className="m3-maintenanceoverview-042">
                                                    <p className="m3-maintenanceoverview-043">
                                                        {formatDuration(downtime)}
                                                    </p>
                                                    <p className="m3-maintenanceoverview-026">
                                                        {item.workOrderCount} work order(s)
                                                    </p>
                                                </div>
                                            </div>

                                            <div className="m3-maintenanceoverview-044">
                                                <div
                                                    className="m3-maintenanceoverview-045"
                                                    style={{ width: `${percentage}%` }}
                                                />
                                            </div>

                                            <div className="m3-maintenanceoverview-046">
                        <span>
                          Open-hours downtime: {formatDuration(openDowntime)}
                        </span>
                                                <span>
                          {percentage.toFixed(1)}% of highest equipment downtime
                        </span>
                                            </div>
                                        </div>
                                    )
                                })}
                            </div>
                        )}
                    </section>

                    <section className="m3-maintenanceoverview-001">
                        <div className="m3-maintenanceoverview-047">
                            <h2 className="m3-maintenanceoverview-037">
                                Equipment Downtime Details
                            </h2>
                            <p className="m3-maintenanceoverview-038">
                                Exact values returned by the maintenance analytics API.
                            </p>
                        </div>

                        {equipmentDetails.length === 0 ? (
                            <EmptyState />
                        ) : (
                            <div className="m3-maintenanceoverview-048">
                                <table className="m3-maintenanceoverview-049">
                                    <thead>
                                    <tr className="m3-maintenanceoverview-050">
                                        <th className="m3-maintenanceoverview-051">Equipment</th>
                                        <th className="m3-maintenanceoverview-051">Resource ID</th>
                                        <th className="m3-maintenanceoverview-051">Total Downtime</th>
                                        <th className="m3-maintenanceoverview-051">Open-Hours Downtime</th>
                                        <th className="m3-maintenanceoverview-051">Work Orders</th>
                                    </tr>
                                    </thead>
                                    <tbody className="m3-maintenanceoverview-052">
                                    {equipmentDetails.map(item => (
                                        <tr key={item.resourceId} className="m3-maintenanceoverview-053">
                                            <td className="m3-maintenanceoverview-054">
                                                {item.resourceName || '—'}
                                            </td>
                                            <td className="m3-maintenanceoverview-055">
                                                {item.resourceId ?? '—'}
                                            </td>
                                            <td className="m3-maintenanceoverview-056">
                                                {formatDuration(item.downtimeMinutes)}
                                            </td>
                                            <td className="m3-maintenanceoverview-055">
                                                {formatDuration(item.downtimeDuringOpenHoursMinutes)}
                                            </td>
                                            <td className="m3-maintenanceoverview-055">
                                                {item.workOrderCount ?? 0}
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </section>

                    <div className="m3-maintenanceoverview-057">
                        <div className="m3-maintenanceoverview-058">
                            <BarChart3 size={20} className="m3-maintenanceoverview-059" />
                            <div>
                                <p className="m3-maintenanceoverview-060">
                                    Analytics calculation
                                </p>
                                <p className="m3-maintenanceoverview-061">
                                    Open-hours loss is calculated by the backend using institution
                                    opening time, closing time and working days. This dashboard
                                    displays the returned percentage without recalculating it.
                                </p>
                            </div>
                        </div>
                    </div>
                </>
            ) : (
                <EmptyState />
            )}
        </div>
    )
}
