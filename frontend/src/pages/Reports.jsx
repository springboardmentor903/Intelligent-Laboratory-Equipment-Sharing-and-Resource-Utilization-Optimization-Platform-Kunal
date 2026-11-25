
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
    FileText, FileSpreadsheet, Download, RefreshCw,
    BarChart3, CalendarDays, AlertCircle, CheckCircle2
} from 'lucide-react';
import {
    ResponsiveContainer, BarChart, Bar, XAxis, YAxis,
    CartesianGrid, Tooltip
} from 'recharts';
import api from '../api/client';
import './Reports.css';

const today = () => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

const monthStart = () => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
};

const formatValue = value => {
    if (value == null || value === '') return '—';
    if (typeof value === 'number') return value.toLocaleString('en-IN');
    return String(value);
};

const reportIcons = {
    UTILIZATION: BarChart3,
    DEPARTMENT_USAGE: FileSpreadsheet,
    MAINTENANCE_DOWNTIME: AlertCircle,
    INTER_INSTITUTION_SHARING: FileText,
    PROCUREMENT_COST_ANALYSIS: FileSpreadsheet
};

function getErrorMessage(error) {
    const data = error.response?.data;

    if (data instanceof Blob) {
        return data.text().then(text => {
            try {
                const parsed = JSON.parse(text);
                return parsed.message || parsed.error || text;
            } catch {
                return text || 'Unable to complete request.';
            }
        });
    }

    return Promise.resolve(
        data?.message ||
        data?.error ||
        error.message ||
        'Unable to complete request.'
    );
}

function getFilename(response, fallback) {
    const disposition = response.headers?.['content-disposition'] || '';
    const utfMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i);
    const normalMatch = disposition.match(/filename="?([^";]+)"?/i);

    if (utfMatch) return decodeURIComponent(utfMatch[1]);
    if (normalMatch) return normalMatch[1];
    return fallback;
}

function saveBlob(blob, filename) {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();

    window.URL.revokeObjectURL(url);
}

