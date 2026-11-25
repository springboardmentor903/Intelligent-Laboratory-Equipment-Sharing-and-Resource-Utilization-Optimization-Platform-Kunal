
import { useEffect, useState } from 'react'
import api from '../../api/client'
import '../../styles/T9.css'

const formatDate = value => {
    if (!value) return '—'

    return new Date(`${value}T00:00:00`).toLocaleDateString('en-IN')
}

export default function CalibrationOverview() {
    const [rows, setRows] = useState([])
    const [departments, setDepartments] = useState([])

    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    const [keyword, setKeyword] = useState('')
    const [state, setState] = useState('')
    const [departmentId, setDepartmentId] = useState('')
    const [sort, setSort] = useState('name,asc')

    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [total, setTotal] = useState(0)

    const load = async (nextPage = 0, searchKeyword = keyword) => {
        setLoading(true)
        setError('')

        try {
            const response = await api.get('/calibrations/overview', {
                params: {
                    page: nextPage,
                    size: 10,
                    sort,
                    keyword: searchKeyword.trim() || undefined,
                    state: state || undefined,
                    departmentId: departmentId || undefined
                }
            })

            const data = response.data

            setRows(data?.content || [])
            setPage(data?.number ?? 0)
            setTotalPages(data?.totalPages ?? 0)
            setTotal(data?.totalElements ?? 0)
        } catch (e) {
            setError(
                e.response?.data?.message ||
                e.response?.data?.error ||
                'Calibration overview could not be loaded.'
            )
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        api.get('/departments')
            .then(response => {
                const data = response.data
                setDepartments(
                    Array.isArray(data) ? data : data?.content || []
                )
            })
            .catch(() => setDepartments([]))
    }, [])

    useEffect(() => {
        load(0)
    }, [state, departmentId, sort])

    const handleSearch = event => {
        event.preventDefault()
        load(0, keyword)
    }

    const clearFilters = () => {
        setKeyword('')
        setState('')
        setDepartmentId('')
        setSort('name,asc')
    }

    return (
        <div className="t9-page">
            <div className="t9-header">
                <div>
                    <h1>Calibration Overview</h1>
                    <p>
                        Monitor equipment calibration status, latest dates
                        and upcoming renewals.
                    </p>
                </div>

                <button
                    className="t9-btn secondary"
                    onClick={() => load(page)}
                    disabled={loading}
                >
                    Refresh
                </button>
            </div>

            <div className="t9-stat-grid">
                <div className="t9-card t9-stat">
                    <span>Total Equipment</span>
                    <strong>{total}</strong>
                </div>

                <div className="t9-card t9-stat">
                    <span>Valid</span>
                    <strong>
                        {rows.filter(r => r.calibrationState === 'VALID').length}
                    </strong>
                </div>

                <div className="t9-card t9-stat">
                    <span>Due Soon</span>
                    <strong>
                        {rows.filter(r => r.calibrationState === 'DUE_SOON').length}
                    </strong>
                </div>

                <div className="t9-card t9-stat">
                    <span>Expired</span>
                    <strong>
                        {rows.filter(r => r.calibrationState === 'EXPIRED').length}
                    </strong>
                </div>

                <div className="t9-card t9-stat">
                    <span>No Calibration</span>
                    <strong>
                        {rows.filter(r => r.calibrationState === 'NONE').length}
                    </strong>
                </div>
            </div>

            <form className="t9-card t9-filters" onSubmit={handleSearch}>
                <input
                    className="t9-input"
                    placeholder="Search equipment or department..."
                    value={keyword}
                    onChange={e => setKeyword(e.target.value)}
                />

                <select
                    className="t9-select"
                    value={state}
                    onChange={e => setState(e.target.value)}
                >
                    <option value="">All calibration states</option>
                    <option value="VALID">Valid</option>
                    <option value="DUE_SOON">Due Soon</option>
                    <option value="EXPIRED">Expired</option>
                    <option value="NONE">No Calibration</option>
                </select>

                <select
                    className="t9-select"
                    value={departmentId}
                    onChange={e => setDepartmentId(e.target.value)}
                >
                    <option value="">All Departments</option>

                    {departments.map(department => (
                        <option key={department.id} value={department.id}>
                            {department.name}
                        </option>
                    ))}
                </select>

                <select
                    className="t9-select"
                    value={sort}
                    onChange={e => setSort(e.target.value)}
                >
                    <option value="name,asc">Equipment: A to Z</option>
                    <option value="name,desc">Equipment: Z to A</option>
                </select>

                <button className="t9-btn" type="submit" disabled={loading}>
                    Search
                </button>

                <button
                    className="t9-btn secondary"
                    type="button"
                    onClick={clearFilters}
                >
                    Clear
                </button>
            </form>

            {error && <div className="t9-error">{error}</div>}

            <div className="t9-card t9-table-wrap">
                <table className="t9-table">
                    <thead>
                    <tr>
                        <th>Equipment</th>
                        <th>Department</th>
                        <th>Latest Calibration</th>
                        <th>Next Due Date</th>
                        <th>Days Remaining</th>
                        <th>Calibration State</th>
                    </tr>
                    </thead>

                    <tbody>
                    {rows.map(record => (
                        <tr key={record.resourceId}>
                            <td>
                                <strong>{record.equipmentName}</strong>
                                <div className="t9-muted">
                                    Resource #{record.resourceId}
                                </div>
                            </td>

                            <td>{record.departmentName || '—'}</td>

                            <td>{formatDate(record.latestCalibrationDate)}</td>

                            <td>{formatDate(record.nextDueDate)}</td>

                            <td>
                                {record.daysUntilDue == null
                                    ? '—'
                                    : record.daysUntilDue}
                            </td>

                            <td>
                  <span
                      className={`t9-pill ${record.calibrationState}`}
                  >
                    {record.calibrationState?.replaceAll('_', ' ')}
                  </span>
                            </td>
                        </tr>
                    ))}

                    {!loading && rows.length === 0 && (
                        <tr>
                            <td colSpan="6" className="t9-empty">
                                No calibration records found.
                            </td>
                        </tr>
                    )}
                    </tbody>
                </table>

                {loading && (
                    <div className="t9-empty">
                        Loading calibration records...
                    </div>
                )}

                <div className="t9-pagination">
          <span className="t9-muted">
            Showing {rows.length} records · Page {page + 1} of{' '}
              {Math.max(totalPages, 1)}
          </span>

                    <div className="t9-inline">
                        <button
                            className="t9-btn secondary"
                            disabled={page <= 0 || loading}
                            onClick={() => load(page - 1)}
                        >
                            Previous
                        </button>

                        <button
                            className="t9-btn secondary"
                            disabled={page + 1 >= totalPages || loading}
                            onClick={() => load(page + 1)}
                        >
                            Next
                        </button>
                    </div>
                </div>
            </div>
        </div>
    )
}
