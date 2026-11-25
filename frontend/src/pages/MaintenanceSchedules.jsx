import './MaintenanceSchedules.css'
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
    AlertCircle,
    CalendarDays,
    CheckCircle2,
    Clock,
    Edit,
    Plus,
    RefreshCw,
    Search,
    ToggleLeft,
    ToggleRight,
    Wrench,
    X,
} from 'lucide-react'
import api from '../api/client'
import { useAuth } from '../context/AuthContext'


const PAGE_SIZE = 8

const STATUS_STYLES = {
    OK: 'm3-schedule-status-ok',
    DUE_SOON: 'm3-schedule-status-due_soon',
    OVERDUE: 'm3-schedule-status-overdue',
    INACTIVE: 'm3-schedule-status-inactive',
    UNKNOWN: 'm3-schedule-status-unknown',
}

function getItems(response) {
    const data = response?.data

    if (Array.isArray(data)) return data
    if (Array.isArray(data?.content)) return data.content
    if (Array.isArray(data?.items)) return data.items
    if (Array.isArray(data?.data)) return data.data

    return []
}

function getErrorMessage(error, fallback) {
    return (
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        error?.response?.data?.detail ||
        fallback
    )
}

function formatDate(value) {
    if (!value) return 'Not available'

    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value

    return date.toLocaleString('en-IN', {
        dateStyle: 'medium',
        timeStyle: 'short',
    })
}

function toLocalInputValue(value) {
    if (!value) return ''

    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return ''

    const localDate = new Date(
        date.getTime() - date.getTimezoneOffset() * 60000
    )

    return localDate.toISOString().slice(0, 16)
}

function getScheduleStatus(schedule) {
    if (schedule.active === false) return 'INACTIVE'

    return String(schedule.status || 'UNKNOWN').toUpperCase()
}

const EMPTY_FORM = {
    resourceId: '',
    title: '',
    description: '',
    repeatEveryDays: '30',
    nextDueAt: '',
}

