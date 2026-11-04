import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Activity, CalendarDays, FlaskConical, LayoutDashboard, LogOut, PackageSearch, UserRound } from 'lucide-react'
import { useAuth } from '../context/AuthContext'

const links = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/equipment', label: 'Equipment catalog', icon: PackageSearch },
  { to: '/bookings', label: 'Bookings & calendar', icon: CalendarDays },
  { to: '/profile', label: 'My profile', icon: UserRound },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const role = user?.role || user?.roleName || 'RESEARCHER_STUDENT'
  const name = user?.fullName || user?.name || user?.email || 'Platform user'

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><span className="brand-mark"><FlaskConical size={22}/></span><span>Lab Resource<span className="brand-light">Platform</span></span></div>
      <div className="side-label">WORKSPACE</div>
      <nav className="side-nav">{links.map(({ to, label, icon: Icon }) => <NavLink key={to} to={to} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}><Icon size={18}/><span>{label}</span></NavLink>)}</nav>
      <div className="sidebar-bottom"><div className="status-dot"/> Services connected <span className="online-dot"/></div>
    </aside>
    <main className="main-area">
      <header className="topbar"><div><div className="eyebrow">LAB RESOURCE MANAGEMENT</div><div className="topbar-title">Workspace</div></div><div className="user-menu"><div className="avatar">{String(name).charAt(0).toUpperCase()}</div><div className="user-copy"><strong>{name}</strong><span>{role.replaceAll('_', ' ')}</span></div><button className="icon-button" onClick={handleLogout} title="Logout" aria-label="Logout"><LogOut size={18}/></button></div></header>
      <div className="page-content"><Outlet /></div>
    </main>
  </div>
}
