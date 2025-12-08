import { useState } from 'react'
import StatusBadge from './StatusBadge'
import { updateAlertStatus } from '../services/api'
import './AlertDetail.css'

const AlertDetail = ({ alert, onClose, onUpdate }) => {
  const [status, setStatus] = useState(alert.status)
  const [reviewedBy, setReviewedBy] = useState('')
  const [notes, setNotes] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      await updateAlertStatus(alert.id, status, reviewedBy, notes)
      onUpdate()
    } catch (error) {
      console.error('Failed to update alert:', error)
      alert('Failed to update alert status')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Alert Details</h2>
          <button className="close-btn" onClick={onClose}>&times;</button>
        </div>

        <div className="modal-body">
          <div className="detail-grid">
            <div className="detail-item">
              <label>Alert ID:</label>
              <span>{alert.id}</span>
            </div>
            <div className="detail-item">
              <label>Transaction ID:</label>
              <span>{alert.transactionId}</span>
            </div>
            <div className="detail-item">
              <label>Risk Score:</label>
              <span className="risk-value">{alert.riskScore}</span>
            </div>
            <div className="detail-item">
              <label>Current Status:</label>
              <StatusBadge status={alert.status} />
            </div>
            <div className="detail-item">
              <label>Created At:</label>
              <span>{new Date(alert.createdAt).toLocaleString()}</span>
            </div>
            {alert.reviewedBy && (
              <div className="detail-item">
                <label>Reviewed By:</label>
                <span>{alert.reviewedBy}</span>
              </div>
            )}
          </div>

          {alert.alertData && (
            <div className="alert-data">
              <label>Alert Data:</label>
              <pre>{JSON.stringify(alert.alertData, null, 2)}</pre>
            </div>
          )}

          <form onSubmit={handleSubmit} className="update-form">
            <h3>Update Alert Status</h3>

            <div className="form-group">
              <label>New Status:</label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                required
              >
                <option value="NEW">New</option>
                <option value="REVIEWED">Reviewed</option>
                <option value="CONFIRMED_FRAUD">Confirmed Fraud</option>
                <option value="FALSE_POSITIVE">False Positive</option>
              </select>
            </div>

            <div className="form-group">
              <label>Reviewed By:</label>
              <input
                type="text"
                value={reviewedBy}
                onChange={(e) => setReviewedBy(e.target.value)}
                placeholder="Enter your name"
                required
              />
            </div>

            <div className="form-group">
              <label>Notes:</label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Add review notes..."
                rows="4"
              />
            </div>

            <div className="form-actions">
              <button
                type="button"
                onClick={onClose}
                className="btn-secondary"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading}
                className="btn-primary"
              >
                {loading ? 'Updating...' : 'Update Status'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}

export default AlertDetail
