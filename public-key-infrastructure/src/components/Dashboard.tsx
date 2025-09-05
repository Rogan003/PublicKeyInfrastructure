import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import '../styles/Dashboard.css';

const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!user) {
    return <div>Loading...</div>;
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1>Public Key Infrastructure Dashboard</h1>
        <div className="user-info">
          <span>Welcome, {user.email}</span>
          <button onClick={handleLogout} className="logout-button">
            Logout
          </button>
        </div>
      </header>

      <main className="dashboard-content">
        <div className="dashboard-card">
          <h2>User Information</h2>
          <div className="user-details">
            <p><strong>Email:</strong> {user.email}</p>
            <p><strong>Rola:</strong> {user.role}</p>
              <p><strong>Status:</strong> {user.enabled ? 'Active' : 'Inactive'}</p>
            {user.name && <p><strong>Name:</strong> {user.name}</p>}
            {user.surname && <p><strong>Surname:</strong> {user.surname}</p>}
            {user.organization && <p><strong>Organization:</strong> {user.organization}</p>}
          </div>
        </div>

        <div className="dashboard-card">
          <h2>PKI Functionalities</h2>
          <p>Here will be implemented PKI functionalities...</p>
        </div>
      </main>
    </div>
  );
};

export default Dashboard;