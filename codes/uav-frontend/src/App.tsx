import { Navigate, Route, Routes } from 'react-router-dom';
import MainLayout from './layout/MainLayout';
import Dashboard from './pages/Dashboard';
import FleetCenter from './pages/FleetCenter';
import MissionCommander from './pages/MissionCommander';
import Monitoring from './pages/Monitoring';
import PersonnelManagement from './pages/PersonnelManagement';
import Login from './pages/Login';
import DataAnalytics from './pages/DataAnalytics';
import { useAuth } from './context/AuthContext';

function App() {
  const { currentUser } = useAuth();

  if (!currentUser) {
    return (
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/fleet" element={<FleetCenter />} />
        <Route path="/missions" element={<MissionCommander />} />
        <Route path="/monitoring" element={<Monitoring />} />
        <Route path="/analytics" element={<DataAnalytics />} />
        {currentUser.role === 'superadmin' ? (
          <Route path="/personnel" element={<PersonnelManagement />} />
        ) : null}
      </Route>
      <Route path="/login" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default App;
