
import { useCallback, useEffect, useState } from 'react'
import { RefreshCw, TrendingUp, TrendingDown, Clock3, FileText } from 'lucide-react'
import api from '../../api/client'
import '../../styles/T10.css'

const money = value => Number(value || 0).toLocaleString('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
})

const year = new Date().getFullYear()

export default function FinancialSummary() {
    const [period, setPeriod] = useState({
        startDate: `${year}-01-01`,
        endDate: `${year}-12-31`
    })
    const [summary, setSummary] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    const load = useCallback(async () => {
        setLoading(true)
        setError('')
        try {
            const response = await api.get('/financial-summary', { params: period })
            setSummary(response.data)
        } catch (e) {
            setError(e.response?.data?.message || 'Financial summary could not be loaded.')
        } finally {
            setLoading(false)
        }
    }, [period])

    useEffect(() => {
        load()
    }, [load])

    const Card = ({ title, value, icon: Icon, description }) => (
        <div className="t10-stat-card">
            <div className="t10-stat-icon"><Icon size={20} /></div>
            <span>{title}</span>
            <strong>{money(value)}</strong>
            <small>{description}</small>
        </div>
    )

    return (
        <div className="t10-page">
            <div className="t10-header">
                <div>
                    <h1>Financial Summary</h1>
                    <p>Track institution billing, collections and outstanding balances.</p>
                </div>
                <button className="t10-btn secondary" onClick={load} disabled={loading}>
                    <RefreshCw size={16} /> Refresh
                </button>
            </div>

            <section className="t10-panel">
                <div className="t10-filters">
                    <label>From
                        <input type="date" value={period.startDate}
                               onChange={e => setPeriod(p => ({ ...p, startDate: e.target.value }))} />
                    </label>
                    <label>To
                        <input type="date" value={period.endDate}
                               onChange={e => setPeriod(p => ({ ...p, endDate: e.target.value }))} />
                    </label>
                    <button className="t10-btn primary" onClick={load} disabled={loading}>Apply Period</button>
                </div>

                {error && <div className="t10-error">{error}</div>}

                {loading ? (
                    <div className="t10-empty">Loading financial summary...</div>
                ) : summary ? (
                    <>
                        <h2 className="t10-section-title">Issued Invoices</h2>
                        <div className="t10-stat-grid">
                            <Card title="Invoiced" value={summary.issuedInvoiced} icon={FileText} description="Total issued amount" />
                            <Card title="Paid" value={summary.issuedPaid} icon={TrendingUp} description="Amount collected" />
                            <Card title="Outstanding" value={summary.issuedOutstanding} icon={Clock3} description="Awaiting payment" />
                            <Card title="Overdue" value={summary.issuedOverdue} icon={TrendingDown} description="Past due date" />
                        </div>

                        <h2 className="t10-section-title">Received Invoices</h2>
                        <div className="t10-stat-grid">
                            <Card title="Invoiced" value={summary.receivedInvoiced} icon={FileText} description="Total received amount" />
                            <Card title="Paid" value={summary.receivedPaid} icon={TrendingUp} description="Amount paid" />
                            <Card title="Outstanding" value={summary.receivedOutstanding} icon={Clock3} description="Awaiting payment" />
                            <Card title="Overdue" value={summary.receivedOverdue} icon={TrendingDown} description="Past due date" />
                        </div>

                        <h2 className="t10-section-title">Pending Charges</h2>
                        <div className="t10-stat-grid">
                            <Card title="Uninvoiced Charges" value={summary.pendingUninvoicedCharges} icon={FileText} description="Pending eligible usage charges" />
                        </div>
                    </>
                ) : (
                    <div className="t10-empty">No financial summary available.</div>
                )}
            </section>
        </div>
    )
}
