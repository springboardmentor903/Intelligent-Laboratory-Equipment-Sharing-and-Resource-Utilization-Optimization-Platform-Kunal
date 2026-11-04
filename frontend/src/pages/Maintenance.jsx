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
} from "lucide-react";
import api from "../api/client";
import { useAuth } from "../context/AuthContext";

function getRoleKey(user) {
    const rawRole =
        user?.role?.name ??
        user?.role?.roleName ??
        user?.roleName ??
        (typeof user?.role === "string" ? user.role : "") ??
        user?.authorities?.[0]?.authority ??
        "";

    return String(rawRole).replace(/^ROLE_/, "").toUpperCase();
}

function getList(data) {
    if (Array.isArray(data)) return data;
    if (Array.isArray(data?.content)) return data.content;
    if (Array.isArray(data?.items)) return data.items;
    if (Array.isArray(data?.data)) return data.data;
    return [];
}

function formatDate(value) {
    if (!value) return "Not scheduled";

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);

    return date.toLocaleString();
}

function getStatus(item) {
    return String(item?.status ?? "PENDING").toUpperCase();
}

function statusClass(status) {
    switch (status) {
        case "COMPLETED":
        case "RESOLVED":
            return "completed";
        case "IN_PROGRESS":
            return "in-progress";
        case "CANCELLED":
        case "REJECTED":
            return "cancelled";
        case "SCHEDULED":
            return "scheduled";
        default:
            return "pending";
    }
}export default function Maintenance() {
    const { user } = useAuth();
    const role = getRoleKey(user);

    const canCreate = [
        "LAB_TECHNICIAN",
        "LAB_MANAGER",
        "SYSTEM_ADMIN",
    ].includes(role);

    const canUpdateStatus = [
        "LAB_MANAGER",
        "SYSTEM_ADMIN",
    ].includes(role);

    const [requests, setRequests] = useState([]);
    const [resources, setResources] = useState([]);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [updatingId, setUpdatingId] = useState(null);
    const [showForm, setShowForm] = useState(false);
    const [search, setSearch] = useState("");
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    const [form, setForm] = useState({
        resourceId: "",
        description: "",
        scheduledStart: "",
        scheduledEnd: "",
    });

    const [statusForms, setStatusForms] = useState({});

    const loadData = useCallback(async () => {
        setLoading(true);
        setError("");

        try {
            const [maintenanceResponse, resourceResponse] =
                await Promise.all([
                    api.get("/maintenance"),
                    api.get("/resources"),
                ]);

            setRequests(getList(maintenanceResponse.data));
            setResources(getList(resourceResponse.data));
        } catch (err) {
            setError(
                err?.response?.data?.message ||
                err?.response?.data?.error ||
                "Maintenance data load nahi hua. API aur login permission check karein."
            );
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadData();
    }, [loadData]);

    const filteredRequests = useMemo(() => {
        const query = search.trim().toLowerCase();

        if (!query) return requests;

        return requests.filter((item) => {
            const resource = item?.resource ?? item?.equipment ?? {};

            const searchable = [
                item?.id,
                item?.description,
                item?.status,
                item?.resolutionNotes,
                resource?.id,
                resource?.name,
                resource?.resourceName,
                resource?.location,
            ]
                .filter(Boolean)
                .join(" ")
                .toLowerCase();

            return searchable.includes(query);
        });
    }, [requests, search]);

    const counts = useMemo(
        () => ({
            total: requests.length,
            pending: requests.filter((item) =>
                ["PENDING", "REQUESTED", "OPEN"].includes(getStatus(item))
            ).length,
            inProgress: requests.filter((item) =>
                ["IN_PROGRESS", "IN PROGRESS"].includes(getStatus(item))
            ).length,
            completed: requests.filter((item) =>
                ["COMPLETED", "RESOLVED"].includes(getStatus(item))
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

        if (!form.resourceId || !form.description.trim()) {
            setError(
                "Equipment aur maintenance description bharna zaroori hai."
            );
            return;
        }

        if (
            form.scheduledStart &&
            form.scheduledEnd &&
            form.scheduledEnd <= form.scheduledStart
        ) {
            setError(
                "Scheduled end time, start time ke baad hona chahiye."
            );
            return;
        }

        const payload = {
            resourceId: String(form.resourceId),
            description: form.description.trim(),
            scheduledStart: form.scheduledStart
                ? form.scheduledStart.length === 16
                    ? `${form.scheduledStart}:00`
                    : form.scheduledStart
                : "",
            scheduledEnd: form.scheduledEnd
                ? form.scheduledEnd.length === 16
                    ? `${form.scheduledEnd}:00`
                    : form.scheduledEnd
                : "",
        };

        setSubmitting(true);

        try {
            await api.post("/maintenance", payload);

            setSuccess("Maintenance request successfully create ho gayi.");

            setForm({
                resourceId: "",
                description: "",
                scheduledStart: "",
                scheduledEnd: "",
            });

            setShowForm(false);
            await loadData();
        } catch (err) {
            setError(
                err?.response?.data?.message ||
                err?.response?.data?.error ||
                "Request create nahi hui. Resource ID aur backend validation check karein."
            );
        } finally {
            setSubmitting(false);
        }
    }

    async function handleStatusUpdate(item) {
        const id = item?.id;

        if (id == null) {
            setError("Request ID response mein nahi mila.");
            return;
        }

        const values = statusForms[id] ?? {};
        const status = String(values.status ?? "").trim();

        if (!status) {
            setError("Status select karein.");
            return;
        }

        setError("");
        setSuccess("");
        setUpdatingId(id);

        try {
            await api.patch(`/maintenance/${id}/status`, {
                status,
                resolutionNotes: values.resolutionNotes ?? "",
            });

            setSuccess(
                `Maintenance request #${id} ka status update ho gaya.`
            );

            await loadData();
        } catch (err) {
            setError(
                err?.response?.data?.message ||
                err?.response?.data?.error ||
                "Status update nahi hua. Backend mein allowed status values check karein."
            );
        } finally {
            setUpdatingId(null);
        }
    }

    function updateStatusForm(id, field, value) {
        setStatusForms((previous) => ({
            ...previous,
            [id]: {
                ...previous[id],
                [field]: value,
            },
        }));
    }  return (
        <div className="maintenance-page min-h-screen bg-slate-50 p-4 sm:p-6 lg:p-8">
            <div className="mx-auto max-w-7xl space-y-6">
                <header className="maintenance-header flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                        <div className="mb-2 flex items-center gap-2 text-sm font-medium text-blue-700">
                            <Wrench size={17} />
                            Maintenance Management
                        </div>

                        <h1 className="text-2xl font-bold text-slate-900 sm:text-3xl">
                            Maintenance Requests
                        </h1>

                        <p className="mt-2 text-sm text-slate-500">
                            Equipment repair, preventive maintenance aur service
                            requests track karein.
                        </p>
                    </div>

                    <div className="flex flex-wrap gap-2">
                        <button
                            type="button"
                            onClick={loadData}
                            disabled={loading}
                            className="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-100 disabled:opacity-50"
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
                                    setShowForm((previous) => !previous);
                                    setError("");
                                }}
                                className="maintenance-btn inline-flex items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-700"
                            >
                                {showForm ? <X size={16} /> : <Plus size={17} />}
                                {showForm ? "Close Form" : "New Request"}
                            </button>
                        )}
                    </div>
                </header>

                {error && (
                    <div
                        role="alert"
                        className="maintenance-error flex items-start gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700"
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

                <div className="maintenance-stats grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
                    {[
                        {
                            label: "Total Requests",
                            value: counts.total,
                            icon: Wrench,
                            color: "text-blue-600 bg-blue-50",
                        },
                        {
                            label: "Pending",
                            value: counts.pending,
                            icon: Clock,
                            color: "text-amber-600 bg-amber-50",
                        },
                        {
                            label: "In Progress",
                            value: counts.inProgress,
                            icon: RefreshCw,
                            color: "text-indigo-600 bg-indigo-50",
                        },
                        {
                            label: "Completed",
                            value: counts.completed,
                            icon: CheckCircle2,
                            color: "text-green-600 bg-green-50",
                        },
                    ].map((card) => {
                        const Icon = card.icon;

                        return (
                            <div
                                key={card.label}
                                className="maintenance-stat-card rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
                            >
                                <div className="flex items-center justify-between">
                                    <p className="text-sm font-medium text-slate-500">
                                        {card.label}
                                    </p>

                                    <span className={`rounded-xl p-2.5 ${card.color}`}>
                    <Icon size={20} />
                  </span>
                                </div>

                                <p className="mt-4 text-3xl font-bold text-slate-900">
                                    {card.value}
                                </p>
                            </div>
                        );
                    })}
                </div>

                {showForm && canCreate && (
                    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
                        <h2 className="text-lg font-bold text-slate-900">
                            Create Maintenance Request
                        </h2>

                        <p className="mt-1 text-sm text-slate-500">
                            Equipment aur problem ki details enter karein.
                        </p>

                        <form onSubmit={handleCreate} className="maintenance-form mt-5 space-y-5">
                            <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
                                <div className="maintenance-form-group">
                                    <label htmlFor="maintenance-resource">
                                        Equipment <span className="text-red-500">*</span>
                                    </label>

                                    <select
                                        id="maintenance-resource"
                                        value={form.resourceId}
                                        onChange={(event) =>
                                            updateForm("resourceId", event.target.value)
                                        }
                                        required
                                    >
                                        <option value="">Select equipment</option>

                                        {resources.map((resource) => {
                                            const id = resource.id ?? resource.resourceId;
                                            const name =
                                                resource.name ??
                                                resource.resourceName ??
                                                resource.title ??
                                                `Equipment #${id}`;

                                            return (
                                                <option key={id} value={id}>
                                                    {name} (ID: {id})
                                                </option>
                                            );
                                        })}
                                    </select>

                                    {resources.length === 0 && (
                                        <p className="text-xs text-amber-700">
                                            Equipment list empty hai. /resources API check karein.
                                        </p>
                                    )}
                                </div>

                                <div className="maintenance-form-group">
                                    <label htmlFor="maintenance-description">
                                        Problem / Description{" "}
                                        <span className="text-red-500">*</span>
                                    </label>

                                    <input
                                        id="maintenance-description"
                                        value={form.description}
                                        onChange={(event) =>
                                            updateForm("description", event.target.value)
                                        }
                                        placeholder="e.g. Projector display issue"
                                        required
                                        maxLength={1000}
                                    />
                                </div>

                                <div className="maintenance-form-group">
                                    <label htmlFor="maintenance-start">
                                        Scheduled Start (optional)
                                    </label>

                                    <input
                                        id="maintenance-start"
                                        type="datetime-local"
                                        value={form.scheduledStart}
                                        onChange={(event) =>
                                            updateForm("scheduledStart", event.target.value)
                                        }
                                    />
                                </div>

                                <div className="maintenance-form-group">
                                    <label htmlFor="maintenance-end">
                                        Scheduled End (optional)
                                    </label>

                                    <input
                                        id="maintenance-end"
                                        type="datetime-local"
                                        value={form.scheduledEnd}
                                        onChange={(event) =>
                                            updateForm("scheduledEnd", event.target.value)
                                        }
                                        min={form.scheduledStart || undefined}
                                    />
                                </div>
                            </div>

                            <div className="flex flex-wrap gap-3">
                                <button
                                    type="submit"
                                    disabled={submitting || resources.length === 0}
                                    className="maintenance-btn"
                                >
                                    <Plus size={17} />
                                    {submitting ? "Creating..." : "Create Request"}
                                </button>

                                <button
                                    type="button"
                                    onClick={() => setShowForm(false)}
                                    className="maintenance-btn-secondary"
                                >
                                    Cancel
                                </button>
                            </div>
                        </form>
                    </section>
                )}        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
                <div className="flex flex-col gap-4 border-b border-slate-100 p-5 sm:flex-row sm:items-center sm:justify-between">
                    <div>
                        <h2 className="text-lg font-bold text-slate-900">
                            All Maintenance Requests
                        </h2>

                        <p className="mt-1 text-sm text-slate-500">
                            {filteredRequests.length} request(s) found
                        </p>
                    </div>

                    <div className="relative w-full sm:max-w-xs">
                        <Search
                            size={17}
                            className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                        />

                        <input
                            value={search}
                            onChange={(event) => setSearch(event.target.value)}
                            placeholder="Search requests..."
                            aria-label="Search maintenance requests"
                            className="w-full rounded-xl border border-slate-200 py-2.5 pl-10 pr-3 text-sm outline-none focus:border-blue-500"
                        />
                    </div>
                </div>

                {loading ? (
                    <div className="maintenance-loading flex items-center justify-center gap-3 p-12 text-sm text-slate-500">
                        <RefreshCw size={18} className="animate-spin" />
                        Loading maintenance requests...
                    </div>
                ) : filteredRequests.length === 0 ? (
                    <div className="maintenance-empty p-12 text-center">
                        <Wrench size={36} className="mx-auto text-slate-300" />

                        <h3 className="mt-3 font-semibold text-slate-800">
                            No maintenance requests found
                        </h3>

                        <p className="mt-1 text-sm text-slate-500">
                            New Request se pehli maintenance request create kar sakte hain.
                        </p>
                    </div>
                ) : (
                    <div className="maintenance-grid grid grid-cols-1 gap-4 p-4 lg:grid-cols-2">
                        {filteredRequests.map((item) => {
                            const id = item.id;
                            const status = getStatus(item);
                            const resource = item.resource ?? item.equipment ?? {};

                            const resourceId =
                                resource.id ?? item.resourceId ?? item.equipmentId;

                            const resourceName =
                                resource.name ??
                                resource.resourceName ??
                                resource.title ??
                                (resourceId != null
                                    ? `Equipment #${resourceId}`
                                    : "Equipment details unavailable");

                            const values = statusForms[id] ?? {};

                            return (
                                <article
                                    key={id ?? `${item.description}-${resourceId}`}
                                    className="maintenance-card rounded-2xl border border-slate-200 p-5 transition hover:border-blue-200"
                                >
                                    <div className="maintenance-card-top flex flex-wrap items-start justify-between gap-3">
                                        <div>
                                            <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                                                Request #{id ?? "N/A"}
                                            </p>

                                            <h3 className="mt-1 font-bold text-slate-900">
                                                {resourceName}
                                            </h3>

                                            {resourceId != null && (
                                                <p className="mt-1 text-xs text-slate-500">
                                                    Equipment ID: {resourceId}
                                                </p>
                                            )}
                                        </div>

                                        <span className={`maintenance-status ${statusClass(status)}`}>
                        {status.replaceAll("_", " ")}
                      </span>
                                    </div>

                                    <div className="mt-4 rounded-xl bg-slate-50 p-3">
                                        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                                            Description
                                        </p>

                                        <p className="mt-1 whitespace-pre-wrap text-sm text-slate-800">
                                            {item.description || "No description provided"}
                                        </p>
                                    </div>

                                    <div className="maintenance-details mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
                                        <div className="flex items-start gap-2 text-sm text-slate-600">
                                            <CalendarDays
                                                size={17}
                                                className="mt-0.5 shrink-0 text-slate-400"
                                            />

                                            <div>
                                                <p className="maintenance-detail-label">Start</p>
                                                <p className="maintenance-detail-value">
                                                    {formatDate(item.scheduledStart)}
                                                </p>
                                            </div>
                                        </div>

                                        <div className="flex items-start gap-2 text-sm text-slate-600">
                                            <Clock
                                                size={17}
                                                className="mt-0.5 shrink-0 text-slate-400"
                                            />

                                            <div>
                                                <p className="maintenance-detail-label">End</p>
                                                <p className="maintenance-detail-value">
                                                    {formatDate(item.scheduledEnd)}
                                                </p>
                                            </div>
                                        </div>
                                    </div>

                                    {item.resolutionNotes && (
                                        <div className="mt-4 rounded-xl border border-green-100 bg-green-50 p-3">
                                            <p className="text-xs font-semibold uppercase tracking-wide text-green-700">
                                                Resolution Notes
                                            </p>

                                            <p className="mt-1 whitespace-pre-wrap text-sm text-green-900">
                                                {item.resolutionNotes}
                                            </p>
                                        </div>
                                    )}

                                    {canUpdateStatus && id != null && (
                                        <div className="mt-5 border-t border-slate-100 pt-4">
                                            <p className="mb-3 text-sm font-semibold text-slate-800">
                                                Update Status
                                            </p>

                                            <div className="space-y-3">
                                                <div className="maintenance-form-group">
                                                    <label htmlFor={`maintenance-status-${id}`}>
                                                        Status value
                                                    </label>

                                                    <input
                                                        id={`maintenance-status-${id}`}
                                                        value={values.status ?? ""}
                                                        onChange={(event) =>
                                                            updateStatusForm(
                                                                id,
                                                                "status",
                                                                event.target.value.toUpperCase()
                                                            )
                                                        }
                                                        placeholder={`Current: ${status}`}
                                                    />

                                                    <p className="text-xs text-slate-400">
                                                        Backend ke allowed status value ka exact naam enter karein.
                                                    </p>
                                                </div>

                                                <div className="maintenance-form-group">
                                                    <label htmlFor={`maintenance-notes-${id}`}>
                                                        Resolution Notes
                                                    </label>

                                                    <textarea
                                                        id={`maintenance-notes-${id}`}
                                                        rows={2}
                                                        value={values.resolutionNotes ?? ""}
                                                        onChange={(event) =>
                                                            updateStatusForm(
                                                                id,
                                                                "resolutionNotes",
                                                                event.target.value
                                                            )
                                                        }
                                                        placeholder="Work performed / resolution details"
                                                    />
                                                </div>

                                                <button
                                                    type="button"
                                                    onClick={() => handleStatusUpdate(item)}
                                                    disabled={updatingId === id}
                                                    className="w-full rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-slate-700 disabled:opacity-50"
                                                >
                                                    {updatingId === id
                                                        ? "Updating..."
                                                        : "Save Status & Notes"}
                                                </button>
                                            </div>
                                        </div>
                                    )}
                                </article>
                            );
                        })}
                    </div>
                )}
            </section>
            </div>
        </div>
    );
}