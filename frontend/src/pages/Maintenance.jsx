
import { useCallback, useEffect, useMemo, useState } from "react";
import {
    Wrench,
    RefreshCw,
    Plus,
    Search,
    CalendarDays,
    Clock,
    AlertCircle,
    CheckCircle2,
    X,
    ClipboardList,
    Check,
    XCircle,
} from "lucide-react";

import api from "../api/client";
import { useAuth } from "../context/AuthContext";

const PAGE_SIZE = 8;

function getRoleKey(user) {
    const role =
        user?.role?.name ??
        user?.role?.roleName ??
        user?.roleName ??
        (typeof user?.role === "string" ? user.role : "") ??
        user?.authorities?.[0]?.authority ??
        "";

    return String(role).replace(/^ROLE_/, "").trim().toUpperCase();
}

function getList(data) {
    if (Array.isArray(data)) return data;
    if (Array.isArray(data?.content)) return data.content;
    if (Array.isArray(data?.items)) return data.items;
    if (Array.isArray(data?.data)) return data.data;
    return [];
}

function getResourceName(item) {
    return (
        item?.resource?.name ||
        item?.resourceName ||
        item?.equipment?.name ||
        `Equipment #${item?.resource?.id || item?.resourceId || "—"}`
    );
}

function getRequesterName(item) {
    return (
        item?.requester?.fullName ||
        item?.requester?.name ||
        item?.requesterName ||
        item?.createdBy?.fullName ||
        "Requester"
    );
}

function formatDate(value) {
    if (!value) return "—";

    const date = new Date(value);

    return Number.isNaN(date.getTime())
        ? String(value)
        : date.toLocaleString();
}

function getStatus(item) {
    return String(item?.status || "PENDING").toUpperCase();
}

function getStatusStyle(status) {
    switch (status) {
        case "APPROVED":
        case "COMPLETED":
            return "bg-green-100 text-green-700";

        case "REJECTED":
        case "CANCELLED":
            return "bg-red-100 text-red-700";

        case "IN_PROGRESS":
            return "bg-blue-100 text-blue-700";

        case "PENDING":
        case "REQUESTED":
            return "bg-amber-100 text-amber-700";

        default:
            return "bg-slate-100 text-slate-700";
    }
}

function getPriorityStyle(priority) {
    switch (String(priority || "").toUpperCase()) {
        case "HIGH":
        case "URGENT":
            return "bg-red-100 text-red-700";

        case "MEDIUM":
            return "bg-amber-100 text-amber-700";

        case "LOW":
            return "bg-green-100 text-green-700";

        default:
            return "bg-slate-100 text-slate-600";
    }
}

