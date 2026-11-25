import Maintenance from "./pages/Maintenance";
import ResourceSharing from './pages/ResourceSharing';
import SharingAnalytics from './pages/SharingAnalytics';
import {
    BrowserRouter,
    Link,
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
    Users,
    BarChart3,
    Activity,
    History,
    Wallet,
    ReceiptText,
} from 'lucide-react';

import { useState, useEffect } from 'react';

import CalibrationOverview from './pages/t9/CalibrationOverview';
import CalibrationReminders from './pages/t9/CalibrationReminders';
import MyTasks from './pages/t9/MyTasks';
import Technicians from './pages/t9/Technicians';
import MaintenanceOverview from './pages/MaintenanceOverview';
import MaintenanceHistory from './pages/MaintenanceHistory';
import MaintenanceSchedules from './pages/MaintenanceSchedules';
import MaintenanceRequests from './pages/MaintenanceRequests';
import WorkOrders from "./pages/WorkOrders";
import LiveEquipmentStatus from './pages/LiveEquipmentStatus';
import Reports from './pages/Reports';
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import Equipment from './pages/Equipment';
import Bookings from './pages/Bookings';
import Profile from './pages/Profile';
import { AuthProvider, useAuth } from './context/AuthContext';
import api from './api/client';
import RoleDashboardContent from './RoleDashboard';
import InstitutionAdminDashboard from './pages/InstitutionAdminDashboard';
import ForgotPassword from "./pages/ForgotPassword.jsx";
import ResetPassword from './pages/ResetPassword';
import OAuthCallback from './pages/OAuthCallback';
import { MyUsageCharges, CostOverview } from './pages/t10';
import Budgets from './pages/t10/Budgets';
import Invoices from './pages/t10/Invoices';
import FinancialSummary from './pages/t10/FinancialSummary';

// T13 Notifications
import NotificationBell from './components/notifications/NotificationBell';
import Notifications from './pages/notifications/Notifications';
import NotificationPreferences from './pages/notifications/NotificationPreferences';
import {
    connectNotificationSocket,
    disconnectNotificationSocket,
} from './services/notificationSocket';
import {
    listenForForegroundMessages,
    disableWebPush,
} from './services/firebaseMessaging';

