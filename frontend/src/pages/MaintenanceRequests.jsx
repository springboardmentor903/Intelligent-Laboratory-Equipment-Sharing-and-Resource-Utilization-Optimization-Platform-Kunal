import '../styles/MaintenanceRequests.css'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
    AlertCircle,
    CheckCircle2,
    Clock,
    RefreshCw,
    Search,
    Send,
    Wrench,
    XCircle,
} from 'lucide-react'
import api from '../api/client'
import { useAuth } from '../context/AuthContext'


const PAGE_SIZE = 8

const REQUESTER_ROLES = [
    'RESEARCHER_STUDENT',
    'DEPARTMENT_HEAD',
    'LAB_TECHNICIAN',
]

const STATUS_STYLES = {
    PENDING: 'm3-request-status-pending',
    APPROVED: 'm3-request-status-approved',
    REJECTED: 'm3-request-status-rejected',
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
    if (!value) return '—'

    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value

    return date.toLocaleString('en-IN', {
        dateStyle: 'medium',
        timeStyle: 'short',
    })
}

function getResourceName(request) {
    return (
        request?.resource?.name ||
        request?.resourceName ||
        `Resource #${request?.resource?.id || request?.resourceId || 'N/A'}`
    )
}

function getStatusClass(status) {
    return STATUS_STYLES[String(status || '').toUpperCase()] ||
        'bg-gray-100 text-gray-700'
}

