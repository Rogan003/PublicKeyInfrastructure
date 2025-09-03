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
    return <div>Učitavanje...</div>;
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1>PKI Dashboard</h1>
        <div className="user-info">
          <span>Dobrodošli, {user.email}</span>
          <button onClick={handleLogout} className="logout-button">
            Odjavi se
          </button>
        </div>
      </header>

      <main className="dashboard-content">
        <div className="dashboard-card">
          <h2>Informacije o korisniku</h2>
          <div className="user-details">
            <p><strong>Email:</strong> {user.email}</p>
            <p><strong>Rola:</strong> {user.role}</p>
            <p><strong>Status:</strong> {user.enabled ? 'Aktivan' : 'Neaktivan'}</p>
            {user.name && <p><strong>Ime:</strong> {user.name}</p>}
            {user.surname && <p><strong>Prezime:</strong> {user.surname}</p>}
            {user.organization && <p><strong>Organizacija:</strong> {user.organization}</p>}
          </div>
        </div>

        <div className="dashboard-card">
          <h2>PKI Funkcionalnosti</h2>
          <p>Ovde će biti implementirane PKI funkcionalnosti...</p>
        </div>
      </main>
    </div>
  );
};

export default Dashboard;