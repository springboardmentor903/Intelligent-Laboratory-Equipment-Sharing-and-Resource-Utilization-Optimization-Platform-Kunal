
import { useEffect, useState } from 'react'
import api from '../../api/client'

const MAX_FILE_SIZE = 10 * 1024 * 1024

const formatSize = bytes => {
    if (!bytes) return '0 KB'
    return `${(bytes / 1024).toFixed(1)} KB`
}

const formatDate = value => {
    if (!value) return '—'
    return new Date(value).toLocaleString('en-IN')
}

export default function DocumentPanel({
                                          resourceId,
                                          workOrderId,
                                          title = 'Documents',
                                          documentTypes = [
                                              'CALIBRATION_CERTIFICATE',
                                              'MANUAL',
                                              'REPORT',
                                              'OTHER'
                                          ]
                                      }) {
    const [documents, setDocuments] = useState([])
    const [file, setFile] = useState(null)
    const [documentType, setDocumentType] = useState(
        documentTypes[0] || 'OTHER'
    )

    const [loading, setLoading] = useState(false)
    const [uploading, setUploading] = useState(false)
    const [error, setError] = useState('')
    const [message, setMessage] = useState('')

    const loadDocuments = async () => {
        if (!resourceId && !workOrderId) return

        setLoading(true)
        setError('')

        try {
            const response = await api.get(
                workOrderId
                    ? `/documents/work-order/${workOrderId}`
                    : `/documents/resource/${resourceId}`
            )

            setDocuments(
                Array.isArray(response.data) ? response.data : []
            )
        } catch (e) {
            setError(
                e.response?.data?.message ||
                e.response?.data?.error ||
                'Documents could not be loaded.'
            )
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        setDocumentType(documentTypes[0] || 'OTHER')
        loadDocuments()
    }, [resourceId, workOrderId])

    const handleFileChange = event => {
        const selectedFile = event.target.files?.[0] || null

        setError('')
        setMessage('')

        if (!selectedFile) {
            setFile(null)
            return
        }

        const allowedTypes = [
            'application/pdf',
            'image/png',
            'image/jpeg'
        ]

        if (!allowedTypes.includes(selectedFile.type)) {
            setFile(null)
            event.target.value = ''
            setError('Only PDF, PNG and JPEG files are allowed.')
            return
        }

        if (selectedFile.size > MAX_FILE_SIZE) {
            setFile(null)
            event.target.value = ''
            setError('Maximum file size allowed is 10 MB.')
            return
        }

        setFile(selectedFile)
    }

    const uploadDocument = async event => {
        event.preventDefault()

        if (!resourceId) {
            setError('Resource ID is required for document upload.')
            return
        }

        if (!file) {
            setError('Please select a document first.')
            return
        }

        const formData = new FormData()

        formData.append('resourceId', String(resourceId))

        if (workOrderId) {
            formData.append('workOrderId', String(workOrderId))
        }

        formData.append('documentType', documentType)
        formData.append('file', file)

        setUploading(true)
        setError('')
        setMessage('')

        try {
            // Let Axios/browser set the multipart boundary automatically.
            await api.post('/documents/upload', formData)

            setFile(null)

            const fileInput = event.currentTarget.querySelector(
                'input[type="file"]'
            )

            if (fileInput) fileInput.value = ''

            setMessage('Document uploaded successfully.')

            await loadDocuments()
        } catch (e) {
            setError(
                e.response?.data?.message ||
                e.response?.data?.error ||
                'Document upload failed. Please try again.'
            )
        } finally {
            setUploading(false)
        }
    }

    const viewDocument = async documentId => {
        setError('')

        try {
            const response = await api.get(
                `/documents/${documentId}/view-link`
            )

            const url = response.data?.url

            if (!url) {
                throw new Error('Secure document URL was not returned.')
            }

            window.open(url, '_blank', 'noopener,noreferrer')
        } catch (e) {
            setError(
                e.response?.data?.message ||
                e.message ||
                'Unable to open document.'
            )
        }
    }

    const deleteDocument = async documentId => {
        const confirmed = window.confirm(
            'Are you sure you want to delete this document?'
        )

        if (!confirmed) return

        setError('')
        setMessage('')

        try {
            await api.delete(`/documents/${documentId}`)

            setMessage('Document deleted successfully.')

            await loadDocuments()
        } catch (e) {
            setError(
                e.response?.data?.message ||
                e.response?.data?.error ||
                'Document deletion failed.'
            )
        }
    }

    return (
        <section className="t9-card">
            <div className="t9-header">
                <div>
                    <h3>{title}</h3>
                    <p>Upload and manage equipment documents securely.</p>
                </div>

                <button
                    type="button"
                    className="t9-btn secondary"
                    onClick={loadDocuments}
                    disabled={loading}
                >
                    Refresh
                </button>
            </div>

            {error && (
                <div className="t9-error">{error}</div>
            )}

            {message && (
                <div className="t9-success">{message}</div>
            )}

            <form
                className="t9-document-upload"
                onSubmit={uploadDocument}
            >
                <div className="t9-field">
                    <label>Document Type</label>

                    <select
                        className="t9-select"
                        value={documentType}
                        onChange={e => setDocumentType(e.target.value)}
                    >
                        {documentTypes.map(type => (
                            <option key={type} value={type}>
                                {type.replaceAll('_', ' ')}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="t9-field">
                    <label>Select File</label>

                    <input
                        className="t9-input"
                        type="file"
                        accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
                        onChange={handleFileChange}
                    />

                    <span className="t9-muted">
            PDF, PNG or JPEG · Maximum 10 MB
          </span>
                </div>

                {file && (
                    <div className="t9-muted">
                        Selected: {file.name} ({formatSize(file.size)})
                    </div>
                )}

                <button
                    className="t9-btn"
                    type="submit"
                    disabled={uploading || !file}
                >
                    {uploading ? 'Uploading...' : 'Upload Document'}
                </button>
            </form>

            <div className="t9-document-list">
                <div className="t9-section-heading">
                    <h3>Uploaded Documents</h3>
                    <span className="t9-pill">{documents.length}</span>
                </div>

                {loading && (
                    <div className="t9-empty">
                        Loading documents...
                    </div>
                )}

                {!loading && documents.length === 0 && (
                    <div className="t9-empty">
                        No documents uploaded yet.
                    </div>
                )}

                {!loading && documents.map(doc => (
                    <div className="t9-doc" key={doc.id}>
                        <div className="t9-document-info">
                            <strong>{doc.originalFileName}</strong>

                            <div className="t9-muted">
                                {doc.documentType?.replaceAll('_', ' ')}
                                {' · '}
                                {formatSize(doc.fileSize)}
                            </div>

                            <div className="t9-muted">
                                Uploaded by: {doc.uploadedBy || '—'}
                                {' · '}
                                {formatDate(doc.uploadedAt)}
                            </div>
                        </div>

                        <div className="t9-inline">
                            <button
                                type="button"
                                className="t9-btn secondary"
                                onClick={() => viewDocument(doc.id)}
                            >
                                View
                            </button>

                            <button
                                type="button"
                                className="t9-btn danger"
                                onClick={() => deleteDocument(doc.id)}
                            >
                                Delete
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </section>
    )
}