import './styles/notifications.css';
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
        { to: '/my-usage-charges', label: 'My Usage Charges', icon: Wallet },
        { to: '/equipment', label: 'Equipment', icon: Microscope },
        { to: '/bookings', label: 'My Bookings', icon: CalendarDays },
        { to: '/maintenance', label: 'Maintenance', icon: Wrench },
        { to: '/maintenance-requests', label: 'Maintenance Requests', icon: ClipboardCheck },
        { to: '/profile', label: 'My Profile', icon: UserRound },
    ],

    LAB_TECHNICIAN: [
        { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
        { to: '/my-usage-charges', label: 'My Usage Charges', icon: Wallet },
        { to: '/equipment', label: 'Equipment', icon: Microscope },
        { to: '/bookings', label: 'Booking Operations', icon: ClipboardCheck },
        { to: '/maintenance', label: 'Maintenance', icon: Wrench },
        { to: '/maintenance-history', label: 'Maintenance History', icon: History },
        { to: '/maintenance-requests', label: 'Maintenance Requests', icon: ClipboardCheck },
        { to: '/work-orders', label: 'Work Orders', icon: ClipboardCheck },
        { to: '/my-tasks', label: 'My Tasks', icon: ClipboardCheck },
        { to: '/profile', label: 'My Profile', icon: UserRound },
        { to: '/live-monitoring', label: 'Live Monitoring', icon: Activity },
    ],

    LAB_MANAGER: [
        { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
        { to: '/my-usage-charges', label: 'My Usage Charges', icon: Wallet },
        { to: '/equipment', label: 'Equipment', icon: Microscope },
        { to: '/bookings', label: 'Booking Approvals', icon: ClipboardCheck },
        { to: '/maintenance', label: 'Maintenance', icon: Wrench },
        { to: '/maintenance-overview', label: 'Maintenance Overview', icon: BarChart3 },
        { to: '/maintenance-history', label: 'Maintenance History', icon: History },
        { to: '/maintenance-requests', label: 'Maintenance Requests', icon: ClipboardCheck },
        { to: '/maintenance-schedules', label: 'Preventive Maintenance', icon: CalendarDays },
        { to: '/work-orders', label: 'Work Orders', icon: ClipboardCheck },
        { to: '/calibration-overview', label: 'Calibration Overview', icon: FlaskConical },
        { to: '/calibration-reminders', label: 'Calibration Reminders', icon: Bell },
        { to: '/technicians', label: 'Technician Workload', icon: Users },
        { to: '/profile', label: 'My Profile', icon: UserRound },
        { to: '/live-monitoring', label: 'Live Monitoring', icon: Activity },
        { to: '/reports', label: 'Reports & Export', icon: BarChart3 },
        { to: '/resource-sharing', label: 'Resource Sharing', icon: Building2 },
        { to: '/sharing-analytics', label: 'Sharing Analytics', icon: BarChart3 },
    ],

    DEPARTMENT_HEAD: [
        { to: '/dashboard', label: 'Department Overview', icon: LayoutDashboard },
        { to: '/my-usage-charges', label: 'My Usage Charges', icon: Wallet },
        { to: '/budgets', label: 'Department Budgets', icon: ReceiptText },
        { to: '/invoices', label: 'Department Invoices', icon: ReceiptText },
        { to: '/equipment', label: 'Equipment', icon: Microscope },
        { to: '/bookings', label: 'Booking Access', icon: CalendarDays },
        { to: '/profile', label: 'My Profile', icon: UserRound },
        { to: '/maintenance-requests', label: 'Maintenance Requests', icon: ClipboardCheck },
        { to: '/maintenance-history', label: 'Maintenance History', icon: History },
        { to: '/maintenance-schedules', label: 'Preventive Maintenance', icon: CalendarDays },
        { to: '/live-monitoring', label: 'Live Monitoring', icon: Activity },
        { to: '/reports', label: 'Reports & Export', icon: BarChart3 },
        { to: '/sharing-analytics', label: 'Sharing Analytics', icon: BarChart3 },
    ],

    INSTITUTION_ADMIN: [
        { to: '/dashboard', label: 'Institution Overview', icon: LayoutDashboard },
        { to: '/my-usage-charges', label: 'My Usage Charges', icon: Wallet },
        { to: '/cost-overview', label: 'Cost Overview', icon: BarChart3 },
        { to: '/budgets', label: 'Department Budgets', icon: ReceiptText },
        { to: '/invoices', label: 'Invoices', icon: ReceiptText },
        { to: '/financial-summary', label: 'Financial Summary', icon: BarChart3 },
        { to: '/equipment', label: 'Equipment', icon: Microscope },
        { to: '/bookings', label: 'Booking Access', icon: CalendarDays },
        { to: '/maintenance', label: 'Maintenance', icon: Wrench },
        { to: '/maintenance-history', label: 'Maintenance History', icon: History },
        { to: '/calibration-overview', label: 'Calibration Overview', icon: FlaskConical },
        { to: '/calibration-reminders', label: 'Calibration Reminders', icon: Bell },
        { to: '/technicians', label: 'Technician Workload', icon: Users },
        { to: '/profile', label: 'My Profile', icon: UserRound },
        { to: '/live-monitoring', label: 'Live Monitoring', icon: Activity },
        { to: '/reports', label: 'Reports & Export', icon: BarChart3 },
        { to: '/resource-sharing', label: 'Resource Sharing', icon: Building2 },
        { to: '/sharing-analytics', label: 'Sharing Analytics', icon: BarChart3 },
    ],

    SYSTEM_ADMIN: [
        { to: '/dashboard', label: 'System Overview', icon: LayoutDashboard },
        { to: '/my-usage-charges', label: 'My Usage Charges', icon: Wallet },
        { to: '/cost-overview', label: 'Cost Overview', icon: BarChart3 },
        { to: '/budgets', label: 'Department Budgets', icon: ReceiptText },
        { to: '/invoices', label: 'Invoices', icon: ReceiptText },
        { to: '/financial-summary', label: 'Financial Summary', icon: BarChart3 },
        { to: '/equipment', label: 'Equipment', icon: Microscope },
        { to: '/bookings', label: 'Booking Operations', icon: ClipboardCheck },
        { to: '/calibration-overview', label: 'Calibration Overview', icon: FlaskConical },
        { to: '/calibration-reminders', label: 'Calibration Reminders', icon: Bell },
        { to: '/technicians', label: 'Technician Workload', icon: Users },
        { to: '/maintenance', label: 'Maintenance', icon: Wrench },
        { to: '/profile', label: 'My Profile', icon: UserRound },
        { to: '/live-monitoring', label: 'Live Monitoring', icon: Activity },
        { to: '/reports', label: 'Reports & Export', icon: BarChart3 },
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
    const [unreadCount, setUnreadCount] = useState(0);

    const role = getRoleKey(user);
    const name = getDisplayName(user);

    const navItems = roleNavigation[role] || [
        { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
        { to: '/profile', label: 'My Profile', icon: UserRound },
    ];

    // T13: Notification WebSocket + polling fallback
    useEffect(() => {
        const token = localStorage.getItem('token');
        const userId = user?.id ?? user?.userId;

        if (!token || !userId) {
            disconnectNotificationSocket();
            setUnreadCount(0);
            return undefined;
        }

        let active = true;
        let unsubscribeForeground = () => {};

        const loadUnreadCount = async () => {
            try {
                const response = await api.get('/notifications/unread-count');

                if (!active) return;

                const count =
                    response.data?.unreadCount ??
                    response.data?.count ??
                    0;

                setUnreadCount(Number(count) || 0);
            } catch (error) {
                console.warn('Unread count could not be loaded:', error);
            }
        };

        loadUnreadCount();

        const pollingId = window.setInterval(loadUnreadCount, 60000);

        try {
            connectNotificationSocket({
                token,
                userId,
                onUnreadCount: (count) => {
                    if (active) {
                        setUnreadCount(Number(count) || 0);
                    }
                },
                onError: (error) => {
                    console.warn('Notification socket:', error);
                },
            });
        } catch (error) {
            console.warn('Notification socket unavailable:', error);
        }

        listenForForegroundMessages((payload) => {
            if (!active) return;

            const title =
                payload?.notification?.title || 'Lab Resource Platform';

            const body =
                payload?.notification?.body || 'You have a new notification.';

            if (
                'Notification' in window &&
                Notification.permission === 'granted'
            ) {
                new Notification(title, { body });
            }

            loadUnreadCount();
        })
            .then((unsubscribe) => {
                if (active && typeof unsubscribe === 'function') {
                    unsubscribeForeground = unsubscribe;
                } else if (typeof unsubscribe === 'function') {
                    unsubscribe();
                }
            })
            .catch((error) => {
                console.warn(
                    'Foreground notification listener unavailable:',
                    error
                );
            });

        return () => {
            active = false;
            window.clearInterval(pollingId);
            unsubscribeForeground();
            disconnectNotificationSocket();
        };
    }, [user?.id, user?.userId]);

    function handleLogout() {
        const authToken = localStorage.getItem('token');

        // Start push cleanup in the background; never block the logout flow.
        Promise.resolve(
            disableWebPush({
                authToken,
                apiBaseUrl: import.meta.env.VITE_API_BASE_URL,
            })
        ).catch((error) => {
            console.warn('Push device cleanup skipped:', error);
        });

        disconnectNotificationSocket();
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

                    <NavLink
                        to="/notifications"
                        className={({ isActive }) =>
                            `sidebar-link ${isActive ? 'active' : ''}`
                        }
                        onClick={() => setSidebarOpen(false)}
                    >
                        <Bell size={19} />
                        <span>Notifications</span>

                        {unreadCount > 0 && (
                            <span className="sidebar-notification-count">
                                {unreadCount > 99 ? '99+' : unreadCount}
                            </span>
                        )}
                    </NavLink>

                    <NavLink
                        to="/notification-preferences"
                        className={({ isActive }) =>
                            `sidebar-link ${isActive ? 'active' : ''}`
                        }
                        onClick={() => setSidebarOpen(false)}
                    >
                        <Bell size={19} />
                        <span>Notification Settings</span>
                    </NavLink>
                </nav>

                <div className="sidebar-bottom">
                    <div className="sidebar-user">
                        <div className="avatar">
                            {name.charAt(0).toUpperCase()}
                        </div>

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

                        <NotificationBell
                            unreadCount={unreadCount}
                            onUnreadCountChange={setUnreadCount}
                        />

                        <Link
                            to="/profile"
                            className="topbar-avatar"
                            aria-label="Open my profile"
                            title="My Profile"
                        >
                            {name.charAt(0).toUpperCase()}
                        </Link>
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

    if (['RESEARCHER_STUDENT', 'LAB_MANAGER', 'DEPARTMENT_HEAD'].includes(role)) {
        const config = {
            RESEARCHER_STUDENT: { title: 'Researcher / Student Dashboard', subtitle: 'Your bookings, availability, usage and equipment recommendations.' },
            LAB_MANAGER: { title: 'Lab Manager Dashboard', subtitle: 'Institution utilization, booking approvals and maintenance overview.' },
            DEPARTMENT_HEAD: { title: 'Department Head Dashboard', subtitle: 'Department utilization, maintenance and sharing overview.' },
        }[role];

        return (
            <DashboardLayout title={config.title} subtitle={config.subtitle}>
                <RoleDashboardContent />
            </DashboardLayout>
        );
    }

    if (['INSTITUTION_ADMIN', 'SYSTEM_ADMIN'].includes(role)) {
        return (
            <DashboardLayout
                title={role === 'SYSTEM_ADMIN' ? 'System Administration' : 'Institution Overview'}
                subtitle="Institution utilization, sharing, procurement, lifecycle and system monitoring."
            >
                <InstitutionAdminDashboard user={user} />
            </DashboardLayout>
        );
    }

    return <LegacyDashboard />;
}

function LegacyDashboard() {
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

            setHealth(
                results[0].status === 'fulfilled' ? 'UP' : 'unavailable'
            );

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
                    setLoadError(
                        'Bookings load nahi hui. Backend access aur role check karo.'
                    );
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
            String(
                item.status ?? item.availabilityStatus ?? ''
            ).toUpperCase()
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
        <DashboardLayout
            title={config.title}
            subtitle={config.subtitle}
        >
            <section className="welcome-banner">
                <div>
                    <span className="welcome-tag">
                        {getRoleLabel(user).toUpperCase()}
                    </span>

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
                    <h2>Workspace Overview</h2>
                    <p>Current platform and resource information</p>
                </div>
            </div>

            {loadError && (
                <div className="dashboard-alert">
                    <AlertCircle size={17} />
                    {loadError}
                </div>
            )}

            <section className="stats-grid">
                {stats.map((stat) => {
                    const Icon = stat.icon;

                    return (
                        <article className={`stat-card ${stat.tone}`} key={stat.label}>
                            <div className="stat-card-top">
                                <span>{stat.label}</span>
                                <div className="stat-icon">
                                    <Icon size={20} />
                                </div>
                            </div>

                            <strong>{stat.value}</strong>
                            <p>{stat.note}</p>
                        </article>
                    );
                })}
            </section>

            <section className="dashboard-content-grid">
                <article className="dashboard-panel">
                    <div className="panel-heading">
                        <div>
                            <h3>Recent Equipment</h3>
                            <p>Equipment available in your institution</p>
                        </div>

                        <NavLink to="/equipment" className="text-link">
                            View all <ArrowUpRight size={16} />
                        </NavLink>
                    </div>

                    {equipment.length === 0 ? (
                        <div className="empty-state">
                            <Microscope size={28} />
                            <p>No equipment records available.</p>
                        </div>
                    ) : (
                        <div className="equipment-preview-list">
                            {equipment.slice(0, 5).map((item) => (
                                <div
                                    className="equipment-preview-item"
                                    key={item.id}
                                >
                                    <div className="equipment-preview-icon">
                                        <Microscope size={18} />
                                    </div>

                                    <div>
                                        <strong>
                                            {item.name || item.resourceName || 'Equipment'}
                                        </strong>
                                        <span>
                                            {item.category || item.departmentName || 'Laboratory resource'}
                                        </span>
                                    </div>

                                    <span className="equipment-status">
                                        {item.status || item.availabilityStatus || 'UNKNOWN'}
                                    </span>
                                </div>
                            ))}
                        </div>
                    )}
                </article>

                <article className="dashboard-panel">
                    <div className="panel-heading">
                        <div>
                            <h3>Platform Status</h3>
                            <p>Backend connectivity status</p>
                        </div>
                    </div>

                    <div className="platform-status-card">
                        <div className={`platform-status-icon ${health}`}>
                            {health === 'UP'
                                ? <CheckCircle2 size={24} />
                                : <AlertCircle size={24} />}
                        </div>

                        <div>
                            <strong>
                                {health === 'checking'
                                    ? 'Checking backend'
                                    : health === 'UP'
                                        ? 'Backend connected'
                                        : 'Backend unavailable'}
                            </strong>

                            <p>
                                {health === 'UP'
                                    ? 'The platform API is responding.'
                                    : health === 'checking'
                                        ? 'Waiting for API response.'
                                        : 'Check that the Spring Boot backend is running.'}
                            </p>
                        </div>
                    </div>

                    <div className="platform-quick-links">
                        <NavLink to="/bookings">
                            <CalendarDays size={17} />
                            <span>Bookings</span>
                            <ArrowUpRight size={15} />
                        </NavLink>

                        <NavLink to="/equipment">
                            <Microscope size={17} />
                            <span>Equipment</span>
                            <ArrowUpRight size={15} />
                        </NavLink>

                        <NavLink to="/notifications">
                            <Bell size={17} />
                            <span>Notifications</span>
                            <ArrowUpRight size={15} />
                        </NavLink>
                    </div>
                </article>
            </section>

            <section className="system-strip">
                <div className="system-strip-icon">
                    <Bell size={20} />
                </div>

                <div>
                    <strong>Laboratory resource workspace</strong>
                    <p>
                        Only data available from connected backend endpoints is displayed.
                    </p>
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
                element={
                    <Navigate
                        to={isLoggedIn ? '/dashboard' : '/login'}
                        replace
                    />
                }
            />

            <Route
                path="/login"
                element={
                    isLoggedIn
                        ? <Navigate to="/dashboard" replace />
                        : <Login />
                }
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
                            title="Reports & Export"
                            subtitle="Preview permitted reports and download PDF or Excel files."
                        >
                            <Reports />
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
                path="/work-orders"
                element={
                    <ProtectedRoute
                        allowedRoles={[
                            'LAB_MANAGER',
                            'LAB_TECHNICIAN',
                        ]}
                    >
                        <DashboardLayout
                            title="Work Orders"
                            subtitle="Manage equipment maintenance work orders."
                        >
                            <WorkOrders />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            {/* T9 - Calibration Overview */}
            <Route
                path="/calibration-overview"
                element={
                    <ProtectedRoute
                        allowedRoles={[
                            'LAB_MANAGER',
                            'INSTITUTION_ADMIN',
                            'SYSTEM_ADMIN',
                        ]}
                    >
                        <DashboardLayout
                            title="Calibration Overview"
                            subtitle="Track calibration status and upcoming due dates."
                        >
                            <CalibrationOverview />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            {/* T9 - Calibration Reminders */}
            <Route
                path="/calibration-reminders"
                element={
                    <ProtectedRoute
                        allowedRoles={[
                            'LAB_MANAGER',
                            'INSTITUTION_ADMIN',
                            'SYSTEM_ADMIN',
                        ]}
                    >
                        <DashboardLayout
                            title="Calibration Reminders"
                            subtitle="Manage upcoming and overdue calibration renewals."
                        >
                            <CalibrationReminders />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            {/* T9 - Technician My Tasks */}
            <Route
                path="/my-tasks"
                element={
                    <ProtectedRoute allowedRoles={['LAB_TECHNICIAN']}>
                        <DashboardLayout
                            title="My Tasks"
                            subtitle="Manage your assigned technician tasks."
                        >
                            <MyTasks />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            {/* T9 - Technician Workload */}
            <Route
                path="/technicians"
                element={
                    <ProtectedRoute
                        allowedRoles={[
                            'LAB_MANAGER',
                            'INSTITUTION_ADMIN',
                            'SYSTEM_ADMIN',
                        ]}
                    >
                        <DashboardLayout
                            title="Technician Workload"
                            subtitle="Monitor technician assignments and workload."
                        >
                            <Technicians />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            <Route
                path="/maintenance-requests"
                element={
                    <ProtectedRoute
                        allowedRoles={[
                            'RESEARCHER_STUDENT',
                            'DEPARTMENT_HEAD',
                            'LAB_TECHNICIAN',
                            'LAB_MANAGER',
                        ]}
                    >
                        <DashboardLayout
                            title="Maintenance Requests"
                            subtitle="Report and manage equipment maintenance requests"
                        >
                            <MaintenanceRequests />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            <Route
                path="/maintenance-schedules"
                element={
                    <ProtectedRoute allowedRoles={['LAB_MANAGER']}>
                        <DashboardLayout
                            title="Preventive Maintenance"
                            subtitle="Plan and manage recurring equipment maintenance"
                        >
                            <MaintenanceSchedules />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            <Route
                path="/maintenance-history"
                element={
                    <ProtectedRoute
                        allowedRoles={[
                            'LAB_MANAGER',
                            'LAB_TECHNICIAN',
                            'INSTITUTION_ADMIN',
                            'DEPARTMENT_HEAD',
                        ]}
                    >
                        <DashboardLayout
                            title="Maintenance History"
                            subtitle="Review maintenance records and equipment downtime"
                        >
                            <MaintenanceHistory />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            <Route
                path="/maintenance-overview"
                element={
                    <ProtectedRoute allowedRoles={['LAB_MANAGER']}>
                        <DashboardLayout
                            title="Maintenance Overview"
                            subtitle="Institution maintenance performance and downtime analytics"
                        >
                            <MaintenanceOverview />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            <Route
                path="/maintenance"
                element={
                    <ProtectedRoute
                        allowedRoles={[
                            'RESEARCHER_STUDENT',
                            'LAB_TECHNICIAN',
                            'LAB_MANAGER',
                            'DEPARTMENT_HEAD',
                            'INSTITUTION_ADMIN',
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

            {/* T10 - My Usage Charges */}
            <Route
                path="/my-usage-charges"
                element={
                    <ProtectedRoute>
                        <DashboardLayout
                            title="My Usage Charges"
                            subtitle="Review your equipment usage charges and billing records."
                        >
                            <MyUsageCharges />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            {/* T10 - Institution Cost Overview */}
            <Route
                path="/cost-overview"
                element={
                    <ProtectedRoute
                        allowedRoles={['INSTITUTION_ADMIN', 'SYSTEM_ADMIN']}
                    >
                        <DashboardLayout
                            title="Cost Overview"
                            subtitle="Analyze equipment usage costs and department allocation."
                        >
                            <CostOverview />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            {/* T10 - Department Budgets */}
            <Route
                path="/budgets"
                element={
                    <ProtectedRoute
                        allowedRoles={[
                            'INSTITUTION_ADMIN',
                            'SYSTEM_ADMIN',
                            'DEPARTMENT_HEAD',
                        ]}
                    >
                        <DashboardLayout
                            title="Department Budgets"
                            subtitle="Monitor department budgets, utilization and remaining balances."
                        >
                            <Budgets />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            {/* T10 - Invoices */}
            <Route
                path="/invoices"
                element={
                    <ProtectedRoute
                        allowedRoles={[
                            'INSTITUTION_ADMIN',
                            'SYSTEM_ADMIN',
                            'DEPARTMENT_HEAD',
                        ]}
                    >
                        <DashboardLayout
                            title="Invoices"
                            subtitle="Review issued, received and department chargeback invoices."
                        >
                            <Invoices />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            {/* T10 - Financial Summary */}
            <Route
                path="/financial-summary"
                element={
                    <ProtectedRoute
                        allowedRoles={['INSTITUTION_ADMIN', 'SYSTEM_ADMIN']}
                    >
                        <DashboardLayout
                            title="Financial Summary"
                            subtitle="Track institutional billing, collections and outstanding balances."
                        >
                            <FinancialSummary />
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

            {/* T13 - Notification Center */}
            <Route
                path="/notifications"
                element={
                    <ProtectedRoute>
                        <DashboardLayout
                            title="Notification Center"
                            subtitle="View and manage all your platform notifications."
                        >
                            <Notifications />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            {/* T13 - Notification Preferences */}
            <Route
                path="/notification-preferences"
                element={
                    <ProtectedRoute>
                        <DashboardLayout
                            title="Notification Preferences"
                            subtitle="Manage how you receive platform notifications."
                        >
                            <NotificationPreferences />
                        </DashboardLayout>
                    </ProtectedRoute>
                }
            />

            {/* Public password recovery routes */}
            <Route
                path="/forgot-password"
                element={<ForgotPassword />}
            />

            <Route
                path="/reset-password"
                element={<ResetPassword />}
            />

            {/* Google OAuth callback must be before the fallback route */}
            <Route
                path="/oauth2/callback"
                element={<OAuthCallback />}
            />

            {/* Keep the fallback route last */}
            <Route
                path="*"
                element={<Navigate to="/" replace />}
            />
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
