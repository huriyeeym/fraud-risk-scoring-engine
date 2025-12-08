import './StatusBadge.css'

const StatusBadge = ({ status }) => {
  const getStatusClass = () => {
    switch (status) {
      case 'NEW':
        return 'status-new'
      case 'REVIEWED':
        return 'status-reviewed'
      case 'CONFIRMED_FRAUD':
        return 'status-fraud'
      case 'FALSE_POSITIVE':
        return 'status-false'
      default:
        return ''
    }
  }

  const getStatusLabel = () => {
    switch (status) {
      case 'NEW':
        return 'New'
      case 'REVIEWED':
        return 'Reviewed'
      case 'CONFIRMED_FRAUD':
        return 'Confirmed Fraud'
      case 'FALSE_POSITIVE':
        return 'False Positive'
      default:
        return status
    }
  }

  return (
    <span className={`status-badge ${getStatusClass()}`}>
      {getStatusLabel()}
    </span>
  )
}

export default StatusBadge
