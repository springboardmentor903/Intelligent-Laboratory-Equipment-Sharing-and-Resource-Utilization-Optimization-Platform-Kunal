import { useCallback, useEffect, useMemo, useState } from 'react';
import {
    Activity,
    CalendarDays,
    CheckCircle2,
    Clock3,
    Download,
    RefreshCw,
    BarChart3,
    AlertCircle,
    Package,
} from 'lucide-react';
import api from '../api/client';
import './ResourceUtilizationReport.css';
import ResourceAnalytics from './ResourceAnalytics';

function getToday() {
    return new Date().toISOString().slice(0, 10);
}

function getMonthStart() {
    const date = new Date();
    date.setDate(1);
    return date.toISOString().slice(0, 10);
}

function formatHours(value) {
    const hours = Number(value || 0);
    return `${hours.toFixed(2)} hrs`;
}

export default function ResourceUtilizationReport() {
    const [fromDate, setFromDate] = useState(getMonthStart);
    const [toDate, setToDate] = useState(getToday);
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState('');

    const loadReport = useCallback(async (showLoader = true) => {
        if (!fromDate || !toDate) {
            setError('Please select both dates.');
            return;
        }

        if (fromDate > toDate) {
            setError('From date cannot be later than To date.');
            return;
        }

        try {
            if (showLoader) {
                setLoading(true);
            } else {
                setRefreshing(true);
            }

            setError('');

            const response = await api.get(
                '/reports/resource-utilization',
                {
                    params: {
                        fromDate,
                        toDate,
                    },
                }
            );

            setItems(Array.isArray(response.data) ? response.data : []);
        } catch (err) {
            setItems([]);
            setError(
                err.response?.data?.message ||
                err.response?.data?.error ||
                'Unable to load resource utilization report.'
            );
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }, [fromDate, toDate]);

    useEffect(() => {
        loadReport();
    }, [loadReport]);

    const summary = useMemo(() => {
        return items.reduce((total, item) => ({
            totalBookings:
                total.totalBookings + Number(item.totalBookings || 0),

            completedBookings:
                total.completedBookings + Number(item.completedBookings || 0),

            scheduledHours:
                total.scheduledHours + Number(item.totalBookedHours || 0),

            actualHours:
                total.actualHours + Number(item.actualUsageHours || 0),
        }), {
            totalBookings: 0,
            completedBookings: 0,
            scheduledHours: 0,
            actualHours: 0,
        });
    }, [items]);

    function exportCSV() {
        if (!items.length) return;

        const headers = [
            'Resource ID',
            'Resource Name',
            'Total Bookings',
            'Completed Bookings',
            'Scheduled Hours',
            'Actual Usage Hours',
        ];

        const rows = items.map((item) => [
            item.resourceId ?? '',
            item.resourceName ?? '',
            item.totalBookings ?? 0,
            item.completedBookings ?? 0,
            Number(item.totalBookedHours || 0).toFixed(2),
            Number(item.actualUsageHours || 0).toFixed(2),
        ]);

        const csv = [headers, ...rows]
            .map((row) =>
                row.map((value) =>
                    `"${String(value).replaceAll('"', '""')}"`
                ).join(',')
            )
            .join('\n');

        const blob = new Blob([csv], {
            type: 'text/csv;charset=utf-8;',
        });

        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');

        link.href = url;
        link.download = `resource-utilization-${fromDate}-to-${toDate}.csv`;
        link.click();

        URL.revokeObjectURL(url);
    }

    const maxHours = Math.max(
        1,
        ...items.map((item) =>
            Math.max(
                Number(item.totalBookedHours || 0),
                Number(item.actualUsageHours || 0)
            )
        )
    );

    return (
        <div className="utilization-page">

            <section className="utilization-intro">
                <div className="utilization-intro-icon">
                    <BarChart3 size={25} />
                </div>

                <div className="utilization-intro-content">
                    <h2>Resource Utilization Overview</h2>
                    <p>
                        Analyze equipment bookings and actual usage
                        during the selected period.
                    </p>
                </div>

                <button
                    className="utilization-refresh"
                    onClick={() => loadReport(false)}
                    disabled={loading || refreshing}
                >
                    <RefreshCw
                        size={16}
                        className={refreshing ? 'utilization-spin' : ''}
                    />
                    Refresh
                </button>
            </section>

            <section className="utilization-filter panel">
                <div className="utilization-filter-heading">
                    <div>
                        <h3>Report Filters</h3>
                        <p>Select a date range to view report data.</p>
                    </div>
                    <CalendarDays size={21} />
                </div>

                <div className="utilization-filter-controls">
                    <div className="utilization-field">
                        <label htmlFor="report-from-date">From date</label>
                        <input
                            id="report-from-date"
                            type="date"
                            value={fromDate}
                            max={toDate || getToday()}
                            onChange={(e) => setFromDate(e.target.value)}
                        />
                    </div>

                    <div className="utilization-field">
                        <label htmlFor="report-to-date">To date</label>
                        <input
                            id="report-to-date"
                            type="date"
                            value={toDate}
                            min={fromDate || undefined}
                            max={getToday()}
                            onChange={(e) => setToDate(e.target.value)}
                        />
                    </div>

                    <button
                        className="utilization-apply"
                        onClick={() => loadReport()}
                        disabled={loading || refreshing || !fromDate || !toDate}
                    >
                        Apply Filters
                    </button>

                    <button
                        className="utilization-export"
                        onClick={exportCSV}
                        disabled={!items.length}
                    >
                        <Download size={16} />
                        Export CSV
                    </button>
                </div>
            </section>

            {error && (
                <div className="utilization-error" role="alert">
                    <AlertCircle size={19} />
                    <span>{error}</span>
                </div>
            )}

            <section className="utilization-stat-grid">
                <article className="utilization-stat-card">
                    <div className="utilization-stat-icon blue">
                        <CalendarDays size={21} />
                    </div>
                    <span>Total Bookings</span>
                    <strong>{loading ? '—' : summary.totalBookings}</strong>
                    <small>Bookings in selected period</small>
                </article>

                <article className="utilization-stat-card">
                    <div className="utilization-stat-icon green">
                        <CheckCircle2 size={21} />
                    </div>
                    <span>Completed Bookings</span>
                    <strong>{loading ? '—' : summary.completedBookings}</strong>
                    <small>Successfully completed</small>
                </article>

                <article className="utilization-stat-card">
                    <div className="utilization-stat-icon purple">
                        <Clock3 size={21} />
                    </div>
                    <span>Scheduled Hours</span>
                    <strong>
                        {loading ? '—' : formatHours(summary.scheduledHours)}
                    </strong>
                    <small>Based on booking schedule</small>
                </article>

                <article className="utilization-stat-card">
                    <div className="utilization-stat-icon orange">
                        <Activity size={21} />
                    </div>
                    <span>Actual Usage Hours</span>
                    <strong>
                        {loading ? '—' : formatHours(summary.actualHours)}
                    </strong>
                    <small>Based on check-in / check-out</small>
                </article>
            </section>

            <section className="panel utilization-chart-panel">
                <div className="utilization-section-heading">
                    <div>
                        <h3>Equipment Usage Comparison</h3>
                        <p>
                            Scheduled hours compared with actual recorded usage.
                        </p>
                    </div>
                    <BarChart3 size={21} />
                </div>

                {loading ? (
                    <div className="utilization-empty">
                        Loading report data...
                    </div>
                ) : items.length ? (
                    <div className="utilization-bars">
                        {items.map((item, index) => {
                            const scheduled = Number(item.totalBookedHours || 0);
                            const actual = Number(item.actualUsageHours || 0);

                            return (
                                <div
                                    className="utilization-bar-row"
                                    key={item.resourceId ?? index}
                                >
                                    <div className="utilization-bar-title">
                                        <strong>
                                            {item.resourceName || `Resource #${item.resourceId}`}
                                        </strong>
                                        <span>
                                            Actual: {formatHours(actual)}
                                            {' · '}
                                            Scheduled: {formatHours(scheduled)}
                                        </span>
                                    </div>

                                    <div className="utilization-bar-track">
                                        <div
                                            className="utilization-bar scheduled"
                                            style={{
                                                width: `${Math.min(
                                                    100,
                                                    (scheduled / maxHours) * 100
                                                )}%`,
                                            }}
                                        />
                                    </div>

                                    <div className="utilization-bar-track actual-track">
                                        <div
                                            className="utilization-bar actual"
                                            style={{
                                                width: `${Math.min(
                                                    100,
                                                    (actual / maxHours) * 100
                                                )}%`,
                                            }}
                                        />
                                    </div>
                                </div>
                            );
                        })}

                        <div className="utilization-legend">
                            <span><i className="scheduled-dot" /> Scheduled hours</span>
                            <span><i className="actual-dot" /> Actual usage hours</span>
                        </div>
                    </div>
                ) : (
                    <div className="utilization-empty">
                        <Package size={30} />
                        <strong>No report data available</strong>
                        <span>
                            No equipment booking records were found for this date range.
                        </span>
                    </div>
                )}
            </section>

            <section className="panel utilization-table-panel">
                <div className="utilization-section-heading">
                    <div>
                        <h3>Resource-wise Report</h3>
                        <p>
                            Detailed booking and usage information for each resource.
                        </p>
                    </div>
                    <Package size={21} />
                </div>

                {loading ? (
                    <div className="utilization-empty">
                        Loading resource details...
                    </div>
                ) : items.length ? (
                    <div className="utilization-table-scroll">
                        <table className="utilization-table">
                            <thead>
                            <tr>
                                <th>Resource</th>
                                <th>Total Bookings</th>
                                <th>Completed</th>
                                <th>Scheduled Hours</th>
                                <th>Actual Usage</th>
                            </tr>
                            </thead>

                            <tbody>
                            {items.map((item, index) => (
                                <tr key={item.resourceId ?? index}>
                                    <td>
                                        <div className="utilization-resource-name">
                                                <span className="utilization-resource-icon">
                                                    <Package size={17} />
                                                </span>
                                            <div>
                                                <strong>
                                                    {item.resourceName || 'Unnamed resource'}
                                                </strong>
                                                <small>
                                                    ID: {item.resourceId ?? '—'}
                                                </small>
                                            </div>
                                        </div>
                                    </td>

                                    <td>{item.totalBookings ?? 0}</td>
                                    <td>
                                            <span className="utilization-completed">
                                                {item.completedBookings ?? 0}
                                            </span>
                                    </td>
                                    <td>{formatHours(item.totalBookedHours)}</td>
                                    <td>
                                        <strong>
                                            {formatHours(item.actualUsageHours)}
                                        </strong>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <div className="utilization-empty">
                        No resource records to display.
                    </div>
                )}
            </section>
            <ResourceAnalytics />
        </div>
    );
}