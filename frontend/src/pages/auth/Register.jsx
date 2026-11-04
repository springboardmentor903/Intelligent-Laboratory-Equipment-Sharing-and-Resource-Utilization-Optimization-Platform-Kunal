import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import axios from 'axios'
import {
    FlaskConical,
    UserRound,
    Mail,
    Lock,
    Phone,
    Eye,
    EyeOff,
    ArrowRight,
    Building2,
    GraduationCap,
    BriefcaseBusiness,
    Plus,
    X,
    MapPin,
} from 'lucide-react'

const API = 'http://localhost:8080/api'

const ROLES = [
    { id: 1, name: 'RESEARCHER_STUDENT', label: 'Student / Researcher' },
    { id: 2, name: 'LAB_TECHNICIAN', label: 'Lab Technician' },
    { id: 3, name: 'LAB_MANAGER', label: 'Lab Manager' },
    { id: 4, name: 'DEPARTMENT_HEAD', label: 'Department Head' },
    { id: 5, name: 'INSTITUTION_ADMIN', label: 'Institution Administrator' },
]

// SYSTEM_ADMIN public self-registration ke liye available nahi hai.

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
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    width: '100%',
    marginTop: 9,
    padding: '10px 12px',
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
    maxWidth: 560,
    maxHeight: '90vh',
    overflowY: 'auto',
    background: '#fff',
    borderRadius: 16,
    padding: 24,
    boxSizing: 'border-box',
    boxShadow: '0 20px 60px rgba(0,0,0,0.25)',
}

const modalNoticeStyle = {
    padding: 12,
    borderRadius: 8,
    marginBottom: 14,
    fontSize: 13,
}

function getId(item) {
    return item?.id ?? item?.institutionId ?? item?.institution_id
}

