
import { useEffect, useRef, useState } from 'react';
import {
  UserRound,
  Mail,
  ShieldCheck,
  Building2,
  Camera,
  Pencil,
  Save,
  X,
  Hash,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';

export default function Profile() {
  const { user, setUser } = useAuth();
  const fileInputRef = useRef(null);

  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const [photoPreview, setPhotoPreview] = useState(
      user?.profileImage || user?.avatar || ''
  );

  const [institutions, setInstitutions] = useState([]);
  const [departments, setDepartments] = useState([]);

  const [form, setForm] = useState({
    fullName: user?.fullName || user?.name || '',
    institutionId: '',
    departmentId: '',
  });

  const roleValue =
      typeof user?.role === 'object'
          ? user?.role?.name
          : user?.role;

  const role = String(
      roleValue || user?.roleName || 'RESEARCHER_STUDENT'
  ).replace(/^ROLE_/, '');

  const displayRole = role.replaceAll('_', ' ');
  const email = user?.email || 'Not provided';

  const fullName = form.fullName.trim() || 'Platform user';
  const initial = (fullName !== 'Platform user' ? fullName : email)
      .charAt(0)
      .toUpperCase();

  // Load profile and institutions from backend
  useEffect(() => {
    async function loadProfile() {
      try {
        setLoading(true);

        const [profileResponse, institutionResponse] =
            await Promise.all([
              api.get('/users/me'),
              api.get('/institutions'),
            ]);

        const profile = profileResponse.data;
        const institutionList = Array.isArray(institutionResponse.data)
            ? institutionResponse.data
            : [];

        setInstitutions(institutionList);

        // Preserve existing browser-only profile photo
        const updatedUser = {
          ...(user || {}),
          ...profile,
          profileImage: user?.profileImage || user?.avatar || '',
        };

        setUser(updatedUser);

        let selectedInstitutionId = '';

        if (profile.institutionName) {
          const matchedInstitution = institutionList.find(
              (item) =>
                  item.name?.toLowerCase() ===
                  profile.institutionName?.toLowerCase()
          );

          if (matchedInstitution) {
            selectedInstitutionId = String(matchedInstitution.id);
          }
        }

        let departmentList = [];
        let selectedDepartmentId = '';

        if (selectedInstitutionId) {
          const departmentResponse = await api.get('/departments', {
            params: {
              institutionId: selectedInstitutionId,
            },
          });

          departmentList = Array.isArray(departmentResponse.data)
              ? departmentResponse.data
              : [];

          const matchedDepartment = departmentList.find(
              (item) =>
                  item.name?.toLowerCase() ===
                  profile.departmentName?.toLowerCase()
          );

          if (matchedDepartment) {
            selectedDepartmentId = String(matchedDepartment.id);
          }
        }

        setDepartments(departmentList);

        setForm({
          fullName: profile.fullName || '',
          institutionId: selectedInstitutionId,
          departmentId: selectedDepartmentId,
        });
      } catch (err) {
        console.error('Profile loading error:', err);
        setError(
            err.response?.data?.message ||
            'Profile load nahi ho paayi. Dobara try karo.'
        );
      } finally {
        setLoading(false);
      }
    }

    loadProfile();
    // Load profile once when page opens.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Existing profile photo feature - unchanged
  function handlePhotoChange(event) {
    const file = event.target.files?.[0];

    if (!file) return;

    setError('');
    setMessage('');

    if (!file.type.startsWith('image/')) {
      setError('Sirf image file select karo.');
      event.target.value = '';
      return;
    }

    if (file.size > 2 * 1024 * 1024) {
      setError('Photo ka size 2 MB se kam hona chahiye.');
      event.target.value = '';
      return;
    }

    const reader = new FileReader();

    reader.onload = () => {
      setPhotoPreview(reader.result);
    };

    reader.onerror = () => {
      setError('Photo read nahi ho paayi. Dobara try karo.');
    };

    reader.readAsDataURL(file);
  }

  async function handleInstitutionChange(institutionId) {
    setForm((prev) => ({
      ...prev,
      institutionId,
      departmentId: '',
    }));

    setDepartments([]);

    if (!institutionId) return;

    try {
      const response = await api.get('/departments', {
        params: { institutionId },
      });

      setDepartments(
          Array.isArray(response.data) ? response.data : []
      );
    } catch (err) {
      console.error('Department loading error:', err);
      setError('Departments load nahi ho paaye.');
    }
  }

  function handleEdit() {
    setError('');
    setMessage('');
    setPhotoPreview(user?.profileImage || user?.avatar || '');
    setEditing(true);
  }

  function handleCancel() {
    setForm((prev) => ({ ...prev }));
    setPhotoPreview(user?.profileImage || user?.avatar || '');
    setEditing(false);
    setError('');
    setMessage('');

    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  }

  async function handleSave(event) {
    event.preventDefault();

    setError('');
    setMessage('');

    if (!form.fullName.trim()) {
      setError('Full name khaali nahi ho sakta.');
      return;
    }

    if (
        Boolean(form.institutionId) !==
        Boolean(form.departmentId)
    ) {
      setError('Institution aur Department dono select karo.');
      return;
    }

    setSaving(true);

    try {
      const payload = {
        fullName: form.fullName.trim(),
      };

      if (form.institutionId && form.departmentId) {
        payload.institutionId = Number(form.institutionId);
        payload.departmentId = Number(form.departmentId);
      }

      const response = await api.put('/users/me', payload);
      const profile = response.data;

      // Preserve browser-only photo
      const updatedUser = {
        ...(user || {}),
        ...profile,
        profileImage: photoPreview,
      };

      setUser(updatedUser);

      setForm((prev) => ({
        ...prev,
        fullName: profile.fullName || prev.fullName,
      }));

      setEditing(false);
      setMessage('Profile updated successfully in the database.');

      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    } catch (err) {
      console.error('Profile update error:', err);

      setError(
          err.response?.data?.message ||
          err.response?.data?.error ||
          'Profile not save Try again.'
      );
    } finally {
      setSaving(false);
    }
  }

  const fields = [
    {
      label: 'Full name',
      value: user?.fullName || user?.name || 'Not provided',
      Icon: UserRound,
    },
    {
      label: 'Email address',
      value: email,
      Icon: Mail,
    },
    {
      label: 'User ID',
      value: user?.id ?? user?.userId ?? 'Not provided',
      Icon: Hash,
    },
    {
      label: 'Assigned role',
      value: displayRole,
      Icon: ShieldCheck,
    },
    {
      label: 'Institution',
      value: user?.institutionName || 'Not provided',
      Icon: Building2,
    },
    {
      label: 'Department',
      value: user?.departmentName || 'Not provided',
      Icon: Building2,
    },
  ];

  if (loading) {
    return (
        <div className="profile-page">
          <section className="panel profile-panel">
            <p>Loading profile...</p>
          </section>
        </div>
    );
  }

  return (
      <div className="profile-page">
        <div className="page-heading-row">
          <div>
            <span className="eyebrow">ACCOUNT</span>
            <h1>My profile</h1>
            <p>Manage your personal details and profile photo.</p>
          </div>

          {!editing && (
              <button
                  type="button"
                  className="primary-button profile-edit-button"
                  onClick={handleEdit}
              >
                <Pencil size={17} />
                Edit profile
              </button>
          )}
        </div>

        <section className="panel profile-panel">
          <div className="profile-banner">
            <div className="profile-photo-wrapper">
              {photoPreview ? (
                  <img
                      src={photoPreview}
                      alt="Profile"
                      className="profile-photo"
                  />
              ) : (
                  <div className="profile-avatar">{initial}</div>
              )}

              {editing && (
                  <>
                    <button
                        type="button"
                        className="profile-camera-button"
                        title="Change profile photo"
                        aria-label="Change profile photo"
                        onClick={() => fileInputRef.current?.click()}
                    >
                      <Camera size={18} />
                    </button>

                    <input
                        ref={fileInputRef}
                        type="file"
                        accept="image/*"
                        hidden
                        onChange={handlePhotoChange}
                    />
                  </>
              )}
            </div>

            <div className="profile-banner-info">
              <h2>{user?.fullName || user?.name || 'Platform user'}</h2>
              <p>{email}</p>
              <span className="role-pill">{displayRole}</span>

              {editing && (
                  <p className="profile-photo-hint">
                    Photo select karne ke liye camera icon dabao.
                    Maximum size: 2 MB.
                  </p>
              )}
            </div>
          </div>

          {message && (
              <div className="notice success" role="status">
                {message}
              </div>
          )}

          {error && (
              <div className="notice error" role="alert">
                {error}
              </div>
          )}

          {editing ? (
              <form className="profile-edit-form" onSubmit={handleSave}>
                <div className="profile-edit-heading">
                  <div>
                    <h3>Edit personal information</h3>
                    <p>Update the details shown on your profile.</p>
                  </div>
                  <Pencil size={20} />
                </div>

                <div className="profile-edit-grid">
                  <div className="field">
                    <label htmlFor="profileFullName">Full name</label>
                    <input
                        id="profileFullName"
                        className="standalone-input"
                        type="text"
                        maxLength={150}
                        value={form.fullName}
                        onChange={(e) =>
                            setForm((prev) => ({
                              ...prev,
                              fullName: e.target.value,
                            }))
                        }
                        required
                    />
                  </div>

                  <div className="field">
                    <label htmlFor="profileEmail">Email address</label>
                    <input
                        id="profileEmail"
                        className="standalone-input"
                        type="email"
                        value={email}
                        disabled
                    />
                    <small className="field-hint">
                      Email change karne ke liye backend support chahiye.
                    </small>
                  </div>

                  <div className="field">
                    <label htmlFor="profileInstitution">Institution</label>
                    <select
                        id="profileInstitution"
                        className="standalone-input"
                        value={form.institutionId}
                        onChange={(e) =>
                            handleInstitutionChange(e.target.value)
                        }
                    >
                      <option value="">Select Institution</option>
                      {institutions.map((institution) => (
                          <option
                              key={institution.id}
                              value={institution.id}
                          >
                            {institution.name}
                          </option>
                      ))}
                    </select>
                  </div>

                  <div className="field">
                    <label htmlFor="profileDepartment">Department</label>
                    <select
                        id="profileDepartment"
                        className="standalone-input"
                        value={form.departmentId}
                        onChange={(e) =>
                            setForm((prev) => ({
                              ...prev,
                              departmentId: e.target.value,
                            }))
                        }
                        disabled={!form.institutionId}
                    >
                      <option value="">Select Department</option>
                      {departments.map((department) => (
                          <option
                              key={department.id}
                              value={department.id}
                          >
                            {department.name}
                          </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="profile-form-actions">
                  <button
                      type="button"
                      className="secondary-button"
                      onClick={handleCancel}
                      disabled={saving}
                  >
                    <X size={17} />
                    Cancel
                  </button>

                  <button
                      type="submit"
                      className="primary-button"
                      disabled={saving}
                  >
                    <Save size={17} />
                    {saving ? 'Saving…' : 'Save changes'}
                  </button>
                </div>

                <p className="field-hint profile-local-note">
                  Personal details database mein save hongi.
                  Profile photo abhi browser mein hi rahegi.
                </p>
              </form>
          ) : (
              <>
                <div className="profile-fields">
                  {fields.map(({ label, value, Icon }) => (
                      <div className="profile-field" key={label}>
                  <span className="profile-field-icon">
                    <Icon size={18} />
                  </span>
                        <div>
                          <span>{label}</span>
                          <strong>{String(value)}</strong>
                        </div>
                      </div>
                  ))}
                </div>

                <p className="field-hint profile-local-note">
                  Account roles are retrieved via email login.
                  Personal details are loaded from the backend.
                </p>
              </>
          )}
        </section>
      </div>
  );
}
