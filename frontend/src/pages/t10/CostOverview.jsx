
import { useCallback, useEffect, useState } from 'react'
import {
    BarChart3,
    RefreshCw,
    IndianRupee,
    Clock3
} from 'lucide-react'

import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    Legend
} from 'recharts'

import api from '../../api/client'
import '../../styles/T10.css'

const money = value =>
    Number(value || 0).toLocaleString('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2
    })

const currentYear = new Date().getFullYear()

const today = new Date()

const todayDate = [
    today.getFullYear(),
    String(today.getMonth() + 1).padStart(2, '0'),
    String(today.getDate()).padStart(2, '0')
].join('-')

export default function CostOverview() {

    const [tab, setTab] = useState('equipment')

    const [equipment, setEquipment] = useState([])
    const [departments, setDepartments] = useState([])
    const [departmentOptions, setDepartmentOptions] = useState([])

    const [filters, setFilters] = useState({
        fromDate: `${currentYear}-01-01`,
        toDate: todayDate,
        departmentId: ''
    })

    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')

    // Load Cost Overview Data
    const load = useCallback(async () => {

        setLoading(true)
        setError('')

        try {

            const params = {
                fromDate: filters.fromDate || undefined,
                toDate: filters.toDate || undefined,
                departmentId: filters.departmentId || undefined
            }

            const endpoint = tab === 'equipment'
                ? '/cost-overview/equipment'
                : '/cost-overview/departments'

            const response = await api.get(endpoint, { params })

            const rows = Array.isArray(response.data)
                ? response.data
                : response.data?.content || []

            if (tab === 'equipment') {
                setEquipment(rows)
            } else {
                setDepartments(rows)
            }

        } catch (e) {

            setError(
                e.response?.data?.message ||
                'Cost overview could not be loaded.'
            )

        } finally {

            setLoading(false)

        }

    }, [tab, filters])

    // Load Department Dropdown
    useEffect(() => {

        api.get('/departments')
            .then(response => {

                const result = response.data

                setDepartmentOptions(
                    Array.isArray(result)
                        ? result
                        : result?.content || []
                )

            })
            .catch(() => {
                setDepartmentOptions([])
            })

    }, [])

    // Automatically load data when filters or tab change
    useEffect(() => {

        load()

    }, [load])

    // Update Filter
    const updateFilter = (key, value) => {

        setFilters(prev => ({
            ...prev,
            [key]: value
        }))

    }

    // Equipment Statistics
    const equipmentTotal = equipment.reduce(
        (sum, row) => sum + Number(row.totalCost || 0),
        0
    )

    const usageTotal = equipment.reduce(
        (sum, row) => sum + Number(row.totalUsageCost || 0),
        0
    )

    const maintenanceTotal = equipment.reduce(
        (sum, row) => sum + Number(row.maintenanceCost || 0),
        0
    )

    const hoursTotal = equipment.reduce(
        (sum, row) => sum + Number(row.usageHours || 0),
        0
    )

    // Chart Data
    const chartData = tab === 'equipment'
        ? equipment.map(row => ({
            name: row.equipmentName || `Resource ${row.resourceId}`,
            usage: Number(row.totalUsageCost || 0),
            maintenance: Number(row.maintenanceCost || 0)
        }))
        : departments.map(row => ({
            name: row.departmentName || 'Unallocated',
            internal: Number(row.internalCost || 0),
            external: Number(row.externalCost || 0)
        }))

    return (

        <div className="t10-page">

            {/* Header */}
            <div className="t10-header">

                <div>
                    <h1>Cost Overview</h1>

                    <p>
                        Analyze equipment usage costs and department allocation.
                    </p>
                </div>

                <button
                    className="t10-btn secondary"
                    onClick={load}
                    disabled={loading}
                >
                    <RefreshCw size={16} />
                    Refresh
                </button>

            </div>

            {/* Tabs */}
            <div className="t10-tabs">

                <button
                    className={tab === 'equipment' ? 'active' : ''}
                    onClick={() => setTab('equipment')}
                >
                    Equipment Cost
                </button>

                <button
                    className={tab === 'departments' ? 'active' : ''}
                    onClick={() => setTab('departments')}
                >
                    Department Allocation
                </button>

            </div>

            <section className="t10-panel">

                {/* Filters */}
                <div className="t10-filters">

                    <label>
                        From

                        <input
                            type="date"
                            value={filters.fromDate}
                            max={todayDate}
                            onChange={e =>
                                updateFilter('fromDate', e.target.value)
                            }
                        />
                    </label>

                    <label>
                        To

                        <input
                            type="date"
                            value={filters.toDate}
                            max={todayDate}
                            onChange={e =>
                                updateFilter('toDate', e.target.value)
                            }
                        />
                    </label>

                    <select
                        value={filters.departmentId}
                        onChange={e =>
                            updateFilter('departmentId', e.target.value)
                        }
                    >

                        <option value="">
                            All departments
                        </option>

                        {departmentOptions.map(d => (

                            <option key={d.id} value={d.id}>
                                {d.name}
                            </option>

                        ))}

                    </select>

                    <button
                        className="t10-btn primary"
                        onClick={load}
                        disabled={loading}
                    >
                        Apply
                    </button>

                </div>

                {/* Error */}
                {error && (
                    <div className="t10-error">
                        {error}
                    </div>
                )}

                {/* Equipment Statistics */}
                {tab === 'equipment' && (

                    <div className="t10-stat-grid">

                        <div className="t10-stat-card">

                            <div className="t10-stat-icon">
                                <IndianRupee size={20} />
                            </div>

                            <span>Total Cost</span>

                            <strong>
                                {money(equipmentTotal)}
                            </strong>

                        </div>

                        <div className="t10-stat-card">

                            <div className="t10-stat-icon">
                                <BarChart3 size={20} />
                            </div>

                            <span>Usage Cost</span>

                            <strong>
                                {money(usageTotal)}
                            </strong>

                        </div>

                        <div className="t10-stat-card">

                            <div className="t10-stat-icon">
                                <IndianRupee size={20} />
                            </div>

                            <span>Maintenance Cost</span>

                            <strong>
                                {money(maintenanceTotal)}
                            </strong>

                        </div>

                        <div className="t10-stat-card">

                            <div className="t10-stat-icon">
                                <Clock3 size={20} />
                            </div>

                            <span>Usage Hours</span>

                            <strong>
                                {hoursTotal.toFixed(2)}
                            </strong>

                        </div>

                    </div>

                )}

                {/* Department Statistics */}
                {tab === 'departments' && (

                    <div className="t10-stat-grid">

                        <div className="t10-stat-card">

                            <span>Departments / Groups</span>

                            <strong>
                                {departments.length}
                            </strong>

                        </div>

                        <div className="t10-stat-card">

                            <span>Allocated Cost</span>

                            <strong>
                                {money(
                                    departments.reduce(
                                        (sum, d) =>
                                            sum + Number(d.totalCost || 0),
                                        0
                                    )
                                )}
                            </strong>

                        </div>

                    </div>

                )}

                {/* Charts */}
                <div className="t10-chart-panel">

                    <h2>
                        {tab === 'equipment'
                            ? 'Equipment Cost Breakdown'
                            : 'Department Cost Allocation'}
                    </h2>

                    {loading ? (

                        <div className="t10-empty">
                            Loading chart...
                        </div>

                    ) : chartData.length === 0 ? (

                        <div className="t10-empty">
                            No cost data for this period.
                        </div>

                    ) : (

                        <div className="t10-chart">

                            <ResponsiveContainer width="100%" height={330}>

                                <BarChart
                                    data={chartData}
                                    margin={{
                                        top: 12,
                                        right: 20,
                                        left: 12,
                                        bottom: 65
                                    }}
                                >

                                    <CartesianGrid strokeDasharray="3 3" />

                                    <XAxis
                                        dataKey="name"
                                        angle={-25}
                                        textAnchor="end"
                                        interval={0}
                                        height={75}
                                    />

                                    <YAxis />

                                    <Tooltip
                                        formatter={value => money(value)}
                                    />

                                    <Legend />

                                    {tab === 'equipment' ? (

                                        <>
                                            <Bar
                                                dataKey="usage"
                                                name="Usage Cost"
                                                fill="#3976c8"
                                                radius={[5, 5, 0, 0]}
                                            />

                                            <Bar
                                                dataKey="maintenance"
                                                name="Maintenance Cost"
                                                fill="#e59a38"
                                                radius={[5, 5, 0, 0]}
                                            />
                                        </>

                                    ) : (

                                        <>
                                            <Bar
                                                dataKey="internal"
                                                name="Internal Cost"
                                                fill="#3976c8"
                                                radius={[5, 5, 0, 0]}
                                            />

                                            <Bar
                                                dataKey="external"
                                                name="External Cost"
                                                fill="#2c9b78"
                                                radius={[5, 5, 0, 0]}
                                            />
                                        </>

                                    )}

                                </BarChart>

                            </ResponsiveContainer>

                        </div>

                    )}

                </div>

                {/* Data Table */}
                <div className="t10-table-wrap">

                    <table className="t10-table">

                        <thead>

                        {tab === 'equipment' ? (

                            <tr>
                                <th>Equipment</th>
                                <th>Hours</th>
                                <th>Internal</th>
                                <th>External</th>
                                <th>Unassigned</th>
                                <th>Usage Cost</th>
                                <th>Maintenance</th>
                                <th>Total</th>
                            </tr>

                        ) : (

                            <tr>
                                <th>Department</th>
                                <th>Internal Cost</th>
                                <th>External Cost</th>
                                <th>Total Cost</th>
                                <th>Share</th>
                            </tr>

                        )}

                        </thead>

                        <tbody>

                        {loading ? (

                            <tr>
                                <td
                                    colSpan={tab === 'equipment' ? 8 : 5}
                                    className="t10-empty"
                                >
                                    Loading...
                                </td>
                            </tr>

                        ) : tab === 'equipment' ? (

                            equipment.length === 0 ? (

                                <tr>
                                    <td colSpan="8" className="t10-empty">
                                        No equipment cost records found.
                                    </td>
                                </tr>

                            ) : (

                                equipment.map(row => (

                                    <tr key={row.resourceId}>

                                        <td>
                                            <strong>
                                                {row.equipmentName}
                                            </strong>
                                        </td>

                                        <td>
                                            {Number(row.usageHours || 0).toFixed(2)}
                                        </td>

                                        <td>{money(row.internalCost)}</td>

                                        <td>{money(row.externalCost)}</td>

                                        <td>{money(row.unassignedCost)}</td>

                                        <td>{money(row.totalUsageCost)}</td>

                                        <td>{money(row.maintenanceCost)}</td>

                                        <td>
                                            <strong>
                                                {money(row.totalCost)}
                                            </strong>
                                        </td>

                                    </tr>

                                ))

                            )

                        ) : (

                            departments.length === 0 ? (

                                <tr>
                                    <td colSpan="5" className="t10-empty">
                                        No department allocation found.
                                    </td>
                                </tr>

                            ) : (

                                departments.map((row, index) => (

                                    <tr
                                        key={
                                            row.departmentId ??
                                            `unallocated-${index}`
                                        }
                                    >

                                        <td>
                                            <strong>
                                                {row.departmentName || 'Unallocated'}
                                            </strong>
                                        </td>

                                        <td>{money(row.internalCost)}</td>

                                        <td>{money(row.externalCost)}</td>

                                        <td>
                                            <strong>
                                                {money(row.totalCost)}
                                            </strong>
                                        </td>

                                        <td>
                                            {Number(
                                                row.percentageShare || 0
                                            ).toFixed(2)}%
                                        </td>

                                    </tr>

                                ))

                            )

                        )}

                        </tbody>

                    </table>

                </div>

            </section>

        </div>

    )

}
