
import { useCallback, useEffect, useState } from 'react';
import {
    BarChart3,
    Building2,
    CalendarCheck,
    IndianRupee,
    RefreshCw,
    Clock,
    AlertCircle,
} from 'lucide-react';
import api from '../api/client';
import './SharingAnalytics.css';

function formatCurrency(value) {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2,
    }).format(Number(value || 0));
}

function formatNumber(value) {
    return Number(value || 0).toLocaleString('en-IN', {
        maximumFractionDigits: 2,
    });
}

function PartnerChart({ partners = [], field, label, color }) {
    const max = Math.max(
        1,
        ...partners.map((partner) => Number(partner[field] || 0))
    );

    if (!partners.length) {
        return <p className="analytics-empty">No partner data available.</p>;
    }

    return (
        <div className="analytics-chart">
            {partners.map((partner, index) => {
                const value = Number(partner[field] || 0);
                const width = (value / max) * 100;

                return (
                    <div className="analytics-bar-row" key={partner.institutionId ?? index}>
                        <div className="analytics-bar-label">
                            {partner.institutionName || `Institution ${partner.institutionId}`}
                        </div>

                        <div className="analytics-bar-track">
                            <div
                                className="analytics-bar-fill"
                                style={{
                                    width: `${width}%`,
                                    background: color,
                                }}
                            />
                        </div>

                        <strong>
                            {field === 'feesEarned'
                                ? formatCurrency(value)
                                : formatNumber(value)}
                        </strong>
                    </div>
                );
            })}
            <p className="analytics-chart-caption">{label}</p>
        </div>
    );
}

export default function SharingAnalytics() {
    const today = new Date();
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(today.getDate() - 29);

    const toISO = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    const [fromDate, setFromDate] = useState(toISO(thirtyDaysAgo));
    const [toDate, setToDate] = useState(toISO(today));
    const [analytics, setAnalytics] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const loadAnalytics = useCallback(async () => {
        setLoading(true);
        setError('');

        try {
            const response = await api.get('/sharing-analytics', {
                params: { fromDate, toDate },
            });

            setAnalytics(response.data);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                err.response?.data?.error ||
                err.message ||
                'Unable to load sharing analytics.'
            );
        } finally {
            setLoading(false);
        }
    }, [fromDate, toDate]);

    useEffect(() => {
        loadAnalytics();
    }, [loadAnalytics]);

    const partners = analytics?.partners || [];

    return (
        <section className="sharing-analytics-page">
            <div className="analytics-toolbar">
                <div>
                    <h2>Partnership Overview</h2>
                    <p>Monitor institutional resource sharing and revenue.</p>
                </div>

                <button
                    className="analytics-refresh"
                    onClick={loadAnalytics}
                    disabled={loading}
                >
                    <RefreshCw size={16} />
                    {loading ? 'Loading...' : 'Refresh'}
                </button>
            </div>

            <div className="analytics-date-filter">
                <label>
                    From Date
                    <input
                        type="date"
                        value={fromDate}
                        max={toDate}
                        onChange={(e) => setFromDate(e.target.value)}
                    />
                </label>

                <label>
                    To Date
                    <input
                        type="date"
                        value={toDate}
                        min={fromDate}
                        onChange={(e) => setToDate(e.target.value)}
                    />
                </label>

                <button
                    onClick={loadAnalytics}
                    disabled={loading || !fromDate || !toDate}
                >
                    Apply Filter
                </button>
            </div>

            {error && (
                <div className="analytics-error">
                    <AlertCircle size={18} />
                    {error}
                </div>
            )}

            {loading && !analytics ? (
                <div className="analytics-loading">Loading sharing analytics...</div>
            ) : (
                <>
                    <div className="analytics-kpi-grid">
                        <article className="analytics-kpi-card">
                            <div className="analytics-kpi-icon blue">
                                <CalendarCheck size={23} />
                            </div>
                            <p>Approved Bookings</p>
                            <h3>{formatNumber(analytics?.totalApprovedBookings)}</h3>
                            <span>Successfully approved bookings</span>
                        </article>

                        <article className="analytics-kpi-card">
                            <div className="analytics-kpi-icon purple">
                                <Clock size={23} />
                            </div>
                            <p>Total Hours Shared</p>
                            <h3>{formatNumber(analytics?.totalHoursShared)}</h3>
                            <span>Hours of resource utilization</span>
                        </article>

                        <article className="analytics-kpi-card">
                            <div className="analytics-kpi-icon green">
                                <IndianRupee size={23} />
                            </div>
                            <p>Total Fees Earned</p>
                            <h3>{formatCurrency(analytics?.totalFeesEarned)}</h3>
                            <span>Revenue from approved bookings</span>
                        </article>

                        <article className="analytics-kpi-card">
                            <div className="analytics-kpi-icon orange">
                                <Building2 size={23} />
                            </div>
                            <p>Partner Institutions</p>
                            <h3>{formatNumber(analytics?.partnerInstitutionCount)}</h3>
                            <span>Institutions in sharing partnerships</span>
                        </article>
                    </div>

                    <div className="analytics-section-heading">
                        <div>
                            <h2>Partnership Reports</h2>
                            <p>Performance breakdown by partner institution.</p>
                        </div>
                    </div>

                    <div className="analytics-chart-grid">
                        <article className="analytics-panel">
                            <h3>
                                <BarChart3 size={19} />
                                Approved Bookings
                            </h3>
                            <PartnerChart
                                partners={partners}
                                field="approvedBookings"
                                label="Number of approved bookings"
                                color="#3978dc"
                            />
                        </article>

                        <article className="analytics-panel">
                            <h3>
                                <Clock size={19} />
                                Shared Hours
                            </h3>
                            <PartnerChart
                                partners={partners}
                                field="hoursShared"
                                label="Total hours shared"
                                color="#8056cf"
                            />
                        </article>

                        <article className="analytics-panel">
                            <h3>
                                <IndianRupee size={19} />
                                Fees Earned
                            </h3>
                            <PartnerChart
                                partners={partners}
                                field="feesEarned"
                                label="Revenue earned from partners"
                                color="#19976b"
                            />
                        </article>
                    </div>

                    <article className="analytics-panel analytics-partner-table">
                        <h3>Partner Institution Details</h3>

                        {partners.length ? (
                            <div className="analytics-table-scroll">
                                <table>
                                    <thead>
                                    <tr>
                                        <th>Institution</th>
                                        <th>Approved Bookings</th>
                                        <th>Hours Shared</th>
                                        <th>Fees Earned</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {partners.map((partner, index) => (
                                        <tr key={partner.institutionId ?? index}>
                                            <td>
                                                <strong>
                                                    {partner.institutionName ||
                                                        `Institution ${partner.institutionId}`}
                                                </strong>
                                            </td>
                                            <td>{formatNumber(partner.approvedBookings)}</td>
                                            <td>{formatNumber(partner.hoursShared)} hrs</td>
                                            <td>{formatCurrency(partner.feesEarned)}</td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <p className="analytics-empty">
                                No partnership records found for this period.
                            </p>
                        )}
                    </article>
                </>
            )}
        </section>
    );
}
