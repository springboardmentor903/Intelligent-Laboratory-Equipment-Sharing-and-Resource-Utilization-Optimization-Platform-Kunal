
import Maintenance from "./pages/Maintenance";
import ResourceSharing from './pages/ResourceSharing';
import SharingAnalytics from './pages/SharingAnalytics';
import {
    BrowserRouter,
    Navigate,
    NavLink,
    Route,
    Routes,
    useLocation,
} from 'react-router-dom';

import {
    LayoutDashboard,
    Microscope,
    CalendarDays,
    UserRound,
    LogOut,
    Bell,
    Search,
    FlaskConical,
    ArrowUpRight,
    Clock3,
    CheckCircle2,
    AlertCircle,
    Menu,
    X,
    Wrench,
    ClipboardCheck,
    Building2,
    Landmark,
    ShieldCheck,
    Users,
    BarChart3,
    Activity,
} from 'lucide-react';

import { useState, useEffect } from 'react';

import LiveEquipmentStatus from './pages/LiveEquipmentStatus';
import ResourceUtilizationReport from './pages/ResourceUtilizationReport';
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import Equipment from './pages/Equipment';
import Bookings from './pages/Bookings';
import Profile from './pages/Profile';
import { AuthProvider, useAuth } from './context/AuthContext';
import api from './api/client';
import ForgotPassword from "./pages/ForgotPassword.jsx";
import ResetPassword from './pages/ResetPassword';
import OAuthCallback from './pages/OAuthCallback';
import './App.css';

function getRoleKey(user) {
    const role =
        user?.role?.name ??
        user?.role?.roleName ??
        user?.roleName ??
        (typeof user?.role === 'string' ? user.role : null) ??
        user?.authorities?.[0]?.authority ??
        '';

    return String(role).replace(/^ROLE_/, '').trim().toUpperCase();
}

function getRoleLabel(user) {
    const labels = {
        RESEARCHER_STUDENT: 'Researcher / Student',
        LAB_TECHNICIAN: 'Lab Technician',
        LAB_MANAGER: 'Lab Manager',
        DEPARTMENT_HEAD: 'Department Head',
        INSTITUTION_ADMIN: 'Institution Admin',
        SYSTEM_ADMIN: 'System Admin',
    };

    return labels[getRoleKey(user)] || 'Role not assigned';
}

function getDisplayName(user) {
    return (
        user?.fullName ||
        user?.name ||
        user?.firstName ||
        user?.email?.split('@')[0] ||
        'User'
    );
}

const STAFF_ROLES = [
    'LAB_TECHNICIAN',
    'LAB_MANAGER',
    'SYSTEM_ADMIN',
];

