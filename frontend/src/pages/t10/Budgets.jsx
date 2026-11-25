
import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { RefreshCw, Plus, X } from 'lucide-react';
import api from '../../api/client';
import '../../styles/T10.css';

const money = (value) =>
    Number(value || 0).toLocaleString('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2,
    });

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

export default function Budgets() {
    const { user } = useAuth();

    const [budgets, setBudgets] = useState([]);
    const [fiscalYear, setFiscalYear] = useState(new Date().getFullYear());
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [showForm, setShowForm] = useState(false);
    const [saving, setSaving] = useState(false);

    const [form, setForm] = useState({
        departmentId: '',
        fiscalYear: new Date().getFullYear(),
        budgetAmount: '',
    });

    const role = getUserRole(user);
    const canManage = ['INSTITUTION_ADMIN', 'SYSTEM_ADMIN'].includes(role);
    const isDepartmentHead = role === 'DEPARTMENT_HEAD';

    const load = useCallback(async () => {
        setLoading(true);
        setError('');

        try {
            let endpoint;

            if (canManage) {
                endpoint = '/budgets';
            } else if (isDepartmentHead) {
                endpoint = '/budgets/my-department';
            } else {
                throw new Error(`Budget access is not available for role: ${role || 'UNKNOWN'}`);
            }

            console.log('Budgets API:', endpoint, 'Role:', role);

            const response = await api.get(endpoint, {
                params: { fiscalYear },
            });

            const result = response.data;

            setBudgets(
                Array.isArray(result)
                    ? result
                    : result?.content || []
            );
        } catch (e) {
            console.error('Budget loading error:', e);

            setBudgets([]);
            setError(
                e.response?.data?.message ||
                e.message ||
                'Budgets could not be loaded.'
            );
        } finally {
            setLoading(false);
        }
    }, [fiscalYear, canManage, isDepartmentHead, role]);

    useEffect(() => {
        load();
    }, [load]);

    const saveBudget = async (e) => {
        e.preventDefault();

        if (
            !form.departmentId ||
            !form.budgetAmount ||
            Number(form.budgetAmount) <= 0
        ) {
            setError('Department and a valid budget amount are required.');
            return;
        }

        setSaving(true);
        setError('');

        try {
            await api.post('/budgets', {
                departmentId: Number(form.departmentId),
                fiscalYear: Number(form.fiscalYear),
                budgetAmount: Number(form.budgetAmount),
            });

            setShowForm(false);

            setForm({
                departmentId: '',
                fiscalYear,
                budgetAmount: '',
            });

            await load();
        } catch (e) {
            console.error('Budget save error:', e);

            setError(
                e.response?.data?.message ||
                'Could not save the budget.'
            );
        } finally {
            setSaving(false);
        }
    };

    const statusClass = (status) =>
        String(status || '').toLowerCase().replace(/_/g, '-');

    return (
        <div className="t10-page">
            <div className="t10-header">
                <div>
                    <h1>Department Budgets</h1>
                    <p>
                        Monitor annual budgets, utilization and remaining balances.
                    </p>
                    <small>
                        Logged-in role: {role || 'Not detected'}
                    </small>
                </div>

                <div className="t10-actions">
                    <button
                        className="t10-btn secondary"
                        onClick={load}
                        disabled={loading}
                    >
                        <RefreshCw size={16} /> Refresh
                    </button>

                    {canManage && (
                        <button
                            className="t10-btn primary"
                            onClick={() => setShowForm(true)}
                        >
                            <Plus size={16} /> Set Budget
                        </button>
                    )}
                </div>
            </div>

            <section className="t10-panel">
                <div className="t10-filters">
                    <label>
                        Fiscal Year
                        <select
                            value={fiscalYear}
                            onChange={(e) => setFiscalYear(Number(e.target.value))}
                        >
                            {Array.from(
                                { length: 7 },
                                (_, i) => new Date().getFullYear() - 3 + i
                            ).map((year) => (
                                <option key={year} value={year}>
                                    {year}-{year + 1}
                                </option>
                            ))}
                        </select>
                    </label>
                </div>

                {error && (
                    <div className="t10-error">
                        {error}
                    </div>
                )}

                {showForm && canManage && (
                    <form
                        className="t10-form-panel"
                        onSubmit={saveBudget}
                    >
                        <div className="t10-form-heading">
                            <h2>Set Department Budget</h2>

                            <button
                                type="button"
                                className="t10-icon-btn"
                                onClick={() => setShowForm(false)}
                            >
                                <X size={18} />
                            </button>
                        </div>

                        <div className="t10-form-grid">
                            <label>
                                Department ID
                                <input
                                    type="number"
                                    min="1"
                                    required
                                    value={form.departmentId}
                                    onChange={(e) =>
                                        setForm((p) => ({
                                            ...p,
                                            departmentId: e.target.value,
                                        }))
                                    }
                                />
                            </label>

                            <label>
                                Fiscal Year
                                <input
                                    type="number"
                                    required
                                    value={form.fiscalYear}
                                    onChange={(e) =>
                                        setForm((p) => ({
                                            ...p,
                                            fiscalYear: e.target.value,
                                        }))
                                    }
                                />
                            </label>

                            <label>
                                Budget Amount (₹)
                                <input
                                    type="number"
                                    min="0.01"
                                    step="0.01"
                                    required
                                    value={form.budgetAmount}
                                    onChange={(e) =>
                                        setForm((p) => ({
                                            ...p,
                                            budgetAmount: e.target.value,
                                        }))
                                    }
                                />
                            </label>
                        </div>

                        <button
                            className="t10-btn primary"
                            type="submit"
                            disabled={saving}
                        >
                            {saving ? 'Saving...' : 'Save Budget'}
                        </button>
                    </form>
                )}

                <div className="t10-table-wrap">
                    <table className="t10-table">
                        <thead>
                        <tr>
                            <th>Department</th>
                            <th>Budget</th>
                            <th>Used</th>
                            <th>Remaining</th>
                            <th>Utilization</th>
                            <th>Status</th>
                        </tr>
                        </thead>

                        <tbody>
                        {loading ? (
                            <tr>
                                <td colSpan="6" className="t10-empty">
                                    Loading budgets...
                                </td>
                            </tr>
                        ) : budgets.length === 0 ? (
                            <tr>
                                <td colSpan="6" className="t10-empty">
                                    No budget records found for this fiscal year.
                                </td>
                            </tr>
                        ) : (
                            budgets.map((row) => (
                                <tr
                                    key={`${row.departmentId}-${row.fiscalYear}`}
                                >
                                    <td>
                                        <strong>
                                            {row.departmentName ||
                                                `Department #${row.departmentId}`}
                                        </strong>
                                    </td>

                                    <td>
                                        {row.budgetAmount == null
                                            ? '—'
                                            : money(row.budgetAmount)}
                                    </td>

                                    <td>{money(row.usedAmount)}</td>

                                    <td>
                                        {row.remainingAmount == null
                                            ? '—'
                                            : money(row.remainingAmount)}
                                    </td>

                                    <td>
                                        {row.utilizationPercent == null
                                            ? '—'
                                            : `${Number(row.utilizationPercent).toFixed(2)}%`}
                                    </td>

                                    <td>
                                            <span
                                                className={`t10-status ${statusClass(row.status)}`}
                                            >
                                                {row.status}
                                            </span>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                </div>
            </section>
        </div>
    );
}
