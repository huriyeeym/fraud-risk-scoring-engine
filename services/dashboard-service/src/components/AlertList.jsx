import StatusBadge from './StatusBadge'
import './AlertList.css'

const AlertList = ({ alerts, onAlertClick }) => {
  if (alerts.length === 0) {
    return (
      <div className="empty-state">
        <p>No alerts found</p>
      </div>
    )
  }

  return (
    <div className="alert-list">
      <table>
        <thead>
          <tr>
            <th>Alert ID</th>
            <th>Transaction ID</th>
            <th>Risk Score</th>
            <th>Status</th>
            <th>Created At</th>
            <th>Reviewed By</th>
          </tr>
        </thead>
        <tbody>
          {alerts.map((alert) => (
            <tr
              key={alert.id}
              onClick={() => onAlertClick(alert)}
              className="alert-row"
            >
              <td>{alert.id}</td>
              <td>{alert.transactionId}</td>
              <td>
                <span className={`risk-score ${getRiskClass(alert.riskScore)}`}>
                  {alert.riskScore}
                </span>
              </td>
              <td>
                <StatusBadge status={alert.status} />
              </td>
              <td>{formatDate(alert.createdAt)}</td>
              <td>{alert.reviewedBy || '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

const getRiskClass = (score) => {
  if (score >= 80) return 'risk-critical'
  if (score >= 60) return 'risk-high'
  if (score >= 40) return 'risk-medium'
  return 'risk-low'
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export default AlertList
