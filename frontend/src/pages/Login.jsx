//
// import { useState } from 'react';
// import { Link, useNavigate } from 'react-router-dom';
// import {
//   Mail,
//   Lock,
//   Eye,
//   EyeOff,
//   ArrowRight,
// } from 'lucide-react';
// import api from '../../api/client';
// import { useAuth } from '../../context/AuthContext';
//
// export default function Login() {
//   const navigate = useNavigate();
//   const { login } = useAuth();
//
//   const [email, setEmail] = useState('');
//   const [password, setPassword] = useState('');
//   const [showPassword, setShowPassword] = useState(false);
//   const [loading, setLoading] = useState(false);
//   const [error, setError] = useState('');
//
//   const handleSubmit = async (e) => {
//     e.preventDefault();
//
//     if (loading) return;
//
//     setError('');
//     setLoading(true);
//
//     try {
//       const response = await api.post('/users/login', {
//         email: email.trim(),
//         password,
//       });
//
//       const data = response.data;
//
//       if (!data?.token) {
//         throw new Error('Login response mein token nahi mila.');
//       }
//
//       login(data.token, data.user ?? data);
//       navigate('/dashboard', { replace: true });
//
//     } catch (err) {
//       console.error('Login error:', err);
//
//       if (err.response) {
//         const message = err.response.data?.message;
//
//         setError(
//             message ||
//             (err.response.status === 401
//                 ? 'Email ya password galat hai.'
//                 : `Login failed (${err.response.status}). Backend response check karo.`)
//         );
//       } else if (err.request) {
//         setError(
//             'Backend se response nahi mila. Check karo Spring Boot port 8080 par chal raha hai.'
//         );
//       } else {
//         setError(err.message || 'Login karte waqt error aaya.');
//       }
//     } finally {
//       setLoading(false);
//     }
//   };
//
//   const openForgotPassword = (e) => {
//     e.preventDefault();
//     navigate('/forgot-password');
//   };
//
//   return (
//       <main className="auth-page">
//         <section className="auth-brand">
//           <div className="auth-brand-content">
//             <h1>LabResourcePlatform</h1>
//             <p>
//               Smart laboratory resource management and equipment sharing.
//             </p>
//           </div>
//         </section>
//
//         <section className="auth-panel">
//           <div className="auth-card">
//             <h2>Welcome back</h2>
//             <p>Sign in to access your laboratory dashboard.</p>
//
//             <form onSubmit={handleSubmit}>
//               <label htmlFor="email">Email address</label>
//
//               <div className="input-wrap">
//                 <Mail size={18} />
//                 <input
//                     id="email"
//                     type="email"
//                     placeholder="you@institution.edu"
//                     value={email}
//                     onChange={(e) => setEmail(e.target.value)}
//                     autoComplete="email"
//                     required
//                     disabled={loading}
//                 />
//               </div>
//
//               <div className="label-row">
//                 <span></span>
//
//                 <a
//                     href="/forgot-password"
//                     className="text-button"
//                     onClick={openForgotPassword}
//                     style={{
//                       position: 'relative',
//                       zIndex: 99999,
//                       display: 'inline-block',
//                       cursor: 'pointer',
//                       pointerEvents: 'auto',
//                       color: '#2563eb',
//                       textDecoration: 'none',
//                     }}
//                 >
//                   Forgot Password?
//                 </a>
//               </div>
//
//               <div className="input-wrap">
//                 <Lock size={18} />
//                 <input
//                     id="password"
//                     type={showPassword ? 'text' : 'password'}
//                     placeholder="Enter your password"
//                     value={password}
//                     onChange={(e) => setPassword(e.target.value)}
//                     autoComplete="current-password"
//                     required
//                     disabled={loading}
//                 />
//
//                 <button
//                     type="button"
//                     className="icon-button"
//                     aria-label="Toggle password visibility"
//                     onClick={() => setShowPassword((value) => !value)}
//                     disabled={loading}
//                 >
//                   {showPassword ? (
//                       <EyeOff size={18} />
//                   ) : (
//                       <Eye size={18} />
//                   )}
//                 </button>
//               </div>
//
//               {error && (
//                   <div className="notice error" role="alert">
//                     {error}
//                   </div>
//               )}
//
//               <button
//                   className="primary-button"
//                   type="submit"
//                   disabled={loading}
//               >
//                 {loading ? 'Signing in...' : 'Sign in'}
//                 {!loading && <ArrowRight size={18} />}
//               </button>
//             </form>
//
//             <div className="auth-switch">
//               Don't have an account?{' '}
//               <Link to="/register">Create account</Link>
//             </div>
//           </div>
//         </section>
//       </main>
//   );
// }