export default function Maintenance() {
    const { user } = useAuth();
    const role = getRoleKey(user);

    const canCreate = [
        "RESEARCHER_STUDENT",
        "DEPARTMENT_HEAD",
        "LAB_TECHNICIAN",
    ].includes(role);

    const isManager = role === "LAB_MANAGER";

    const [requests, setRequests] = useState([]);
    const [resources, setResources] = useState([]);

    const [loading, setLoading] = useState(true);
    const [resourceLoading, setResourceLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [actionId, setActionId] = useState(null);

    const [showForm, setShowForm] = useState(false);
    const [reviewItem, setReviewItem] = useState(null);
    const [reviewAction, setReviewAction] = useState("");

    const [search, setSearch] = useState("");
    const [statusFilter, setStatusFilter] = useState("ALL");
    const [priorityFilter, setPriorityFilter] = useState("ALL");
    const [page, setPage] = useState(1);

    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const [form, setForm] = useState({
        resourceId: "",
        title: "",
        description: "",
        priority: "MEDIUM",
    });

    const [remarks, setRemarks] = useState("");

    const loadResources = useCallback(async () => {
        if (!canCreate) return;

        setResourceLoading(true);

        try {
            const response = await api.get("/resources");
            setResources(
                getList(response.data).filter(
                    (resource) =>
                        resource.isActive !== false &&
                        String(resource.status || "AVAILABLE").toUpperCase()
                        !== "UNDER_MAINTENANCE"
                )
            );
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Equipment list load nahi hui."
            );
        } finally {
            setResourceLoading(false);
        }
    }, [canCreate]);

    const loadRequests = useCallback(async () => {
        setLoading(true);
        setError("");

        try {
            const endpoint = isManager
                ? "/maintenance/requests/queue"
                : "/maintenance/requests/my";

            const response = await api.get(endpoint);
            setRequests(getList(response.data));
            setPage(1);
        } catch (err) {
            setRequests([]);
            setError(
                err.response?.data?.message ||
                `Maintenance requests load nahi hui. HTTP ${err.response?.status || "network error"}.`
            );
        } finally {
            setLoading(false);
        }
    }, [isManager]);

    useEffect(() => {
        loadRequests();
        loadResources();
    }, [loadRequests, loadResources]);

    const filteredRequests = useMemo(() => {
        const query = search.trim().toLowerCase();

        return requests.filter((item) => {
            const status = getStatus(item);
            const priority = String(item?.priority || "").toUpperCase();

            const searchable = [
                item?.id,
                item?.title,
                item?.description,
                item?.requester?.fullName,
                item?.requesterName,
                getResourceName(item),
            ]
                .filter(Boolean)
                .join(" ")
                .toLowerCase();

            return (
                (!query || searchable.includes(query)) &&
                (statusFilter === "ALL" || status === statusFilter) &&
                (priorityFilter === "ALL" || priority === priorityFilter)
            );
        });
    }, [requests, search, statusFilter, priorityFilter]);

    const totalPages = Math.max(
        1,
        Math.ceil(filteredRequests.length / PAGE_SIZE)
    );

    const visibleRequests = filteredRequests.slice(
        (page - 1) * PAGE_SIZE,
        page * PAGE_SIZE
    );

    const counts = useMemo(
        () => ({
            total: requests.length,
            pending: requests.filter((item) =>
                ["PENDING", "REQUESTED"].includes(getStatus(item))
            ).length,
            approved: requests.filter(
                (item) => getStatus(item) === "APPROVED"
            ).length,
            rejected: requests.filter(
                (item) => getStatus(item) === "REJECTED"
            ).length,
        }),
        [requests]
    );

    function updateForm(field, value) {
        setForm((previous) => ({
            ...previous,
            [field]: value,
        }));
    }

    async function handleCreate(event) {
        event.preventDefault();
        setError("");
        setSuccess("");

        if (
            !form.resourceId ||
            !form.title.trim() ||
            !form.description.trim()
        ) {
            setError("Equipment, title aur description bharna zaroori hai.");
            return;
        }

        setSubmitting(true);

        try {
            await api.post("/maintenance/requests", {
                resourceId: Number(form.resourceId),
                title: form.title.trim(),
                description: form.description.trim(),
                priority: form.priority,
            });

            setSuccess("Maintenance request successfully submit ho gayi.");

            setForm({
                resourceId: "",
                title: "",
                description: "",
                priority: "MEDIUM",
            });

            setShowForm(false);
            await loadRequests();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Maintenance request submit nahi hui."
            );
        } finally {
            setSubmitting(false);
        }
    }

    function openReview(item, action) {
        setReviewItem(item);
        setReviewAction(action);
        setRemarks("");
        setError("");
        setSuccess("");
    }

    async function submitReview(event) {
        event.preventDefault();

        if (!reviewItem?.id) {
            setError("Maintenance request ID nahi mila.");
            return;
        }

        if (!remarks.trim()) {
            setError("Review remarks likhna zaroori hai.");
            return;
        }

        setActionId(reviewItem.id);
        setError("");
        setSuccess("");

        try {
            const endpoint =
                `/maintenance/requests/${reviewItem.id}/${reviewAction}`;

            await api.post(endpoint, {
                remarks: remarks.trim(),
            });

            setSuccess(
                `Maintenance request #${reviewItem.id} successfully ${reviewAction === "approve" ? "approved" : "rejected"} ho gayi.`
            );

            setReviewItem(null);
            setReviewAction("");
            setRemarks("");

            await loadRequests();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Request review nahi ho saki. Backend response check karo."
            );
        } finally {
            setActionId(null);
        }
    }

    if (
        !canCreate &&
        !isManager &&
        role !== "INSTITUTION_ADMIN"
    ) {
        return (
            <div className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-700">
                <div className="flex items-center gap-2 font-semibold">
                    <AlertCircle size={20} />
                    Maintenance access unavailable
                </div>
                <p className="mt-2 text-sm">
                    Is account role ko maintenance requests access nahi diya gaya hai.
                </p>
            </div>
        );
    }

    if (role === "INSTITUTION_ADMIN") {
        return (
            <div className="rounded-2xl border border-blue-200 bg-blue-50 p-6">
                <h2 className="text-lg font-bold text-slate-900">
                    Maintenance Overview
                </h2>
                <p className="mt-2 text-sm text-slate-600">
                    Institution maintenance history aur downtime analytics
                    dashboard agle T8 steps mein connect kiye jayenge.
                </p>
            </div>
        );
    }

    return (
        <div className="min-h-screen space-y-6 bg-slate-50 p-4 sm:p-6">
            <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
                <div>
                    <div className="mb-2 flex items-center gap-2 text-sm font-semibold text-blue-700">
                        <Wrench size={17} />
                        Maintenance Management
                    </div>

                    <h1 className="text-2xl font-bold text-slate-900 sm:text-3xl">
                        {isManager ? "Maintenance Request Queue" : "My Maintenance Requests"}
                    </h1>

                    <p className="mt-2 text-sm text-slate-500">
                        {isManager
                            ? "Review equipment maintenance requests from your institution."
                            : "Report equipment problems and track your maintenance requests."}
                    </p>
                </div>

                <div className="flex flex-wrap gap-2">
                    <button
                        type="button"
                        onClick={loadRequests}
                        disabled={loading}
                        className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-100 disabled:opacity-50"
                    >
                        <RefreshCw
                            size={16}
                            className={loading ? "animate-spin" : ""}
                        />
                        Refresh
                    </button>

                    {canCreate && (
                        <button
                            type="button"
                            onClick={() => {
                                setShowForm((value) => !value);
                                setError("");
                                setSuccess("");
                            }}
                            className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-blue-700"
                        >
                            {showForm ? <X size={17} /> : <Plus size={17} />}
                            {showForm ? "Close Form" : "Report a Problem"}
                        </button>
                    )}
                </div>
            </header>

            {error && (
                <div
                    role="alert"
                    className="flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700"
                >
                    <AlertCircle size={19} className="mt-0.5 shrink-0" />
                    <span>{error}</span>
                </div>
            )}

            {success && (
                <div
                    role="status"
                    className="flex items-start gap-3 rounded-xl border border-green-200 bg-green-50 p-4 text-sm text-green-700"
                >
                    <CheckCircle2 size={19} className="mt-0.5 shrink-0" />
                    <span>{success}</span>
                </div>
            )}

            {showForm && canCreate && (
                <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
                    <div className="mb-5">
                        <h2 className="text-lg font-bold text-slate-900">
                            Report Equipment Problem
                        </h2>
                        <p className="mt-1 text-sm text-slate-500">
                            Problem ki details submit karo. Lab Manager request review karega.
                        </p>
                    </div>

                    <form onSubmit={handleCreate} className="space-y-4">
                        <div className="grid gap-4 md:grid-cols-2">
                            <label className="block">
                                <span className="mb-1.5 block text-sm font-semibold text-slate-700">
                                    Equipment *
                                </span>
                                <select
                                    value={form.resourceId}
                                    onChange={(event) =>
                                        updateForm("resourceId", event.target.value)
                                    }
                                    disabled={resourceLoading}
                                    className="w-full rounded-xl border border-slate-300 bg-white px-3 py-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                                    required
                                >
                                    <option value="">
                                        {resourceLoading
                                            ? "Loading equipment..."
                                            : "Select equipment"}
                                    </option>
                                    {resources.map((resource) => (
                                        <option key={resource.id} value={resource.id}>
                                            {resource.name} — #{resource.id}
                                        </option>
                                    ))}
                                </select>
                            </label>

                            <label className="block">
                                <span className="mb-1.5 block text-sm font-semibold text-slate-700">
                                    Priority *
                                </span>
                                <select
                                    value={form.priority}
                                    onChange={(event) =>
                                        updateForm("priority", event.target.value)
                                    }
                                    className="w-full rounded-xl border border-slate-300 bg-white px-3 py-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                                >
                                    <option value="LOW">Low</option>
                                    <option value="MEDIUM">Medium</option>
                                    <option value="HIGH">High</option>
                                    <option value="URGENT">Urgent</option>
                                </select>
                            </label>
                        </div>

                        <label className="block">
                            <span className="mb-1.5 block text-sm font-semibold text-slate-700">
                                Problem Title *
                            </span>
                            <input
                                type="text"
                                value={form.title}
                                onChange={(event) =>
                                    updateForm("title", event.target.value)
                                }
                                maxLength={150}
                                placeholder="Example: Microscope not powering on"
                                className="w-full rounded-xl border border-slate-300 px-3 py-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                                required
                            />
                        </label>

                        <label className="block">
                            <span className="mb-1.5 block text-sm font-semibold text-slate-700">
                                Problem Description *
                            </span>
                            <textarea
                                value={form.description}
                                onChange={(event) =>
                                    updateForm("description", event.target.value)
                                }
                                rows={4}
                                maxLength={1000}
                                placeholder="Describe the issue, error, or repair required..."
                                className="w-full resize-y rounded-xl border border-slate-300 px-3 py-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                                required
                            />
                            <span className="mt-1 block text-right text-xs text-slate-400">
                                {form.description.length}/1000
                            </span>
                        </label>

                        <div className="flex flex-wrap justify-end gap-2">
                            <button
                                type="button"
                                onClick={() => setShowForm(false)}
                                className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"
                            >
                                Cancel
                            </button>
                            <button
                                type="submit"
                                disabled={submitting || resources.length === 0}
                                className="inline-flex items-center gap-2 rounded-xl bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                {submitting ? (
                                    <RefreshCw size={16} className="animate-spin" />
                                ) : (
                                    <Plus size={16} />
                                )}
                                {submitting ? "Submitting..." : "Submit Request"}
                            </button>
                        </div>
                    </form>
                </section>
            )}

            <section className="grid grid-cols-2 gap-3 lg:grid-cols-4">
                {[
                    {
                        label: "Total Requests",
                        value: counts.total,
                        icon: ClipboardList,
                        color: "text-blue-600 bg-blue-50",
                    },
                    {
                        label: "Pending",
                        value: counts.pending,
                        icon: Clock,
                        color: "text-amber-600 bg-amber-50",
                    },
                    {
                        label: "Approved",
                        value: counts.approved,
                        icon: CheckCircle2,
                        color: "text-green-600 bg-green-50",
                    },
                    {
                        label: "Rejected",
                        value: counts.rejected,
                        icon: XCircle,
                        color: "text-red-600 bg-red-50",
                    },
                ].map((card) => {
                    const Icon = card.icon;

                    return (
                        <div
                            key={card.label}
                            className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"
                        >
                            <div className="flex items-center justify-between gap-2">
                                <span className="text-sm text-slate-500">
                                    {card.label}
                                </span>
                                <span className={`rounded-lg p-2 ${card.color}`}>
                                    <Icon size={17} />
                                </span>
                            </div>
                            <div className="mt-3 text-2xl font-bold text-slate-900">
                                {card.value}
                            </div>
                        </div>
                    );
                })}
            </section>

            <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                <div className="flex flex-col gap-3 border-b border-slate-100 p-4 sm:p-5">
                    <div>
                        <h2 className="font-bold text-slate-900">
                            {isManager ? "Requests to Review" : "Request History"}
                        </h2>
                        <p className="mt-1 text-sm text-slate-500">
                            {filteredRequests.length} request(s) found
                        </p>
                    </div>

                    <div className="grid gap-2 sm:grid-cols-3">
                        <div className="relative sm:col-span-1">
                            <Search
                                size={17}
                                className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                            />
                            <input
                                type="search"
                                value={search}
                                onChange={(event) => {
                                    setSearch(event.target.value);
                                    setPage(1);
                                }}
                                placeholder="Search requests..."
                                className="w-full rounded-xl border border-slate-200 py-2.5 pl-9 pr-3 text-sm outline-none focus:border-blue-500"
                            />
                        </div>

                        <select
                            value={statusFilter}
                            onChange={(event) => {
                                setStatusFilter(event.target.value);
                                setPage(1);
                            }}
                            className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-blue-500"
                        >
                            <option value="ALL">All statuses</option>
                            <option value="PENDING">Pending</option>
                            <option value="APPROVED">Approved</option>
                            <option value="REJECTED">Rejected</option>
                        </select>

                        <select
                            value={priorityFilter}
                            onChange={(event) => {
                                setPriorityFilter(event.target.value);
                                setPage(1);
                            }}
                            className="rounded-xl border border-slate-200 px-3 py-2.5 text-sm outline-none focus:border-blue-500"
                        >
                            <option value="ALL">All priorities</option>
                            <option value="LOW">Low</option>
                            <option value="MEDIUM">Medium</option>
                            <option value="HIGH">High</option>
                            <option value="URGENT">Urgent</option>
                        </select>
                    </div>
                </div>

                {loading ? (
                    <div className="flex items-center justify-center gap-2 p-12 text-sm text-slate-500">
                        <RefreshCw size={18} className="animate-spin" />
                        Loading maintenance requests...
                    </div>
                ) : visibleRequests.length === 0 ? (
                    <div className="p-12 text-center">
                        <ClipboardList
                            size={34}
                            className="mx-auto text-slate-300"
                        />
                        <h3 className="mt-3 font-semibold text-slate-800">
                            No maintenance requests found
                        </h3>
                        <p className="mt-1 text-sm text-slate-500">
                            {search || statusFilter !== "ALL" || priorityFilter !== "ALL"
                                ? "Try changing your search or filters."
                                : "New maintenance requests will appear here."}
                        </p>
                    </div>
                ) : (
                    <>
                        <div className="overflow-x-auto">
                            <table className="w-full min-w-[850px] text-left text-sm">
                                <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                                <tr>
                                    <th className="px-5 py-3">Request</th>
                                    <th className="px-5 py-3">Equipment</th>
                                    {isManager && (
                                        <th className="px-5 py-3">Requester</th>
                                    )}
                                    <th className="px-5 py-3">Priority</th>
                                    <th className="px-5 py-3">Status</th>
                                    <th className="px-5 py-3">Created</th>
                                    {isManager && (
                                        <th className="px-5 py-3">Actions</th>
                                    )}
                                </tr>
                                </thead>

                                <tbody className="divide-y divide-slate-100">
                                {visibleRequests.map((item) => {
                                    const status = getStatus(item);
                                    const pending = [
                                        "PENDING",
                                        "REQUESTED",
                                    ].includes(status);

                                    return (
                                        <tr key={item.id} className="hover:bg-slate-50">
                                            <td className="px-5 py-4">
                                                <div className="font-semibold text-slate-900">
                                                    {item.title || `Request #${item.id}`}
                                                </div>
                                                <div className="mt-1 max-w-xs truncate text-xs text-slate-500">
                                                    {item.description || "No description"}
                                                </div>
                                                <div className="mt-1 text-xs text-slate-400">
                                                    ID: #{item.id}
                                                </div>
                                            </td>

                                            <td className="px-5 py-4 font-medium text-slate-700">
                                                {getResourceName(item)}
                                            </td>

                                            {isManager && (
                                                <td className="px-5 py-4 text-slate-600">
                                                    {getRequesterName(item)}
                                                </td>
                                            )}

                                            <td className="px-5 py-4">
                                                    <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${getPriorityStyle(item.priority)}`}>
                                                        {item.priority || "—"}
                                                    </span>
                                            </td>

                                            <td className="px-5 py-4">
                                                    <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${getStatusStyle(status)}`}>
                                                        {status.replaceAll("_", " ")}
                                                    </span>
                                            </td>

                                            <td className="px-5 py-4 text-xs text-slate-500">
                                                    <span className="inline-flex items-center gap-1.5">
                                                        <CalendarDays size={14} />
                                                        {formatDate(item.createdAt)}
                                                    </span>
                                            </td>

                                            {isManager && (
                                                <td className="px-5 py-4">
                                                    {pending ? (
                                                        <div className="flex items-center gap-2">
                                                            <button
                                                                type="button"
                                                                onClick={() => openReview(item, "approve")}
                                                                className="rounded-lg bg-green-50 p-2 text-green-700 hover:bg-green-100"
                                                                title="Approve request"
                                                            >
                                                                <Check size={17} />
                                                            </button>
                                                            <button
                                                                type="button"
                                                                onClick={() => openReview(item, "reject")}
                                                                className="rounded-lg bg-red-50 p-2 text-red-700 hover:bg-red-100"
                                                                title="Reject request"
                                                            >
                                                                <X size={17} />
                                                            </button>
                                                        </div>
                                                    ) : (
                                                        <span className="text-xs text-slate-400">
                                                                Reviewed
                                                            </span>
                                                    )}
                                                </td>
                                            )}
                                        </tr>
                                    );
                                })}
                                </tbody>
                            </table>
                        </div>

                        <div className="flex flex-col justify-between gap-3 border-t border-slate-100 px-5 py-4 sm:flex-row sm:items-center">
                            <span className="text-xs text-slate-500">
                                Page {page} of {totalPages}
                            </span>

                            <div className="flex gap-2">
                                <button
                                    type="button"
                                    onClick={() => setPage((value) => Math.max(1, value - 1))}
                                    disabled={page <= 1}
                                    className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 disabled:opacity-40"
                                >
                                    Previous
                                </button>
                                <button
                                    type="button"
                                    onClick={() => setPage((value) => Math.min(totalPages, value + 1))}
                                    disabled={page >= totalPages}
                                    className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 disabled:opacity-40"
                                >
                                    Next
                                </button>
                            </div>
                        </div>
                    </>
                )}
            </section>

            {reviewItem && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4">
                    <form
                        onSubmit={submitReview}
                        className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
                    >
                        <div className="flex items-start justify-between gap-3">
                            <div>
                                <h2 className="text-lg font-bold text-slate-900">
                                    {reviewAction === "approve"
                                        ? "Approve Maintenance Request"
                                        : "Reject Maintenance Request"}
                                </h2>
                                <p className="mt-1 text-sm text-slate-500">
                                    Request #{reviewItem.id} — {getResourceName(reviewItem)}
                                </p>
                            </div>

                            <button
                                type="button"
                                onClick={() => setReviewItem(null)}
                                className="rounded-lg p-2 text-slate-400 hover:bg-slate-100"
                                aria-label="Close dialog"
                            >
                                <X size={18} />
                            </button>
                        </div>

                        <label className="mt-5 block">
                            <span className="mb-1.5 block text-sm font-semibold text-slate-700">
                                Review remarks *
                            </span>
                            <textarea
                                value={remarks}
                                onChange={(event) => setRemarks(event.target.value)}
                                rows={4}
                                maxLength={500}
                                required
                                placeholder="Enter the reason or review remarks..."
                                className="w-full rounded-xl border border-slate-300 px-3 py-3 text-sm outline-none focus:border-blue-500"
                            />
                        </label>

                        <div className="mt-5 flex justify-end gap-2">
                            <button
                                type="button"
                                onClick={() => setReviewItem(null)}
                                className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-700"
                            >
                                Cancel
                            </button>
                            <button
                                type="submit"
                                disabled={actionId === reviewItem.id}
                                className={`rounded-xl px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50 ${
                                    reviewAction === "approve"
                                        ? "bg-green-600 hover:bg-green-700"
                                        : "bg-red-600 hover:bg-red-700"
                                }`}
                            >
                                {actionId === reviewItem.id
                                    ? "Saving..."
                                    : reviewAction === "approve"
                                        ? "Confirm Approval"
                                        : "Confirm Rejection"}
                            </button>
                        </div>
                    </form>
                </div>
            )}
        </div>
    );
}
