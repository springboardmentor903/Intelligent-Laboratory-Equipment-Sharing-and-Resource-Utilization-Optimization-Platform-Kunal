import { useEffect, useState } from 'react'
import { Activity, ArrowUpRight, CalendarDays, CheckCircle2, Clock3, Package, RefreshCw } from 'lucide-react'
import api from '../api/client'
import { useAuth } from '../context/AuthContext'

const asArray = value => Array.isArray(value) ? value : value?.content || value?.data || []
const statCards = [
  { key: 'resources', label: 'Total equipment', icon: Package, color: 'blue' },
  { key: 'available', label: 'Available now', icon: CheckCircle2, color: 'green' },
  { key: 'bookings', label: 'Total bookings', icon: CalendarDays, color: 'purple' },
  { key: 'pending', label: 'Pending requests', icon: Clock3, color: 'orange' },
]

export default function Dashboard() {
  const { user } = useAuth()
  const [resources, setResources] = useState([])
  const [bookings, setBookings] = useState([])
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true); setLoadError('')
    const [resourceResult, bookingResult] = await Promise.allSettled([api.get('/resources'), api.get('/bookings')])
    if (resourceResult.status === 'fulfilled') setResources(asArray(resourceResult.value.data))
    else setLoadError('Resources API available nahi hai ya access denied hai. Backend endpoint check karo.')
    if (bookingResult.status === 'fulfilled') setBookings(asArray(bookingResult.value.data))
    setLoading(false)
  }
  useEffect(() => { load() }, [])

  const available = resources.filter(r => String(r.status || '').toUpperCase() === 'AVAILABLE' || Number(r.availableQuantity ?? r.available_quantity) > 0).length
  const pending = bookings.filter(b => String(b.status || '').toUpperCase().includes('PENDING')).length
  const stats = { resources: resources.length, available, bookings: bookings.length, pending }
  const displayName = user?.fullName || user?.name || user?.email?.split('@')[0] || 'there'
  const recent = [...bookings].sort((a, b) => new Date(b.createdAt || b.created_at || b.startTime || b.start_time || 0) - new Date(a.createdAt || a.created_at || a.startTime || a.start_time || 0)).slice(0, 5)

  return <div className="dashboard-page"><div className="page-heading-row"><div><span className="eyebrow">OVERVIEW</span><h1>Good to see you, {displayName.split(' ')[0]}.</h1><p>Here's what's happening across your lab workspace today.</p></div><button className="secondary-button" onClick={load} disabled={loading}><RefreshCw size={16}/> Refresh</button></div>
    {loadError && <div className="notice warning">{loadError} Dashboard layout ready hai; live metrics backend endpoints se aayenge.</div>}
    <div className="stats-grid">{statCards.map(({ key, label, icon: Icon, color }) => <article className="stat-card" key={key}><div className={`stat-icon ${color}`}><Icon size={20}/></div><div className="stat-label">{label}</div><div className="stat-number">{loading ? '—' : stats[key]}</div><div className="stat-foot"><span className="tiny-dot"/> {loading ? 'Loading data' : 'Live backend data'}</div></article>)}</div>
    <div className="dashboard-grid"><section className="panel"><div className="panel-heading"><div><h2>Recent bookings</h2><p>Latest requests and reservations</p></div><a href="/bookings" className="text-link">View all <ArrowUpRight size={15}/></a></div>{recent.length ? <div className="table-scroll"><table><thead><tr><th>Booking</th><th>Equipment</th><th>Date</th><th>Status</th></tr></thead><tbody>{recent.map((b, i) => <tr key={b.id ?? i}><td>#{b.id ?? '—'}</td><td>{b.resourceName || b.resource?.name || b.name || `Resource ${b.resourceId ?? b.resource_id ?? ''}`}</td><td>{(b.startTime || b.start_time) ? new Date(b.startTime || b.start_time).toLocaleDateString() : '—'}</td><td><span className={`status-chip ${String(b.status || 'pending').toLowerCase().replaceAll(' ', '-')}`}>{b.status || 'Unknown'}</span></td></tr>)}</tbody></table></div> : <div className="empty-state"><CalendarDays size={28}/><strong>{loading ? 'Loading bookings…' : 'No bookings to show yet'}</strong><span>When bookings are created, their latest status will appear here.</span></div>}</section>
      <section className="panel activity-panel"><div className="panel-heading"><div><h2>Workspace health</h2><p>Quick system snapshot</p></div><Activity size={20} className="muted-icon"/></div><div className="health-row"><span className="health-symbol green"><CheckCircle2 size={17}/></span><div><strong>Authentication</strong><span>Login and OAuth flow configured</span></div><span className="health-state">Configured</span></div><div className="health-row"><span className="health-symbol blue"><Package size={17}/></span><div><strong>Equipment inventory</strong><span>{resources.length} resources returned by API</span></div><span className="health-state">{loading ? 'Loading' : resources.length ? 'Connected' : 'Check API'}</span></div><div className="health-row"><span className="health-symbol purple"><CalendarDays size={17}/></span><div><strong>Booking service</strong><span>{bookings.length} booking records returned</span></div><span className="health-state">{loading ? 'Loading' : 'Checked'}</span></div><div className="tip-card"><strong>Quick tip</strong><p>Check equipment availability before creating a booking request.</p><a href="/equipment">Explore equipment <ArrowUpRight size={15}/></a></div></section></div>
  </div>
}
