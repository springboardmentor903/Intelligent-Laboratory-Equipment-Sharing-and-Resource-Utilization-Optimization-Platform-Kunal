
import { useCallback, useEffect, useState } from 'react'
import { RefreshCw, Wallet, ReceiptText } from 'lucide-react'
import api from '../../api/client'
import '../../styles/T10.css'

const money = value => Number(value || 0).toLocaleString('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
})

const dateTime = value => value ? new Date(value).toLocaleString('en-IN') : '—'

export default function MyUsageCharges() {
    const [data, setData] = useState({
        charges: [],
        totalElements: 0,
        totalPages: 0,
        currentPage: 0,
        totalAmount: 0
    })
    const [filters, setFilters] = useState({
        chargeType: '',
        status: '',
        fromDate: '',
        toDate: ''
    })
    const [page, setPage] = useState(0)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    const load = useCallback(async (nextPage = page) => {
        setLoading(true)
        setError('')

        try {
            const params = {
                page: nextPage,
                size: 10,
                chargeType: filters.chargeType || undefined,
                status: filters.status || undefined,
                fromDate: filters.fromDate || undefined,
                toDate: filters.toDate || undefined
            }

            const response = await api.get('/usage-charges/my', { params })
            const result = response.data || {}

            setData({
                charges: result.charges || [],
                totalElements: result.totalElements || 0,
                totalPages: result.totalPages || 0,
                currentPage: result.currentPage || 0,
                totalAmount: result.totalAmount || 0
            })
            setPage(result.currentPage || 0)
        } catch (e) {
            setError(e.response?.data?.message || 'Usage charges could not be loaded.')
        } finally {
            setLoading(false)
        }
    }, [page, filters])

    useEffect(() => {
        load(0)
    }, [filters, load])

    const updateFilter = (key, value) => {
        setFilters(prev => ({ ...prev, [key]: value }))
        setPage(0)
    }

    return (
        <div className="t10-page">
            <div className="t10-header">
                <div>
                    <h1>My Usage Charges</h1>
                    <p>Review your equipment usage charges and billing records.</p>
                </div>
                <button className="t10-btn secondary" onClick={() => load(page)} disabled={loading}>
                    <RefreshCw size={16} /> Refresh
                </button>
            </div>

            <div className="t10-stat-grid">
                <div className="t10-stat-card">
                    <div className="t10-stat-icon"><Wallet size={20} /></div>
                    <span>Total Charges</span>
                    <strong>{money(data.totalAmount)}</strong>
                </div>
                <div className="t10-stat-card">
                    <div className="t10-stat-icon"><ReceiptText size={20} /></div>
                    <span>Charge Records</span>
                    <strong>{data.totalElements}</strong>
                </div>
            </div>

            <section className="t10-panel">
                <div className="t10-panel-heading">
                    <div>
                        <h2>Charge History</h2>
                        <p>Filter your charges by type, status and date.</p>
                    </div>
                </div>

                <div className="t10-filters">
                    <select value={filters.chargeType} onChange={e => updateFilter('chargeType', e.target.value)}>
                        <option value="">All charge types</option>
                        <option value="INTERNAL">Internal</option>
                        <option value="EXTERNAL">External</option>
                        <option value="UNASSIGNED">Unassigned</option>
                    </select>

                    <select value={filters.status} onChange={e => updateFilter('status', e.target.value)}>
                        <option value="">All statuses</option>
                        <option value="PENDING">Pending</option>
                        <option value="INVOICED">Invoiced</option>
                        <option value="PAID">Paid</option>
                    </select>

                    <label>
                        From
                        <input type="date" value={filters.fromDate}
                               onChange={e => updateFilter('fromDate', e.target.value)} />
                    </label>

                    <label>
                        To
                        <input type="date" value={filters.toDate}
                               onChange={e => updateFilter('toDate', e.target.value)} />
                    </label>

                    <button className="t10-btn secondary"
                            onClick={() => setFilters({ chargeType: '', status: '', fromDate: '', toDate: '' })}>
                        Clear
                    </button>
                </div>

                {error && <div className="t10-error">{error}</div>}

                <div className="t10-table-wrap">
                    <table className="t10-table">
                        <thead>
                        <tr>
                            <th>Equipment</th>
                            <th>Booking ID</th>
                            <th>Usage</th>
                            <th>Hourly Rate</th>
                            <th>Amount</th>
                            <th>Type</th>
                            <th>Status</th>
                            <th>Created</th>
                        </tr>
                        </thead>
                        <tbody>
                        {loading ? (
                            <tr><td colSpan="8" className="t10-empty">Loading charges...</td></tr>
                        ) : data.charges.length === 0 ? (
                            <tr><td colSpan="8" className="t10-empty">No usage charges found.</td></tr>
                        ) : data.charges.map(charge => (
                            <tr key={charge.id}>
                                <td><strong>{charge.equipmentName || `Resource #${charge.resourceId}`}</strong></td>
                                <td>#{charge.bookingId}</td>
                                <td>{charge.usageMinutes ?? '—'} min</td>
                                <td>{money(charge.hourlyRate)}</td>
                                <td><strong>{money(charge.amount)}</strong></td>
                                <td><span className={`t10-type ${String(charge.chargeType).toLowerCase()}`}>{charge.chargeType}</span></td>
                                <td><span className={`t10-status ${String(charge.status).toLowerCase()}`}>{charge.status}</span></td>
                                <td>{dateTime(charge.createdAt)}</td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>

                <div className="t10-pagination">
                    <span>{data.totalElements} records</span>
                    <div>
                        <button className="t10-btn secondary" disabled={loading || page <= 0}
                                onClick={() => load(page - 1)}>Previous</button>
                        <span>Page {page + 1} of {Math.max(data.totalPages, 1)}</span>
                        <button className="t10-btn secondary" disabled={loading || page + 1 >= data.totalPages}
                                onClick={() => load(page + 1)}>Next</button>
                    </div>
                </div>
            </section>
        </div>
    )
}
