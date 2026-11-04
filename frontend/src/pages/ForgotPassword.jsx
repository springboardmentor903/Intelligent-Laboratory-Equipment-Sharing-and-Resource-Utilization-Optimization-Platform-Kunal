
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, FlaskConical, Mail } from 'lucide-react'
import api from '../api/client'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()

    setError('')
    setMessage('')

    const trimmedEmail = email.trim()

    if (!trimmedEmail) {
      setError('Please enter your email address.')
      return
    }

    setBusy(true)

    try {
      const { data } = await api.post('/users/forgot-password', {
        email: trimmedEmail,
      })

      setMessage(
          data?.message ||
          'If an account exists with this email, password reset instructions will be sent.'
      )
    } catch (err) {
      setError(
          err.response?.data?.message ||
          'Unable to process your request. Please try again later.'
      )
    } finally {
      setBusy(false)
    }
  }

  return (
      <div className="auth-page single-auth">
        <div className="auth-form-card">

          <div className="brand mobile-brand">
          <span className="brand-mark">
            <FlaskConical size={22} />
          </span>

            <span>
            Lab Resource
            <span className="brand-light">Platform</span>
          </span>
          </div>

          <div className="auth-heading">
            <span className="eyebrow">ACCOUNT RECOVERY</span>

            <h2>Forgot your password?</h2>

            <p>
              Enter your account email to request password reset instructions.
            </p>
          </div>

          <form className="stack-form" onSubmit={submit}>
            <div className="field">
              <label htmlFor="email">Email address</label>

              <div className="input-wrap">
                <Mail size={17} />

                <input
                    id="email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    disabled={busy}
                    placeholder="you@institution.edu"
                    autoComplete="email"
                />
              </div>
            </div>

            {error && (
                <div className="notice error" role="alert">
                  {error}
                </div>
            )}

            {message && (
                <div className="notice success" role="status">
                  {message}
                </div>
            )}

            <button
                type="submit"
                className="primary-button"
                disabled={busy || !email.trim()}
            >
              {busy ? 'Sending...' : 'Request reset'}
            </button>
          </form>

          <p className="auth-switch">
            <Link to="/login">
              <ArrowLeft size={15} />
              Back to sign in
            </Link>
          </p>

        </div>
      </div>
  )
}
