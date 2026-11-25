import { useCallback, useEffect, useMemo, useState } from 'react'
import api from '../../api/client'
import '../../styles/T9.css'

const count = value => Number(value || 0)

const formatDate = value => {
    if (!value) return 'No upcoming task'

    const date = new Date(value)
    return Number.isNaN(date.getTime())
        ? 'No upcoming task'
        : date.toLocaleString('en-IN')
}

export default function Technicians() {
    const [technicians, setTechnicians] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [keyword, setKeyword] = useState('')
    const [sort, setSort] = useState('active-desc')

    const load = useCallback(async () => {
        setLoading(true)
        setError('')

        try {
            const response = await api.get('/technicians/workload')
            setTechnicians(Array.isArray(response.data) ? response.data : [])
        } catch (e) {
            setError(
                e.response?.data?.message ||
                e.response?.data?.error ||
                'Technician workload could not be loaded.'
            )
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => {
        load()
    }, [load])

    const filtered = useMemo(() => {
        const query = keyword.trim().toLowerCase()

        const result = technicians.filter(technician =>
            (technician.technicianName || '').toLowerCase().includes(query) ||
            (technician.email || '').toLowerCase().includes(query)
        )

        return [...result].sort((a, b) => {
            if (sort === 'active-asc') return count(a.activeTasks) - count(b.activeTasks)
            if (sort === 'overdue-desc') return count(b.overdueTasks) - count(a.overdueTasks)
            if (sort === 'name-asc') {
                return (a.technicianName || '').localeCompare(b.technicianName || '')
            }
            return count(b.activeTasks) - count(a.activeTasks)
        })
    }, [technicians, keyword, sort])

    const totals = useMemo(() => ({
        active: technicians.reduce((sum, item) => sum + count(item.activeTasks), 0),
        progress: technicians.reduce((sum, item) => sum + count(item.inProgressTasks), 0),
        overdue: technicians.reduce((sum, item) => sum + count(item.overdueTasks), 0)
    }), [technicians])

    return (
        <div className="t9-page">
            <div className="t9-header">
                <div>
                    <h1>Technician Workload</h1>
                    <p>Monitor technician assignments, active work and overdue tasks.</p>
                </div>

                <button
                    className="t9-btn secondary"
                    onClick={load}
                    disabled={loading}
                >
                    Refresh
                </button>
            </div>

            <div className="t9-stat-grid">
                <div className="t9-card t9-stat">
                    <span>Total Technicians</span>
                    <strong>{technicians.length}</strong>
                </div>
                <div className="t9-card t9-stat">
                    <span>Active Tasks</span>
                    <strong>{totals.active}</strong>
                </div>
                <div className="t9-card t9-stat">
                    <span>In Progress</span>
                    <strong>{totals.progress}</strong>
                </div>
                <div className="t9-card t9-stat">
                    <span>Overdue Tasks</span>
                    <strong>{totals.overdue}</strong>
                </div>
            </div>

            <div className="t9-card t9-filters">
                <input
                    className="t9-input"
                    placeholder="Search technician name or email"
                    value={keyword}
                    onChange={e => setKeyword(e.target.value)}
                />

                <select
                    className="t9-select"
                    value={sort}
                    onChange={e => setSort(e.target.value)}
                >
                    <option value="active-desc">Most active tasks</option>
                    <option value="active-asc">Least active tasks</option>
                    <option value="overdue-desc">Most overdue tasks</option>
                    <option value="name-asc">Name A–Z</option>
                </select>

                <span className="t9-muted">{filtered.length} technician(s)</span>
            </div>

            {error && <div className="t9-error">{error}</div>}

            <div className="t9-card t9-table-wrap">
                <table className="t9-table">
                    <thead>
                    <tr>
                        <th>Technician</th>
                        <th>Active Tasks</th>
                        <th>In Progress</th>
                        <th>Overdue</th>
                        <th>Next Scheduled Date</th>
                    </tr>
                    </thead>

                    <tbody>
                    {filtered.map(technician => (
                        <tr key={technician.technicianId}>
                            <td>
                                <strong>{technician.technicianName || 'Technician'}</strong>
                                <div className="t9-muted">{technician.email || '—'}</div>
                            </td>
                            <td>
                                <span className="t9-pill">{count(technician.activeTasks)}</span>
                            </td>
                            <td>{count(technician.inProgressTasks)}</td>
                            <td>
                                    <span className={`t9-pill ${
                                        count(technician.overdueTasks) > 0 ? 'EXPIRED' : 'VALID'
                                    }`}>
                                        {count(technician.overdueTasks)}
                                    </span>
                            </td>
                            <td>{formatDate(technician.nextScheduledDate)}</td>
                        </tr>
                    ))}

                    {!loading && filtered.length === 0 && (
                        <tr>
                            <td colSpan="5" className="t9-empty">
                                No technicians found.
                            </td>
                        </tr>
                    )}
                    </tbody>
                </table>

                {loading && (
                    <div className="t9-empty">Loading technician workload...</div>
                )}
            </div>
        </div>
    )
}