export default function MaintenanceRequests() {
    const [searchParams] = useSearchParams()
    const { user } = useAuth()

    const rawRole =
        user?.role?.name ??
        user?.role?.roleName ??
        user?.roleName ??
        (typeof user?.role === 'string' ? user.role : null) ??
        user?.authorities?.[0]?.authority ??
        user?.authorities?.[0] ??
        ''

    const role = String(rawRole).replace(/^ROLE_/, '').trim().toUpperCase()

    const isManager = role === 'LAB_MANAGER'
    const isInstitutionAdmin = role === 'INSTITUTION_ADMIN'
    const canCreateRequest = REQUESTER_ROLES.includes(role)

    const [requests, setRequests] = useState([])
    const [loading, setLoading] = useState(true)
    const [submitting, setSubmitting] = useState(false)
    const [processingId, setProcessingId] = useState(null)

    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')

    const [resourceId, setResourceId] = useState(
        searchParams.get('resourceId') || ''
    )
    const [title, setTitle] = useState('')
    const [description, setDescription] = useState('')
    const [priority, setPriority] = useState('MEDIUM')

    const [search, setSearch] = useState('')
    const [statusFilter, setStatusFilter] = useState('ALL')
    const [page, setPage] = useState(1)

    const [actionRequest, setActionRequest] = useState(null)
    const [actionType, setActionType] = useState('')
    const [remarks, setRemarks] = useState('')

    // Keep Resource ID synchronized with the equipment page URL.
    useEffect(() => {
        const urlResourceId = searchParams.get('resourceId')

        if (urlResourceId) {
            setResourceId(urlResourceId)
        }
    }, [searchParams])

    const loadRequests = useCallback(async () => {
        setLoading(true)
        setError('')

        try {
            const endpoint = isManager
                ? '/maintenance/requests/queue'
                : '/maintenance/requests/my'

            const response = await api.get(endpoint, {
                params: {
                    page: 0,
                    size: 100,
                    sortBy: 'createdAt',
                    direction: 'DESC',
                },
            })

            setRequests(getItems(response))
        } catch (err) {
            setError(
                getErrorMessage(
                    err,
                    'Maintenance requests could not be loaded.'
                )
            )
        } finally {
            setLoading(false)
        }
    }, [isManager])

    useEffect(() => {
        loadRequests()
    }, [loadRequests])

    const filteredRequests = useMemo(() => {
        const query = search.trim().toLowerCase()

        return requests.filter((request) => {
            const status = String(request.status || '').toUpperCase()

            const matchesStatus =
                statusFilter === 'ALL' || status === statusFilter

            const matchesSearch =
                !query ||
                String(request.id || '').includes(query) ||
                getResourceName(request).toLowerCase().includes(query) ||
                String(request.title || '').toLowerCase().includes(query) ||
                String(request.description || '').toLowerCase().includes(query)

            return matchesStatus && matchesSearch
        })
    }, [requests, search, statusFilter])

    const totalPages = Math.max(
        1,
        Math.ceil(filteredRequests.length / PAGE_SIZE)
    )

    const visibleRequests = filteredRequests.slice(
        (page - 1) * PAGE_SIZE,
        page * PAGE_SIZE
    )

    const counts = useMemo(() => {
        return requests.reduce(
            (result, request) => {
                const status = String(request.status || '').toUpperCase()

                if (status === 'PENDING') result.pending += 1
                if (status === 'APPROVED') result.approved += 1
                if (status === 'REJECTED') result.rejected += 1

                return result
            },
            { pending: 0, approved: 0, rejected: 0 }
        )
    }, [requests])

    const handleSubmit = async (event) => {
        event.preventDefault()

        setError('')
        setSuccess('')

        if (!resourceId.trim() || Number(resourceId) <= 0) {
            setError('Please enter a valid Resource ID.')
            return
        }

        if (!title.trim()) {
            setError('Please enter a request title.')
            return
        }

        if (!description.trim()) {
            setError('Please describe the maintenance problem.')
            return
        }

        if (title.trim().length > 150) {
            setError('Title cannot exceed 150 characters.')
            return
        }

        if (description.trim().length > 1000) {
            setError('Description cannot exceed 1000 characters.')
            return
        }

        setSubmitting(true)

        try {
            const payload = {
                resourceId: Number(resourceId),
                title: title.trim(),
                description: description.trim(),
                priority,
            }

            await api.post('/maintenance/requests', payload)

            setTitle('')
            setDescription('')
            setPriority('MEDIUM')

            // Keep Resource ID when the user came from an equipment card.
            setSuccess('Maintenance request submitted successfully.')
            await loadRequests()
        } catch (err) {
            setError(
                getErrorMessage(
                    err,
                    'Request could not be submitted. Please try again.'
                )
            )
        } finally {
            setSubmitting(false)
        }
    }

    const openAction = (request, type) => {
        setActionRequest(request)
        setActionType(type)
        setRemarks('')
        setError('')
        setSuccess('')
    }

    const closeAction = () => {
        if (processingId !== null) return

        setActionRequest(null)
        setActionType('')
        setRemarks('')
    }

    const handleReview = async () => {
        if (!actionRequest) return

        setError('')
        setSuccess('')

        if (actionType === 'REJECT' && !remarks.trim()) {
            setError('Please provide a reason for rejecting this request.')
            return
        }

        if (remarks.trim().length > 1000) {
            setError('Remarks cannot exceed 1000 characters.')
            return
        }

        setProcessingId(actionRequest.id)

        try {
            const endpoint =
                actionType === 'APPROVE'
                    ? `/maintenance/requests/${actionRequest.id}/approve`
                    : `/maintenance/requests/${actionRequest.id}/reject`

            await api.post(endpoint, {
                remarks: remarks.trim(),
            })

            setSuccess(
                actionType === 'APPROVE'
                    ? 'Request approved. A corrective work order has been created.'
                    : 'Maintenance request rejected.'
            )

            setActionRequest(null)
            setActionType('')
            setRemarks('')

            await loadRequests()
        } catch (err) {
            setError(
                getErrorMessage(
                    err,
                    'Unable to update this request. Please try again.'
                )
            )
        } finally {
            setProcessingId(null)
        }
    }

    const resetFilters = () => {
        setSearch('')
        setStatusFilter('ALL')
        setPage(1)
    }

    return (
        <div className="m3-maintenancerequests-001">
            <div className="m3-maintenancerequests-002">
                <div>
                    <div className="m3-maintenancerequests-003">
                        <div className="m3-maintenancerequests-004">
                            <Wrench size={23} />
                        </div>
                        <h1 className="m3-maintenancerequests-005">
                            Maintenance Requests
                        </h1>
                    </div>

                    <p className="m3-maintenancerequests-006">
                        {isManager
                            ? 'Review and manage maintenance requests from your institution.'
                            : isInstitutionAdmin
                                ? 'View maintenance requests available to your account.'
                                : 'Report equipment problems and track your maintenance requests.'}
                    </p>
                </div>

                <button
                    type="button"
                    onClick={loadRequests}
                    disabled={loading}
                    className="m3-maintenancerequests-007"
                >
                    <RefreshCw size={16} className={loading ? 'm3-spin' : ''} />
                    Refresh
                </button>
            </div>

            {error && <ErrorBanner message={error} />}
            {success && (
                <div className="m3-maintenancerequests-008">
                    <CheckCircle2 size={19} className="m3-maintenancerequests-009" />
                    <span>{success}</span>
                </div>
            )}

            <div className="m3-maintenancerequests-010">
                <StatCard
                    title="Pending"
                    value={counts.pending}
                    icon={<Clock size={20} />}
                    color="amber"
                />
                <StatCard
                    title="Approved"
                    value={counts.approved}
                    icon={<CheckCircle2 size={20} />}
                    color="green"
                />
                <StatCard
                    title="Rejected"
                    value={counts.rejected}
                    icon={<XCircle size={20} />}
                    color="red"
                />
            </div>

            {canCreateRequest && (
                <section className="m3-maintenancerequests-011">
                    <div className="m3-maintenancerequests-012">
                        <h2 className="m3-maintenancerequests-013">
                            Report an Equipment Problem
                        </h2>
                        <p className="m3-maintenancerequests-014">
                            Submit a maintenance request for laboratory equipment.
                        </p>
                    </div>

                    <form onSubmit={handleSubmit} className="m3-maintenancerequests-015">
                        <div className="m3-maintenancerequests-016">
                            <div>
                                <label className="m3-maintenancerequests-017">
                                    Resource ID *
                                </label>
                                <input
                                    type="number"
                                    min="1"
                                    value={resourceId}
                                    onChange={(event) => setResourceId(event.target.value)}
                                    placeholder="Equipment Resource ID"
                                    className="m3-maintenancerequests-018"
                                    required
                                />
                                {searchParams.get('resourceId') && (
                                    <p className="m3-maintenancerequests-019">
                                        Resource selected from Equipment catalog.
                                    </p>
                                )}
                            </div>

                            <div>
                                <label className="m3-maintenancerequests-017">
                                    Priority *
                                </label>
                                <select
                                    value={priority}
                                    onChange={(event) => setPriority(event.target.value)}
                                    className="m3-maintenancerequests-020"
                                >
                                    <option value="LOW">Low</option>
                                    <option value="MEDIUM">Medium</option>
                                    <option value="HIGH">High</option>
                                    <option value="CRITICAL">Critical</option>
                                </select>
                            </div>
                        </div>

                        <div>
                            <label className="m3-maintenancerequests-017">
                                Request Title *
                            </label>
                            <input
                                type="text"
                                maxLength={150}
                                value={title}
                                onChange={(event) => setTitle(event.target.value)}
                                placeholder="e.g. Microscope not powering on"
                                className="m3-maintenancerequests-018"
                                required
                            />
                        </div>

                        <div>
                            <div className="m3-maintenancerequests-021">
                                <label className="m3-maintenancerequests-022">
                                    Problem Description *
                                </label>
                                <span className="m3-maintenancerequests-023">
                  {description.length}/1000
                </span>
                            </div>

                            <textarea
                                rows={4}
                                maxLength={1000}
                                value={description}
                                onChange={(event) => setDescription(event.target.value)}
                                placeholder="Describe the equipment problem, damage or maintenance required..."
                                className="m3-maintenancerequests-024"
                                required
                            />
                        </div>

                        <div className="m3-maintenancerequests-025">
                            <button
                                type="submit"
                                disabled={submitting}
                                className="m3-maintenancerequests-026"
                            >
                                <Send size={16} />
                                {submitting ? 'Submitting...' : 'Submit Request'}
                            </button>
                        </div>
                    </form>
                </section>
            )}

            <section className="m3-maintenancerequests-011">
                <div className="m3-maintenancerequests-027">
                    <div>
                        <h2 className="m3-maintenancerequests-013">
                            {isManager ? 'Request Review Queue' : isInstitutionAdmin ? 'Maintenance Requests' : 'My Requests'}
                        </h2>
                        <p className="m3-maintenancerequests-014">
                            {filteredRequests.length} request(s) found
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={resetFilters}
                        className="m3-maintenancerequests-028"
                    >
                        Clear filters
                    </button>
                </div>

                <div className="m3-maintenancerequests-029">
                    <div className="m3-maintenancerequests-030">
                        <Search
                            size={18}
                            className="m3-maintenancerequests-031"
                        />
                        <input
                            type="text"
                            value={search}
                            onChange={(event) => {
                                setSearch(event.target.value)
                                setPage(1)
                            }}
                            placeholder="Search by resource, title or request ID..."
                            className="m3-maintenancerequests-032"
                        />
                    </div>

                    <select
                        value={statusFilter}
                        onChange={(event) => {
                            setStatusFilter(event.target.value)
                            setPage(1)
                        }}
                        className="m3-maintenancerequests-033"
                    >
                        <option value="ALL">All statuses</option>
                        <option value="PENDING">Pending</option>
                        <option value="APPROVED">Approved</option>
                        <option value="REJECTED">Rejected</option>
                    </select>
                </div>

                {loading ? (
                    <div className="m3-maintenancerequests-034">
                        <RefreshCw size={25} className="m3-maintenancerequests-035" />
                        <p className="m3-maintenancerequests-036">Loading maintenance requests...</p>
                    </div>
                ) : visibleRequests.length === 0 ? (
                    <div className="m3-maintenancerequests-037">
                        <div className="m3-maintenancerequests-038">
                            <Wrench size={26} />
                        </div>
                        <h3 className="m3-maintenancerequests-039">
                            No maintenance requests found
                        </h3>
                        <p className="m3-maintenancerequests-040">
                            {requests.length === 0
                                ? 'There are no requests to display yet.'
                                : 'Try changing your search or status filter.'}
                        </p>
                    </div>
                ) : (
                    <>
                        <div className="m3-maintenancerequests-041">
                            <table className="m3-maintenancerequests-042">
                                <thead>
                                <tr className="m3-maintenancerequests-043">
                                    <th className="m3-maintenancerequests-044">Request</th>
                                    <th className="m3-maintenancerequests-044">Resource</th>
                                    <th className="m3-maintenancerequests-044">Title / Description</th>
                                    <th className="m3-maintenancerequests-044">Priority</th>
                                    <th className="m3-maintenancerequests-044">Submitted</th>
                                    <th className="m3-maintenancerequests-044">Status</th>
                                    {isManager && (
                                        <th className="m3-maintenancerequests-045">
                                            Actions
                                        </th>
                                    )}
                                </tr>
                                </thead>

                                <tbody className="m3-maintenancerequests-046">
                                {visibleRequests.map((request) => {
                                    const status = String(request.status || '').toUpperCase()
                                    const canReview = status === 'PENDING'

                                    return (
                                        <tr key={request.id} className="m3-maintenancerequests-047">
                                            <td className="m3-maintenancerequests-048">
                                                #{request.id}
                                            </td>

                                            <td className="m3-maintenancerequests-049">
                          <span className="m3-maintenancerequests-050">
                            {getResourceName(request)}
                          </span>
                                            </td>

                                            <td className="m3-maintenancerequests-051">
                                                <p className="m3-maintenancerequests-050">
                                                    {request.title || 'Maintenance request'}
                                                </p>
                                                <p className="m3-maintenancerequests-052">
                                                    {request.description || 'No description'}
                                                </p>
                                            </td>

                                            <td className="m3-maintenancerequests-053">
                                                {request.priority || 'MEDIUM'}
                                            </td>

                                            <td className="m3-maintenancerequests-054">
                                                {formatDate(request.createdAt)}
                                            </td>

                                            <td className="m3-maintenancerequests-049">
                          <span
                              className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${getStatusClass(status)}`}
                          >
                            {status || 'UNKNOWN'}
                          </span>
                                            </td>

                                            {isManager && (
                                                <td className="m3-maintenancerequests-049">
                                                    {canReview ? (
                                                        <div className="m3-maintenancerequests-055">
                                                            <button
                                                                type="button"
                                                                onClick={() => openAction(request, 'APPROVE')}
                                                                className="m3-maintenancerequests-056"
                                                            >
                                                                Approve
                                                            </button>
                                                            <button
                                                                type="button"
                                                                onClick={() => openAction(request, 'REJECT')}
                                                                className="m3-maintenancerequests-057"
                                                            >
                                                                Reject
                                                            </button>
                                                        </div>
                                                    ) : (
                                                        <span className="m3-maintenancerequests-058">
                                No action
                              </span>
                                                    )}
                                                </td>
                                            )}
                                        </tr>
                                    )
                                })}
                                </tbody>
                            </table>
                        </div>

                        <div className="m3-maintenancerequests-059">
                            <p className="m3-maintenancerequests-060">
                                Showing {(page - 1) * PAGE_SIZE + 1}–
                                {Math.min(page * PAGE_SIZE, filteredRequests.length)} of{' '}
                                {filteredRequests.length}
                            </p>

                            <div className="m3-maintenancerequests-003">
                                <button
                                    type="button"
                                    disabled={page <= 1}
                                    onClick={() => setPage((current) => current - 1)}
                                    className="m3-maintenancerequests-061"
                                >
                                    Previous
                                </button>

                                <span className="m3-maintenancerequests-062">
                  Page {page} of {totalPages}
                </span>

                                <button
                                    type="button"
                                    disabled={page >= totalPages}
                                    onClick={() => setPage((current) => current + 1)}
                                    className="m3-maintenancerequests-061"
                                >
                                    Next
                                </button>
                            </div>
                        </div>
                    </>
                )}
            </section>

            {actionRequest && (
                <div className="m3-maintenancerequests-063">
                    <div className="m3-maintenancerequests-064">
                        <div className="m3-maintenancerequests-065">
                            <h3 className="m3-maintenancerequests-066">
                                {actionType === 'APPROVE'
                                    ? 'Approve Maintenance Request'
                                    : 'Reject Maintenance Request'}
                            </h3>

                            <button
                                type="button"
                                onClick={closeAction}
                                disabled={processingId !== null}
                                className="m3-maintenancerequests-067"
                            >
                                <XCircle size={21} />
                            </button>
                        </div>

                        <div className="m3-maintenancerequests-068">
                            <p className="m3-maintenancerequests-039">
                                Request #{actionRequest.id}
                            </p>
                            <p className="m3-maintenancerequests-069">
                                {getResourceName(actionRequest)}
                            </p>
                            <p className="m3-maintenancerequests-070">
                                {actionRequest.title}
                            </p>
                            <p className="m3-maintenancerequests-071">
                                {actionRequest.description}
                            </p>
                        </div>

                        <label className="m3-maintenancerequests-017">
                            {actionType === 'APPROVE'
                                ? 'Remarks (Optional)'
                                : 'Reason for rejection'}
                            {actionType === 'REJECT' && (
                                <span className="m3-maintenancerequests-072"> *</span>
                            )}
                        </label>

                        <textarea
                            rows={3}
                            maxLength={1000}
                            value={remarks}
                            onChange={(event) => setRemarks(event.target.value)}
                            placeholder={
                                actionType === 'APPROVE'
                                    ? 'Add approval remarks...'
                                    : 'Explain why this request is being rejected...'
                            }
                            className="m3-maintenancerequests-018"
                        />

                        <div className="m3-maintenancerequests-073">
                            <button
                                type="button"
                                onClick={closeAction}
                                disabled={processingId !== null}
                                className="m3-maintenancerequests-074"
                            >
                                Cancel
                            </button>

                            <button
                                type="button"
                                onClick={handleReview}
                                disabled={processingId !== null}
                                className={`rounded-lg px-4 py-2 text-sm font-semibold text-white disabled:opacity-60 ${
                                    actionType === 'APPROVE'
                                        ? 'bg-green-600 hover:bg-green-700'
                                        : 'bg-red-600 hover:bg-red-700'
                                }`}
                            >
                                {processingId !== null
                                    ? 'Processing...'
                                    : actionType === 'APPROVE'
                                        ? 'Confirm Approval'
                                        : 'Confirm Rejection'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}

function ErrorBanner({ message }) {
    return (
        <div className="m3-maintenancerequests-075">
            <AlertCircle size={19} className="m3-maintenancerequests-009" />
            <span>{message}</span>
        </div>
    )
}

function StatCard({ title, value, icon, color }) {
    const colors = {
        amber: 'm3-tone-amber',
        green: 'm3-tone-green',
        red: 'm3-tone-red',
    }

    return (
        <div className="m3-maintenancerequests-076">
            <div className="m3-maintenancerequests-077">
                <p className="m3-maintenancerequests-060">{title}</p>
                <div className={`rounded-lg p-2 ${colors[color]}`}>
                    {icon}
                </div>
            </div>
            <p className="m3-maintenancerequests-078">{value}</p>
        </div>
    )
}
