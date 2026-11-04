
import { useEffect, useState } from 'react';
import api from '../api/client';
import './ResourceAnalytics.css';

export default function ResourceAnalytics() {
    const today = new Date();
    const formatDate = (date) => {
        const y = date.getFullYear();
        const m = String(date.getMonth() + 1).padStart(2, '0');
        const d = String(date.getDate()).padStart(2, '0');
        return `${y}-${m}-${d}`;
    };

    const [fromDate, setFromDate] = useState(() => {
        const d = new Date();
        d.setDate(d.getDate() - 6);
        return formatDate(d);
    });
    const [toDate, setToDate] = useState(formatDate(today));
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    async function loadAnalytics() {
        try {
            setLoading(true);
            setError('');
            const response = await api.get('/reports/analytics', {
                params: { fromDate, toDate }
            });
            setData(response.data);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                'Analytics data load nahi ho paya.'
            );
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadAnalytics();
    }, []);

    const resources = data?.resources || [];
    const peakHours = data?.peakHours || [];
    const heatmap = data?.heatmap || [];

    const maxUsage = Math.max(
        1,
        ...peakHours.map(x => Number(x.usageHours || 0))
    );

    const getHeatValue = (day, hour) => {
        const cell = heatmap.find(
            x => Number(x.hour) === hour &&
                (x.date === day || x.day === day)
        );
        return Number(cell?.usageHours || 0);
    };

    const days = [...new Set(
        heatmap.map(x => x.date || x.day).filter(Boolean)
    )];

    const format = (value) => Number(value || 0).toFixed(2);

    return (
        <section className="analytics-section">
            <div className="analytics-heading">
                <div>
                    <h2>Resource Analytics</h2>
                    <p>Actual utilization, idle time and equipment usage patterns.</p>
                </div>
                <button onClick={loadAnalytics} disabled={loading}>
                    {loading ? 'Loading...' : 'Refresh Analytics'}
                </button>
            </div>

            <div className="analytics-filters">
                <label>
                    From date
                    <input type="date" value={fromDate}
                           onChange={e => setFromDate(e.target.value)} />
                </label>
                <label>
                    To date
                    <input type="date" value={toDate}
                           onChange={e => setToDate(e.target.value)} />
                </label>
                <button onClick={loadAnalytics} disabled={loading}>
                    Apply Filter
                </button>
            </div>

            {error && <p className="analytics-error">{error}</p>}

            {loading && !data && <p>Loading analytics...</p>}

            {data && (
                <>
                    <div className="analytics-cards">
                        <div className="analytics-card">
                            <span>Total Capacity</span>
                            <strong>{format(data.totalCapacityHours)} hrs</strong>
                        </div>
                        <div className="analytics-card">
                            <span>Actual Usage</span>
                            <strong>{format(data.totalActualUsageHours)} hrs</strong>
                        </div>
                        <div className="analytics-card">
                            <span>Total Idle Time</span>
                            <strong>{format(data.totalIdleHours)} hrs</strong>
                        </div>
                        <div className="analytics-card">
                            <span>Utilization Rate</span>
                            <strong>{format(data.utilizationPercentage)}%</strong>
                        </div>
                    </div>

                    <div className="analytics-panel">
                        <h3>Resource-wise Idle Time</h3>
                        {resources.map((item, index) => (
                            <div className="idle-row" key={item.resourceId ?? index}>
                                <div className="idle-info">
                                    <strong>{item.resourceName || `Resource ${index + 1}`}</strong>
                                    <span>{format(item.idleHours)} idle hrs</span>
                                </div>
                                <div className="idle-track">
                                    <div style={{
                                        width: `${Math.min(100,
                                            Number(item.capacityHours || item.totalCapacityHours)
                                                ? (Number(item.idleHours || 0) /
                                                Number(item.capacityHours || item.totalCapacityHours)) * 100
                                                : 0)}%`
                                    }} />
                                </div>
                            </div>
                        ))}
                    </div>

                    <div className="analytics-panel">
                        <h3>Peak Usage Hours (24-hour)</h3>
                        <div className="peak-chart">
                            {peakHours.slice().sort((a, b) => a.hour - b.hour).map(item => (
                                <div className="peak-column" key={item.hour}>
                                    <div className="peak-bar-area">
                                        <div
                                            className="peak-bar"
                                            style={{ height: `${Number(item.usageHours || 0) / maxUsage * 100}%` }}
                                            title={`${item.label}: ${format(item.usageHours)} hrs`}
                                        />
                                    </div>
                                    <small>{String(item.hour).padStart(2, '0')}</small>
                                </div>
                            ))}
                        </div>
                        <p className="analytics-note">
                            Highest usage hours are highlighted by bar height.
                        </p>
                    </div>

                    <div className="analytics-panel">
                        <h3>Usage Heatmap</h3>
                        {days.length > 0 ? (
                            <div className="heatmap-scroll">
                                <table className="heatmap-table">
                                    <thead>
                                    <tr>
                                        <th>Date</th>
                                        {Array.from({ length: 24 }, (_, h) => (
                                            <th key={h}>{String(h).padStart(2, '0')}</th>
                                        ))}
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {days.map(day => (
                                        <tr key={day}>
                                            <th>{day}</th>
                                            {Array.from({ length: 24 }, (_, h) => {
                                                const value = getHeatValue(day, h);
                                                return (
                                                    <td key={h}
                                                        title={`${day} ${h}:00 — ${format(value)} hrs`}
                                                        className={value > 0 ? 'heat-active' : ''}
                                                    >
                                                        {value > 0 ? format(value) : ''}
                                                    </td>
                                                );
                                            })}
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <p className="analytics-note">
                                Heatmap data is not available for this date range.
                            </p>
                        )}
                    </div>
                </>
            )}
        </section>
    );
}
