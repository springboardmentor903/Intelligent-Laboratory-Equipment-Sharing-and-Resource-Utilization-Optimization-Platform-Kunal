
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function OAuthCallback() {
    const navigate = useNavigate();
    const { login } = useAuth();
    const processed = useRef(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (processed.current) return;
        processed.current = true;

        try {
            const params = new URLSearchParams(
                window.location.hash.substring(1)
            );

            const token = params.get('token');
            const email = params.get('email');
            const name = params.get('name');
            const role = params.get('role');

            // Remove token from browser address/history
            window.history.replaceState(
                null,
                '',
                window.location.pathname
            );

            if (!token || !email) {
                throw new Error('Google login token ya email nahi mila.');
            }

            const user = {
                email,
                fullName: name || '',
                role: role || 'UNKNOWN',
            };

            // Save JWT and user through existing AuthContext
            login(token, user);

            navigate('/dashboard', { replace: true });

        } catch (err) {
            console.error('Google OAuth callback error:', err);
            setError(err.message || 'Google login failed.');
        }
    }, [login, navigate]);

    if (error) {
        return (
            <main style={{
                minHeight: '100vh',
                display: 'grid',
                placeItems: 'center',
                padding: '24px',
            }}>
                <div style={{ textAlign: 'center' }}>
                    <h2>Google Login Failed</h2>
                    <p>{error}</p>
                    <button onClick={() => navigate('/login', { replace: true })}>
                        Back to Login
                    </button>
                </div>
            </main>
        );
    }

    return (
        <main style={{
            minHeight: '100vh',
            display: 'grid',
            placeItems: 'center',
        }}>
            <p>Google login processing... Please wait.</p>
        </main>
    );
}
