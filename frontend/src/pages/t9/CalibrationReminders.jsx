import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../api/client'
import DocumentPanel from '../../components/t9/DocumentPanel'
import '../../styles/T9.css'

export default function CalibrationReminders() {
    const [rows, setRows] = useState([])
    const [resources, setResources] = useState([])
    const [workOrders, setWorkOrders] = useState([])

    const [filter, setFilter] = useState('')
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState('')
    const [message, setMessage] = useState('')

    const [showCreate, setShowCreate] = useState(false)
    const [documentResource, setDocumentResource] = useState(null)

    const [form, setForm] = useState({
        resourceId: '',
        title: 'Calibration Renewal',
        description: '',
        scheduledStart: '',
        scheduledEnd: ''
    })

    const savedUser = JSON.parse(localStorage.getItem('user') || '{}')

    const role = String(
        savedUser?.role?.name ||
        savedUser?.role ||
        savedUser?.roleName ||
        ''
    ).replace('ROLE_', '').toUpperCase()

    const isManager = role === 'LAB_MANAGER'

    const getArray = data => {
        if (Array.isArray(data)) return data
        if (Array.isArray(data?.content)) return data.content
        if (Array.isArray(data?.data)) return data.data
        return []
    }

    const getApiError = (e, apiName) => {
        return `${apiName}: ${
            e.response?.data?.message ||
            e.response?.data?.error ||
            e.message ||
            'Request failed'
        }`
    }

    // LOAD CALIBRATION REMINDERS
    const load = async () => {
        setLoading(true)
        setError('')
        setMessage('')

        let reminderData = []
        let resourceData = []
        let workOrderData = []

        const errors = []

        // 1. Reminders API
        try {
            const reminderResponse = await api.get(
                '/calibrations/reminders',
                {
                    params: {
                        status: filter || undefined
                    }
                }
            )

            reminderData = getArray(reminderResponse.data)
        } catch (e) {
            errors.push(getApiError(e, 'Reminders API'))
        }

        if (isManager) {
            // 2. Resources API
            try {
                const resourceResponse = await api.get('/resources')

                resourceData = getArray(resourceResponse.data)
                setResources(resourceData)
            } catch (e) {
                errors.push(getApiError(e, 'Resources API'))
                setResources([])
            }

            // 3. Work Orders API
            try {
                const workOrderResponse = await api.get('/work-orders', {
                    params: {
                        type: 'CALIBRATION',
                        page: 0,
                        size: 100,
                        sort: 'createdAt,desc'
                    }
                })

                workOrderData = getArray(workOrderResponse.data)
                setWorkOrders(workOrderData)
            } catch (e) {
                errors.push(getApiError(e, 'Work Orders API'))
                setWorkOrders([])
            }

            // 4. Merge resources with reminder records
            const mergedRows = resourceData.map(resource => {
                const reminder = reminderData.find(
                    item =>
                        Number(item.resourceId) === Number(resource.id)
                )

                return {
                    ...(reminder || {}),

                    calibrationId:
                        reminder?.calibrationId ??
                        `resource-${resource.id}`,

                    resourceId: resource.id,

                    equipmentName:
                        reminder?.equipmentName ||
                        resource.name ||
                        resource.resourceName ||
                        `Resource #${resource.id}`,

                    calibrationDate:
                        reminder?.calibrationDate || null,

                    nextDueDate:
                        reminder?.nextDueDate || null,

                    daysUntilDue:
                        reminder?.daysUntilDue ?? null,

                    reminderStatus:
                        reminder?.reminderStatus || 'NO_CALIBRATION'
                }
            })

            // Add reminder records not present in resource response
            const resourceIds = new Set(
                resourceData.map(item => Number(item.id))
            )

            reminderData.forEach(reminder => {
                if (!resourceIds.has(Number(reminder.resourceId))) {
                    mergedRows.push(reminder)
                }
            })

            // Apply selected filter
            const filteredRows = filter
                ? mergedRows.filter(
                    item => item.reminderStatus === filter
                )
                : mergedRows

            setRows(filteredRows)
        } else {
            setRows(reminderData)
        }

        if (errors.length > 0) {
            setError(errors.join(' | '))
        }

        setLoading(false)
    }

    useEffect(() => {
        load()
    }, [filter])

    // GET WORK ORDERS FOR A RESOURCE
    const relatedFor = resourceId =>
        workOrders.filter(
            workOrder =>
                Number(workOrder.resourceId) === Number(resourceId)
        )

    // CREATE CALIBRATION WORK ORDER
    const createWorkOrder = async event => {
        event.preventDefault()

        if (!form.resourceId) {
            setError('Please select equipment.')
            return
        }

        if (
            form.scheduledStart &&
            form.scheduledEnd &&
            form.scheduledEnd < form.scheduledStart
        ) {
            setError(
                'Scheduled end cannot be earlier than scheduled start.'
            )
            return
        }

        setSaving(true)
        setError('')
        setMessage('')

        try {
            await api.post('/work-orders', {
                resourceId: Number(form.resourceId),
                title: form.title.trim(),
                description: form.description.trim(),
                type: 'CALIBRATION',
                scheduledStart: form.scheduledStart || null,
                scheduledEnd: form.scheduledEnd || null
            })

            setShowCreate(false)

            setForm({
                resourceId: '',
                title: 'Calibration Renewal',
                description: '',
                scheduledStart: '',
                scheduledEnd: ''
            })

            setMessage(
                'Calibration Work Order created successfully.'
            )

            await load()
        } catch (e) {
            setError(
                e.response?.data?.message ||
                e.response?.data?.error ||
                'Could not create Calibration Work Order.'
            )
        } finally {
            setSaving(false)
        }
    }

    return (
        <div className="t9-page">

            {/* HEADER */}
            <div className="t9-header">
                <div>
                    <h1>Calibration Reminders</h1>

                    <p>
                        Monitor upcoming and expired equipment calibration renewals.
                    </p>
                </div>

                <div className="t9-actions">
                    <button
                        className="t9-btn secondary"
                        onClick={load}
                        disabled={loading}
                    >
                        Refresh
                    </button>

                    {isManager && (
                        <button
                            className="t9-btn"
                            onClick={() => {
                                setError('')
                                setShowCreate(true)
                            }}
                        >
                            Create Calibration Work Order
                        </button>
                    )}
                </div>
            </div>

            {/* STATISTICS */}
            <div className="t9-stat-grid">

                <div className="t9-card t9-stat">
                    <span>Total Equipment</span>
                    <strong>{rows.length}</strong>
                </div>

                <div className="t9-card t9-stat">
                    <span>Due Soon</span>

                    <strong>
                        {
                            rows.filter(
                                r => r.reminderStatus === 'DUE_SOON'
                            ).length
                        }
                    </strong>
                </div>

                <div className="t9-card t9-stat">
                    <span>Expired</span>

                    <strong>
                        {
                            rows.filter(
                                r => r.reminderStatus === 'EXPIRED'
                            ).length
                        }
                    </strong>
                </div>

            </div>

            {/* FILTER */}
            <div className="t9-card t9-filters">

                <select
                    className="t9-select"
                    value={filter}
                    onChange={e => setFilter(e.target.value)}
                >
                    <option value="">All Equipment</option>
                    <option value="NO_CALIBRATION">
                        No Calibration
                    </option>
                    <option value="DUE_SOON">
                        Due Soon
                    </option>
                    <option value="EXPIRED">
                        Expired
                    </option>
                    <option value="VALID">
                        Valid
                    </option>
                </select>

                <span className="t9-muted">
                    {rows.length} equipment record(s) found
                </span>

            </div>

            {/* ALERTS */}
            {error && (
                <div className="t9-error">
                    {error}
                </div>
            )}

            {message && (
                <div className="t9-success">
                    {message}
                </div>
            )}

            {/* REMINDERS TABLE */}
            <div className="t9-card t9-table-wrap">

                <table className="t9-table">

                    <thead>
                    <tr>
                        <th>Equipment</th>
                        <th>Last Calibration</th>
                        <th>Next Due Date</th>
                        <th>Days Remaining</th>
                        <th>Reminder Status</th>
                        <th>Work Orders</th>
                        <th>Documents</th>
                    </tr>
                    </thead>

                    <tbody>

                    {rows.map(record => {
                        const related = relatedFor(
                            record.resourceId
                        )

                        const hasDueDays =
                            record.daysUntilDue !== null &&
                            record.daysUntilDue !== undefined

                        return (
                            <tr key={record.calibrationId}>

                                <td>
                                    <strong>
                                        {record.equipmentName}
                                    </strong>

                                    <div className="t9-muted">
                                        Resource #{record.resourceId}
                                    </div>
                                </td>

                                <td>
                                    {record.calibrationDate || '—'}
                                </td>

                                <td>
                                    {record.nextDueDate || '—'}
                                </td>

                                <td>
                                    {!hasDueDays
                                        ? '—'
                                        : record.daysUntilDue < 0
                                            ? `${Math.abs(record.daysUntilDue)} days overdue`
                                            : `${record.daysUntilDue} days`
                                    }
                                </td>

                                <td>
                                        <span
                                            className={`t9-pill ${record.reminderStatus}`}
                                        >
                                            {record.reminderStatus?.replaceAll(
                                                '_',
                                                ' '
                                            )}
                                        </span>
                                </td>

                                <td>
                                    {related.length > 0 ? (
                                        related.map(workOrder => (
                                            <div key={workOrder.id}>

                                                <Link to="/work-orders">
                                                    {
                                                        workOrder.workOrderNumber ||
                                                        `Work Order #${workOrder.id}`
                                                    }
                                                </Link>

                                                <div className="t9-muted">
                                                    {workOrder.status}
                                                </div>

                                            </div>
                                        ))
                                    ) : (
                                        <span className="t9-muted">
                                                No work order found
                                            </span>
                                    )}
                                </td>

                                <td>
                                    <button
                                        className="t9-btn secondary"
                                        onClick={() =>
                                            setDocumentResource(
                                                record.resourceId
                                            )
                                        }
                                    >
                                        Documents
                                    </button>
                                </td>

                            </tr>
                        )
                    })}

                    {!loading && rows.length === 0 && (
                        <tr>
                            <td
                                colSpan="7"
                                className="t9-empty"
                            >
                                No equipment records found.
                            </td>
                        </tr>
                    )}

                    </tbody>

                </table>

                {loading && (
                    <div className="t9-empty">
                        Loading calibration reminders...
                    </div>
                )}

            </div>

            {/* CREATE WORK ORDER MODAL */}
            {showCreate && (
                <div className="t9-modal-backdrop">

                    <form
                        className="t9-modal"
                        onSubmit={createWorkOrder}
                    >

                        <div className="t9-header">

                            <h2>
                                Create Calibration Work Order
                            </h2>

                            <button
                                type="button"
                                className="t9-btn secondary"
                                onClick={() => setShowCreate(false)}
                            >
                                Close
                            </button>

                        </div>

                        {/* EQUIPMENT */}
                        <div className="t9-field">

                            <label>Equipment *</label>

                            <select
                                className="t9-select"
                                required
                                value={form.resourceId}
                                onChange={e =>
                                    setForm({
                                        ...form,
                                        resourceId: e.target.value
                                    })
                                }
                            >

                                <option value="">
                                    Select equipment
                                </option>

                                {resources.map(resource => (
                                    <option
                                        key={resource.id}
                                        value={resource.id}
                                    >
                                        {
                                            resource.name ||
                                            resource.resourceName
                                        } (#{resource.id})
                                    </option>
                                ))}

                            </select>

                            {!resources.length && (
                                <span className="t9-muted">
                                    No equipment available for your institution.
                                </span>
                            )}

                        </div>

                        {/* TITLE */}
                        <div className="t9-field">

                            <label>Work Order Title *</label>

                            <input
                                className="t9-input"
                                required
                                maxLength={200}
                                value={form.title}
                                onChange={e =>
                                    setForm({
                                        ...form,
                                        title: e.target.value
                                    })
                                }
                            />

                        </div>

                        {/* DESCRIPTION */}
                        <div className="t9-field">

                            <label>Description</label>

                            <textarea
                                className="t9-textarea"
                                maxLength={2000}
                                value={form.description}
                                onChange={e =>
                                    setForm({
                                        ...form,
                                        description: e.target.value
                                    })
                                }
                                placeholder="Enter calibration work details..."
                            />

                        </div>

                        {/* SCHEDULE */}
                        <div className="t9-form-grid">

                            <div className="t9-field">

                                <label>Scheduled Start</label>

                                <input
                                    className="t9-input"
                                    type="datetime-local"
                                    value={form.scheduledStart}
                                    onChange={e =>
                                        setForm({
                                            ...form,
                                            scheduledStart: e.target.value
                                        })
                                    }
                                />

                            </div>

                            <div className="t9-field">

                                <label>Scheduled End</label>

                                <input
                                    className="t9-input"
                                    type="datetime-local"
                                    min={
                                        form.scheduledStart || undefined
                                    }
                                    value={form.scheduledEnd}
                                    onChange={e =>
                                        setForm({
                                            ...form,
                                            scheduledEnd: e.target.value
                                        })
                                    }
                                />

                            </div>

                        </div>

                        <button
                            className="t9-btn"
                            disabled={saving || !resources.length}
                        >
                            {saving
                                ? 'Creating...'
                                : 'Create Work Order'
                            }
                        </button>

                    </form>

                </div>
            )}

            {/* DOCUMENT MODAL */}
            {documentResource && (
                <div className="t9-modal-backdrop">

                    <div className="t9-modal">

                        <div className="t9-header">

                            <h2>Equipment Documents</h2>

                            <button
                                className="t9-btn secondary"
                                onClick={() => setDocumentResource(null)}
                            >
                                Close
                            </button>

                        </div>

                        <DocumentPanel
                            resourceId={documentResource}
                            title="Certificates, manuals and reports"
                        />

                    </div>

                </div>
            )}

        </div>
    )
}