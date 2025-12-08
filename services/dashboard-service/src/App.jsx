import { useState, useEffect } from 'react'
import AlertList from './components/AlertList'
import AlertDetail from './components/AlertDetail'
import { getAlerts, getAlertsByStatus } from './services/api'
import './App.css'

function App() {
  const [alerts, setAlerts] = useState([])
  const [selectedAlert, setSelectedAlert] = useState(null)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadAlerts()
  }, [statusFilter])

  const loadAlerts = async () => {
    setLoading(true)
    try {
      const data = statusFilter === 'ALL'
        ? await getAlerts()
        : await getAlertsByStatus(statusFilter)
      setAlerts(data)
    } catch (error) {
      console.error('Failed to load alerts:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleAlertClick = (alert) => {
    setSelectedAlert(alert)
  }

  const handleCloseDetail = () => {
    setSelectedAlert(null)
  }

  const handleStatusUpdate = () => {
    loadAlerts()
    setSelectedAlert(null)
  }

  return (
    <div className="app">
      <header className="header">
        <h1>Fraud Detection Dashboard</h1>
      </header>

      <div className="container">
        <div className="filters">
          <label>Filter by Status:</label>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All Alerts</option>
            <option value="NEW">New</option>
            <option value="REVIEWED">Reviewed</option>
            <option value="CONFIRMED_FRAUD">Confirmed Fraud</option>
            <option value="FALSE_POSITIVE">False Positive</option>
          </select>
        </div>

        {loading ? (
          <div className="loading">Loading alerts...</div>
        ) : (
          <AlertList
            alerts={alerts}
            onAlertClick={handleAlertClick}
          />
        )}

        {selectedAlert && (
          <AlertDetail
            alert={selectedAlert}
            onClose={handleCloseDetail}
            onUpdate={handleStatusUpdate}
          />
        )}
      </div>
    </div>
  )
}

export default App
