
import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function ProtectedRoute() {
  const { user } = useAuth()
  const token = localStorage.getItem('token')

  return user && token
      ? <Outlet />
      : <Navigate to="/login" replace />
}
