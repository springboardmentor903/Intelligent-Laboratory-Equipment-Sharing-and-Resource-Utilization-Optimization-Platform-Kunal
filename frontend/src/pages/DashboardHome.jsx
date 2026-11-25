import { useAuth } from '../context/AuthContext';
import RoleDashboardContent from '../RoleDashboard';

export default function DashboardHome() {
    const { user } = useAuth();

    if (!user) {
        return (
            <div className="dashboard-page">
                <p>Loading dashboard...</p>
            </div>
        );
    }

    return <RoleDashboardContent />;
}