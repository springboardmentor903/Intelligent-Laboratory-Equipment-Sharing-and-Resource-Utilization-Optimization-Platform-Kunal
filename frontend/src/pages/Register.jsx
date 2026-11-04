
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ArrowRight,
  Building2,
  Eye,
  EyeOff,
  FlaskConical,
  GraduationCap,
  LockKeyhole,
  Mail,
  Phone,
  UserRound,
  Plus,
  X,
  MapPin,
} from 'lucide-react'
import api from '../api/client'

const roleOptions = [
  { id: 1, value: 'RESEARCHER_STUDENT', label: 'Researcher / Student' },
  { id: 2, value: 'LAB_TECHNICIAN', label: 'Lab Technician' },
  { id: 3, value: 'LAB_MANAGER', label: 'Lab Manager' },
  { id: 4, value: 'DEPARTMENT_HEAD', label: 'Department Head' },
  { id: 5, value: 'INSTITUTION_ADMIN', label: 'Institution Administrator' },
]

const initialForm = {
  fullName: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  roleId: '1',
  institutionId: '',
  departmentId: '',
}

const initialInstitute = {
  name: '',
  code: '',
  institutionType: 'UNIVERSITY',
  addressLine: '',
  city: '',
  state: 'Madhya Pradesh',
  country: 'India',
  contactEmail: '',
  contactPhone: '',
  website: '',
}

const initialDepartment = {
  name: '',
  code: '',
}

const inputStyle = {
  width: '100%',
  padding: '11px 12px',
  border: '1px solid #d7dce5',
  borderRadius: 8,
  fontSize: 14,
  boxSizing: 'border-box',
  background: '#fff',
  color: '#172033',
}

const addButtonStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: 8,
  width: '100%',
  marginTop: 10,
  padding: '11px 14px',
  border: '1px solid #2563eb',
  borderRadius: 8,
  background: '#eff6ff',
  color: '#1d4ed8',
  fontWeight: 700,
  fontSize: 14,
  cursor: 'pointer',
  boxSizing: 'border-box',
}

const overlayStyle = {
  position: 'fixed',
  inset: 0,
  background: 'rgba(15, 23, 42, 0.65)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  padding: 16,
  zIndex: 9999,
}

const modalStyle = {
  width: '100%',
  maxWidth: 540,
  maxHeight: '90vh',
  overflowY: 'auto',
  background: '#fff',
  borderRadius: 16,
  padding: 24,
  boxSizing: 'border-box',
  boxShadow: '0 20px 60px rgba(0,0,0,0.25)',
}

function getId(item) {
  return item?.id ?? item?.institutionId ?? item?.institution_id
}

function getName(item) {
  return item?.name ?? item?.institutionName ?? item?.departmentName ?? 'Unnamed'
}

function getErrorMessage(error, fallback) {
  return (
      error?.response?.data?.message ||
      error?.response?.data?.error ||
      fallback
  )
}

function ModalField({
                      label,
                      name,
                      value,
                      onChange,
                      required = false,
                      type = 'text',
                      placeholder,
                      options,
                      maxLength = 150,
                    }) {
  return (
      <div style={{ marginBottom: 14, minWidth: 0 }}>
        <label
            htmlFor={name}
            style={{
              display: 'block',
              marginBottom: 6,
              fontSize: 13,
              fontWeight: 600,
              color: '#344054',
            }}
        >
          {label}
        </label>

        {options ? (
            <select
                id={name}
                name={name}
                value={value}
                onChange={onChange}
                required={required}
                style={inputStyle}
            >
              {options.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
              ))}
            </select>
        ) : (
            <input
                id={name}
                name={name}
                type={type}
                value={value}
                onChange={onChange}
                required={required}
                placeholder={placeholder || label}
                maxLength={maxLength}
                style={inputStyle}
            />
        )}
      </div>
  )
}