function getErrorMessage(error, fallback) {
    return (
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        (typeof error?.response?.data === 'string'
            ? error.response.data
            : '') ||
        error?.message ||
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
    const [loading, setLoading] = useState(false)

    const [showPassword, setShowPassword] = useState(false)
    const [showConfirmPassword, setShowConfirmPassword] = useState(false)

    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')

    const [showInstituteModal, setShowInstituteModal] = useState(false)
    const [showDepartmentModal, setShowDepartmentModal] = useState(false)

    const [newInstitute, setNewInstitute] = useState(initialInstitute)
    const [newDepartment, setNewDepartment] = useState(initialDepartment)

    const [savingInstitute, setSavingInstitute] = useState(false)
    const [savingDepartment, setSavingDepartment] = useState(false)

    const [modalError, setModalError] = useState('')
    const [modalSuccess, setModalSuccess] = useState('')

    const selectedInstitute = institutions.find(
        (item) => String(getId(item)) === String(form.institutionId),
    )

    async function loadInstitutions(preferredId = '') {
        setLoadingInstitutions(true)

        try {
            const response = await axios.get(`${API}/institutions`)

            const list = (
                Array.isArray(response.data) ? response.data : []
            ).filter(
                (item) =>
                    item.isActive !== false && item.is_active !== false,
            )

            setInstitutions(list)

            if (
                preferredId &&
                list.some(
                    (item) => String(getId(item)) === String(preferredId),
                )
            ) {
                setForm((prev) => ({
                    ...prev,
                    institutionId: String(preferredId),
                    departmentId: '',
                }))
            }
        } catch (err) {
            setError(
                getErrorMessage(
                    err,
                    'Institutes load nahi ho sake. Backend API check karo.',
                ),
            )
        } finally {
            setLoadingInstitutions(false)
        }
    }

    async function loadDepartments(
        institutionId,
        preferredDepartmentId = '',
    ) {
        if (!institutionId) {
            setDepartments([])
            setLoadingDepartments(false)
            return
        }

        setLoadingDepartments(true)

        try {
            const response = await axios.get(`${API}/departments`, {
                params: {
                    institutionId: Number(institutionId),
                },
            })

            const list = (
                Array.isArray(response.data) ? response.data : []
            ).filter(
                (item) =>
                    item.isActive !== false && item.is_active !== false,
            )

            setDepartments(list)

            if (
                preferredDepartmentId &&
                list.some(
                    (item) =>
                        String(getId(item)) ===
                        String(preferredDepartmentId),
                )
            ) {
                setForm((prev) => ({
                    ...prev,
                    departmentId: String(preferredDepartmentId),
                }))
            }
        } catch (err) {
            setDepartments([])

            setError(
                getErrorMessage(
                    err,
                    'Departments load nahi ho sake. Backend API check karo.',
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
        setForm((prev) => ({
            ...prev,
            departmentId: '',
        }))

        if (form.institutionId) {
            loadDepartments(form.institutionId)
        } else {
            setLoadingDepartments(false)
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [form.institutionId])

    function handleChange(event) {
        const { name, value } = event.target

        setError('')
        setSuccess('')

        setForm((prev) => ({
            ...prev,
            [name]: value,
            ...(name === 'institutionId'
                ? { departmentId: '' }
                : {}),
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
            setModalError(
                'Institute ka naam aur code bharna zaroori hai.',
            )
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

            const createdResponse = await axios.post(
                `${API}/institutions`,
                payload,
            )

            const created = createdResponse.data
            const createdId = getId(created)

            const listResponse = await axios.get(
                `${API}/institutions`,
            )

            const list = (
                Array.isArray(listResponse.data)
                    ? listResponse.data
                    : []
            ).filter(
                (item) =>
                    item.isActive !== false && item.is_active !== false,
            )

            const found =
                list.find(
                    (item) =>
                        createdId != null &&
                        String(getId(item)) === String(createdId),
                ) ||
                list.find(
                    (item) =>
                        String(item.code || '').toUpperCase() === code ||
                        String(item.name || '').trim().toLowerCase() ===
                        name.toLowerCase(),
                ) ||
                created

            const id = getId(found)

            setInstitutions(list)

            if (id == null) {
                throw new Error(
                    'Institute save hua, lekin response mein institute ID nahi mili.',
                )
            }

            setForm((prev) => ({
                ...prev,
                institutionId: String(id),
                departmentId: '',
            }))

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
                    'Institute save nahi hua. POST /api/institutions check karo.',
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
            setModalError('Pehle institute select karo.')
            return
        }

        const name = newDepartment.name.trim()
        const code = newDepartment.code.trim().toUpperCase()

        if (!name || !code) {
            setModalError(
                'Department ka naam aur code bharna zaroori hai.',
            )
            return
        }

        setSavingDepartment(true)

        try {
            const payload = {
                name,
                code,
                institutionId: Number(form.institutionId),
                isActive: true,
                utilizationTargetPercent: 80,
            }

            const createdResponse = await axios.post(
                `${API}/departments`,
                payload,
            )

            const created = createdResponse.data
            const createdId = getId(created)

            const response = await axios.get(`${API}/departments`, {
                params: {
                    institutionId: Number(form.institutionId),
                },
            })

            const list = (
                Array.isArray(response.data) ? response.data : []
            ).filter(
                (item) =>
                    item.isActive !== false && item.is_active !== false,
            )

            const found =
                list.find(
                    (item) =>
                        createdId != null &&
                        String(getId(item)) === String(createdId),
                ) ||
                list.find(
                    (item) =>
                        String(item.code || '').toUpperCase() === code ||
                        String(item.name || '').trim().toLowerCase() ===
                        name.toLowerCase(),
                ) ||
                created

            const departmentId = getId(found)

            setDepartments(list)

            if (departmentId == null) {
                throw new Error(
                    'Department save hua, lekin response mein department ID nahi mili.',
                )
            }

            setForm((prev) => ({
                ...prev,
                departmentId: String(departmentId),
            }))

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
                    'Department save nahi hua. POST /api/departments check karo.',
                ),
            )
        } finally {
            setSavingDepartment(false)
        }
    }

    async function handleSubmit(event) {
        event.preventDefault()

        setError('')
        setSuccess('')

        if (!form.fullName.trim()) {
            setError('Apna full name enter karo.')
            return
        }

        if (!form.email.trim()) {
            setError('Email enter karo.')
            return
        }

        if (form.password.length < 8) {
            setError('Password kam se kam 8 characters ka hona chahiye.')
            return
        }

        if (form.password !== form.confirmPassword) {
            setError('Password aur Confirm Password match nahi kar rahe.')
            return
        }

        if (!form.institutionId) {
            setError('Institute select karo ya naya institute add karo.')
            return
        }

        if (!form.departmentId) {
            setError(
                'Department select karo ya selected institute mein naya department add karo.',
            )
            return
        }

        // Yahan department.institutionId check nahi kar rahe,
        // kyunki backend Department JSON mein institutionId hide ho sakta hai.
        // Backend UserService actual department-institute relation validate karega.

        setLoading(true)

        try {
            await axios.post(`${API}/users/register`, {
                fullName: form.fullName.trim(),
                email: form.email.trim(),
                password: form.password,
                phone: form.phone.trim() || null,
                roleId: Number(form.roleId),
                institutionId: Number(form.institutionId),
                departmentId: Number(form.departmentId),
            })

            setSuccess(
                'Account successfully create ho gaya! Ab login kar sakte ho.',
            )

            setForm(initialForm)

            window.setTimeout(() => {
                navigate('/login')
            }, 1200)
        } catch (err) {
            setError(
                getErrorMessage(
                    err,
                    'Registration fail ho gaya. Backend logs check karo.',
                ),
            )
        } finally {
            setLoading(false)
        }
    }

    return (
        <main className="auth-page register-page">
            <section className="auth-card register-card">
                <div className="auth-brand register-brand">
                    <div className="auth-logo">
                        <FlaskConical size={28} />
                    </div>

                    <h1>Lab Resource Platform</h1>
                    <p>Create your account to get started</p>
                </div>

                <div className="auth-heading">
                    <h2>Create Account</h2>
                    <p>Register to access laboratory resources.</p>
                </div>

                {error && (
                    <div className="auth-error" role="alert">
                        {error}
                    </div>
                )}

                {success && (
                    <div className="auth-success" role="status">
                        {success}
                    </div>
                )}

                <form className="auth-form" onSubmit={handleSubmit}>
                    <label htmlFor="fullName">Full Name</label>
                    <div className="auth-input-wrap">
                        <UserRound size={18} />
                        <input
                            id="fullName"
                            name="fullName"
                            type="text"
                            value={form.fullName}
                            onChange={handleChange}
                            placeholder="Enter your full name"
                            autoComplete="name"
                            maxLength={120}
                            required
                        />
                    </div>

                    <label htmlFor="email">Email Address</label>
                    <div className="auth-input-wrap">
                        <Mail size={18} />
                        <input
                            id="email"
                            name="email"
                            type="email"
                            value={form.email}
                            onChange={handleChange}
                            placeholder="Enter your email"
                            autoComplete="email"
                            maxLength={150}
                            required
                        />
                    </div>

                    <label htmlFor="phone">Phone Number</label>
                    <div className="auth-input-wrap">
                        <Phone size={18} />
                        <input
                            id="phone"
                            name="phone"
                            type="tel"
                            value={form.phone}
                            onChange={handleChange}
                            placeholder="Enter your phone number"
                            autoComplete="tel"
                            maxLength={20}
                        />
                    </div>

                    <label htmlFor="roleId">Register As</label>
                    <div className="auth-input-wrap">
                        <BriefcaseBusiness size={18} />
                        <select
                            id="roleId"
                            name="roleId"
                            value={form.roleId}
                            onChange={handleChange}
                            required
                        >
                            {ROLES.map((role) => (
                                <option key={role.id} value={String(role.id)}>
                                    {role.label}
                                </option>
                            ))}
                        </select>
                    </div>

                    <label htmlFor="institutionId">Institute</label>
                    <div className="auth-input-wrap">
                        <Building2 size={18} />
                        <select
                            id="institutionId"
                            name="institutionId"
                            value={form.institutionId}
                            onChange={handleChange}
                            required
                            disabled={loadingInstitutions}
                        >
                            <option value="">
                                {loadingInstitutions
                                    ? 'Loading institutes...'
                                    : institutions.length === 0
                                        ? 'No institutes available'
                                        : 'Select Institute'}
                            </option>

                            {institutions.map((institution) => (
                                <option
                                    key={getId(institution)}
                                    value={String(getId(institution))}
                                >
                                    {institution.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <button
                        type="button"
                        style={addButtonStyle}
                        onClick={() => {
                            setModalError('')
                            setModalSuccess('')
                            setShowInstituteModal(true)
                        }}
                    >
                        <Plus size={17} />
                        Add New Institute
                    </button>

                    <label htmlFor="departmentId">Department</label>
                    <div className="auth-input-wrap">
                        <GraduationCap size={18} />
                        <select
                            id="departmentId"
                            name="departmentId"
                            value={form.departmentId}
                            onChange={handleChange}
                            required
                            disabled={
                                !form.institutionId ||
                                loadingDepartments ||
                                departments.length === 0
                            }
                        >
                            <option value="">
                                {!form.institutionId
                                    ? 'Select Institute first'
                                    : loadingDepartments
                                        ? 'Loading departments...'
                                        : departments.length === 0
                                            ? 'No departments available'
                                            : 'Select Department'}
                            </option>

                            {departments.map((department) => (
                                <option
                                    key={getId(department)}
                                    value={String(getId(department))}
                                >
                                    {department.name}
                                </option>
                            ))}
                        </select>
                    </div>

                    <button
                        type="button"
                        style={{
                            ...addButtonStyle,
                            opacity: form.institutionId ? 1 : 0.55,
                        }}
                        disabled={!form.institutionId}
                        onClick={() => {
                            setModalError('')
                            setModalSuccess('')
                            setShowDepartmentModal(true)
                        }}
                    >
                        <Plus size={17} />
                        Add New Department
                    </button>

                    <label htmlFor="password">Password</label>
                    <div className="auth-input-wrap">
                        <Lock size={18} />
                        <input
                            id="password"
                            name="password"
                            type={showPassword ? 'text' : 'password'}
                            value={form.password}
                            onChange={handleChange}
                            placeholder="Minimum 8 characters"
                            minLength={8}
                            maxLength={72}
                            autoComplete="new-password"
                            required
                        />

                        <button
                            type="button"
                            onClick={() => setShowPassword((prev) => !prev)}
                            aria-label={
                                showPassword ? 'Hide password' : 'Show password'
                            }
                        >
                            {showPassword ? (
                                <EyeOff size={18} />
                            ) : (
                                <Eye size={18} />
                            )}
                        </button>
                    </div>

                    <label htmlFor="confirmPassword">Confirm Password</label>
                    <div className="auth-input-wrap">
                        <Lock size={18} />
                        <input
                            id="confirmPassword"
                            name="confirmPassword"
                            type={showConfirmPassword ? 'text' : 'password'}
                            value={form.confirmPassword}
                            onChange={handleChange}
                            placeholder="Re-enter your password"
                            minLength={8}
                            maxLength={72}
                            autoComplete="new-password"
                            required
                        />

                        <button
                            type="button"
                            onClick={() =>
                                setShowConfirmPassword((prev) => !prev)
                            }
                            aria-label={
                                showConfirmPassword
                                    ? 'Hide password'
                                    : 'Show password'
                            }
                        >
                            {showConfirmPassword ? (
                                <EyeOff size={18} />
                            ) : (
                                <Eye size={18} />
                            )}
                        </button>
                    </div>

                    <button
                        type="submit"
                        className="auth-submit"
                        disabled={
                            loading ||
                            loadingInstitutions ||
                            loadingDepartments ||
                            Boolean(success)
                        }
                    >
                        {loading ? 'Creating account...' : 'Create Account'}
                        {!loading && <ArrowRight size={18} />}
                    </button>
                </form>

                <p className="auth-footer">
                    Already have an account? <Link to="/login">Sign in</Link>
                </p>      </section>

            {/* Add New Institute Modal */}
            {showInstituteModal && (
                <div
                    style={overlayStyle}
                    onMouseDown={(event) => {
                        if (event.target === event.currentTarget) {
                            closeInstituteModal()
                        }
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
                                    style={{
                                        margin: 0,
                                        fontSize: 21,
                                        color: '#172033',
                                    }}
                                >
                                    Add New Institute
                                </h2>

                                <p
                                    style={{
                                        margin: '6px 0 0',
                                        color: '#667085',
                                        fontSize: 13,
                                    }}
                                >
                                    Enter institute details below.
                                </p>
                            </div>

                            <button
                                type="button"
                                onClick={closeInstituteModal}
                                disabled={savingInstitute}
                                aria-label="Close institute form"
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
                                    gridTemplateColumns:
                                        'repeat(auto-fit, minmax(190px, 1fr))',
                                    gap: 12,
                                }}
                            >
                                <ModalField
                                    label="Institute name *"
                                    name="newInstituteName"
                                    value={newInstitute.name}
                                    onChange={(event) =>
                                        setNewInstitute((prev) => ({
                                            ...prev,
                                            name: event.target.value,
                                        }))
                                    }
                                    required
                                    placeholder="e.g. ABC University"
                                />

                                <ModalField
                                    label="Institute code *"
                                    name="newInstituteCode"
                                    value={newInstitute.code}
                                    onChange={(event) =>
                                        setNewInstitute((prev) => ({
                                            ...prev,
                                            code: event.target.value,
                                        }))
                                    }
                                    required
                                    placeholder="e.g. ABC"
                                    maxLength={30}
                                />
                            </div>

                            <ModalField
                                label="Institution type *"
                                name="newInstituteType"
                                value={newInstitute.institutionType}
                                onChange={(event) =>
                                    setNewInstitute((prev) => ({
                                        ...prev,
                                        institutionType: event.target.value,
                                    }))
                                }
                                options={[
                                    { value: 'UNIVERSITY', label: 'University' },
                                    { value: 'COLLEGE', label: 'College' },
                                    {
                                        value: 'RESEARCH_INSTITUTE',
                                        label: 'Research Institute',
                                    },
                                    { value: 'OTHER', label: 'Other' },
                                ]}
                                required
                            />

                            <ModalField
                                label="Address"
                                name="newInstituteAddress"
                                value={newInstitute.addressLine}
                                onChange={(event) =>
                                    setNewInstitute((prev) => ({
                                        ...prev,
                                        addressLine: event.target.value,
                                    }))
                                }
                            />

                            <div
                                style={{
                                    display: 'grid',
                                    gridTemplateColumns:
                                        'repeat(auto-fit, minmax(190px, 1fr))',
                                    gap: 12,
                                }}
                            >
                                <ModalField
                                    label="City"
                                    name="newInstituteCity"
                                    value={newInstitute.city}
                                    onChange={(event) =>
                                        setNewInstitute((prev) => ({
                                            ...prev,
                                            city: event.target.value,
                                        }))
                                    }
                                />

                                <ModalField
                                    label="State"
                                    name="newInstituteState"
                                    value={newInstitute.state}
                                    onChange={(event) =>
                                        setNewInstitute((prev) => ({
                                            ...prev,
                                            state: event.target.value,
                                        }))
                                    }
                                />
                            </div>

                            <div
                                style={{
                                    display: 'grid',
                                    gridTemplateColumns:
                                        'repeat(auto-fit, minmax(190px, 1fr))',
                                    gap: 12,
                                }}
                            >
                                <ModalField
                                    label="Country"
                                    name="newInstituteCountry"
                                    value={newInstitute.country}
                                    onChange={(event) =>
                                        setNewInstitute((prev) => ({
                                            ...prev,
                                            country: event.target.value,
                                        }))
                                    }
                                />

                                <ModalField
                                    label="Contact phone"
                                    name="newInstitutePhone"
                                    value={newInstitute.contactPhone}
                                    onChange={(event) =>
                                        setNewInstitute((prev) => ({
                                            ...prev,
                                            contactPhone: event.target.value,
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
                                onChange={(event) =>
                                    setNewInstitute((prev) => ({
                                        ...prev,
                                        contactEmail: event.target.value,
                                    }))
                                }
                                type="email"
                            />

                            <ModalField
                                label="Website"
                                name="newInstituteWebsite"
                                value={newInstitute.website}
                                onChange={(event) =>
                                    setNewInstitute((prev) => ({
                                        ...prev,
                                        website: event.target.value,
                                    }))
                                }
                                type="url"
                                placeholder="https://example.com"
                            />

                            <div
                                style={{
                                    display: 'flex',
                                    justifyContent: 'flex-end',
                                    gap: 10,
                                    marginTop: 8,
                                }}
                            >
                                <button
                                    type="button"
                                    onClick={closeInstituteModal}
                                    disabled={savingInstitute}
                                >
                                    Cancel
                                </button>

                                <button
                                    type="submit"
                                    disabled={savingInstitute}
                                >
                                    <Plus size={16} />
                                    {savingInstitute
                                        ? 'Saving...'
                                        : 'Save Institute'}
                                </button>
                            </div>
                        </form>
                    </section>
                </div>
            )}

            {/* Add New Department Modal */}
            {showDepartmentModal && (
                <div
                    style={overlayStyle}
                    onMouseDown={(event) => {
                        if (event.target === event.currentTarget) {
                            closeDepartmentModal()
                        }
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
                                    style={{
                                        margin: 0,
                                        fontSize: 21,
                                        color: '#172033',
                                    }}
                                >
                                    Add New Department
                                </h2>

                                <p
                                    style={{
                                        margin: '6px 0 0',
                                        color: '#667085',
                                        fontSize: 13,
                                    }}
                                >
                                    Add a department to the selected institute.
                                </p>
                            </div>

                            <button
                                type="button"
                                onClick={closeDepartmentModal}
                                disabled={savingDepartment}
                                aria-label="Close department form"
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
                Institute:{' '}
                                <strong>
                  {selectedInstitute?.name || 'Selected institute'}
                </strong>
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
                                onChange={(event) =>
                                    setNewDepartment((prev) => ({
                                        ...prev,
                                        name: event.target.value,
                                    }))
                                }
                                required
                                placeholder="e.g. Computer Science"
                            />

                            <ModalField
                                label="Department code *"
                                name="newDepartmentCode"
                                value={newDepartment.code}
                                onChange={(event) =>
                                    setNewDepartment((prev) => ({
                                        ...prev,
                                        code: event.target.value,
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
                                    gap: 10,
                                    marginTop: 20,
                                }}
                            >
                                <button
                                    type="button"
                                    onClick={closeDepartmentModal}
                                    disabled={savingDepartment}
                                >
                                    Cancel
                                </button>

                                <button
                                    type="submit"
                                    disabled={
                                        savingDepartment || !form.institutionId
                                    }
                                >
                                    <Plus size={16} />
                                    {savingDepartment
                                        ? 'Saving...'
                                        : 'Save Department'}
                                </button>
                            </div>
                        </form>
                    </section>
                </div>
            )}
        </main>
    )
}