
import { useEffect, useState } from 'react'
import api from '../../api/client'
import DocumentPanel from '../../components/t9/DocumentPanel'
import '../../styles/T9.css'

const groups = [
    ['OVERDUE', 'Overdue'],
    ['TODAY', 'Today'],
    ['THIS_WEEK', 'This Week'],
    ['LATER', 'Later']
]

export default function MyTasks() {
    const [tasks, setTasks] = useState([])
    const [summary, setSummary] = useState({})
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [status, setStatus] = useState('')
    const [keyword, setKeyword] = useState('')
    const [departmentId, setDepartmentId] = useState('')
    const [departments, setDepartments] = useState([])
    const [prioritySort, setPrioritySort] = useState('NONE')
    const [page, setPage] = useState(0)
    const [totalPages, setTotalPages] = useState(0)

    const [selected, setSelected] = useState(null)
    const [selectedDocuments, setSelectedDocuments] = useState([])
    const [documentsLoading, setDocumentsLoading] = useState(false)
    const [documentsError, setDocumentsError] = useState('')
    const [documentTask, setDocumentTask] = useState(null)
    const [calibrationTask, setCalibrationTask] = useState(null)
    const [certificateAfterCalibration, setCertificateAfterCalibration] = useState(false)

    const [calibration, setCalibration] = useState({
        calibrationDate: '',
        nextDueDate: '',
        certificateNumber: '',
        certifiedBy: '',
        remarks: ''
    })

    const [saving, setSaving] = useState(false)

    const load = async (nextPage = page) => {
        setLoading(true)
        setError('')

        try {
            const [taskResponse, summaryResponse] = await Promise.all([
                api.get('/technician/my-tasks', {
                    params: {
                        page: nextPage,
                        size: 10,
                        sort: 'scheduledEnd,asc',
                        status: status || undefined,
                        departmentId: departmentId || undefined,
                        keyword: keyword.trim() || undefined
                    }
                }),
                api.get('/technician/my-tasks/summary')
            ])

            setTasks(taskResponse.data?.content || [])
            setPage(taskResponse.data?.number || 0)
            setTotalPages(taskResponse.data?.totalPages || 0)
            setSummary(summaryResponse.data || {})
        } catch (e) {
            setError(
                e.response?.data?.message ||
                e.response?.data?.error ||
                'My Tasks could not be loaded.'
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
                    Array.isArray(data) ? data : data?.content || data?.data || []
                )
            })
            .catch(() => setDepartments([]))
    }, [])

    useEffect(() => {
        load(0)
    }, [status])

    useEffect(() => {
        if (!selected?.workOrderId) {
            setSelectedDocuments([])
            setDocumentsError('')
            return
        }

        let active = true
        setDocumentsLoading(true)
        setDocumentsError('')

        api.get(`/documents/work-order/${selected.workOrderId}`)
            .then(response => {
                if (!active) return
                setSelectedDocuments(Array.isArray(response.data) ? response.data : [])
            })
            .catch(e => {
                if (!active) return
                setSelectedDocuments([])
                setDocumentsError(
                    e.response?.data?.message ||
                    e.response?.data?.error ||
                    'Uploaded documents could not be loaded.'
                )
            })
            .finally(() => {
                if (active) setDocumentsLoading(false)
            })

        return () => { active = false }
    }, [selected?.workOrderId])

    const openTaskDocument = async documentId => {
        setDocumentsError('')
        try {
            const response = await api.get(`/documents/${documentId}/view-link`)
            const url = response.data?.url
            if (!url) throw new Error('Secure document URL was not returned.')
            window.open(url, '_blank', 'noopener,noreferrer')
        } catch (e) {
            setDocumentsError(
                e.response?.data?.message ||
                e.response?.data?.error ||
                e.message ||
                'Unable to open document.'
            )
        }
    }

    const startTask = async task => {
        setError('')

        try {
            await api.patch(`/work-orders/${task.workOrderId}/start`, {})
            await load(page)
        } catch (e) {
            setError(
                e.response?.data?.message ||
                e.response?.data?.error ||
                'Could not start this task.'
            )
        }
    }

    const completeTask = async task => {
        if (task.workOrderType === 'CALIBRATION') {
            setCalibrationTask(task)
            setError('')
            return
        }

        if (!window.confirm('Mark this work order as completed?')) return

        setError('')

        try {
            await api.patch(`/work-orders/${task.workOrderId}/complete`, {})
            await load(page)
        } catch (e) {
            setError(
                e.response?.data?.message ||
                e.response?.data?.error ||
                'Could not complete this task.'
            )
        }
    }

    const submitCalibration = async e => {
        e.preventDefault()

        if (!calibrationTask) return

        if (calibration.nextDueDate < calibration.calibrationDate) {
            setError('Next due date cannot be earlier than calibration date.')
            return
        }

        setSaving(true)
        setError('')

        try {
            await api.patch(
                `/work-orders/${calibrationTask.workOrderId}/complete-calibration`,
                {
                    calibrationDate: calibration.calibrationDate,
                    nextDueDate: calibration.nextDueDate,
                    certificateNumber: calibration.certificateNumber,
                    certifiedBy: calibration.certifiedBy,
                    remarks: calibration.remarks
                }
            )

            // Keep completed task details for certificate upload.
            const completedTask = calibrationTask

            setCalibrationTask(null)

            setCalibration({
                calibrationDate: '',
                nextDueDate: '',
                certificateNumber: '',
                certifiedBy: '',
                remarks: ''
            })

            // Open certificate upload after successful calibration completion.
            setCertificateAfterCalibration(true)
            setDocumentTask(completedTask)

            await load(0)
        } catch (e) {
            setError(
                e.response?.data?.message ||
                e.response?.data?.error ||
                'Calibration completion failed.'
            )
        } finally {
            setSaving(false)
        }
    }

    const closeDocumentModal = () => {
        setDocumentTask(null)
        setCertificateAfterCalibration(false)
    }

    const taskGroup = task => {
        const value = String(task.taskGroup || '').toUpperCase()

        if (value.includes('OVERDUE')) return 'OVERDUE'
        if (value.includes('TODAY')) return 'TODAY'
        if (value.includes('WEEK')) return 'THIS_WEEK'

        return 'LATER'
    }

    const priorityOrder = {
        HIGH: 1,
        URGENT: 1,
        CRITICAL: 1,
        MEDIUM: 2,
        NORMAL: 3,
        LOW: 4
    }

    const displayedTasks = [...tasks].sort((a, b) => {
        if (prioritySort === 'NONE') return 0
        const aPriority = priorityOrder[String(a.priority || '').toUpperCase()] || 5
        const bPriority = priorityOrder[String(b.priority || '').toUpperCase()] || 5
        return aPriority - bPriority
    })

    const countCards = [
        ['Total Tasks', summary.totalTasks],
        ['Open', summary.openTasks],
        ['In Progress', summary.inProgressTasks],
        ['Overdue', summary.overdueTasks],
        ['Completed', summary.completedTasks]
    ]

    return (
        <div className="t9-page">
            <div className="t9-header">
                <div>
                    <h1>My Tasks</h1>
                    <p>View assigned work orders and manage your technician tasks.</p>
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
                {countCards.map(([label, value]) => (
                    <div className="t9-card t9-stat" key={label}>
                        <span>{label}</span>
                        <strong>{value ?? 0}</strong>
                    </div>
                ))}
            </div>

            <div className="t9-card t9-filters">
                <input
                    className="t9-input"
                    placeholder="Search task or equipment"
                    value={keyword}
                    onChange={e => setKeyword(e.target.value)}
                    onKeyDown={e => {
                        if (e.key === 'Enter') load(0)
                    }}
                />

                <select
                    className="t9-select"
                    value={status}
                    onChange={e => setStatus(e.target.value)}
                >
                    <option value="">All statuses</option>
                    <option value="OPEN">Open</option>
                    <option value="ASSIGNED">Assigned</option>
                    <option value="IN_PROGRESS">In Progress</option>
                    <option value="COMPLETED">Completed</option>
                </select>

                <select
                    className="t9-select"
                    value={departmentId}
                    onChange={e => setDepartmentId(e.target.value)}
                >
                    <option value="">All Departments</option>
                    {departments.map(department => (
                        <option key={department.id} value={department.id}>
                            {department.name || department.departmentName || `Department ${department.id}`}
                        </option>
                    ))}
                </select>

                <select
                    className="t9-select"
                    value={prioritySort}
                    onChange={e => setPrioritySort(e.target.value)}
                >
                    <option value="NONE">Default Priority</option>
                    <option value="HIGH">High Priority First</option>
                    <option value="MEDIUM">Medium Priority First</option>
                    <option value="LOW">Low Priority First</option>
                </select>

                <button
                    className="t9-btn"
                    onClick={() => load(0)}
                    disabled={loading}
                >
                    Search
                </button>
            </div>

            {error && <div className="t9-error">{error}</div>}

            {groups.map(([key, label]) => {
                const list = displayedTasks.filter(task => taskGroup(task) === key)

                const summaryKey = {
                    OVERDUE: 'overdueGroup',
                    TODAY: 'todayGroup',
                    THIS_WEEK: 'thisWeekGroup',
                    LATER: 'laterGroup'
                }[key]

                return (
                    <section className="t9-card t9-task-section" key={key}>
                        <div className="t9-section-heading">
                            <h2>{label}</h2>
                            <span className="t9-pill">
                {summary[summaryKey] ?? list.length}
              </span>
                        </div>

                        {list.map(task => (
                            <article className="t9-task" key={task.workOrderId}>
                                <div className="t9-task-main">
                                    <div className="t9-inline">
                                        <strong>{task.title}</strong>

                                        <span className={`t9-pill ${task.overdue ? 'EXPIRED' : ''}`}>
                      {task.status}
                    </span>

                                        {task.overdue && (
                                            <span className="t9-pill EXPIRED">OVERDUE</span>
                                        )}
                                    </div>

                                    <p>{task.description || 'No description provided.'}</p>

                                    <div className="t9-task-meta">
                                        <span>{task.workOrderNumber}</span>
                                        <span>{task.equipmentName}</span>
                                        <span>{task.workOrderType}</span>
                                        <span>{task.departmentName || 'No department'}</span>
                                        <span>Priority: {task.priority || 'NORMAL'}</span>
                                    </div>

                                    <div className="t9-muted">
                                        Scheduled:{' '}
                                        {task.scheduledStart
                                            ? new Date(task.scheduledStart).toLocaleString()
                                            : '—'}
                                        {' → '}
                                        {task.scheduledEnd
                                            ? new Date(task.scheduledEnd).toLocaleString()
                                            : '—'}
                                    </div>
                                </div>

                                <div className="t9-task-actions">
                                    <button
                                        className="t9-btn secondary"
                                        onClick={() => setSelected(task)}
                                    >
                                        Details
                                    </button>

                                    {['OPEN', 'ASSIGNED'].includes(task.status) && (
                                        <button
                                            className="t9-btn"
                                            onClick={() => startTask(task)}
                                        >
                                            Start
                                        </button>
                                    )}

                                    {task.status === 'IN_PROGRESS' && (
                                        <button
                                            className="t9-btn"
                                            onClick={() => completeTask(task)}
                                        >
                                            {task.workOrderType === 'CALIBRATION'
                                                ? 'Complete Calibration'
                                                : 'Complete'}
                                        </button>
                                    )}

                                    <button
                                        className="t9-btn secondary"
                                        onClick={() => {
                                            setCertificateAfterCalibration(false)
                                            setDocumentTask(task)
                                        }}
                                    >
                                        Upload Report
                                    </button>
                                </div>
                            </article>
                        ))}

                        {!loading && !list.length && (
                            <p className="t9-muted">No tasks in this group.</p>
                        )}
                    </section>
                )
            })}

            {loading && (
                <div className="t9-empty">Loading tasks…</div>
            )}

            <div className="t9-pagination">
        <span className="t9-muted">
          Page {page + 1} of {Math.max(totalPages, 1)}
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

            {/* Document Upload Modal */}
            {documentTask && (
                <div className="t9-modal-backdrop">
                    <div className="t9-modal">
                        <div className="t9-header">
                            <h2>
                                {certificateAfterCalibration
                                    ? 'Upload Calibration Certificate'
                                    : 'Work Order Report'}
                            </h2>

                            <button
                                className="t9-btn secondary"
                                onClick={closeDocumentModal}
                            >
                                Close
                            </button>
                        </div>

                        <DocumentPanel
                            resourceId={documentTask.resourceId}
                            workOrderId={documentTask.workOrderId}
                            title={
                                certificateAfterCalibration
                                    ? 'Upload certificate for completed calibration'
                                    : 'Upload report or supporting document'
                            }
                            documentTypes={
                                certificateAfterCalibration
                                    ? ['CALIBRATION_CERTIFICATE']
                                    : ['REPORT', 'CALIBRATION_CERTIFICATE', 'OTHER']
                            }
                        />
                    </div>
                </div>
            )}

            {/* Task Details Modal */}
            {selected && (
                <div className="t9-modal-backdrop">
                    <div className="t9-modal">
                        <div className="t9-header">
                            <h2>Task Details</h2>

                            <button
                                className="t9-btn secondary"
                                onClick={() => setSelected(null)}
                            >
                                Close
                            </button>
                        </div>

                        <p>
                            <strong>Work Order:</strong> {selected.workOrderNumber}
                        </p>

                        <p>
                            <strong>Equipment:</strong> {selected.equipmentName}
                        </p>

                        <p>
                            <strong>Type:</strong> {selected.workOrderType}
                        </p>

                        <p>
                            <strong>Status:</strong> {selected.status}
                        </p>

                        <p>
                            <strong>Description:</strong> {selected.description || '—'}
                        </p>

                        <p>
                            <strong>Priority:</strong> {selected.priority || '—'}
                        </p>

                        <div className="t9-document-list">
                            <div className="t9-section-heading">
                                <h3>Uploaded Documents</h3>
                                <span className="t9-pill">{selectedDocuments.length}</span>
                            </div>

                            {documentsLoading && <div className="t9-empty">Loading documents...</div>}
                            {documentsError && <div className="t9-error">{documentsError}</div>}

                            {!documentsLoading && !documentsError && selectedDocuments.length === 0 && (
                                <div className="t9-empty">No documents uploaded for this work order.</div>
                            )}

                            {!documentsLoading && selectedDocuments.map(doc => (
                                <div className="t9-doc" key={doc.id}>
                                    <div className="t9-document-info">
                                        <strong>{doc.originalFileName || 'Untitled document'}</strong>
                                        <div className="t9-muted">
                                            {doc.documentType?.replaceAll('_', ' ') || 'DOCUMENT'}
                                            {' · '}
                                            {doc.fileSize ? `${(doc.fileSize / 1024).toFixed(1)} KB` : '—'}
                                        </div>
                                        <div className="t9-muted">
                                            Uploaded by: {doc.uploadedBy || '—'}
                                            {' · '}
                                            {doc.uploadedAt ? new Date(doc.uploadedAt).toLocaleString('en-IN') : '—'}
                                        </div>
                                    </div>
                                    <button
                                        type="button"
                                        className="t9-btn secondary"
                                        onClick={() => openTaskDocument(doc.id)}
                                    >
                                        View / Download
                                    </button>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {/* Calibration Completion Modal */}
            {calibrationTask && (
                <div className="t9-modal-backdrop">
                    <form className="t9-modal" onSubmit={submitCalibration}>
                        <div className="t9-header">
                            <h2>Complete Calibration</h2>

                            <button
                                type="button"
                                className="t9-btn secondary"
                                onClick={() => setCalibrationTask(null)}
                            >
                                Close
                            </button>
                        </div>

                        <p>{calibrationTask.equipmentName}</p>

                        <div className="t9-form-grid">
                            <div className="t9-field">
                                <label>Calibration date *</label>

                                <input
                                    className="t9-input"
                                    type="date"
                                    required
                                    value={calibration.calibrationDate}
                                    onChange={e =>
                                        setCalibration({
                                            ...calibration,
                                            calibrationDate: e.target.value
                                        })
                                    }
                                />
                            </div>

                            <div className="t9-field">
                                <label>Next due date *</label>

                                <input
                                    className="t9-input"
                                    type="date"
                                    required
                                    min={calibration.calibrationDate || undefined}
                                    value={calibration.nextDueDate}
                                    onChange={e =>
                                        setCalibration({
                                            ...calibration,
                                            nextDueDate: e.target.value
                                        })
                                    }
                                />
                            </div>

                            <div className="t9-field">
                                <label>Certificate number</label>

                                <input
                                    className="t9-input"
                                    value={calibration.certificateNumber}
                                    onChange={e =>
                                        setCalibration({
                                            ...calibration,
                                            certificateNumber: e.target.value
                                        })
                                    }
                                />
                            </div>

                            <div className="t9-field">
                                <label>Certified by</label>

                                <input
                                    className="t9-input"
                                    value={calibration.certifiedBy}
                                    onChange={e =>
                                        setCalibration({
                                            ...calibration,
                                            certifiedBy: e.target.value
                                        })
                                    }
                                />
                            </div>
                        </div>

                        <div className="t9-field">
                            <label>Remarks</label>

                            <textarea
                                className="t9-textarea"
                                value={calibration.remarks}
                                onChange={e =>
                                    setCalibration({
                                        ...calibration,
                                        remarks: e.target.value
                                    })
                                }
                            />
                        </div>

                        <button className="t9-btn" disabled={saving}>
                            {saving ? 'Saving…' : 'Complete Calibration'}
                        </button>
                    </form>
                </div>
            )}
        </div>
    )
}