
import { useEffect, useMemo, useState } from 'react';
import {
  ArrowUpRight,
  Filter,
  PackageSearch,
  RefreshCw,
  Search,
  Plus,
  Pencil,
  Trash2,
  RotateCcw,
  X,
  Save,
} from 'lucide-react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';

const emptyForm = {
  name: '',
  category: '',
  description: '',
  quantity: 1,
  location: '',
};

function getRole(user) {
  const role =
      user?.role?.name ??
      user?.role?.roleName ??
      user?.roleName ??
      (typeof user?.role === 'string' ? user.role : null) ??
      user?.authorities?.[0]?.authority ??
      '';

  return String(role).replace(/^ROLE_/, '').trim().toUpperCase();
}

function getItems(data) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  if (Array.isArray(data?.items)) return data.items;
  if (Array.isArray(data?.data)) return data.data;
  return [];
}

export default function Equipment() {
  const { user } = useAuth();
  const canManage = ['LAB_MANAGER', 'SYSTEM_ADMIN'].includes(getRole(user));

  const [items, setItems] = useState([]);
  const [inactiveItems, setInactiveItems] = useState([]);
  const [showInactive, setShowInactive] = useState(false);
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('ALL');
  const [status, setStatus] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);
  const [actionId, setActionId] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(emptyForm);

  async function load() {
    setLoading(true);
    setError('');

    try {
      const { data } = await api.get('/resources');
      setItems(getItems(data));
    } catch (e) {
      setError(
          e.response?.data?.message ||
          'Equipment load nahi hui. Backend API aur login check karo.'
      );
    } finally {
      setLoading(false);
    }
  }

  async function loadInactive() {
    if (!canManage) return;

    try {
      const { data } = await api.get('/resources/inactive');
      setInactiveItems(getItems(data));
    } catch (e) {
      setError(
          e.response?.data?.message ||
          'Inactive equipment load nahi hui. Backend permission check karo.'
      );
    }
  }

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    if (canManage) loadInactive();
  }, [canManage]);

  const categories = useMemo(
      () => [...new Set(items.map((item) => item.category).filter(Boolean))],
      [items]
  );

  const filtered = items.filter((item) => {
    const text =
        `${item.name || ''} ${item.description || ''} ${item.location || ''}`.toLowerCase();

    return (
        text.includes(query.toLowerCase()) &&
        (category === 'ALL' || item.category === category) &&
        (status === 'ALL' ||
            String(item.status || '').toUpperCase() === status)
    );
  });

  function openAddForm() {
    setEditingId(null);
    setForm({ ...emptyForm });
    setError('');
    setMessage('');
    setShowForm(true);
  }

  function openEditForm(item) {
    setEditingId(item.id);
    setForm({
      name: item.name || '',
      category: item.category || '',
      description: item.description || '',
      quantity: Number(item.quantity ?? 1),
      location: item.location || '',
    });
    setError('');
    setMessage('');
    setShowForm(true);
  }

  function closeForm() {
    setShowForm(false);
    setEditingId(null);
    setForm({ ...emptyForm });
  }

  function updateField(event) {
    const { name, value } = event.target;
    setForm((previous) => ({ ...previous, [name]: value }));
  }

  async function saveResource(event) {
    event.preventDefault();
    setError('');
    setMessage('');

    const quantity = Number(form.quantity);

    if (
        !form.name.trim() ||
        !form.category.trim() ||
        !form.location.trim() ||
        !Number.isInteger(quantity) ||
        quantity < 1
    ) {
      setError('Name, category, location aur valid quantity bharna zaroori hai.');
      return;
    }

    const payload = {
      name: form.name.trim(),
      category: form.category.trim(),
      description: form.description.trim(),
      quantity,
      location: form.location.trim(),
    };

    setSaving(true);

    try {
      if (editingId !== null) {
        await api.put(`/resources/${editingId}`, payload);
        setMessage('Equipment successfully update ho gayi.');
      } else {
        await api.post('/resources', payload);
        setMessage('Equipment successfully add ho gayi.');
      }

      closeForm();
      await load();
    } catch (e) {
      setError(
          e.response?.data?.message ||
          (e.response?.status === 403
              ? 'Permission denied. Lab Manager/System Admin account check karo.'
              : 'Save nahi hua. Backend response check karo.')
      );
    } finally {
      setSaving(false);
    }
  }

  async function deactivateResource(item) {
    if (!window.confirm(`"${item.name}" ko deactivate karna hai?`)) return;

    setError('');
    setMessage('');
    setActionId(item.id);

    try {
      await api.delete(`/resources/${item.id}`);
      setMessage('Equipment deactivate ho gayi.');
      await load();
      await loadInactive();
    } catch (e) {
      setError(
          e.response?.data?.message ||
          'Equipment deactivate nahi hui. Permission check karo.'
      );
    } finally {
      setActionId(null);
    }
  }

  async function reactivateResource(item) {
    if (!window.confirm(`"${item.name}" ko dobara active karna hai?`)) return;

    setError('');
    setMessage('');
    setActionId(item.id);

    try {
      await api.put(`/resources/${item.id}/reactivate`);
      setMessage(`${item.name} dobara active ho gaya.`);
      await load();
      await loadInactive();
    } catch (e) {
      setError(
          e.response?.data?.message ||
          'Reactivate nahi hua. Backend endpoint aur permission check karo.'
      );
    } finally {
      setActionId(null);
    }
  }

  return (
      <div>
        <div className="page-heading-row">
          <div>
            <span className="eyebrow">INVENTORY</span>
            <h1>Equipment catalog</h1>
            <p>Discover lab resources and check their current availability.</p>
          </div>

          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <button
                type="button"
                className="secondary-button"
                onClick={async () => {
                  await load();
                  await loadInactive();
                }}
                disabled={loading}
            >
              <RefreshCw size={16} /> Refresh
            </button>

            {canManage && (
                <button
                    type="button"
                    className="primary-button"
                    onClick={openAddForm}
                >
                  <Plus size={17} /> Add Equipment
                </button>
            )}
          </div>
        </div>

        {message && (
            <div className="notice" role="status" style={{ marginBottom: 16 }}>
              {message}
            </div>
        )}

        {error && (
            <div className="notice warning" role="alert" style={{ marginBottom: 16 }}>
              {error}
            </div>
        )}

        {showForm && canManage && (
            <section className="panel" style={{ padding: 24, marginBottom: 24 }}>
              <div className="panel-heading">
                <div>
                  <h2>{editingId !== null ? 'Edit Equipment' : 'Add Equipment'}</h2>
                  <p>Equipment ki details bharo.</p>
                </div>
                <button
                    type="button"
                    className="secondary-button"
                    onClick={closeForm}
                    disabled={saving}
                >
                  <X size={17} /> Close
                </button>
              </div>

              <form onSubmit={saveResource}>
                <div
                    style={{
                      display: 'grid',
                      gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                      gap: 16,
                      marginTop: 20,
                    }}
                >
                  <label>
                    Equipment Name *
                    <input
                        name="name"
                        value={form.name}
                        onChange={updateField}
                        required
                        maxLength={150}
                        style={{ display: 'block', width: '100%', marginTop: 6 }}
                    />
                  </label>

                  <label>
                    Category *
                    <input
                        name="category"
                        value={form.category}
                        onChange={updateField}
                        required
                        maxLength={100}
                        style={{ display: 'block', width: '100%', marginTop: 6 }}
                    />
                  </label>

                  <label>
                    Quantity *
                    <input
                        name="quantity"
                        type="number"
                        min="1"
                        step="1"
                        value={form.quantity}
                        onChange={updateField}
                        required
                        style={{ display: 'block', width: '100%', marginTop: 6 }}
                    />
                  </label>

                  <label>
                    Location *
                    <input
                        name="location"
                        value={form.location}
                        onChange={updateField}
                        required
                        maxLength={150}
                        style={{ display: 'block', width: '100%', marginTop: 6 }}
                    />
                  </label>

                  <label style={{ gridColumn: '1 / -1' }}>
                    Description
                    <textarea
                        name="description"
                        value={form.description}
                        onChange={updateField}
                        rows={3}
                        maxLength={1000}
                        style={{ display: 'block', width: '100%', marginTop: 6 }}
                    />
                  </label>
                </div>

                <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
                  <button
                      type="submit"
                      className="primary-button"
                      disabled={saving}
                  >
                    <Save size={17} />
                    {saving
                        ? 'Saving...'
                        : editingId !== null
                            ? 'Save Changes'
                            : 'Add Equipment'}
                  </button>
                  <button
                      type="button"
                      className="secondary-button"
                      onClick={closeForm}
                      disabled={saving}
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </section>
        )}

        <div className="filter-bar">
          <div className="search-box">
            <Search size={17} />
            <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search equipment, location..."
            />
          </div>

          <div className="filter-control">
            <Filter size={16} />
            <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
            >
              <option value="ALL">All categories</option>
              {categories.map((c) => (
                  <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>

          <select
              className="plain-select"
              value={status}
              onChange={(e) => setStatus(e.target.value)}
          >
            <option value="ALL">All statuses</option>
            {[
              'AVAILABLE',
              'BOOKED',
              'UNDER_MAINTENANCE',
              'OUT_OF_SERVICE',
              'RETIRED',
            ].map((s) => (
                <option key={s} value={s}>{s.replaceAll('_', ' ')}</option>
            ))}
          </select>
        </div>

        {loading ? (
            <div className="loading-panel">Loading equipment catalog...</div>
        ) : filtered.length ? (
            <div className="equipment-grid">
              {filtered.map((item, i) => {
                const s = String(item.status || 'UNKNOWN').toUpperCase();
                const qty = item.availableQuantity ?? item.available_quantity ?? 0;

                return (
                    <article className="equipment-card" key={item.id ?? i}>
                      <div className="equipment-visual">
                        <div className="equipment-visual-icon">
                          <PackageSearch size={30} />
                        </div>
                        <span className={`status-chip ${s.toLowerCase().replaceAll('_', '-')}`}>
                    {s.replaceAll('_', ' ')}
                  </span>
                      </div>

                      <div className="equipment-body">
                        <div className="equipment-category">{item.category || 'LAB RESOURCE'}</div>
                        <h3>{item.name || 'Unnamed equipment'}</h3>
                        <p>{item.description || 'No equipment description provided.'}</p>

                        <div className="equipment-meta">
                          <span>Location</span>
                          <strong>{item.location || 'Not specified'}</strong>
                        </div>

                        <div className="equipment-meta">
                          <span>Available quantity</span>
                          <strong>{qty} / {item.quantity ?? '—'}</strong>
                        </div>

                        <a className="card-action" href="/bookings">
                          View booking options <ArrowUpRight size={15} />
                        </a>

                        {canManage && (
                            <div style={{ display: 'flex', gap: 10, marginTop: 14, flexWrap: 'wrap' }}>
                              <button
                                  type="button"
                                  className="secondary-button"
                                  onClick={() => openEditForm(item)}
                              >
                                <Pencil size={15} /> Edit
                              </button>
                              <button
                                  type="button"
                                  className="secondary-button"
                                  onClick={() => deactivateResource(item)}
                                  disabled={actionId === item.id}
                              >
                                <Trash2 size={15} />
                                {actionId === item.id ? 'Please wait...' : 'Deactivate'}
                              </button>
                            </div>
                        )}
                      </div>
                    </article>
                );
              })}
            </div>
        ) : (
            <div className="empty-state panel">
              <PackageSearch size={30} />
              <strong>No equipment found</strong>
              <span>
            {items.length
                ? 'Try changing your search or filters.'
                : 'Equipment will appear when the backend inventory API returns records.'}
          </span>
              {canManage && items.length === 0 && (
                  <button type="button" className="primary-button" onClick={openAddForm}>
                    <Plus size={17} /> Add Equipment
                  </button>
              )}
            </div>
        )}

        {canManage && (
            <section className="panel" style={{ padding: 24, marginTop: 28 }}>
              <div className="panel-heading">
                <div>
                  <span className="eyebrow">INACTIVE INVENTORY</span>
                  <h2>Inactive Equipment</h2>
                  <p>Deactivated resources yahan milenge. Reactivate karne par active catalog mein wapas aayenge.</p>
                </div>
                <button
                    type="button"
                    className="secondary-button"
                    onClick={loadInactive}
                >
                  <RefreshCw size={16} /> Refresh
                </button>
              </div>

              {inactiveItems.length === 0 ? (
                  <div className="empty-state">
                    <PackageSearch size={26} />
                    <strong>No inactive equipment</strong>
                    <span>Abhi koi deactivated resource nahi mila.</span>
                  </div>
              ) : (
                  <div className="table-scroll" style={{ marginTop: 16 }}>
                    <table>
                      <thead>
                      <tr>
                        <th>Equipment</th>
                        <th>Category</th>
                        <th>Location</th>
                        <th>Status</th>
                        <th>Action</th>
                      </tr>
                      </thead>
                      <tbody>
                      {inactiveItems.map((item) => (
                          <tr key={item.id}>
                            <td>{item.name}</td>
                            <td>{item.category}</td>
                            <td>{item.location || '—'}</td>
                            <td>
                              <span className="status-chip inactive">INACTIVE</span>
                            </td>
                            <td>
                              <button
                                  type="button"
                                  className="secondary-button"
                                  onClick={() => reactivateResource(item)}
                                  disabled={actionId === item.id}
                              >
                                <RotateCcw size={15} />
                                {actionId === item.id ? 'Please wait...' : 'Reactivate'}
                              </button>
                            </td>
                          </tr>
                      ))}
                      </tbody>
                    </table>
                  </div>
              )}
            </section>
        )}
      </div>
  );
}
