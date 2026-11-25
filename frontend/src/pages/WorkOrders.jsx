
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
    ClipboardList, RefreshCw, Search, Wrench, Clock3, CheckCircle2,
    AlertCircle, UserRound, CalendarDays, Play, Check, X, Eye,
    MessageSquare, History, UserPlus, Timer, ChevronLeft, ChevronRight
} from 'lucide-react'
import '../styles/WorkOrders.css'
import api from '../api/client'
import { useAuth } from '../context/AuthContext'

const BASE = '/work-orders'

const statusColors = {
    OPEN: '#64748b',
    ASSIGNED: '#2563eb',
    IN_PROGRESS: '#d97706',
    COMPLETED: '#059669',
    CANCELLED: '#dc2626',
}

const formatDate = (value) => {
    if (!value) return '—'
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

const money = (value) => {
    if (value === null || value === undefined || value === '') return '—'
    return `₹${Number(value).toLocaleString('en-IN')}`
}

const getError = (error) =>
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    'Something went wrong.'

const getRows = (data) => {
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.content)) return data.content
    if (Array.isArray(data?.data)) return data.data
    return []
}

const getRoleName = (user) => {
    const role = user?.role?.name || user?.role || user?.authorities?.[0]?.authority || ''
    return String(role).replace('ROLE_', '').toUpperCase()
}

function StatusBadge({ status }) {
    return (
        <span className="wo-s1">
      {(status || 'UNKNOWN').replaceAll('_', ' ')}
    </span>
    )
}

function Modal({ title, onClose, children, wide = false }) {
    return (
        <div
            onMouseDown={(event) => {
                if (event.target === event.currentTarget) onClose()
            }}
            className="wo-s2"
        >
            <div className="wo-s3">
                <div className="wo-s4">
                    <h2 className="wo-s5">{title}</h2>
                    <button onClick={onClose} className="wo-s6">
                        <X size={17} />
                    </button>
                </div>
                {children}
            </div>
        </div>
    )
}

