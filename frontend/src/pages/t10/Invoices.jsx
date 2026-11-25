
import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { RefreshCw, Plus, X, Eye, Download } from 'lucide-react';
import api from '../../api/client';
import '../../styles/T10.css';

const money = (value) =>
    Number(value || 0).toLocaleString('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2,
    });

const formatDate = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
};

const today = new Date();
const firstDay = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-01`;
const lastDay = formatDate(today);

function getUserRole(user) {
    const role =
        user?.role?.name ??
        user?.role?.roleName ??
        user?.roleName ??
        (typeof user?.role === 'string' ? user.role : null) ??
        user?.authorities?.[0]?.authority ??
        '';

    return String(role).replace(/^ROLE_/, '').trim().toUpperCase();
}

export default function Invoices() {
    const { user } = useAuth();

    const [tab, setTab] = useState('issued');
    const [data, setData] = useState({
        content: [],
        totalElements: 0,
        totalPages: 0,
        number: 0,
    });
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [selected, setSelected] = useState(null);
    const [showGenerate, setShowGenerate] = useState(false);
    const [generating, setGenerating] = useState(false);

    const [form, setForm] = useState({
        invoiceType: 'DEPARTMENT_CHARGEBACK',
        periodStart: firstDay,
        periodEnd: lastDay,
        taxRate: '0',
        dueDays: '30',
    });

    const role = getUserRole(user);
    const canManage = ['INSTITUTION_ADMIN', 'SYSTEM_ADMIN'].includes(role);
    const isDepartmentHead = role === 'DEPARTMENT_HEAD';

    const load = useCallback(async (nextPage = 0) => {
        setLoading(true);
        setError('');

        try {
            let endpoint;

            if (isDepartmentHead) {
                endpoint = '/invoices/department';
            } else if (canManage) {
                endpoint = tab === 'issued'
                    ? '/invoices/issued'
                    : '/invoices/received';
            } else {
                throw new Error(`Invoice access is not available for role: ${role || 'UNKNOWN'}`);
            }

            console.log('Invoices API:', endpoint, 'Role:', role);

            const response = await api.get(endpoint, {
                params: { page: nextPage, size: 10 },
            });

            const result = response.data || {};

            setData({
                content: Array.isArray(result)
                    ? result
                    : result.content || [],
                totalElements: result.totalElements ?? result.length ?? 0,
                totalPages: result.totalPages ?? 1,
                number: result.number ?? nextPage,
            });

            setPage(result.number ?? nextPage);
        } catch (e) {
            console.error('Invoice loading error:', e);
            setData({
                content: [],
                totalElements: 0,
                totalPages: 0,
                number: 0,
            });
            setError(
                e.response?.data?.message ||
                e.message ||
                'Invoices could not be loaded.'
            );
        } finally {
            setLoading(false);
        }
    }, [tab, canManage, isDepartmentHead, role]);

    useEffect(() => {
        load(0);
    }, [load]);

    const generate = async (e) => {
        e.preventDefault();

        if (form.periodEnd < form.periodStart) {
            setError('Period End cannot be before Period Start.');
            return;
        }

        if (!window.confirm('Generate draft invoices for the selected period?')) {
            return;
        }

        setGenerating(true);
        setError('');

        try {
            const response = await api.post('/invoices/drafts', {
                invoiceType: form.invoiceType,
                periodStart: form.periodStart,
                periodEnd: form.periodEnd,
                taxRate: Number(form.taxRate),
                dueDays: Number(form.dueDays),
            });

            setShowGenerate(false);
            setTab('issued');

            await load(0);

            alert(
                `${Array.isArray(response.data) ? response.data.length : 0} draft invoice(s) generated.`
            );
        } catch (e) {
            console.error('Invoice generation error:', e);
            setError(
                e.response?.data?.message ||
                'Invoice generation failed.'
            );
        } finally {
            setGenerating(false);
        }
    };

    const performAction = async (invoice, action, label) => {
        if (!window.confirm(`${label} invoice ${invoice.invoiceNumber}?`)) {
            return;
        }

        try {
            await api.post(`/invoices/${invoice.id}/${action}`);

            if (selected?.id === invoice.id) {
                const response = await api.get(`/invoices/${invoice.id}`);
                setSelected(response.data);
            }

            await load(page);
        } catch (e) {
            console.error('Invoice action error:', e);
            setError(
                e.response?.data?.message ||
                `Could not ${label.toLowerCase()} invoice.`
            );
        }
    };

    const openDetails = async (invoice) => {
        try {
            const response = await api.get(`/invoices/${invoice.id}`);
            setSelected(response.data);
        } catch (e) {
            console.error('Invoice details error:', e);
            setError(
                e.response?.data?.message ||
                'Invoice details could not be loaded.'
            );
        }
    };

    const downloadInvoicePdf = async (invoice) => {
        try {
            setError('');
            const response = await api.get(
                `/billing/invoices/${invoice.id}/pdf`,
                { responseType: 'blob' }
            );

            const disposition = response.headers?.['content-disposition'] || '';
            const utfMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i);
            const normalMatch = disposition.match(/filename="?([^";]+)"?/i);
            const filename = utfMatch
                ? decodeURIComponent(utfMatch[1])
                : normalMatch?.[1] || `invoice-${invoice.invoiceNumber || invoice.id}.pdf`;

            const url = window.URL.createObjectURL(response.data);
            const link = document.createElement('a');
            link.href = url;
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (e) {
            console.error('Invoice PDF download error:', e);
            let message = e.response?.data?.message || 'Invoice PDF download failed.';
            if (e.response?.data instanceof Blob) {
                const responseText = await e.response.data.text();
                try {
                    const parsed = JSON.parse(responseText);
                    message = parsed.message || parsed.error || responseText || message;
                } catch {
                    message = responseText || message;
                }
            }
            setError(message);
        }
    };

    const invoices = data.content || [];

    return (
        <div className="t10-page">
            <div className="t10-header">
                <div>
                    <h1>Invoices</h1>
                    <p>
                        Manage issued invoices and review invoices received.
                    </p>
                    <small>Logged-in role: {role || 'Not detected'}</small>
                </div>

                <div className="t10-actions">
                    <button
                        className="t10-btn secondary"
                        onClick={() => load(page)}
                        disabled={loading}
                    >
                        <RefreshCw size={16} /> Refresh
                    </button>

                    {canManage && (
                        <button
                            className="t10-btn primary"
                            onClick={() => setShowGenerate(true)}
                        >
                            <Plus size={16} /> Generate Draft
                        </button>
                    )}
                </div>
            </div>

            {canManage && (
                <div className="t10-tabs">
                    <button
                        className={tab === 'issued' ? 'active' : ''}
                        onClick={() => setTab('issued')}
                    >
                        Issued
                    </button>

                    <button
                        className={tab === 'received' ? 'active' : ''}
                        onClick={() => setTab('received')}
                    >
                        Received
                    </button>
                </div>
            )}

            {isDepartmentHead && (
                <div className="t10-tabs">
                    <button className="active">
                        Department Invoices
                    </button>
                </div>
            )}

            {error && <div className="t10-error">{error}</div>}

            {showGenerate && canManage && (
                <form
                    className="t10-form-panel"
                    onSubmit={generate}
                >
                    <div className="t10-form-heading">
                        <h2>Generate Invoice Drafts</h2>

                        <button
                            type="button"
                            className="t10-icon-btn"
                            onClick={() => setShowGenerate(false)}
                        >
                            <X size={18} />
                        </button>
                    </div>

                    <div className="t10-form-grid">
                        <label>
                            Invoice Type
                            <select
                                value={form.invoiceType}
                                onChange={(e) =>
                                    setForm((p) => ({
                                        ...p,
                                        invoiceType: e.target.value,
                                    }))
                                }
                            >
                                <option value="EXTERNAL">
                                    External Institution
                                </option>
                                <option value="DEPARTMENT_CHARGEBACK">
                                    Department Chargeback
                                </option>
                            </select>
                        </label>

                        <label>
                            Period Start
                            <input
                                type="date"
                                required
                                value={form.periodStart}
                                onChange={(e) =>
                                    setForm((p) => ({
                                        ...p,
                                        periodStart: e.target.value,
                                    }))
                                }
                            />
                        </label>

                        <label>
                            Period End
                            <input
                                type="date"
                                required
                                value={form.periodEnd}
                                onChange={(e) =>
                                    setForm((p) => ({
                                        ...p,
                                        periodEnd: e.target.value,
                                    }))
                                }
                            />
                        </label>

                        <label>
                            Tax Rate (%)
                            <input
                                type="number"
                                min="0"
                                step="0.01"
                                value={form.taxRate}
                                onChange={(e) =>
                                    setForm((p) => ({
                                        ...p,
                                        taxRate: e.target.value,
                                    }))
                                }
                            />
                        </label>

                        <label>
                            Due Days
                            <input
                                type="number"
                                min="0"
                                value={form.dueDays}
                                onChange={(e) =>
                                    setForm((p) => ({
                                        ...p,
                                        dueDays: e.target.value,
                                    }))
                                }
                            />
                        </label>
                    </div>

                    <button
                        className="t10-btn primary"
                        type="submit"
                        disabled={generating}
                    >
                        {generating ? 'Generating...' : 'Generate Drafts'}
                    </button>
                </form>
            )}

            <section className="t10-panel">
                <div className="t10-table-wrap">
                    <table className="t10-table">
                        <thead>
                        <tr>
                            <th>Invoice No.</th>
                            <th>Type</th>
                            <th>Period</th>
                            <th>Total</th>
                            <th>Due Date</th>
                            <th>Status</th>
                            <th>Overdue</th>
                            <th>Actions</th>
                        </tr>
                        </thead>

                        <tbody>
                        {loading ? (
                            <tr>
                                <td colSpan="8" className="t10-empty">
                                    Loading invoices...
                                </td>
                            </tr>
                        ) : invoices.length === 0 ? (
                            <tr>
                                <td colSpan="8" className="t10-empty">
                                    No invoices found.
                                </td>
                            </tr>
                        ) : (
                            invoices.map((invoice) => (
                                <tr key={invoice.id}>
                                    <td>
                                        <strong>{invoice.invoiceNumber}</strong>
                                    </td>

                                    <td>
                                        {invoice.invoiceType || invoice.type}
                                    </td>

                                    <td>
                                        {invoice.periodStart} – {invoice.periodEnd}
                                    </td>

                                    <td>
                                        <strong>{money(invoice.totalAmount)}</strong>
                                    </td>

                                    <td>{invoice.dueDate || '—'}</td>

                                    <td>
                                            <span
                                                className={`t10-status ${String(invoice.status || '').toLowerCase()}`}
                                            >
                                                {invoice.status}
                                            </span>
                                    </td>

                                    <td>
                                        {invoice.overdue
                                            ? <span className="t10-status overdue">Yes</span>
                                            : 'No'}
                                    </td>

                                    <td>
                                        <div className="t10-actions">
                                            <button
                                                className="t10-btn secondary"
                                                onClick={() => openDetails(invoice)}
                                                title="View details"
                                            >
                                                <Eye size={15} />
                                            </button>
                                            <button
                                                className="t10-btn secondary"
                                                onClick={() => downloadInvoicePdf(invoice)}
                                                title="Download invoice PDF"
                                            >
                                                <Download size={15} />
                                            </button>

                                            {tab === 'issued' &&
                                                canManage &&
                                                invoice.status === 'DRAFT' && (
                                                    <>
                                                        <button
                                                            className="t10-btn primary"
                                                            onClick={() =>
                                                                performAction(invoice, 'issue', 'Issue')
                                                            }
                                                        >
                                                            Issue
                                                        </button>

                                                        <button
                                                            className="t10-btn secondary"
                                                            onClick={() =>
                                                                performAction(invoice, 'cancel', 'Cancel')
                                                            }
                                                        >
                                                            Cancel
                                                        </button>
                                                    </>
                                                )}

                                            {tab === 'issued' &&
                                                canManage &&
                                                invoice.status === 'ISSUED' && (
                                                    <>
                                                        <button
                                                            className="t10-btn primary"
                                                            onClick={() =>
                                                                performAction(invoice, 'pay', 'Mark paid')
                                                            }
                                                        >
                                                            Paid
                                                        </button>

                                                        <button
                                                            className="t10-btn secondary"
                                                            onClick={() =>
                                                                performAction(invoice, 'cancel', 'Cancel')
                                                            }
                                                        >
                                                            Cancel
                                                        </button>
                                                    </>
                                                )}
                                        </div>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                </div>

                <div className="t10-pagination">
                    <span>{data.totalElements || 0} invoices</span>

                    <div>
                        <button
                            className="t10-btn secondary"
                            disabled={loading || page <= 0}
                            onClick={() => load(page - 1)}
                        >
                            Previous
                        </button>

                        <span>
                            Page {page + 1} of {Math.max(data.totalPages || 0, 1)}
                        </span>

                        <button
                            className="t10-btn secondary"
                            disabled={
                                loading ||
                                page + 1 >= (data.totalPages || 0)
                            }
                            onClick={() => load(page + 1)}
                        >
                            Next
                        </button>
                    </div>
                </div>
            </section>

            {selected && (
                <div
                    className="t10-modal-backdrop"
                    onClick={() => setSelected(null)}
                >
                    <section
                        className="t10-modal"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="t10-form-heading">
                            <div>
                                <h2>{selected.invoiceNumber}</h2>
                                <p>
                                    {selected.invoiceType || selected.type}
                                    {' · '}
                                    {selected.status}
                                </p>
                            </div>

                            <div className="t10-actions">
                                <button
                                    type="button"
                                    className="t10-btn primary"
                                    onClick={() => downloadInvoicePdf(selected)}
                                >
                                    <Download size={15} /> Download PDF
                                </button>
                                <button
                                    className="t10-icon-btn"
                                    onClick={() => setSelected(null)}
                                    aria-label="Close invoice details"
                                >
                                    <X size={18} />
                                </button>
                            </div>
                        </div>

                        <div className="t10-invoice-totals">
                            <div>
                                <span>Subtotal</span>
                                <strong>{money(selected.subtotal)}</strong>
                            </div>

                            <div>
                                <span>Tax ({selected.taxRate}%)</span>
                                <strong>{money(selected.taxAmount)}</strong>
                            </div>

                            <div>
                                <span>Total</span>
                                <strong>{money(selected.totalAmount)}</strong>
                            </div>
                        </div>

                        <p className="t10-muted">
                            Period: {selected.periodStart} – {selected.periodEnd}
                        </p>

                        <p className="t10-muted">
                            Due date: {selected.dueDate || '—'}
                        </p>

                        <h3>Linked Usage Charges</h3>

                        <div className="t10-table-wrap">
                            <table className="t10-table">
                                <thead>
                                <tr>
                                    <th>Equipment</th>
                                    <th>Booking</th>
                                    <th>Minutes</th>
                                    <th>Amount</th>
                                </tr>
                                </thead>

                                <tbody>
                                {(selected.charges || []).map((charge) => (
                                    <tr key={charge.chargeId}>
                                        <td>{charge.equipmentName}</td>
                                        <td>#{charge.bookingId}</td>
                                        <td>{charge.usageMinutes}</td>
                                        <td>{money(charge.amount)}</td>
                                    </tr>
                                ))}

                                {(selected.charges || []).length === 0 && (
                                    <tr>
                                        <td colSpan="4" className="t10-empty">
                                            No linked charges.
                                        </td>
                                    </tr>
                                )}
                                </tbody>
                            </table>
                        </div>
                    </section>
                </div>
            )}
        </div>
    );
}
