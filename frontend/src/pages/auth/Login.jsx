
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
    FlaskConical,
    Mail,
    Lock,
    Eye,
    EyeOff,
    ArrowRight,
} from 'lucide-react';

import api from '../../api/client';
import { useAuth } from '../../context/AuthContext';

const BACKEND_URL = (
    api.defaults.baseURL || 'http://localhost:8080/api'
).replace(/\/api\/?$/, '');

export default function Login() {
    const navigate = useNavigate();
    const { login } = useAuth();

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    function handleGoogleLogin() {
        window.location.assign(
            `${BACKEND_URL}/oauth2/authorization/google`
        );
    }

    async function handleSubmit(e) {
        e.preventDefault();

        if (loading) return;

        setLoading(true);
        setError('');

        try {
            const response = await api.post('/users/login', {
                email: email.trim(),
                password,
            });

            const data = response.data;

            console.log('LOGIN RESPONSE:', data);
            console.log('LOGIN USER:', data.user);

            if (!data?.token) {
                console.error('Unexpected login response:', data);
                setError('Backend response mein JWT token nahi mila.');
                return;
            }

            login(data.token, data.user ?? data);
            navigate('/dashboard', { replace: true });

        } catch (err) {
            console.error('Login failed:', err);

            if (err.response) {
                const status = err.response.status;
                const message = err.response.data?.message;

                if (status === 401) {
                    setError('Email ya password galat hai.');
                } else if (status === 403) {
                    setError(
                        message || 'Access denied. Backend security configuration check karo.'
                    );
                } else {
                    setError(
                        message || `Login failed (${status}). Please try again.`
                    );
                }
            } else if (err.request) {
                setError(
                    'Backend se response nahi mila. Spring Boot ko port 8080 par check karo.'
                );
            } else {
                setError(err.message || 'Login karte waqt error aaya.');
            }
        } finally {
            setLoading(false);
        }
    }

    return (
        <main className="auth-page">
            <section className="auth-brand">
                <div className="brand-logo">
                    <FlaskConical size={30} />
                </div>

                <h1>
                    Lab Resource
                    <br />
                    Utilization Platform
                </h1>

                <p>Smarter laboratory management starts here.</p>

                <div className="brand-note">
                    <span className="status-dot" />
                    Equipment · Bookings · Resource Management
                </div>
            </section>

            <section className="auth-panel">
                <div className="auth-form-wrap">
                    <div className="mobile-brand">
                        <FlaskConical /> Lab Resource Platform
                    </div>

                    <span className="eyebrow">WELCOME BACK</span>

                    <h2>Sign in to your account</h2>

                    <p className="form-subtitle">
                        Enter your details to access your workspace.
                    </p>

                    {/* Google Login Button */}
                    <button
                        type="button"
                        onClick={handleGoogleLogin}
                        disabled={loading}
                        style={{
                            width: '100%',
                            minHeight: '46px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: '12px',
                            marginTop: '22px',
                            marginBottom: '20px',
                            padding: '12px 16px',
                            background: '#ffffff',
                            color: '#334155',
                            border: '1px solid #cbd5e1',
                            borderRadius: '8px',
                            fontSize: '15px',
                            fontWeight: 600,
                            cursor: loading ? 'not-allowed' : 'pointer',
                        }}
                    >
                        <span
                            style={{
                                fontSize: '21px',
                                fontWeight: 700,
                                color: '#4285F4',
                            }}
                        >
                            G
                        </span>
                        Continue with Google
                    </button>

                    <div
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '12px',
                            marginBottom: '20px',
                            color: '#94a3b8',
                            fontSize: '12px',
                        }}
                    >
                        <span style={{ flex: 1, height: '1px', background: '#e2e8f0' }} />
                        OR CONTINUE WITH EMAIL
                        <span style={{ flex: 1, height: '1px', background: '#e2e8f0' }} />
                    </div>

                    <form onSubmit={handleSubmit}>
                        <label htmlFor="email">Email address</label>

                        <div className="input-wrap">
                            <Mail size={18} />

                            <input
                                id="email"
                                type="email"
                                placeholder="you@institution.edu"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                autoComplete="email"
                                required
                                disabled={loading}
                            />
                        </div>

                        <div className="label-row">
                            <label htmlFor="password">Password</label>

                            <Link
                                to="/forgot-password"
                                className="text-button"
                                style={{
                                    position: 'relative',
                                    zIndex: 99999,
                                    display: 'inline-block',
                                    cursor: 'pointer',
                                    pointerEvents: 'auto',
                                    textDecoration: 'none',
                                    color: '#2563eb',
                                }}
                                onClick={(e) => e.stopPropagation()}
                            >
                                Forgot password?
                            </Link>
                        </div>

                        <div className="input-wrap">
                            <Lock size={18} />

                            <input
                                id="password"
                                type={showPassword ? 'text' : 'password'}
                                placeholder="Enter your password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                autoComplete="current-password"
                                required
                                disabled={loading}
                            />

                            <button
                                type="button"
                                className="icon-button"
                                aria-label="Toggle password visibility"
                                onClick={() => setShowPassword((value) => !value)}
                                disabled={loading}
                            >
                                {showPassword ? (
                                    <EyeOff size={18} />
                                ) : (
                                    <Eye size={18} />
                                )}
                            </button>
                        </div>

                        {error && (
                            <div className="notice error" role="alert">
                                {error}
                            </div>
                        )}

                        <button
                            className="primary-button"
                            type="submit"
                            disabled={loading}
                        >
                            {loading ? 'Signing in...' : 'Sign in'}
                            {!loading && <ArrowRight size={18} />}
                        </button>
                    </form>

                    <div className="auth-switch">
                        Don't have an account?{' '}
                        <Link to="/register">Create account</Link>
                    </div>
                </div>
            </section>
        </main>
    );
}