export default function WorkOrders() {
    const { user } = useAuth()
    const role = getRoleName(user)
    const isManager = role === 'LAB_MANAGER'
    const isTechnician = role === 'LAB_TECHNICIAN'

    const [orders, setOrders] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)
    const [totalElements, setTotalElements] = useState(0)
    const [statusFilter, setStatusFilter] = useState('')
    const [typeFilter, setTypeFilter] = useState('')
    const [search, setSearch] = useState('')

    const [selected, setSelected] = useState(null)
    const [preview, setPreview] = useState(null)
    const [notes, setNotes] = useState([])
    const [logs, setLogs] = useState([])
    const [noteText, setNoteText] = useState('')
    const [detailsLoading, setDetailsLoading] = useState(false)
    const [actionLoading, setActionLoading] = useState(false)
    const [modalError, setModalError] = useState('')
    const [activeTab, setActiveTab] = useState('details')

    const [technicianId, setTechnicianId] = useState('')
    const [technicians, setTechnicians] = useState([])
    const [techniciansLoading, setTechniciansLoading] = useState(false)
    const [techniciansError, setTechniciansError] = useState('')
    const [technicianWorkloads, setTechnicianWorkloads] = useState([])
    const [workloadLoading, setWorkloadLoading] = useState(false)
    const [remarks, setRemarks] = useState('')
    const [actualCost, setActualCost] = useState('')
    const [expectedEnd, setExpectedEnd] = useState('')

    const loadOrders = useCallback(async () => {
        if (!isManager && !isTechnician) {
            setLoading(false)
            setError('You are not authorized to view work orders.')
            return
        }

        setLoading(true)
        setError('')

        try {
            const endpoint = isTechnician ? `${BASE}/my` : BASE
            const params = {
                page,
                size: 10,
                sort: 'createdAt,desc',
            }

            if (isManager && statusFilter) params.status = statusFilter
            if (isManager && typeFilter) params.type = typeFilter

            const response = await api.get(endpoint, { params })
            const data = response.data

            setOrders(getRows(data))
            setTotalPages(data?.totalPages ?? 0)
            setTotalElements(data?.totalElements ?? getRows(data).length)
        } catch (err) {
            setError(getError(err))
        } finally {
            setLoading(false)
        }
    }, [isManager, isTechnician, page, statusFilter, typeFilter])

    useEffect(() => {
        loadOrders()
    }, [loadOrders])

    const visibleOrders = useMemo(() => {
        const term = search.trim().toLowerCase()
        if (!term) return orders

        return orders.filter((order) =>
            [
                order.workOrderNumber,
                order.resourceName,
                order.title,
                order.description,
                order.assignedTechnicianName,
                order.status,
                order.type,
            ].some((value) => String(value || '').toLowerCase().includes(term))
        )
    }, [orders, search])

    const counts = useMemo(() => ({
        open: orders.filter((item) => item.status === 'OPEN').length,
        assigned: orders.filter((item) => item.status === 'ASSIGNED').length,
        progress: orders.filter((item) => item.status === 'IN_PROGRESS').length,
        completed: orders.filter((item) => item.status === 'COMPLETED').length,
    }), [orders])

    const loadDetails = async (order) => {
        setSelected(order)
        setActiveTab('details')
        setModalError('')
        setNotes([])
        setLogs([])
        setNoteText('')
        setTechnicianId(order.assignedTechnicianId || '')
        setRemarks('')
        setActualCost(order.actualCost ?? '')
        setExpectedEnd(order.expectedEnd || '')
        setDetailsLoading(true)

        try {
            const [detailResponse, notesResponse, logsResponse] = await Promise.all([
                api.get(`${BASE}/${order.id}`),
                api.get(`${BASE}/${order.id}/notes`),
                api.get(`${BASE}/${order.id}/logs`),
            ])

            setSelected(detailResponse.data)
            setNotes(getRows(notesResponse.data))
            setLogs(getRows(logsResponse.data))
            setTechnicianId(detailResponse.data.assignedTechnicianId || '')
            setActualCost(detailResponse.data.actualCost ?? '')
            setExpectedEnd(detailResponse.data.expectedEnd || '')
        } catch (err) {
            setModalError(getError(err))
        } finally {
            setDetailsLoading(false)
        }
    }

    useEffect(() => {
        if (!selected || !isManager) {
            setTechnicians([])
            setTechniciansError('')
            return
        }

        let active = true
        setTechniciansLoading(true)
        setTechniciansError('')

        api.get('/maintenance/technicians')
            .then((response) => {
                if (active) setTechnicians(getRows(response.data))
            })
            .catch((err) => {
                if (active) setTechniciansError(getError(err))
            })
            .finally(() => {
                if (active) setTechniciansLoading(false)
            })

        return () => {
            active = false
        }
    }, [selected?.id, isManager])

    useEffect(() => {
        if (!selected || !isManager) {
            setTechnicianWorkloads([])
            setWorkloadLoading(false)
            return
        }

        let active = true
        setWorkloadLoading(true)

        api.get('/technicians/workload')
            .then((response) => {
                if (active) {
                    setTechnicianWorkloads(getRows(response.data))
                }
            })
            .catch(() => {
                if (active) setTechnicianWorkloads([])
            })
            .finally(() => {
                if (active) setWorkloadLoading(false)
            })

        return () => {
            active = false
        }
    }, [selected?.id, isManager])

    const selectedTechnicianWorkload = technicianWorkloads.find(
        (item) => String(item.technicianId) === String(technicianId)
    )

    const refreshSelected = async () => {
        if (!selected) return
        const response = await api.get(`${BASE}/${selected.id}`)
        setSelected(response.data)
        await loadOrders()
    }

    const runAction = async (action, payload = {}) => {
        if (!selected) return

        setActionLoading(true)
        setModalError('')

        try {
            await api.patch(`${BASE}/${selected.id}/${action}`, payload)
            await refreshSelected()
            setRemarks('')
            setActualCost('')
            setPreview(null)
        } catch (err) {
            setModalError(getError(err))
        } finally {
            setActionLoading(false)
        }
    }

    const openStartPreview = async () => {
        if (!selected) return

        // The preview endpoint is available only to LAB_MANAGER.
        // Assigned technicians can start their own work order directly.
        if (!expectedEnd) {
            setModalError('Please select a future expected maintenance end date and time.')
            return
        }

        if (new Date(expectedEnd) <= new Date()) {
            setModalError('Expected maintenance end must be in the future.')
            return
        }

        if (isTechnician) {
            const confirmed = window.confirm(
                'Are you sure you want to start this assigned maintenance work order?'
            )

            if (confirmed) {
                await runAction('start', {
                    remarks: remarks.trim() || 'Maintenance work started',
                    expectedEnd,
                })
            }
            return
        }

        setActionLoading(true)
        setModalError('')

        try {
            const response = await api.get(`${BASE}/${selected.id}/maintenance-preview`, { params: { expectedEnd } })
            setPreview(response.data)
        } catch (err) {
            setModalError(getError(err))
        } finally {
            setActionLoading(false)
        }
    }

    const confirmStart = async () => {
        if (!selected) return
        await runAction('start', {
            remarks: remarks.trim() || 'Maintenance work started',
            expectedEnd,
        })
    }

    const assignTechnician = async () => {
        if (!technicianId || Number(technicianId) <= 0) {
            setModalError('Please enter a valid Technician User ID.')
            return
        }

        setActionLoading(true)
        setModalError('')

        try {
            await api.patch(`${BASE}/${selected.id}/assign`, {
                technicianId: Number(technicianId),
            })
            await refreshSelected()
        } catch (err) {
            setModalError(getError(err))
        } finally {
            setActionLoading(false)
        }
    }

    const extendWorkOrder = async () => {
        if (!expectedEnd) {
            setModalError('Please select the new expected end date and time.')
            return
        }

        await runAction('extend', {
            expectedEnd,
            remarks: remarks.trim() || 'Expected maintenance time extended',
        })
    }

    const completeWorkOrder = async () => {
        await runAction('complete', {
            actualCost: actualCost === '' ? null : Number(actualCost),
            remarks: remarks.trim() || 'Maintenance work completed',
        })
    }

    const cancelWorkOrder = async () => {
        if (!window.confirm('Are you sure you want to cancel this work order?')) return
        await runAction('cancel', {
            remarks: remarks.trim() || 'Work order cancelled',
        })
    }

    const addNote = async () => {
        if (!noteText.trim()) {
            setModalError('Please enter a note.')
            return
        }

        setActionLoading(true)
        setModalError('')

        try {
            await api.post(`${BASE}/${selected.id}/notes`, {
                content: noteText.trim(),
            })
            setNoteText('')
            const response = await api.get(`${BASE}/${selected.id}/notes`)
            setNotes(getRows(response.data))
        } catch (err) {
            setModalError(getError(err))
        } finally {
            setActionLoading(false)
        }
    }

    const statCards = [
        { label: 'Open', value: counts.open, icon: ClipboardList },
        { label: 'Assigned', value: counts.assigned, icon: UserRound },
        { label: 'In Progress', value: counts.progress, icon: Wrench },
        { label: 'Completed', value: counts.completed, icon: CheckCircle2 },
    ]

    if (!isManager && !isTechnician) {
        return (
            <div className="wo-s7">
                <h2>Work Orders</h2>
                <p>You do not have permission to access this page.</p>
            </div>
        )
    }

    return (
        <div className="wo-s8">
            <div className="wo-s9">
                <div>
                    <h1 className="wo-s10">Work Orders</h1>
                    <p className="wo-s11">
                        {isManager
                            ? 'Manage maintenance work orders and technician assignments.'
                            : 'View and update maintenance tasks assigned to you.'}
                    </p>
                </div>

                <button onClick={loadOrders} className="wo-s12" disabled={loading}>
                    <RefreshCw size={15} /> Refresh
                </button>
            </div>

            <div className="wo-s13">
                {statCards.map(({ label, value, icon: Icon }) => (
                    <div key={label} className="wo-s14">
                        <div className="wo-s15">
                            <span className="wo-s16">{label}</span>
                            <Icon size={18} />
                        </div>
                        <div className="wo-s17">
                            {value}
                        </div>
                    </div>
                ))}
            </div>

            <div className="wo-s18">
                <div className="wo-s19">
                    <Search size={17} className="wo-s20" />
                    <input
                        value={search}
                        onChange={(event) => setSearch(event.target.value)}
                        placeholder="Search work orders..."
                        className="wo-s21"
                    />
                </div>

                {isManager && (
                    <>
                        <select
                            value={statusFilter}
                            onChange={(event) => {
                                setStatusFilter(event.target.value)
                                setPage(0)
                            }}
                            className="wo-s22"
                        >
                            <option value="">All statuses</option>
                            <option value="OPEN">Open</option>
                            <option value="ASSIGNED">Assigned</option>
                            <option value="IN_PROGRESS">In Progress</option>
                            <option value="COMPLETED">Completed</option>
                            <option value="CANCELLED">Cancelled</option>
                        </select>

                        <select
                            value={typeFilter}
                            onChange={(event) => {
                                setTypeFilter(event.target.value)
                                setPage(0)
                            }}
                            className="wo-s23"
                        >
                            <option value="">All types</option>
                            <option value="PREVENTIVE">Preventive</option>
                            <option value="CORRECTIVE">Corrective</option>
                            <option value="CALIBRATION">Calibration</option>
                        </select>
                    </>
                )}
            </div>

            {error && (
                <div className="wo-s24">
                    <AlertCircle size={17} /> {error}
                    <button onClick={loadOrders} className="wo-s25">
                        Retry
                    </button>
                </div>
            )}

            <div className="wo-s26">
                {loading ? (
                    <div className="wo-s27">
                        Loading work orders...
                    </div>
                ) : visibleOrders.length === 0 ? (
                    <div className="wo-s28">
                        <ClipboardList size={35} className="wo-s29" />
                        <div className="wo-s30">No work orders found</div>
                        <p className="wo-s31">Try changing the filters or refresh the page.</p>
                    </div>
                ) : (
                    <div className="wo-s32">
                        <table className="wo-s33">
                            <thead>
                            <tr className="wo-s34">
                                {['Work Order', 'Equipment', 'Type', 'Technician', 'Expected End', 'Status', 'Action'].map((heading) => (
                                    <th key={heading} className="wo-s35">
                                        {heading}
                                    </th>
                                ))}
                            </tr>
                            </thead>
                            <tbody>
                            {visibleOrders.map((order) => (
                                <tr key={order.id}>
                                    <td className="wo-s36">
                                        <div className="wo-s37">
                                            {order.workOrderNumber || `WO-${order.id}`}
                                        </div>
                                        <div className="wo-s38">
                                            {order.title || 'Maintenance'}
                                        </div>
                                    </td>
                                    <td className="wo-s39">
                                        {order.resourceName || `Resource #${order.resourceId || '—'}`}
                                    </td>
                                    <td className="wo-s40">
                                        {order.type || '—'}
                                    </td>
                                    <td className="wo-s41">
                                        {order.assignedTechnicianName || 'Not assigned'}
                                    </td>
                                    <td className="wo-s42">
                                        {formatDate(order.expectedEnd || order.scheduledEnd)}
                                    </td>
                                    <td className="wo-s43">
                                        <StatusBadge status={order.status} />
                                    </td>
                                    <td className="wo-s44">
                                        <button onClick={() => loadDetails(order)} className="wo-s45">
                                            <Eye size={15} /> Manage
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {!loading && totalPages > 1 && (
                    <div className="wo-s46">
            <span className="wo-s47">
              {totalElements} total work orders
            </span>
                        <div className="wo-s48">
                            <button
                                className="wo-s49"
                                disabled={page <= 0}
                                onClick={() => setPage((value) => Math.max(0, value - 1))}
                            >
                                <ChevronLeft size={15} /> Previous
                            </button>
                            <span className="wo-s50">Page {page + 1} of {totalPages}</span>
                            <button
                                className="wo-s51"
                                disabled={page + 1 >= totalPages}
                                onClick={() => setPage((value) => value + 1)}
                            >
                                Next <ChevronRight size={15} />
                            </button>
                        </div>
                    </div>
                )}
            </div>

            {selected && (
                <Modal
                    title={`Work Order: ${selected.workOrderNumber || selected.id}`}
                    onClose={() => {
                        setSelected(null)
                        setPreview(null)
                    }}
                    wide
                >
                    {detailsLoading ? (
                        <p className="wo-s52">Loading work order details...</p>
                    ) : (
                        <>
                            {modalError && (
                                <div className="wo-s53">
                                    {modalError}
                                </div>
                            )}

                            <div className="wo-s54">
                                <div>
                                    <div className="wo-s55">
                                        {selected.title || 'Maintenance Work'}
                                    </div>
                                    <div className="wo-s56">
                                        {selected.resourceName || `Resource #${selected.resourceId}`}
                                    </div>
                                </div>
                                <StatusBadge status={selected.status} />
                            </div>

                            <div className="wo-s57">
                                {[
                                    ['details', 'Details', ClipboardList],
                                    ['notes', 'Notes', MessageSquare],
                                    ['logs', 'Service Logs', History],
                                ].map(([key, label, Icon]) => (
                                    <button
                                        key={key}
                                        onClick={() => setActiveTab(key)}
                                        className="wo-s58"
                                    >
                                        <Icon size={15} /> {label}
                                    </button>
                                ))}
                            </div>

                            {activeTab === 'details' && (
                                <>
                                    <div className="wo-s59">
                                        {[
                                            ['Type', selected.type],
                                            ['Assigned Technician', selected.assignedTechnicianName || 'Not assigned'],
                                            ['Created By', selected.createdByName || '—'],
                                            ['Scheduled Start', formatDate(selected.scheduledStart)],
                                            ['Scheduled End', formatDate(selected.scheduledEnd)],
                                            ['Actual Start', formatDate(selected.actualStart)],
                                            ['Expected End', formatDate(selected.expectedEnd)],
                                            ['Actual End', formatDate(selected.actualEnd)],
                                            ['Estimated Cost', money(selected.estimatedCost)],
                                            ['Actual Cost', money(selected.actualCost)],
                                        ].map(([label, value]) => (
                                            <div key={label} className="wo-s60">
                                                <div className="wo-s61">
                                                    {label}
                                                </div>
                                                <div className="wo-s62">
                                                    {value ?? '—'}
                                                </div>
                                            </div>
                                        ))}
                                    </div>

                                    <div className="wo-s63">
                                        <h3 className="wo-s64">Description</h3>
                                        <p className="wo-s65">
                                            {selected.description || 'No description provided.'}
                                        </p>
                                        {selected.resolutionNotes && (
                                            <>
                                                <h3 className="wo-s66">Resolution Notes</h3>
                                                <p className="wo-s67">
                                                    {selected.resolutionNotes}
                                                </p>
                                            </>
                                        )}
                                    </div>

                                    {isManager && !['COMPLETED', 'CANCELLED'].includes(selected.status) && (
                                        <div className="wo-s68">
                                            <h3 className="wo-s69">
                                                <UserPlus size={16} className="wo-s70" />
                                                Assign / Reassign Technician
                                            </h3>
                                            <div className="wo-s71">
                                                <select
                                                    value={technicianId}
                                                    onChange={(event) => setTechnicianId(event.target.value)}
                                                    className="wo-s72"
                                                    disabled={techniciansLoading || actionLoading}
                                                >
                                                    <option value="">
                                                        {techniciansLoading ? 'Loading technicians...' : 'Select a technician'}
                                                    </option>
                                                    {selected.assignedTechnicianId &&
                                                        !technicians.some((item) => String(item.id) === String(selected.assignedTechnicianId)) && (
                                                            <option value={selected.assignedTechnicianId}>
                                                                {selected.assignedTechnicianName || `Technician #${selected.assignedTechnicianId}`} (currently assigned)
                                                            </option>
                                                        )}
                                                    {technicians.map((technician) => {
                                                        const workload = technicianWorkloads.find(
                                                            (item) => String(item.technicianId) === String(technician.id)
                                                        )

                                                        return (
                                                            <option key={technician.id} value={technician.id}>
                                                                {technician.fullName} — Active: {workload?.activeTasks ?? 0},
                                                                Overdue: {workload?.overdueTasks ?? 0}
                                                            </option>
                                                        )
                                                    })}
                                                </select>
                                                <button
                                                    onClick={assignTechnician}
                                                    disabled={actionLoading || techniciansLoading || !technicianId}
                                                    className="wo-s73"
                                                >
                                                    <UserPlus size={15} /> Assign
                                                </button>
                                            </div>
                                            {techniciansError ? (
                                                <p className="wo-s74">Could not load technicians: {techniciansError}</p>
                                            ) : (
                                                <p className="wo-s74">
                                                    {technicians.length
                                                        ? `${technicians.length} active technician(s) from your institution.`
                                                        : techniciansLoading
                                                            ? 'Loading your institution technicians...'
                                                            : 'No active technicians found in your institution.'}
                                                </p>
                                            )}

                                            {technicianId && (
                                                <div className="wo-s74" style={{ marginTop: 10 }}>
                                                    <strong>Selected Technician Workload</strong>
                                                    {workloadLoading ? (
                                                        <p>Loading workload...</p>
                                                    ) : selectedTechnicianWorkload ? (
                                                        <>
                                                            <p>Active Tasks: {selectedTechnicianWorkload.activeTasks ?? 0}</p>
                                                            <p>In Progress: {selectedTechnicianWorkload.inProgressTasks ?? 0}</p>
                                                            <p>Overdue: {selectedTechnicianWorkload.overdueTasks ?? 0}</p>
                                                            <p>
                                                                Next Scheduled:{' '}
                                                                {formatDate(selectedTechnicianWorkload.nextScheduledDate)}
                                                            </p>
                                                        </>
                                                    ) : (
                                                        <p>Workload information unavailable.</p>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    )}

                                    <div className="wo-s75">
                                        <h3 className="wo-s76">
                                            <Timer size={16} className="wo-s77" />
                                            Work Order Actions
                                        </h3>

                                        {selected.status === 'ASSIGNED' && (
                                            <div className="wo-s82">
                                                <label className="wo-s78" htmlFor="wo-expected-end">
                                                    Expected Maintenance End <span aria-hidden="true">*</span>
                                                </label>
                                                <input
                                                    id="wo-expected-end"
                                                    type="datetime-local"
                                                    value={expectedEnd ? expectedEnd.slice(0, 16) : ''}
                                                    min={new Date(Date.now() + 60 * 1000).toISOString().slice(0, 16)}
                                                    onChange={(event) => setExpectedEnd(event.target.value)}
                                                    className="wo-s90"
                                                    required
                                                />
                                                <p className="wo-s74">Select a future date and time when maintenance is expected to finish.</p>
                                            </div>
                                        )}

                                        <label className="wo-s78">
                                            Remarks
                                        </label>
                                        <textarea
                                            rows={2}
                                            value={remarks}
                                            onChange={(event) => setRemarks(event.target.value)}
                                            placeholder="Enter remarks for this action..."
                                            className="wo-s79"
                                        />

                                        <div className="wo-s80">
                                            {selected.status === 'ASSIGNED' && (
                                                <button
                                                    onClick={openStartPreview}
                                                    disabled={actionLoading}
                                                    className="wo-s81"
                                                >
                                                    <Play size={15} />
                                                    {isManager ? 'Preview & Start' : 'Start Work'}
                                                </button>
                                            )}

                                            {selected.status === 'IN_PROGRESS' && (
                                                <>
                                                    <div className="wo-s82">
                                                        <input
                                                            type="number"
                                                            min="0"
                                                            step="0.01"
                                                            value={actualCost}
                                                            onChange={(event) => setActualCost(event.target.value)}
                                                            placeholder="Actual cost (₹)"
                                                            className="wo-s83"
                                                        />
                                                    </div>
                                                    <button
                                                        onClick={completeWorkOrder}
                                                        disabled={actionLoading}
                                                        className="wo-s84"
                                                    >
                                                        <Check size={15} /> Complete
                                                    </button>
                                                </>
                                            )}

                                            {isManager && ['ASSIGNED', 'IN_PROGRESS'].includes(selected.status) && (
                                                <button
                                                    onClick={cancelWorkOrder}
                                                    disabled={actionLoading}
                                                    className="wo-s85"
                                                >
                                                    <X size={15} /> Cancel Work Order
                                                </button>
                                            )}
                                        </div>
                                    </div>

                                    {isManager && selected.status === 'IN_PROGRESS' && (
                                        <div className="wo-s86">
                                            <h3 className="wo-s87">
                                                <Clock3 size={16} className="wo-s88" />
                                                Extend Expected End Time
                                            </h3>
                                            <div className="wo-s89">
                                                <input
                                                    type="datetime-local"
                                                    value={expectedEnd ? expectedEnd.slice(0, 16) : ''}
                                                    onChange={(event) => setExpectedEnd(event.target.value)}
                                                    className="wo-s90"
                                                />
                                                <button
                                                    onClick={extendWorkOrder}
                                                    disabled={actionLoading}
                                                    className="wo-s91"
                                                >
                                                    <CalendarDays size={15} /> Extend
                                                </button>
                                            </div>
                                        </div>
                                    )}
                                </>
                            )}

                            {activeTab === 'notes' && (
                                <div>
                                    <h3 className="wo-s92">Work Order Notes</h3>
                                    <div className="wo-s93">
                    <textarea
                        rows={2}
                        value={noteText}
                        onChange={(event) => setNoteText(event.target.value)}
                        placeholder="Write a note..."
                        className="wo-s94"
                    />
                                        <button
                                            onClick={addNote}
                                            disabled={actionLoading}
                                            className="wo-s95"
                                        >
                                            <MessageSquare size={15} /> Add Note
                                        </button>
                                    </div>

                                    {notes.length === 0 ? (
                                        <p className="wo-s96">No notes available.</p>
                                    ) : notes.map((note) => (
                                        <div key={note.id} className="wo-s97">
                                            <p className="wo-s98">
                                                {note.content}
                                            </p>
                                            <div className="wo-s99">
                                                {note.author?.fullName || note.author?.name || 'Team member'}
                                                {' · '}
                                                {formatDate(note.createdAt)}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}

                            {activeTab === 'logs' && (
                                <div>
                                    <h3 className="wo-s100">Service Logs / Status History</h3>
                                    {logs.length === 0 ? (
                                        <p className="wo-s101">No service logs available.</p>
                                    ) : (
                                        <div className="wo-s102">
                                            {logs.map((log) => (
                                                <div key={log.id} className="wo-s103">
                                                    <span className="wo-s104" />
                                                    <div className="wo-s105">
                                                        {log.oldStatus || 'CREATED'} → {log.newStatus}
                                                    </div>
                                                    <div className="wo-s106">
                                                        {log.changedBy?.fullName || log.changedBy?.name || 'System'}
                                                        {' · '}
                                                        {formatDate(log.changedAt)}
                                                    </div>
                                                    {log.remarks && (
                                                        <p className="wo-s107">
                                                            {log.remarks}
                                                        </p>
                                                    )}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            )}

                            <div className="wo-s108">
                                <button onClick={() => setSelected(null)} className="wo-s109">Close</button>
                            </div>
                        </>
                    )}

                    {preview && (
                        <Modal
                            title="Confirm Maintenance Start"
                            onClose={() => setPreview(null)}
                            wide
                        >
                            <div className="wo-s110">
                                Starting this maintenance work order may cancel bookings that overlap
                                with the maintenance period. Review the affected bookings before confirming.
                            </div>

                            <div className="wo-s111">
                                <div>
                                    <div className="wo-s112">Equipment</div>
                                    <strong>{preview.resourceName || '—'}</strong>
                                </div>
                                <div>
                                    <div className="wo-s113">Maintenance Start</div>
                                    <strong>{formatDate(preview.maintenanceStart)}</strong>
                                </div>
                                <div>
                                    <div className="wo-s114">Expected End</div>
                                    <strong>{formatDate(preview.expectedEnd)}</strong>
                                </div>
                                <div>
                                    <div className="wo-s115">Affected Bookings</div>
                                    <strong>{preview.affectedBookingCount ?? preview.affectedBookings?.length ?? 0}</strong>
                                </div>
                            </div>

                            <h3 className="wo-s116">Affected Bookings</h3>

                            {!preview.affectedBookings?.length ? (
                                <p className="wo-s117">
                                    No overlapping bookings were found for this maintenance period.
                                </p>
                            ) : (
                                <div className="wo-s118">
                                    <table className="wo-s119">
                                        <thead>
                                        <tr className="wo-s120">
                                            {['Booking ID', 'User', 'Email', 'Start', 'End', 'Status'].map((item) => (
                                                <th key={item} className="wo-s121">
                                                    {item}
                                                </th>
                                            ))}
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {preview.affectedBookings.map((booking) => (
                                            <tr key={booking.bookingId}>
                                                <td className="wo-s122">
                                                    #{booking.bookingId}
                                                </td>
                                                <td className="wo-s123">
                                                    {booking.userName || '—'}
                                                </td>
                                                <td className="wo-s124">
                                                    {booking.userEmail || '—'}
                                                </td>
                                                <td className="wo-s125">
                                                    {formatDate(booking.startTime)}
                                                </td>
                                                <td className="wo-s126">
                                                    {formatDate(booking.endTime)}
                                                </td>
                                                <td className="wo-s127">
                                                    {booking.currentStatus || '—'}
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}

                            <label className="wo-s128">
                                Start remarks (optional)
                            </label>
                            <textarea
                                rows={2}
                                value={remarks}
                                onChange={(event) => setRemarks(event.target.value)}
                                placeholder="Add remarks..."
                                className="wo-s129"
                            />

                            <div className="wo-s130">
                                <button onClick={() => setPreview(null)} className="wo-s131">
                                    Go Back
                                </button>
                                <button
                                    onClick={confirmStart}
                                    disabled={actionLoading}
                                    className="wo-s132"
                                >
                                    <Check size={15} /> Confirm & Start Maintenance
                                </button>
                            </div>
                        </Modal>
                    )}
                </Modal>
            )}
        </div>
    )
}

