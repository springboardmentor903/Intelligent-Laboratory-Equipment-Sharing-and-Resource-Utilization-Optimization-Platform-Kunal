import './BookingMaintenanceDetails.css'
import { useEffect, useState } from 'react'
import { AlertCircle, Clock3, RefreshCw, X } from 'lucide-react'
import api from '../api/client'


const formatDate = (value) => {
    if (!value) return '—'
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString()
}

export default function BookingMaintenanceDetails({ booking, onClose }) {
    const [history, setHistory] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    const bookingId = booking?.id ?? booking?.bookingId

    const loadHistory = async () => {
        if (!bookingId) {
            setError('Booking ID is missing.')
            setLoading(false)
            return
        }

        setLoading(true)
        setError('')

        try {
            const response = await api.get(`/bookings/${bookingId}/history`)
            setHistory(Array.isArray(response.data) ? response.data : [])
        } catch (err) {
            setError(
                err.response?.data?.message ||
                `Could not load booking history. HTTP ${err.response?.status ?? 'network error'}.`
            )
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadHistory()
    }, [bookingId])

    const isMaintenanceCancellation =
        String(booking?.statusRemark || '').toLowerCase().includes('maintenance')

    return (
        <div
            role="dialog"
            aria-modal="true"
            className="bmd-style-01"
        >
            <div className="bmd-style-02">
                <div className="bmd-style-03">
                    <div>
                        <h2 className="bmd-style-04">
                            Booking #{bookingId}
                        </h2>
                        <p className="bmd-style-05">
                            {booking?.resourceName || 'Equipment booking'}
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={onClose}
                        aria-label="Close"
                        className="bmd-style-06"
                    >
                        <X size={18} />
                    </button>
                </div>

                {String(booking?.status || '').toUpperCase() === 'CANCELLED' && (
                    <div className="bmd-style-07">
                        <div className="bmd-style-08">
                            <AlertCircle size={17} />
                            {isMaintenanceCancellation
                                ? 'Booking cancelled due to equipment maintenance'
                                : 'Booking cancelled'}
                        </div>

                        <p className="bmd-style-09">
                            **Reason:** {booking?.statusRemark || 'No cancellation reason provided.'}
                        </p>
                    </div>
                )}

                <div className="bmd-style-10">
                    <div>
                        <small className="bmd-style-11">Current status</small>
                        <div className="bmd-style-12">
                            {booking?.status || '—'}
                        </div>
                    </div>
                    <div>
                        <small className="bmd-style-13">Scheduled start</small>
                        <div className="bmd-style-14">
                            {formatDate(booking?.startTime)}
                        </div>
                    </div>
                    <div>
                        <small className="bmd-style-15">Scheduled end</small>
                        <div className="bmd-style-16">
                            {formatDate(booking?.endTime)}
                        </div>
                    </div>
                </div>

                <div className="bmd-style-17">
                    <h3 className="bmd-style-18">
                        <Clock3 size={16} className="bmd-style-19" />
                        Booking Status History
                    </h3>

                    <button
                        type="button"
                        onClick={loadHistory}
                        disabled={loading}
                        className="bmd-style-20"
                    >
                        <RefreshCw size={13} /> Refresh
                    </button>
                </div>

                {loading ? (
                    <p className="bmd-style-21">
                        Loading booking history...
                    </p>
                ) : error ? (
                    <div className="bmd-style-22">
                        {error}
                    </div>
                ) : history.length === 0 ? (
                    <p className="bmd-style-23">
                        No status history is available for this booking.
                    </p>
                ) : (
                    <div className="bmd-style-24">
                        {history.map((item) => (
                            <div
                                key={item.id}
                                className="bmd-style-25"
                            >
                <span className="bmd-style-26" />

                                <div className="bmd-style-27">
                                    {item.oldStatus || 'CREATED'} → {item.newStatus || '—'}
                                </div>

                                <div className="bmd-style-28">
                                    {formatDate(item.changedAt)}
                                    {' · '}
                                    {item.changedBy || 'System'}
                                </div>

                                {item.remark && (
                                    <p className="bmd-style-29">
                                        {item.remark}
                                    </p>
                                )}
                            </div>
                        ))}
                    </div>
                )}

                <div className="bmd-style-30">
                    <button
                        type="button"
                        onClick={onClose}
                        className="bmd-style-31"
                    >
                        Close
                    </button>
                </div>
            </div>
        </div>
    )
}