const roleNavigation = {
    RESEARCHER_STUDENT: [
        { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
        { to: '/equipment', label: 'Equipment', icon: Microscope },
        { to: '/bookings', label: 'My Bookings', icon: CalendarDays },
        { to: '/profile', label: 'My Profile', icon: UserRound },
    ],

    LAB_TECHNICIAN: [
        { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
        { to: '/equipment', label: 'Equipment', icon: Microscope },
        { to: '/bookings', label: 'Booking Operations', icon: ClipboardCheck },
        { to: '/maintenance', label: 'Maintenance', icon: Wrench },
        { to: '/profile', label: 'My Profile', icon: UserRound },
        { to: '/live-monitoring', label: 'Live Monitoring', icon: Activity },
    ],

    LAB_MANAGER: [
        { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
        { to: '/equipment', label: 'Equipment', icon: Microscope },
        { to: '/bookings', label: 'Booking Approvals', icon: ClipboardCheck },
        { to: '/maintenance', label: 'Maintenance', icon: Wrench },
        { to: '/profile', label: 'My Profile', icon: UserRound },
        { to: '/live-monitoring', label: 'Live Monitoring', icon: Activity },
        { to: '/reports', label: 'Utilization Reports', icon: BarChart3 },
        { to: '/resource-sharing', label: 'Resource Sharing', icon: Building2 },
        { to: '/sharing-analytics', label: 'Sharing Analytics', icon: BarChart3 },
    ],

    DEPARTMENT_HEAD: [
        { to: '/dashboard', label: 'Department Overview', icon: LayoutDashboard },
        { to: '/equipment', label: 'Equipment', icon: Microscope },
        { to: '/bookings', label: 'Booking Access', icon: CalendarDays },
        { to: '/maintenance', label: 'Maintenance', icon: Wrench },
        { to: '/profile', label: 'My Profile', icon: UserRound },
        { to: '/live-monitoring', label: 'Live Monitoring', icon: Activity },
        { to: '/reports', label: 'Utilization Reports', icon: BarChart3 },
        { to: '/sharing-analytics', label: 'Sharing Analytics', icon: BarChart3 },
    ],

    INSTITUTION_ADMIN: [
        { to: '/dashboard', label: 'Institution Overview', icon: LayoutDashboard },
        { to: '/equipment', label: 'Equipment', icon: Microscope },
        { to: '/bookings', label: 'Booking Access', icon: CalendarDays },
        { to: '/maintenance', label: 'Maintenance', icon: Wrench },
        { to: '/profile', label: 'My Profile', icon: UserRound },
        { to: '/live-monitoring', label: 'Live Monitoring', icon: Activity },
        { to: '/reports', label: 'Utilization Reports', icon: BarChart3 },
        { to: '/resource-sharing', label: 'Resource Sharing', icon: Building2 },
        { to: '/sharing-analytics', label: 'Sharing Analytics', icon: BarChart3 },
    ],

    SYSTEM_ADMIN: [
        { to: '/dashboard', label: 'System Overview', icon: LayoutDashboard },
        { to: '/equipment', label: 'Equipment', icon: Microscope },
        { to: '/bookings', label: 'Booking Operations', icon: ClipboardCheck },
        { to: '/maintenance', label: 'Maintenance', icon: Wrench },
        { to: '/profile', label: 'My Profile', icon: UserRound },
        { to: '/live-monitoring', label: 'Live Monitoring', icon: Activity },
        { to: '/reports', label: 'Utilization Reports', icon: BarChart3 },
        { to: '/resource-sharing', label: 'Resource Sharing', icon: Building2 },
        { to: '/sharing-analytics', label: 'Sharing Analytics', icon: BarChart3 },
    ],
};


function ProtectedRoute({ children, allowedRoles }) {
    const { user } = useAuth();
    const token = localStorage.getItem('token');
    const location = useLocation();

    if (!token || !user) {
        return <Navigate to="/login" replace state={{ from: location }} />;
    }

    const role = getRoleKey(user);

    if (allowedRoles && !allowedRoles.includes(role)) {
        return <Navigate to="/dashboard" replace />;
    }

    return children;
}

function DashboardLayout({ children, title, subtitle }) {
    const { user, logout } = useAuth();
    const [sidebarOpen, setSidebarOpen] = useState(false);

    const role = getRoleKey(user);
    const name = getDisplayName(user);
    const navItems = roleNavigation[role] || [
        { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
        { to: '/profile', label: 'My Profile', icon: UserRound },
    ];

    function handleLogout() {
        logout();
        window.location.replace('/login');
    }

    return (
        <div className="app-shell">
            {sidebarOpen && (
                <button
                    className="sidebar-overlay"
                    aria-label="Close navigation"
                    onClick={() => setSidebarOpen(false)}
                />
            )}

            <aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : ''}`}>
                <div className="sidebar-brand">
                    <div className="brand-icon">
                        <FlaskConical size={24} />
                    </div>
                    <div>
                        <strong>LabResource</strong>
                        <span>Management Platform</span>
                    </div>
                    <button
                        className="mobile-close"
                        aria-label="Close menu"
                        onClick={() => setSidebarOpen(false)}
                    >
                        <X size={20} />
                    </button>
                </div>

                <div className="sidebar-section-label">WORKSPACE</div>

                <nav className="sidebar-nav">
                    {navItems.map(({ to, label, icon: Icon }) => (
                        <NavLink
                            key={to}
                            to={to}
                            end={to === '/dashboard'}
                            className={({ isActive }) =>
                                `sidebar-link ${isActive ? 'active' : ''}`
                            }
                            onClick={() => setSidebarOpen(false)}
                        >
                            <Icon size={19} />
                            <span>{label}</span>
                        </NavLink>
                    ))}
                </nav>

                <div className="sidebar-bottom">
                    <div className="sidebar-user">
                        <div className="avatar">{name.charAt(0).toUpperCase()}</div>
                        <div className="sidebar-user-info">
                            <strong>{name}</strong>
                            <span>{getRoleLabel(user)}</span>
                        </div>
                    </div>

                    <button className="logout-button" onClick={handleLogout}>
                        <LogOut size={18} />
                        <span>Sign out</span>
                    </button>
                </div>
            </aside>

            <div className="main-area">
                <header className="topbar">
                    <button
                        className="mobile-menu"
                        aria-label="Open navigation"
                        onClick={() => setSidebarOpen(true)}
                    >
                        <Menu size={22} />
                    </button>

                    <div className="breadcrumb">
                        <span>Workspace</span>
                        <span>/</span>
                        <strong>{title}</strong>
                    </div>

                    <div className="topbar-right">
            <span className="system-status">
              <span className="online-dot" />
              Lab portal
            </span>
                        <div className="topbar-avatar">{name.charAt(0).toUpperCase()}</div>
                    </div>
                </header>

                <main className="page-content">
                    <div className="page-heading">
                        <div>
                            <p className="page-eyebrow">LAB RESOURCE PLATFORM</p>
                            <h1>{title}</h1>
                            <p className="page-subtitle">{subtitle}</p>
                        </div>
                    </div>
                    {children}
                </main>

                <footer className="app-footer">
                    Lab Resource Platform <span>·</span> Resource management workspace
                </footer>
            </div>
        </div>
    );
}

function Dashboard() {
    const { user } = useAuth();
    const role = getRoleKey(user);
    const name = getDisplayName(user);
    const isStudent = role === 'RESEARCHER_STUDENT';
    const canReview = STAFF_ROLES.includes(role);

    const [health, setHealth] = useState('checking');
    const [equipment, setEquipment] = useState([]);
    const [bookings, setBookings] = useState([]);
    const [loadError, setLoadError] = useState('');

    useEffect(() => {
        let active = true;

        async function loadDashboard() {
            const results = await Promise.allSettled([
                api.get('/health'),
                api.get('/resources'),
                ...(isStudent
                    ? [api.get('/bookings/my-bookings')]
                    : canReview
                        ? [api.get('/bookings')]
                        : []),
            ]);

            if (!active) return;

            setHealth(results[0].status === 'fulfilled' ? 'UP' : 'unavailable');

            if (results[1].status === 'fulfilled') {
                const data = results[1].value.data;
                const list = Array.isArray(data)
                    ? data
                    : data?.content ?? data?.items ?? data?.data ?? [];
                setEquipment(Array.isArray(list) ? list : []);
            }

            const bookingIndex = 2;
            if (isStudent || canReview) {
                const result = results[bookingIndex];

                if (result?.status === 'fulfilled') {
                    const data = result.value.data;
                    const list = Array.isArray(data)
                        ? data
                        : data?.content ?? data?.items ?? data?.data ?? [];
                    setBookings(Array.isArray(list) ? list : []);
                } else {
                    setLoadError('Bookings load nahi hui. Backend access aur role check karo.');
                }
            }
        }

        loadDashboard();

        return () => {
            active = false;
        };
    }, [isStudent, canReview]);

    const availableCount = equipment.filter((item) =>
        ['AVAILABLE', 'ACTIVE'].includes(
            String(item.status ?? item.availabilityStatus ?? '').toUpperCase()
        )
    ).length;

    const pendingCount = bookings.filter(
        (item) => String(item.status ?? '').toUpperCase() === 'PENDING'
    ).length;

    const activeBookings = bookings.filter((item) =>
        ['APPROVED', 'CONFIRMED', 'IN_USE'].includes(
            String(item.status ?? '').toUpperCase()
        )
    ).length;

    const titles = {
        RESEARCHER_STUDENT: {
            title: 'Dashboard',
            subtitle: 'Check resource availability and manage your reservations.',
        },
        LAB_TECHNICIAN: {
            title: 'Technician Dashboard',
            subtitle: 'Monitor equipment and manage laboratory sessions.',
        },
        LAB_MANAGER: {
            title: 'Lab Manager Dashboard',
            subtitle: 'Review booking requests and monitor lab resources.',
        },
        DEPARTMENT_HEAD: {
            title: 'Department Overview',
            subtitle: 'Review department resources and current inventory information.',
        },
        INSTITUTION_ADMIN: {
            title: 'Institution Overview',
            subtitle: 'Review institution resource inventory and system availability.',
        },
        SYSTEM_ADMIN: {
            title: 'System Administration',
            subtitle: 'Monitor platform health, resources and booking operations.',
        },
    };

    const config = titles[role] || {
        title: 'Dashboard',
        subtitle: 'Your laboratory resource workspace.',
    };

    const stats = [
        {
            label: 'Total Equipment',
            value: equipment.length,
            icon: Microscope,
            tone: 'blue',
            note: 'Resource records returned by API',
        },
        {
            label: 'Available Equipment',
            value: availableCount,
            icon: CheckCircle2,
            tone: 'green',
            note: 'Marked available or active',
        },
        ...(isStudent
            ? [{
                label: 'My Active Bookings',
                value: activeBookings,
                icon: CalendarDays,
                tone: 'purple',
                note: 'Approved or currently in use',
            }]
            : []),
        ...(canReview
            ? [{
                label: 'Pending Approvals',
                value: pendingCount,
                icon: Clock3,
                tone: 'orange',
                note: 'Requests awaiting a decision',
            }]
            : []),
    ];

    return (
        <DashboardLayout title={config.title} subtitle={config.subtitle}>
            <section className="welcome-banner">
                <div>
                    <span className="welcome-tag">{getRoleLabel(user).toUpperCase()}</span>
                    <h2>Welcome back, {name}!</h2>
                    <p>
                        {isStudent
                            ? 'Explore laboratory resources and track your booking requests.'
                            : canReview
                                ? 'Review booking activity and manage resources assigned to your role.'
                                : 'View the available resource inventory and your account workspace.'}
                    </p>
                </div>
                <div className="welcome-art">
                    <FlaskConical size={74} strokeWidth={1.2} />
                </div>
            </section>

            <div className="section-heading">
                <div>
                    <h2>Workspace overview</h2>
                    <p>Information returned by the connected backend</p>
                </div>
                <span className={`health-pill ${health === 'UP' ? 'healthy' : ''}`}>
          <span className="online-dot" />
          Backend: {health === 'checking' ? 'Checking' : health}
        </span>
            </div>

            <section className="stats-grid">
                {stats.map(({ label, value, icon: Icon, tone, note }) => (
                    <article className="stat-card" key={label}>
                        <div className={`stat-icon ${tone}`}>
                            <Icon size={21} />
                        </div>
                        <p className="stat-label">{label}</p>
                        <div className="stat-value">{value}</div>
                        <p className="stat-note">{note}</p>
                    </article>
                ))}
            </section>

            {loadError && (
                <div className="dashboard-notice">
                    <AlertCircle size={18} />
                    <span>{loadError}</span>
                </div>
            )}

            <section className="dashboard-grid">
                <article className="panel-card">
                    <div className="panel-heading">
                        <div>
                            <h2>Recent equipment</h2>
                            <p>Resource inventory returned by the API</p>
                        </div>
                        <NavLink to="/equipment" className="text-link">
                            View all <ArrowUpRight size={16} />
                        </NavLink>
                    </div>

                    {equipment.length ? (
                        <div className="table-scroll">
                            <table className="data-table">
                                <thead>
                                <tr>
                                    <th>Equipment</th>
                                    <th>Location</th>
                                    <th>Status</th>
                                </tr>
                                </thead>
                                <tbody>
                                {equipment.slice(0, 5).map((item, index) => (
                                    <tr key={item.id ?? item.resourceId ?? index}>
                                        <td>
                                            <div className="table-primary">
                                                <div className="table-item-icon">
                                                    <Microscope size={17} />
                                                </div>
                                                <span>{item.name ?? item.resourceName ?? 'Equipment'}</span>
                                            </div>
                                        </td>
                                        <td>{item.location ?? item.labName ?? '—'}</td>
                                        <td>
                        <span className="status-badge">
                          {String(item.status ?? 'Unknown').replaceAll('_', ' ')}
                        </span>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    ) : (
                        <div className="empty-state">
                            <Search size={24} />
                            <strong>No equipment data to show</strong>
                            <p>The resource API returned no equipment records.</p>
                            <NavLink to="/equipment" className="secondary-button">
                                Open equipment
                            </NavLink>
                        </div>
                    )}
                </article>

                <article className="panel-card">
                    <div className="panel-heading">
                        <div>
                            <h2>{canReview ? 'Recent booking requests' : 'My bookings'}</h2>
                            <p>{canReview ? 'Latest booking activity' : 'Your latest reservations'}</p>
                        </div>
                        <NavLink to="/bookings" className="text-link">
                            View all <ArrowUpRight size={16} />
                        </NavLink>
                    </div>

                    {bookings.length ? (
                        <div className="booking-list">
                            {bookings.slice(0, 5).map((booking, index) => (
                                <div className="booking-row" key={booking.id ?? index}>
                                    <div className="booking-icon">
                                        <CalendarDays size={19} />
                                    </div>
                                    <div className="booking-info">
                                        <strong>
                                            {booking.resourceName ??
                                                booking.resource?.name ??
                                                'Equipment booking'}
                                        </strong>
                                        <span>
                      {booking.startTime
                          ? new Date(booking.startTime).toLocaleString()
                          : 'Date not available'}
                    </span>
                                    </div>
                                    <span className="status-badge">
                    {String(booking.status ?? 'Unknown').replaceAll('_', ' ')}
                  </span>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="empty-state">
                            <CalendarDays size={24} />
                            <strong>No booking data to show</strong>
                            <p>
                                {canReview
                                    ? 'No bookings were returned by the backend.'
                                    : isStudent
                                        ? 'Your reservations will appear here.'
                                        : 'Booking statistics are not available for this role yet.'}
                            </p>
                            {(isStudent || canReview) && (
                                <NavLink to="/bookings" className="secondary-button">
                                    {canReview ? 'Review bookings' : 'View bookings'}
                                </NavLink>
                            )}
                        </div>
                    )}
                </article>
            </section>

            <section className="system-strip">
                <div className="system-strip-icon">
                    <Bell size={20} />
                </div>
                <div>
                    <strong>Laboratory resource workspace</strong>
                    <p>Only data available from connected backend endpoints is displayed.</p>
                </div>
                <NavLink to="/equipment" className="text-link">
                    Explore equipment <ArrowUpRight size={16} />
                </NavLink>
            </section>
        </DashboardLayout>
    );
}

function AppRoutes() {
    const { user } = useAuth();
    const token = localStorage.getItem('token');
    const isLoggedIn = Boolean(token && user);

    return (
        <Routes>
            <Route
                path="/"
                element={<Navigate to={isLoggedIn ? '/dashboard' : '/login'} replace />}
            />
            <Route
                path="/login"
                element={isLoggedIn ? <Navigate to="/dashboard" replace /> : <Login />}
            />
            <Route path="/register" element={<Register />} />

            <Route
                path="/dashboard"
                element={
                    <ProtectedRoute>
                        <Dashboard />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/equipment"
                element={
                    <ProtectedRoute>
                        <DashboardLayout
                            title="Equipment"
                            subtitle="Browse laboratory resource inventory."
                        >
                            <Equipment />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            <Route
                path="/bookings"
                element={
                    <ProtectedRoute>
                        <DashboardLayout
                            title="Bookings"
                            subtitle="Manage bookings available to your account role."
                        >
                            <Bookings />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />
            <Route
                path="/live-monitoring"
                element={
                    <ProtectedRoute
                        allowedRoles={[
                            'LAB_TECHNICIAN',
                            'LAB_MANAGER',
                            'DEPARTMENT_HEAD',
                            'INSTITUTION_ADMIN',
                            'SYSTEM_ADMIN',
                        ]}
                    >
                        <DashboardLayout
                            title="Live Monitoring"
                            subtitle="Monitor live equipment utilization and availability."
                        >
                            <LiveEquipmentStatus />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            <Route
                path="/reports"
                element={
                    <ProtectedRoute
                        allowedRoles={[
                            'LAB_MANAGER',
                            'DEPARTMENT_HEAD',
                            'INSTITUTION_ADMIN',
                            'SYSTEM_ADMIN',
                        ]}
                    >
                        <DashboardLayout
                            title="Utilization Reports"
                            subtitle="Analyze scheduled bookings and actual equipment usage."
                        >
                            <ResourceUtilizationReport />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            <Route
                path="/resource-sharing"
                element={
                    <ProtectedRoute
                        allowedRoles={[
                            'LAB_MANAGER',
                            'INSTITUTION_ADMIN',
                            'SYSTEM_ADMIN',
                        ]}
                    >
                        <DashboardLayout
                            title="Resource Sharing"
                            subtitle="Discover shared resources and manage inter-institution booking requests."
                        >
                            <ResourceSharing />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            <Route
                path="/sharing-analytics"
                element={
                    <ProtectedRoute
                        allowedRoles={[
                            'LAB_MANAGER',
                            'DEPARTMENT_HEAD',
                            'INSTITUTION_ADMIN',
                            'SYSTEM_ADMIN',
                        ]}
                    >
                        <DashboardLayout
                            title="Sharing Analytics"
                            subtitle="Analyze resource sharing, partner institutions and sharing fees."
                        >
                            <SharingAnalytics />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            <Route
                path="/maintenance"
                element={
                    <ProtectedRoute
                        allowedRoles={[
                            'LAB_TECHNICIAN',
                            'LAB_MANAGER',
                            'SYSTEM_ADMIN',
                        ]}
                    >
                        <DashboardLayout
                            title="Maintenance"
                            subtitle="Manage laboratory equipment maintenance requests."
                        >
                            <Maintenance />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />


            <Route
                path="/profile"
                element={
                    <ProtectedRoute>
                        <DashboardLayout
                            title="My Profile"
                            subtitle="View your account information."
                        >
                            <Profile />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            {/* Public password recovery routes */}
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />

            {/* Google OAuth callback must be before the fallback route */}
            <Route
                path="/oauth2/callback"
                element={<OAuthCallback />}
            />

            {/* Keep the fallback route last */}
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>

    );
}

export default function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <AppRoutes />
            </AuthProvider>
        </BrowserRouter>
    );
}