export default function Reports() {
    const [searchParams, setSearchParams] = useSearchParams();

    const [reports, setReports] = useState([]);
    const [selectedType, setSelectedType] = useState(
        searchParams.get('type') || ''
    );
    const [fromDate, setFromDate] = useState(
        searchParams.get('fromDate') || monthStart()
    );
    const [toDate, setToDate] = useState(
        searchParams.get('toDate') || today()
    );

    const [preview, setPreview] = useState(null);
    const [loadingReports, setLoadingReports] = useState(true);
    const [loadingPreview, setLoadingPreview] = useState(false);
    const [exporting, setExporting] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const selectedReport = reports.find(
        report => report.type === selectedType
    );

    const loadReports = useCallback(async () => {
        setLoadingReports(true);
        setError('');

        try {
            const response = await api.get('/reports');
            const list = Array.isArray(response.data) ? response.data : [];

            setReports(list);

            const queryType = searchParams.get('type');
            const validType = list.some(item => item.type === queryType);

            if (validType) {
                setSelectedType(queryType);
            } else if (list.length) {
                setSelectedType(list[0].type);
            } else {
                setSelectedType('');
            }
        } catch (err) {
            setError(await getErrorMessage(err));
            setReports([]);
        } finally {
            setLoadingReports(false);
        }
    }, []);

    useEffect(() => {
        loadReports();
    }, [loadReports]);

    const loadPreview = useCallback(async () => {
        if (!selectedType || !fromDate || !toDate) return;

        if (fromDate > toDate) {
            setError('From date cannot be later than To date.');
            return;
        }

        setLoadingPreview(true);
        setError('');
        setSuccess('');

        try {
            const response = await api.get(`/reports/${selectedType}`, {
                params: { fromDate, toDate }
            });

            setPreview(response.data);

            setSearchParams({
                type: selectedType,
                fromDate,
                toDate
            });
        } catch (err) {
            setPreview(null);
            setError(await getErrorMessage(err));
        } finally {
            setLoadingPreview(false);
        }
    }, [selectedType, fromDate, toDate, setSearchParams]);

    useEffect(() => {
        if (reports.length && selectedType) {
            loadPreview();
        }
    }, [reports.length, selectedType]);

    async function exportReport(format) {
        if (!selectedType || !fromDate || !toDate) return;

        setExporting(format);
        setError('');
        setSuccess('');

        try {
            const response = await api.get(
                `/reports/${selectedType}/export`,
                {
                    params: {
                        fromDate,
                        toDate,
                        format
                    },
                    responseType: 'blob'
                }
            );

            const extension = format === 'xlsx' ? 'xlsx' : 'pdf';
            const fallback = `${selectedType.toLowerCase()}_${fromDate}_to_${toDate}.${extension}`;
            const filename = getFilename(response, fallback);

            saveBlob(response.data, filename);
            setSuccess(`${format.toUpperCase()} report downloaded successfully.`);
        } catch (err) {
            setError(await getErrorMessage(err));
        } finally {
            setExporting('');
        }
    }

    const chartConfig = useMemo(() => {
        if (!preview?.rows?.length || !preview?.columns?.length) return null;

        const rows = preview.rows;
        const columns = preview.columns;

        const numericColumn = columns.find(column =>
            rows.some(row =>
                row[column] != null &&
                row[column] !== '' &&
                Number.isFinite(Number(row[column]))
            )
        );

        if (!numericColumn) return null;

        const labelColumn = columns.find(column =>
            column !== numericColumn &&
            rows.some(row => row[column] != null)
        );

        if (!labelColumn) return null;

        return {
            labelKey: labelColumn,
            valueKey: numericColumn,
            data: rows.slice(0, 20).map(row => ({
                label: String(row[labelColumn] ?? ''),
                value: Number(row[numericColumn] ?? 0)
            }))
        };
    }, [preview]);

    const totals = preview?.totals
        ? Object.entries(preview.totals)
        : [];

    return (
        <div className="reports-page">
            <div className="reports-header">
                <div>
                    <span className="reports-eyebrow">ANALYTICS & REPORTING</span>
                    <h1>Reports & Export</h1>
                    <p>Choose a permitted report, preview the results and export PDF or Excel.</p>
                </div>

                <button
                    className="reports-refresh"
                    onClick={loadReports}
                    disabled={loadingReports}
                >
                    <RefreshCw size={16} />
                    Refresh reports
                </button>
            </div>

            {error && (
                <div className="reports-alert error">
                    <AlertCircle size={18} />
                    <span>{error}</span>
                    <button onClick={() => setError('')}>Dismiss</button>
                </div>
            )}

            {success && (
                <div className="reports-alert success">
                    <CheckCircle2 size={18} />
                    <span>{success}</span>
                    <button onClick={() => setSuccess('')}>Dismiss</button>
                </div>
            )}

            <section className="reports-section">
                <div className="reports-section-heading">
                    <div>
                        <h2>Available reports</h2>
                        <p>Only reports permitted for your role are displayed.</p>
                    </div>
                </div>

                {loadingReports ? (
                    <div className="reports-empty">Loading available reports…</div>
                ) : reports.length ? (
                    <div className="reports-cards">
                        {reports.map(report => {
                            const Icon = reportIcons[report.type] || FileText;

                            return (
                                <button
                                    key={report.type}
                                    type="button"
                                    className={`reports-card ${selectedType === report.type ? 'selected' : ''}`}
                                    onClick={() => {
                                        setSelectedType(report.type);
                                        setPreview(null);
                                    }}
                                >
                                    <span className="reports-card-icon"><Icon size={21} /></span>
                                    <span className="reports-card-title">{report.title}</span>
                                    <span className="reports-card-description">{report.description}</span>
                                    <span className="reports-card-footer">
                    {report.exportSupported ? 'PDF & Excel available' : 'Preview only'}
                  </span>
                                </button>
                            );
                        })}
                    </div>
                ) : (
                    <div className="reports-empty">
                        No reports are available for your account.
                    </div>
                )}
            </section>

            {selectedReport && (
                <>
                    <section className="reports-section">
                        <div className="reports-section-heading">
                            <div>
                                <h2>Report filters</h2>
                                <p>{selectedReport.title}</p>
                            </div>
                            <CalendarDays size={21} />
                        </div>

                        <div className="reports-filters">
                            <div className="reports-field">
                                <label htmlFor="reports-from">From date</label>
                                <input
                                    id="reports-from"
                                    type="date"
                                    value={fromDate}
                                    max={toDate}
                                    onChange={event => setFromDate(event.target.value)}
                                />
                            </div>

                            <div className="reports-field">
                                <label htmlFor="reports-to">To date</label>
                                <input
                                    id="reports-to"
                                    type="date"
                                    value={toDate}
                                    min={fromDate}
                                    max={today()}
                                    onChange={event => setToDate(event.target.value)}
                                />
                            </div>

                            <button
                                className="reports-apply"
                                onClick={loadPreview}
                                disabled={loadingPreview || !fromDate || !toDate}
                            >
                                {loadingPreview ? 'Generating…' : 'Apply filters'}
                            </button>

                            <button
                                className="reports-export pdf"
                                onClick={() => exportReport('pdf')}
                                disabled={!!exporting || !selectedReport.exportSupported}
                            >
                                <FileText size={16} />
                                {exporting === 'pdf' ? 'Generating PDF…' : 'Export PDF'}
                            </button>

                            <button
                                className="reports-export excel"
                                onClick={() => exportReport('xlsx')}
                                disabled={!!exporting || !selectedReport.exportSupported}
                            >
                                <FileSpreadsheet size={16} />
                                {exporting === 'xlsx' ? 'Generating Excel…' : 'Export Excel'}
                            </button>
                        </div>
                    </section>

                    <section className="reports-section reports-preview">
                        <div className="reports-section-heading">
                            <div>
                                <h2>Report preview</h2>
                                <p>{preview?.title || selectedReport.title}</p>
                            </div>
                            {preview && (
                                <span className="reports-row-count">
                  {preview.totalRows} rows
                </span>
                            )}
                        </div>

                        {loadingPreview ? (
                            <div className="reports-empty">Generating report preview…</div>
                        ) : preview ? (
                            <>
                                <div className="reports-preview-meta">
                                    <span>From: {preview.fromDate}</span>
                                    <span>To: {preview.toDate}</span>
                                    <span>Total rows: {preview.totalRows}</span>
                                </div>

                                {preview.truncated && (
                                    <div className="reports-alert warning">
                                        <AlertCircle size={17} />
                                        Preview is truncated. Export may contain the full supported report data.
                                    </div>
                                )}

                                {totals.length > 0 && (
                                    <div className="reports-totals">
                                        {totals.map(([key, value]) => (
                                            <div className="reports-total-card" key={key}>
                                                <span>{key.replaceAll('_', ' ')}</span>
                                                <strong>{formatValue(value)}</strong>
                                            </div>
                                        ))}
                                    </div>
                                )}

                                {chartConfig && (
                                    <div className="reports-chart-panel">
                                        <h3>Report data chart</h3>
                                        <p>
                                            {chartConfig.valueKey} by {chartConfig.labelKey}
                                        </p>
                                        <div className="reports-chart">
                                            <ResponsiveContainer width="100%" height="100%">
                                                <BarChart data={chartConfig.data}>
                                                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                                                    <XAxis dataKey="label" />
                                                    <YAxis />
                                                    <Tooltip />
                                                    <Bar
                                                        dataKey="value"
                                                        name={chartConfig.valueKey}
                                                        fill="#4776e6"
                                                        radius={[5, 5, 0, 0]}
                                                    />
                                                </BarChart>
                                            </ResponsiveContainer>
                                        </div>
                                    </div>
                                )}

                                {preview.columns?.length ? (
                                    preview.rows?.length ? (
                                        <div className="reports-table-wrap">
                                            <table className="reports-table">
                                                <thead>
                                                <tr>
                                                    {preview.columns.map(column => (
                                                        <th key={column}>{column.replaceAll('_', ' ')}</th>
                                                    ))}
                                                </tr>
                                                </thead>
                                                <tbody>
                                                {preview.rows.map((row, index) => (
                                                    <tr key={index}>
                                                        {preview.columns.map(column => (
                                                            <td key={column}>
                                                                {formatValue(row[column])}
                                                            </td>
                                                        ))}
                                                    </tr>
                                                ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    ) : (
                                        <div className="reports-empty">No rows found for the selected period.</div>
                                    )
                                ) : (
                                    <div className="reports-empty">No report columns returned.</div>
                                )}
                            </>
                        ) : (
                            <div className="reports-empty">
                                Select a report and apply filters to view its preview.
                            </div>
                        )}
                    </section>
                </>
            )}
        </div>
    );
}
