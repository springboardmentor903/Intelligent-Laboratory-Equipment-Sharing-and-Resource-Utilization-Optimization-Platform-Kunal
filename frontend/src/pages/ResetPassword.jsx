
import { useState } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { Lock, FlaskConical, ArrowLeft, Eye, EyeOff, CheckCircle2 } from 'lucide-react';
import api from '../api/client';

export default function ResetPassword() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    const token = searchParams.get('token');

    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    async function handleSubmit(e) {
        e.preventDefault();
        setError('');
        setSuccess('');

        if (!token) {
            setError('Reset link is invalid. Please request a new link.');
            return;
        }

        if (password.length < 8) {
            setError('Password must contain at least 8 characters.');
            return;
        }

        if (password !== confirmPassword) {
            setError('Passwords do not match.');
            return;
        }

        try {
            setLoading(true);

            await api.post('/users/reset-password', {
                token,
                newPassword: password,
            });

            setSuccess('Password reset successfully! Redirecting to login...');

            setTimeout(() => {
                navigate('/login', { replace: true });
            }, 2000);

        } catch (err) {
            setError(
                err.response?.data?.message ||
                err.response?.data?.error ||
                'Unable to reset password. The link may be expired or invalid.'
            );
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="auth-page">
            <div className="auth-card">
                <div className="auth-brand">
                    <div className="brand-icon">
                        <FlaskConical size={26} />
                    </div>
                    <div>
                        <strong>LabResource</strong>
                        <span>Management Platform</span>
                    </div>
                </div>

                <div className="auth-heading">
                    <div className="auth-icon">
                        <Lock size={25} />
                    </div>
                    <h1>Reset Password</h1>
                    <p>Create a new password for your account.</p>
                </div>

                {error && (
                    <div className="auth-error" role="alert">
                        {error}
                    </div>
                )}

                {success && (
                    <div className="auth-success" role="status">
                        <CheckCircle2 size={18} />
                        {success}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="new-password">New Password</label>
                        <div className="password-input-wrap">
                            <Lock size={18} />
                            <input
                                id="new-password"
                                type={showPassword ? 'text' : 'password'}
                                placeholder="Enter new password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                minLength={8}
                                maxLength={100}
                                required
                            />
                            <button
                                type="button"
                                className="password-toggle"
                                onClick={() => setShowPassword(!showPassword)}
                                aria-label="Toggle password visibility"
                            >
                                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                            </button>
                        </div>
                    </div>

                    <div className="form-group">
                        <label htmlFor="confirm-password">Confirm Password</label>
                        <div className="password-input-wrap">
                            <Lock size={18} />
                            <input
                                id="confirm-password"
                                type={showConfirmPassword ? 'text' : 'password'}
                                placeholder="Confirm new password"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                minLength={8}
                                maxLength={100}
                                required
                            />
                            <button
                                type="button"
                                className="password-toggle"
                                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                aria-label="Toggle confirm password visibility"
                            >
                                {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                            </button>
                        </div>
                    </div>

                    <p className="form-hint">
                        Password must be at least 8 characters long.
                    </p>

                    <button
                        type="submit"
                        className="auth-submit"
                        disabled={loading || !token}
                    >
                        {loading ? 'Resetting Password...' : 'Reset Password'}
                    </button>
                </form>

                <div className="auth-footer">
                    <Link to="/login" className="auth-back-link">
                        <ArrowLeft size={16} />
                        Back to Login
                    </Link>
                </div>
            </div>
        </div>
    );
}