export default function Register() {
  const navigate = useNavigate()

  const [form, setForm] = useState(initialForm)
  const [institutions, setInstitutions] = useState([])
  const [departments, setDepartments] = useState([])

  const [loadingInstitutions, setLoadingInstitutions] = useState(true)
  const [loadingDepartments, setLoadingDepartments] = useState(false)
  const [busy, setBusy] = useState(false)

  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const [showPassword, setShowPassword] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)

  const [showInstituteModal, setShowInstituteModal] = useState(false)
  const [showDepartmentModal, setShowDepartmentModal] = useState(false)

  const [newInstitute, setNewInstitute] = useState(initialInstitute)
  const [newDepartment, setNewDepartment] = useState(initialDepartment)

  const [savingInstitute, setSavingInstitute] = useState(false)
  const [savingDepartment, setSavingDepartment] = useState(false)

  const [modalError, setModalError] = useState('')
  const [modalSuccess, setModalSuccess] = useState('')

  async function loadInstitutions() {
    setLoadingInstitutions(true)

    try {
      const { data } = await api.get('/institutions')
      const list = Array.isArray(data) ? data : []

      setInstitutions(
          list.filter(
              (item) => item.isActive !== false && item.is_active !== false,
          ),
      )
    } catch (err) {
      setError(
          getErrorMessage(
              err,
              'Institute list load nahi hui. Backend check karo.',
          ),
      )
    } finally {
      setLoadingInstitutions(false)
    }
  }

  async function loadDepartments(institutionId) {
    if (!institutionId) {
      setDepartments([])
      setLoadingDepartments(false)
      return
    }

    setLoadingDepartments(true)

    try {
      const { data } = await api.get('/departments', {
        params: { institutionId },
      })

      const list = Array.isArray(data) ? data : []

      setDepartments(
          list.filter(
              (item) => item.isActive !== false && item.is_active !== false,
          ),
      )
    } catch (err) {
      setDepartments([])
      setError(
          getErrorMessage(
              err,
              'Department list load nahi hui. Backend check karo.',
          ),
      )
    } finally {
      setLoadingDepartments(false)
    }
  }

  useEffect(() => {
    loadInstitutions()
  }, [])

  useEffect(() => {
    setDepartments([])
    setForm((prev) => ({ ...prev, departmentId: '' }))

    if (form.institutionId) {
      loadDepartments(form.institutionId)
    } else {
      setLoadingDepartments(false)
    }
  }, [form.institutionId])

  function change(event) {
    const { name, value } = event.target

    setError('')
    setSuccess('')

    setForm((prev) => ({
      ...prev,
      [name]: value,
      ...(name === 'institutionId' ? { departmentId: '' } : {}),
    }))
  }

  function closeInstituteModal() {
    if (savingInstitute) return

    setShowInstituteModal(false)
    setModalError('')
    setModalSuccess('')
    setNewInstitute(initialInstitute)
  }

  function closeDepartmentModal() {
    if (savingDepartment) return

    setShowDepartmentModal(false)
    setModalError('')
    setModalSuccess('')
    setNewDepartment(initialDepartment)
  }

  async function addInstitute(event) {
    event.preventDefault()
    setModalError('')
    setModalSuccess('')

    const name = newInstitute.name.trim()
    const code = newInstitute.code.trim().toUpperCase()

    if (!name || !code) {
      setModalError('Institute ka naam aur code bharna zaroori hai.')
      return
    }

    setSavingInstitute(true)

    try {
      const payload = {
        ...newInstitute,
        name,
        code,
        addressLine: newInstitute.addressLine.trim() || null,
        city: newInstitute.city.trim() || null,
        state: newInstitute.state.trim() || null,
        country: newInstitute.country.trim() || null,
        contactEmail: newInstitute.contactEmail.trim() || null,
        contactPhone: newInstitute.contactPhone.trim() || null,
        website: newInstitute.website.trim() || null,
        isActive: true,
        utilizationTargetPercent: 80,
      }

      const { data: created } = await api.post('/institutions', payload)

      // Server se latest list reload karo.
      const { data: responseData } = await api.get('/institutions')
      const list = Array.isArray(responseData) ? responseData : []

      const createdInstitute =
          list.find((item) => String(getId(item)) === String(getId(created))) ||
          list.find(
              (item) =>
                  String(item.code || '').toUpperCase() === code ||
                  String(item.name || '').trim().toLowerCase() === name.toLowerCase(),
          ) ||
          created

      const createdId = getId(createdInstitute)

      setInstitutions(
          list.filter(
              (item) => item.isActive !== false && item.is_active !== false,
          ),
      )

      if (createdId != null) {
        setForm((prev) => ({
          ...prev,
          institutionId: String(createdId),
          departmentId: '',
        }))
      }

      setNewInstitute(initialInstitute)
      setModalSuccess('Institute successfully save ho gaya.')

      window.setTimeout(() => {
        setShowInstituteModal(false)
        setModalSuccess('')
      }, 700)
    } catch (err) {
      setModalError(
          getErrorMessage(
              err,
              'Institute save nahi hua. Backend POST /api/institutions check karo.',
          ),
      )
    } finally {
      setSavingInstitute(false)
    }
  }

  async function addDepartment(event) {
    event.preventDefault()
    setModalError('')
    setModalSuccess('')

    if (!form.institutionId) {
      setModalError('Pehle Institute select karo.')
      return
    }

    const name = newDepartment.name.trim()
    const code = newDepartment.code.trim().toUpperCase()

    if (!name || !code) {
      setModalError('Department ka naam aur code bharna zaroori hai.')
      return
    }

    setSavingDepartment(true)

    try {
      const payload = {
        name,
        code,
        institution: { id: Number(form.institutionId) },
        isActive: true,
        utilizationTargetPercent: 80,
      }

      const { data: created } = await api.post('/departments', payload)

      const { data: responseData } = await api.get('/departments', {
        params: { institutionId: form.institutionId },
      })

      const list = Array.isArray(responseData) ? responseData : []

      const createdDepartment =
          list.find((item) => String(getId(item)) === String(getId(created))) ||
          list.find(
              (item) =>
                  String(item.code || '').toUpperCase() === code ||
                  String(item.name || '').trim().toLowerCase() === name.toLowerCase(),
          ) ||
          created

      setDepartments(
          list.filter(
              (item) => item.isActive !== false && item.is_active !== false,
          ),
      )

      const departmentId = getId(createdDepartment)

      if (departmentId != null) {
        setForm((prev) => ({
          ...prev,
          departmentId: String(departmentId),
        }))
      }

      setNewDepartment(initialDepartment)
      setModalSuccess('Department successfully save ho gaya.')

      window.setTimeout(() => {
        setShowDepartmentModal(false)
        setModalSuccess('')
      }, 700)
    } catch (err) {
      setModalError(
          getErrorMessage(
              err,
              'Department save nahi hua. Backend POST /api/departments check karo.',
          ),
      )
    } finally {
      setSavingDepartment(false)
    }
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setSuccess('')

    if (form.password !== form.confirmPassword) {
      setError('Passwords match nahi kar rahe hain.')
      return
    }

    if (form.password.length < 8 || form.password.length > 72) {
      setError('Password 8–72 characters ka hona chahiye.')
      return
    }

    if (!form.institutionId || !form.departmentId) {
      setError('Institute aur Department select karo.')
      return
    }

    const selectedDepartment = departments.find(
        (item) => String(getId(item)) === String(form.departmentId),
    )

    if (!selectedDepartment) {
      setError('Valid Department select karo.')
      return
    }

    setBusy(true)

    try {
      const selectedRole = roleOptions.find(
          (role) => role.id === Number(form.roleId),
      )

      const payload = {
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        password: form.password,
        phone: form.phone.trim() || null,
        roleId: Number(form.roleId),
        role: selectedRole?.value,
        institutionId: Number(form.institutionId),
        departmentId: Number(form.departmentId),
      }

      const { data } = await api.post('/users/register', payload)

      setSuccess(
          `Account successfully create ho gaya, ${
              data?.fullName || form.fullName.trim()
          }! Ab login kar sakte ho.`,
      )

      window.setTimeout(() => {
        navigate('/login', {
          replace: true,
          state: { registered: true, email: form.email.trim() },
        })
      }, 1800)
    } catch (err) {
      const status = err.response?.status

      setError(
          err.response?.data?.message ||
          (status === 409
              ? 'Is email se account pehle se registered hai.'
              : status === 400
                  ? 'Registration details check karo.'
                  : status === 403
                      ? 'Is role ke liye permission nahi hai.'
                      : !err.response
                          ? 'Backend se connection nahi ho raha.'
                          : `Registration fail hua (HTTP ${status}).`),
      )
    } finally {
      setBusy(false)
    }
  }

  const selectedInstitute = institutions.find(
      (item) => String(getId(item)) === String(form.institutionId),
  )

  const modalNoticeStyle = {
    padding: '10px 12px',
    borderRadius: 8,
    fontSize: 13,
    marginBottom: 14,
  }

  return (
      <div className="auth-page">
        <aside className="auth-aside">
          <Link to="/" className="brand auth-brand">
            <FlaskConical size={26} />
            <span>LabResource</span>
          </Link>

          <div className="auth-aside-copy">
            <span className="pill-label">LAB RESOURCE PLATFORM</span>
            <h1>Research starts with the right resources.</h1>
            <p>
              Discover laboratory equipment, manage bookings and make better use
              of shared research facilities.
            </p>
          </div>

          <div className="aside-stat-row">
            <div>
              <strong>Smart</strong>
              <span>Equipment access</span>
            </div>
            <div>
              <strong>Secure</strong>
              <span>Role-based accounts</span>
            </div>
          </div>

          <div className="aside-footer">
            <GraduationCap size={18} />
            <span>Built for students, researchers and laboratory teams.</span>
          </div>
        </aside>

        <main className="auth-main">
          <div className="auth-form-card wide">
            <div className="mobile-brand">
              <FlaskConical size={24} />
              <span>LabResource</span>
            </div>

            <div className="auth-heading">
              <span className="eyebrow">GET STARTED</span>
              <h2>Create your account</h2>
              <p>Enter your details to join your institution's lab network.</p>
            </div>

            {error && <div className="notice error">{error}</div>}
            {success && <div className="notice success">{success}</div>}

            <form onSubmit={submit}>
              <div className="form-grid">
                <div className="field">
                  <label htmlFor="fullName">Full name</label>
                  <div className="input-wrap">
                    <UserRound size={18} />
                    <input
                        id="fullName"
                        name="fullName"
                        value={form.fullName}
                        onChange={change}
                        placeholder="Enter your full name"
                        autoComplete="name"
                        maxLength={150}
                        required
                    />
                  </div>
                </div>

                <div className="field">
                  <label htmlFor="email">Email address</label>
                  <div className="input-wrap">
                    <Mail size={18} />
                    <input
                        id="email"
                        name="email"
                        type="email"
                        value={form.email}
                        onChange={change}
                        placeholder="you@example.com"
                        autoComplete="email"
                        maxLength={150}
                        required
                    />
                  </div>
                </div>

                <div className="field">
                  <label htmlFor="phone">
                    Phone <span className="optional">(optional)</span>
                  </label>
                  <div className="input-wrap">
                    <Phone size={18} />
                    <input
                        id="phone"
                        name="phone"
                        type="tel"
                        value={form.phone}
                        onChange={change}
                        placeholder="Enter phone number"
                        autoComplete="tel"
                        maxLength={30}
                    />
                  </div>
                </div>

                <div className="field">
                  <label htmlFor="roleId">Account role</label>
                  <div className="input-wrap">
                    <UserRound size={18} />
                    <select
                        id="roleId"
                        name="roleId"
                        value={form.roleId}
                        onChange={change}
                        required
                    >
                      {roleOptions.map((role) => (
                          <option key={role.id} value={String(role.id)}>
                            {role.label}
                          </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="field">
                  <label htmlFor="institutionId">Institute</label>
                  <div className="input-wrap">
                    <Building2 size={18} />
                    <select
                        id="institutionId"
                        name="institutionId"
                        value={form.institutionId}
                        onChange={change}
                        disabled={loadingInstitutions}
                        required
                    >
                      <option value="">
                        {loadingInstitutions
                            ? 'Loading institutes...'
                            : institutions.length === 0
                                ? 'No institutes available'
                                : 'Select institute'}
                      </option>

                      {institutions.map((institution) => (
                          <option
                              key={getId(institution)}
                              value={String(getId(institution))}
                          >
                            {getName(institution)}
                          </option>
                      ))}
                    </select>
                  </div>

                  <button
                      type="button"
                      onClick={() => {
                        setModalError('')
                        setModalSuccess('')
                        setShowInstituteModal(true)
                      }}
                      style={addButtonStyle}
                  >
                    <Plus size={17} />
                    Add New Institute
                  </button>
                </div>

                <div className="field">
                  <label htmlFor="departmentId">Department</label>
                  <div className="input-wrap">
                    <Building2 size={18} />
                    <select
                        id="departmentId"
                        name="departmentId"
                        value={form.departmentId}
                        onChange={change}
                        disabled={
                            !form.institutionId ||
                            loadingDepartments ||
                            departments.length === 0
                        }
                        required
                    >
                      <option value="">
                        {!form.institutionId
                            ? 'Select institute first'
                            : loadingDepartments
                                ? 'Loading departments...'
                                : departments.length === 0
                                    ? 'No departments available'
                                    : 'Select department'}
                      </option>

                      {departments.map((department) => (
                          <option
                              key={getId(department)}
                              value={String(getId(department))}
                          >
                            {getName(department)}
                          </option>
                      ))}
                    </select>
                  </div>

                  <button
                      type="button"
                      onClick={() => {
                        setModalError('')
                        setModalSuccess('')
                        setShowDepartmentModal(true)
                      }}
                      disabled={!form.institutionId}
                      style={{
                        ...addButtonStyle,
                        opacity: form.institutionId ? 1 : 0.55,
                        cursor: form.institutionId ? 'pointer' : 'not-allowed',
                      }}
                  >
                    <Plus size={17} />
                    Add New Department
                  </button>
                </div>

                <div className="field">
                  <label htmlFor="password">Password</label>
                  <div className="input-wrap">
                    <LockKeyhole size={18} />
                    <input
                        id="password"
                        name="password"
                        type={showPassword ? 'text' : 'password'}
                        value={form.password}
                        onChange={change}
                        placeholder="Minimum 8 characters"
                        autoComplete="new-password"
                        minLength={8}
                        maxLength={72}
                        required
                    />
                    <button
                        className="field-icon-button"
                        type="button"
                        onClick={() => setShowPassword((value) => !value)}
                        aria-label={showPassword ? 'Hide password' : 'Show password'}
                    >
                      {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>
                </div>

                <div className="field">
                  <label htmlFor="confirmPassword">Confirm password</label>
                  <div className="input-wrap">
                    <LockKeyhole size={18} />
                    <input
                        id="confirmPassword"
                        name="confirmPassword"
                        type={showConfirm ? 'text' : 'password'}
                        value={form.confirmPassword}
                        onChange={change}
                        placeholder="Re-enter password"
                        autoComplete="new-password"
                        minLength={8}
                        maxLength={72}
                        required
                    />
                    <button
                        className="field-icon-button"
                        type="button"
                        onClick={() => setShowConfirm((value) => !value)}
                        aria-label={showConfirm ? 'Hide password' : 'Show password'}
                    >
                      {showConfirm ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>
                </div>
              </div>

              <button
                  className="primary-button"
                  type="submit"
                  disabled={busy || loadingInstitutions || loadingDepartments}
              >
                {busy ? 'Creating account...' : 'Create account'}
                {!busy && <ArrowRight size={18} />}
              </button>
            </form>

            <p className="auth-switch">
              Already have an account? <Link to="/login">Sign in</Link>
            </p>

            <div className="security-note">
              Your account details are handled securely.
            </div>
          </div>
        </main>

        {/* Add Institute Modal */}
        {showInstituteModal && (
            <div
                style={overlayStyle}
                onMouseDown={(event) => {
                  if (event.target === event.currentTarget) closeInstituteModal()
                }}
            >
              <section
                  role="dialog"
                  aria-modal="true"
                  aria-labelledby="institute-modal-title"
                  style={modalStyle}
              >
                <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      gap: 12,
                      marginBottom: 20,
                    }}
                >
                  <div>
                    <h2
                        id="institute-modal-title"
                        style={{ margin: 0, fontSize: 21, color: '#172033' }}
                    >
                      Add New Institute
                    </h2>
                    <p style={{ margin: '6px 0 0', color: '#667085', fontSize: 13 }}>
                      Enter institute details below.
                    </p>
                  </div>

                  <button
                      type="button"
                      onClick={closeInstituteModal}
                      disabled={savingInstitute}
                      aria-label="Close institute form"
                      style={{
                        border: 0,
                        background: '#f2f4f7',
                        borderRadius: 8,
                        padding: 8,
                        cursor: 'pointer',
                      }}
                  >
                    <X size={20} />
                  </button>
                </div>

                {modalError && (
                    <div
                        style={{
                          ...modalNoticeStyle,
                          color: '#b42318',
                          background: '#fef3f2',
                        }}
                    >
                      {modalError}
                    </div>
                )}

                {modalSuccess && (
                    <div
                        style={{
                          ...modalNoticeStyle,
                          color: '#067647',
                          background: '#ecfdf3',
                        }}
                    >
                      {modalSuccess}
                    </div>
                )}

                <form onSubmit={addInstitute}>
                  <div
                      style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))',
                        gap: 12,
                      }}
                  >
                    <ModalField
                        label="Institute name *"
                        name="newInstituteName"
                        value={newInstitute.name}
                        onChange={(e) =>
                            setNewInstitute((prev) => ({ ...prev, name: e.target.value }))
                        }
                        required
                        placeholder="e.g. ABC University"
                    />

                    <ModalField
                        label="Institute code *"
                        name="newInstituteCode"
                        value={newInstitute.code}
                        onChange={(e) =>
                            setNewInstitute((prev) => ({ ...prev, code: e.target.value }))
                        }
                        required
                        placeholder="e.g. ABC"
                    />
                  </div>

                  <ModalField
                      label="Institution type *"
                      name="newInstituteType"
                      value={newInstitute.institutionType}
                      onChange={(e) =>
                          setNewInstitute((prev) => ({
                            ...prev,
                            institutionType: e.target.value,
                          }))
                      }
                      options={[
                        { value: 'UNIVERSITY', label: 'University' },
                        { value: 'COLLEGE', label: 'College' },
                        { value: 'RESEARCH_INSTITUTE', label: 'Research Institute' },
                        { value: 'OTHER', label: 'Other' },
                      ]}
                      required
                  />

                  <ModalField
                      label="Address"
                      name="newInstituteAddress"
                      value={newInstitute.addressLine}
                      onChange={(e) =>
                          setNewInstitute((prev) => ({
                            ...prev,
                            addressLine: e.target.value,
                          }))
                      }
                  />

                  <div
                      style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))',
                        gap: 12,
                      }}
                  >
                    <ModalField
                        label="City"
                        name="newInstituteCity"
                        value={newInstitute.city}
                        onChange={(e) =>
                            setNewInstitute((prev) => ({ ...prev, city: e.target.value }))
                        }
                    />

                    <ModalField
                        label="State"
                        name="newInstituteState"
                        value={newInstitute.state}
                        onChange={(e) =>
                            setNewInstitute((prev) => ({ ...prev, state: e.target.value }))
                        }
                    />
                  </div>

                  <div
                      style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))',
                        gap: 12,
                      }}
                  >
                    <ModalField
                        label="Country"
                        name="newInstituteCountry"
                        value={newInstitute.country}
                        onChange={(e) =>
                            setNewInstitute((prev) => ({
                              ...prev,
                              country: e.target.value,
                            }))
                        }
                    />

                    <ModalField
                        label="Contact phone"
                        name="newInstitutePhone"
                        value={newInstitute.contactPhone}
                        onChange={(e) =>
                            setNewInstitute((prev) => ({
                              ...prev,
                              contactPhone: e.target.value,
                            }))
                        }
                        type="tel"
                        maxLength={30}
                    />
                  </div>

                  <ModalField
                      label="Contact email"
                      name="newInstituteEmail"
                      value={newInstitute.contactEmail}
                      onChange={(e) =>
                          setNewInstitute((prev) => ({
                            ...prev,
                            contactEmail: e.target.value,
                          }))
                      }
                      type="email"
                  />

                  <ModalField
                      label="Website"
                      name="newInstituteWebsite"
                      value={newInstitute.website}
                      onChange={(e) =>
                          setNewInstitute((prev) => ({
                            ...prev,
                            website: e.target.value,
                          }))
                      }
                      type="url"
                      placeholder="https://example.com"
                  />

                  <div
                      style={{
                        display: 'flex',
                        justifyContent: 'flex-end',
                        flexWrap: 'wrap',
                        gap: 10,
                        marginTop: 8,
                      }}
                  >
                    <button
                        type="button"
                        onClick={closeInstituteModal}
                        disabled={savingInstitute}
                        style={{
                          padding: '11px 16px',
                          border: '1px solid #d0d5dd',
                          borderRadius: 8,
                          background: '#fff',
                          cursor: 'pointer',
                        }}
                    >
                      Cancel
                    </button>

                    <button
                        type="submit"
                        disabled={savingInstitute}
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 7,
                          padding: '11px 16px',
                          border: 0,
                          borderRadius: 8,
                          background: '#2563eb',
                          color: '#fff',
                          fontWeight: 600,
                          cursor: savingInstitute ? 'wait' : 'pointer',
                        }}
                    >
                      <Plus size={16} />
                      {savingInstitute ? 'Saving...' : 'Save Institute'}
                    </button>
                  </div>
                </form>
              </section>
            </div>
        )}

        {/* Add Department Modal */}
        {showDepartmentModal && (
            <div
                style={overlayStyle}
                onMouseDown={(event) => {
                  if (event.target === event.currentTarget) closeDepartmentModal()
                }}
            >
              <section
                  role="dialog"
                  aria-modal="true"
                  aria-labelledby="department-modal-title"
                  style={{ ...modalStyle, maxWidth: 440 }}
              >
                <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      gap: 12,
                      marginBottom: 20,
                    }}
                >
                  <div>
                    <h2
                        id="department-modal-title"
                        style={{ margin: 0, fontSize: 21, color: '#172033' }}
                    >
                      Add New Department
                    </h2>
                    <p style={{ margin: '6px 0 0', color: '#667085', fontSize: 13 }}>
                      Add a department to the selected institute.
                    </p>
                  </div>

                  <button
                      type="button"
                      onClick={closeDepartmentModal}
                      disabled={savingDepartment}
                      aria-label="Close department form"
                      style={{
                        border: 0,
                        background: '#f2f4f7',
                        borderRadius: 8,
                        padding: 8,
                        cursor: 'pointer',
                      }}
                  >
                    <X size={20} />
                  </button>
                </div>

                <div
                    style={{
                      padding: 12,
                      borderRadius: 8,
                      background: '#eff6ff',
                      color: '#1d4ed8',
                      marginBottom: 18,
                      fontSize: 13,
                      display: 'flex',
                      gap: 8,
                      alignItems: 'center',
                    }}
                >
                  <MapPin size={17} />
                  <span>
                Institute: <strong>{getName(selectedInstitute || {})}</strong>
              </span>
                </div>

                {modalError && (
                    <div
                        style={{
                          ...modalNoticeStyle,
                          color: '#b42318',
                          background: '#fef3f2',
                        }}
                    >
                      {modalError}
                    </div>
                )}

                {modalSuccess && (
                    <div
                        style={{
                          ...modalNoticeStyle,
                          color: '#067647',
                          background: '#ecfdf3',
                        }}
                    >
                      {modalSuccess}
                    </div>
                )}

                <form onSubmit={addDepartment}>
                  <ModalField
                      label="Department name *"
                      name="newDepartmentName"
                      value={newDepartment.name}
                      onChange={(e) =>
                          setNewDepartment((prev) => ({
                            ...prev,
                            name: e.target.value,
                          }))
                      }
                      required
                      placeholder="e.g. Computer Science"
                  />

                  <ModalField
                      label="Department code *"
                      name="newDepartmentCode"
                      value={newDepartment.code}
                      onChange={(e) =>
                          setNewDepartment((prev) => ({
                            ...prev,
                            code: e.target.value,
                          }))
                      }
                      required
                      maxLength={30}
                      placeholder="e.g. CSE"
                  />

                  <div
                      style={{
                        display: 'flex',
                        justifyContent: 'flex-end',
                        flexWrap: 'wrap',
                        gap: 10,
                        marginTop: 20,
                      }}
                  >
                    <button
                        type="button"
                        onClick={closeDepartmentModal}
                        disabled={savingDepartment}
                        style={{
                          padding: '11px 16px',
                          border: '1px solid #d0d5dd',
                          borderRadius: 8,
                          background: '#fff',
                          cursor: 'pointer',
                        }}
                    >
                      Cancel
                    </button>

                    <button
                        type="submit"
                        disabled={savingDepartment || !form.institutionId}
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 7,
                          padding: '11px 16px',
                          border: 0,
                          borderRadius: 8,
                          background: '#2563eb',
                          color: '#fff',
                          fontWeight: 600,
                          cursor: savingDepartment ? 'wait' : 'pointer',
                        }}
                    >
                      <Plus size={16} />
                      {savingDepartment ? 'Saving...' : 'Save Department'}
                    </button>
                  </div>
                </form>
              </section>
            </div>
        )}
      </div>
  )
}