export default function MaintenanceSchedules() {
    const { user } = useAuth()

    const role = String(user?.role || user?.authorities?.[0] || '')
        .replace('ROLE_', '')
        .toUpperCase()

    const isManager = role === 'LAB_MANAGER'

    const [schedules, setSchedules] = useState([])
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [processingId, setProcessingId] = useState(null)

    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')

    const [search, setSearch] = useState('')
    const [statusFilter, setStatusFilter] = useState('ALL')
    const [page, setPage] = useState(1)

    const [modalOpen, setModalOpen] = useState(false)
    const [editingSchedule, setEditingSchedule] = useState(null)
    const [form, setForm] = useState(EMPTY_FORM)

    const loadSchedules = useCallback(async () => {
        if (!isManager) {
            setLoading(false)
            return
        }

        setLoading(true)
        setError('')

        try {
            const response = await api.get('/maintenance/schedules')
            setSchedules(getItems(response))
        } catch (err) {
            setError(
                getErrorMessage(
                    err,
                    'Preventive maintenance schedules could not be loaded.'
                )
            )
        } finally {
            setLoading(false)
        }
    }, [isManager])

    useEffect(() => {
        loadSchedules()
    }, [loadSchedules])

    const filteredSchedules = useMemo(() => {
        const query = search.trim().toLowerCase()

        return schedules.filter((schedule) => {
            const status = getScheduleStatus(schedule)

            const matchesStatus =
                statusFilter === 'ALL' || status === statusFilter

            const matchesSearch =
                !query ||
                String(schedule.id || '').includes(query) ||
                String(schedule.resourceName || '').toLowerCase().includes(query) ||
                String(schedule.title || '').toLowerCase().includes(query) ||
                String(schedule.description || '').toLowerCase().includes(query)

            return matchesStatus && matchesSearch
        })
    }, [schedules, search, statusFilter])

    const totalPages = Math.max(
        1,
        Math.ceil(filteredSchedules.length / PAGE_SIZE)
    )

    const visibleSchedules = filteredSchedules.slice(
        (page - 1) * PAGE_SIZE,
        page * PAGE_SIZE
    )

    const counts = useMemo(() => {
        return schedules.reduce(
            (result, schedule) => {
                const status = getScheduleStatus(schedule)

                if (status === 'OK') result.ok += 1
                if (status === 'DUE_SOON') result.dueSoon += 1
                if (status === 'OVERDUE') result.overdue += 1
                if (status === 'INACTIVE') result.inactive += 1

                return result
            },
            { ok: 0, dueSoon: 0, overdue: 0, inactive: 0 }
        )
    }, [schedules])

    const openCreateModal = () => {
        setEditingSchedule(null)
        setForm(EMPTY_FORM)
        setError('')
        setSuccess('')
        setModalOpen(true)
    }

    const openEditModal = (schedule) => {
        setEditingSchedule(schedule)

        setForm({
            resourceId: String(schedule.resourceId || ''),
            title: schedule.title || '',
            description: schedule.description || '',
            repeatEveryDays: String(schedule.repeatEveryDays || 30),
            nextDueAt: toLocalInputValue(schedule.nextDueAt),
        })

        setError('')
        setSuccess('')
        setModalOpen(true)
    }

    const closeModal = () => {
        if (saving) return

        setModalOpen(false)
        setEditingSchedule(null)
        setForm(EMPTY_FORM)
    }

    const updateForm = (field, value) => {
        setForm((current) => ({
            ...current,
            [field]: value,
        }))
    }

    const handleSubmit = async (event) => {
        event.preventDefault()

        setError('')
        setSuccess('')

        if (!editingSchedule && Number(form.resourceId) <= 0) {
            setError('Please enter a valid Resource ID.')
            return
        }

        if (!form.title.trim()) {
            setError('Please enter a schedule title.')
            return
        }

        if (!Number.isInteger(Number(form.repeatEveryDays)) ||
            Number(form.repeatEveryDays) <= 0) {
            setError('Repeat interval must be a positive whole number of days.')
            return
        }

        if (!form.nextDueAt) {
            setError('Please select the next due date.')
            return
        }

        const dueDate = new Date(form.nextDueAt)

        if (Number.isNaN(dueDate.getTime())) {
            setError('Please enter a valid next due date.')
            return
        }

        const payload = {
            resourceId: Number(form.resourceId),
            title: form.title.trim(),
            description: form.description.trim(),
            repeatEveryDays: Number(form.repeatEveryDays),
            nextDueAt: form.nextDueAt.length === 16
                ? `${form.nextDueAt}:00`
                : form.nextDueAt,
        }

        setSaving(true)

        try {
            if (editingSchedule) {
                await api.put(
                    `/maintenance/schedules/${editingSchedule.id}`,
                    payload
                )
                setSuccess('Maintenance schedule updated successfully.')
            } else {
                await api.post('/maintenance/schedules', payload)
                setSuccess('Preventive maintenance schedule created successfully.')
            }

            setModalOpen(false)
            setEditingSchedule(null)
            setForm(EMPTY_FORM)

            await loadSchedules()
        } catch (err) {
            setError(
                getErrorMessage(
                    err,
                    'Schedule could not be saved. Please try again.'
                )
            )
        } finally {
            setSaving(false)
        }
    }

    const toggleSchedule = async (schedule) => {
        const currentlyActive = schedule.active !== false
        const nextActive = !currentlyActive

        const confirmed = window.confirm(
            nextActive
                ? `Activate schedule "${schedule.title}"?`
                : `Deactivate schedule "${schedule.title}"?`
        )

        if (!confirmed) return

        setError('')
        setSuccess('')
        setProcessingId(schedule.id)

        try {
            await api.patch(
                `/maintenance/schedules/${schedule.id}/status`,
                { active: nextActive }
            )

            setSuccess(
                nextActive
                    ? 'Schedule activated successfully.'
                    : 'Schedule deactivated successfully.'
            )

            await loadSchedules()
        } catch (err) {
            setError(
                getErrorMessage(
                    err,
                    'Schedule status could not be updated.'
                )
            )
        } finally {
            setProcessingId(null)
        }
    }

    if (!isManager) {
        return (
            <div className="m3-maintenanceschedules-001">
                <div className="m3-maintenanceschedules-002">
                    <AlertCircle size={22} />
                    <h2 className="m3-maintenanceschedules-003">Access Restricted</h2>
                </div>
                <p className="m3-maintenanceschedules-004">
                    Only Lab Managers can manage preventive maintenance schedules.
                </p>
            </div>
        )
    }

    return (
        <div className="m3-maintenanceschedules-005">
            <div className="m3-maintenanceschedules-006">
                <div>
                    <div className="m3-maintenanceschedules-007">
                        <div className="m3-maintenanceschedules-008">
                            <CalendarDays size={23} />
                        </div>
                        <h1 className="m3-maintenanceschedules-009">
                            Preventive Maintenance
                        </h1>
                    </div>

                    <p className="m3-maintenanceschedules-010">
                        Plan recurring maintenance and keep laboratory equipment in good condition.
                    </p>
                </div>

                <div className="m3-maintenanceschedules-011">
                    <button
                        type="button"
                        onClick={loadSchedules}
                        disabled={loading}
                        className="m3-maintenanceschedules-012"
                    >
                        <RefreshCw
                            size={16}
                            className={loading ? 'm3-spin' : ''}
                        />
                        Refresh
                    </button>

                    <button
                        type="button"
                        onClick={openCreateModal}
                        className="m3-maintenanceschedules-013"
                    >
                        <Plus size={17} />
                        Create Schedule
                    </button>
                </div>
            </div>

            {error && (
                <div className="m3-maintenanceschedules-014">
                    <AlertCircle size={19} className="m3-maintenanceschedules-015" />
                    <span>{error}</span>
                </div>
            )}

            {success && (
                <div className="m3-maintenanceschedules-016">
                    <CheckCircle2 size={19} className="m3-maintenanceschedules-015" />
                    <span>{success}</span>
                </div>
            )}

            <div className="m3-maintenanceschedules-017">
                <StatCard
                    title="On Track"
                    value={counts.ok}
                    icon={<CheckCircle2 size={20} />}
                    color="green"
                />
                <StatCard
                    title="Due Soon"
                    value={counts.dueSoon}
                    icon={<Clock size={20} />}
                    color="amber"
                />
                <StatCard
                    title="Overdue"
                    value={counts.overdue}
                    icon={<AlertCircle size={20} />}
                    color="red"
                />
                <StatCard
                    title="Inactive"
                    value={counts.inactive}
                    icon={<ToggleLeft size={20} />}
                    color="gray"
                />
            </div>

            <section className="m3-maintenanceschedules-018">
                <div className="m3-maintenanceschedules-019">
                    <div>
                        <h2 className="m3-maintenanceschedules-020">
                            Maintenance Schedules
                        </h2>
                        <p className="m3-maintenanceschedules-021">
                            {filteredSchedules.length} schedule(s) found
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={() => {
                            setSearch('')
                            setStatusFilter('ALL')
                            setPage(1)
                        }}
                        className="m3-maintenanceschedules-022"
                    >
                        Clear filters
                    </button>
                </div>

                <div className="m3-maintenanceschedules-023">
                    <div className="m3-maintenanceschedules-024">
                        <Search
                            size={18}
                            className="m3-maintenanceschedules-025"
                        />
                        <input
                            type="text"
                            value={search}
                            onChange={(event) => {
                                setSearch(event.target.value)
                                setPage(1)
                            }}
                            placeholder="Search by equipment, title or ID..."
                            className="m3-maintenanceschedules-026"
                        />
                    </div>

                    <select
                        value={statusFilter}
                        onChange={(event) => {
                            setStatusFilter(event.target.value)
                            setPage(1)
                        }}
                        className="m3-maintenanceschedules-027"
                    >
                        <option value="ALL">All statuses</option>
                        <option value="OK">On Track</option>
                        <option value="DUE_SOON">Due Soon</option>
                        <option value="OVERDUE">Overdue</option>
                        <option value="INACTIVE">Inactive</option>
                    </select>
                </div>

                {loading ? (
                    <div className="m3-maintenanceschedules-028">
                        <RefreshCw size={25} className="m3-maintenanceschedules-029" />
                        <p className="m3-maintenanceschedules-030">Loading maintenance schedules...</p>
                    </div>
                ) : visibleSchedules.length === 0 ? (
                    <div className="m3-maintenanceschedules-031">
                        <div className="m3-maintenanceschedules-032">
                            <CalendarDays size={26} />
                        </div>
                        <h3 className="m3-maintenanceschedules-033">
                            No schedules found
                        </h3>
                        <p className="m3-maintenanceschedules-034">
                            {schedules.length === 0
                                ? 'Create a preventive maintenance schedule to get started.'
                                : 'Try changing your search or status filter.'}
                        </p>
                    </div>
                ) : (
                    <>
                        <div className="m3-maintenanceschedules-035">
                            <table className="m3-maintenanceschedules-036">
                                <thead>
                                <tr className="m3-maintenanceschedules-037">
                                    <th className="m3-maintenanceschedules-038">Schedule</th>
                                    <th className="m3-maintenanceschedules-038">Equipment</th>
                                    <th className="m3-maintenanceschedules-038">Repeat</th>
                                    <th className="m3-maintenanceschedules-038">Last Completed</th>
                                    <th className="m3-maintenanceschedules-038">Next Due</th>
                                    <th className="m3-maintenanceschedules-038">Status</th>
                                    <th className="m3-maintenanceschedules-039">Actions</th>
                                </tr>
                                </thead>

                                <tbody className="m3-maintenanceschedules-040">
                                {visibleSchedules.map((schedule) => {
                                    const status = getScheduleStatus(schedule)
                                    const active = schedule.active !== false
                                    const busy = processingId === schedule.id

                                    return (
                                        <tr key={schedule.id} className="m3-maintenanceschedules-041">
                                            <td className="m3-maintenanceschedules-042">
                                                <p className="m3-maintenanceschedules-033">
                                                    {schedule.title || `Schedule #${schedule.id}`}
                                                </p>
                                                <p className="m3-maintenanceschedules-043">
                                                    ID: {schedule.id}
                                                </p>
                                                {schedule.description && (
                                                    <p className="m3-maintenanceschedules-044">
                                                        {schedule.description}
                                                    </p>
                                                )}
                                            </td>

                                            <td className="m3-maintenanceschedules-042">
                                                <p className="m3-maintenanceschedules-045">
                                                    {schedule.resourceName || 'Resource'}
                                                </p>
                                                <p className="m3-maintenanceschedules-043">
                                                    Resource ID: {schedule.resourceId ?? 'N/A'}
                                                </p>
                                            </td>

                                            <td className="m3-maintenanceschedules-046">
                                                Every {schedule.repeatEveryDays} days
                                            </td>

                                            <td className="m3-maintenanceschedules-047">
                                                {formatDate(schedule.lastCompletedAt)}
                                            </td>

                                            <td className="m3-maintenanceschedules-048">
                                                {formatDate(schedule.nextDueAt)}
                                            </td>

                                            <td className="m3-maintenanceschedules-042">
                          <span
                              className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${STATUS_STYLES[status] || STATUS_STYLES.UNKNOWN}`}
                          >
                            {status.replaceAll('_', ' ')}
                          </span>
                                            </td>

                                            <td className="m3-maintenanceschedules-042">
                                                <div className="m3-maintenanceschedules-049">
                                                    <button
                                                        type="button"
                                                        onClick={() => openEditModal(schedule)}
                                                        title="Edit schedule"
                                                        className="m3-maintenanceschedules-050"
                                                    >
                                                        <Edit size={16} />
                                                    </button>

                                                    <button
                                                        type="button"
                                                        onClick={() => toggleSchedule(schedule)}
                                                        disabled={busy}
                                                        title={active ? 'Deactivate schedule' : 'Activate schedule'}
                                                        className={`rounded-lg border border-gray-200 p-2 disabled:opacity-50 ${
                                                            active
                                                                ? 'text-amber-600 hover:bg-amber-50'
                                                                : 'text-green-600 hover:bg-green-50'
                                                        }`}
                                                    >
                                                        {busy ? (
                                                            <RefreshCw size={16} className="m3-maintenanceschedules-051" />
                                                        ) : active ? (
                                                            <ToggleRight size={18} />
                                                        ) : (
                                                            <ToggleLeft size={18} />
                                                        )}
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    )
                                })}
                                </tbody>
                            </table>
                        </div>

                        <div className="m3-maintenanceschedules-052">
                            <p className="m3-maintenanceschedules-053">
                                Showing {(page - 1) * PAGE_SIZE + 1}–
                                {Math.min(page * PAGE_SIZE, filteredSchedules.length)} of{' '}
                                {filteredSchedules.length}
                            </p>

                            <div className="m3-maintenanceschedules-007">
                                <button
                                    type="button"
                                    disabled={page <= 1}
                                    onClick={() => setPage((current) => current - 1)}
                                    className="m3-maintenanceschedules-054"
                                >
                                    Previous
                                </button>

                                <span className="m3-maintenanceschedules-055">
                  Page {page} of {totalPages}
                </span>

                                <button
                                    type="button"
                                    disabled={page >= totalPages}
                                    onClick={() => setPage((current) => current + 1)}
                                    className="m3-maintenanceschedules-054"
                                >
                                    Next
                                </button>
                            </div>
                        </div>
                    </>
                )}
            </section>

            {modalOpen && (
                <div className="m3-maintenanceschedules-056">
                    <div className="m3-maintenanceschedules-057">
                        <div className="m3-maintenanceschedules-058">
                            <div>
                                <h2 className="m3-maintenanceschedules-059">
                                    {editingSchedule ? 'Edit Maintenance Schedule' : 'Create Maintenance Schedule'}
                                </h2>
                                <p className="m3-maintenanceschedules-021">
                                    Configure recurring equipment maintenance.
                                </p>
                            </div>

                            <button
                                type="button"
                                onClick={closeModal}
                                disabled={saving}
                                className="m3-maintenanceschedules-060"
                            >
                                <X size={20} />
                            </button>
                        </div>

                        <form onSubmit={handleSubmit} className="m3-maintenanceschedules-061">
                            {!editingSchedule && (
                                <div>
                                    <label className="m3-maintenanceschedules-062">
                                        Resource ID <span className="m3-maintenanceschedules-063">*</span>
                                    </label>
                                    <input
                                        type="number"
                                        min="1"
                                        value={form.resourceId}
                                        onChange={(event) => updateForm('resourceId', event.target.value)}
                                        placeholder="Enter equipment resource ID"
                                        className="m3-maintenanceschedules-064"
                                        required
                                    />
                                </div>
                            )}

                            {editingSchedule && (
                                <div className="m3-maintenanceschedules-065">
                                    Equipment Resource ID: <strong>{editingSchedule.resourceId}</strong>
                                </div>
                            )}

                            <div>
                                <label className="m3-maintenanceschedules-062">
                                    Schedule Title <span className="m3-maintenanceschedules-063">*</span>
                                </label>
                                <input
                                    type="text"
                                    maxLength={150}
                                    value={form.title}
                                    onChange={(event) => updateForm('title', event.target.value)}
                                    placeholder="e.g. Monthly Microscope Inspection"
                                    className="m3-maintenanceschedules-064"
                                    required
                                />
                            </div>

                            <div>
                                <label className="m3-maintenanceschedules-062">
                                    Description
                                </label>
                                <textarea
                                    rows={3}
                                    maxLength={1000}
                                    value={form.description}
                                    onChange={(event) => updateForm('description', event.target.value)}
                                    placeholder="Describe the preventive maintenance activities..."
                                    className="m3-maintenanceschedules-064"
                                />
                            </div>

                            <div className="m3-maintenanceschedules-066">
                                <div>
                                    <label className="m3-maintenanceschedules-062">
                                        Repeat Every (Days) <span className="m3-maintenanceschedules-063">*</span>
                                    </label>
                                    <input
                                        type="number"
                                        min="1"
                                        step="1"
                                        value={form.repeatEveryDays}
                                        onChange={(event) => updateForm('repeatEveryDays', event.target.value)}
                                        className="m3-maintenanceschedules-064"
                                        required
                                    />
                                </div>

                                <div>
                                    <label className="m3-maintenanceschedules-062">
                                        Next Due Date <span className="m3-maintenanceschedules-063">*</span>
                                    </label>
                                    <input
                                        type="datetime-local"
                                        value={form.nextDueAt}
                                        onChange={(event) => updateForm('nextDueAt', event.target.value)}
                                        className="m3-maintenanceschedules-064"
                                        required
                                    />
                                </div>
                            </div>

                            <div className="m3-maintenanceschedules-067">
                                <button
                                    type="button"
                                    onClick={closeModal}
                                    disabled={saving}
                                    className="m3-maintenanceschedules-068"
                                >
                                    Cancel
                                </button>

                                <button
                                    type="submit"
                                    disabled={saving}
                                    className="m3-maintenanceschedules-069"
                                >
                                    {saving
                                        ? 'Saving...'
                                        : editingSchedule
                                            ? 'Save Changes'
                                            : 'Create Schedule'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    )
}

function StatCard({ title, value, icon, color }) {
    const colors = {
        green: 'm3-tone-green',
        amber: 'm3-tone-amber',
        red: 'm3-tone-red',
        gray: 'm3-tone-gray',
    }

    return (
        <div className="m3-maintenanceschedules-070">
            <div className="m3-maintenanceschedules-071">
                <p className="m3-maintenanceschedules-053">{title}</p>
                <div className={`rounded-lg p-2 ${colors[color]}`}>
                    {icon}
                </div>
            </div>
            <p className="m3-maintenanceschedules-072">{value}</p>
        </div>
    )
}
