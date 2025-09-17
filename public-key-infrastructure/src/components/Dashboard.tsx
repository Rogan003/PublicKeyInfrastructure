import React from 'react';
import { useMemo, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import '../styles/Dashboard.css';
import toast from 'react-hot-toast';
import type { CertificateItem } from '../types/certificates';
import CertificateDetailsModal from './modals/CertificateDetails';
import CreateCertificateModal from './modals/CreateCertificate';

const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [certificates, setCertificates] = useState<CertificateItem[]>(() => [
    {
      id: '1',
      subject: 'CN=Alice, O=Example Corp',
      issuer: 'CN=Example Root CA',
      validFrom: '2025-01-01',
      validTo: '2027-01-01',
      status: 'Valid',
      basicConstraints: 'CA',
      keyUsage: 'Digital Signature',
      extendedKeyUsage: 'Server Auth'
    },
    {
      id: '2',
      subject: 'CN=Bob, O=Example Corp',
      issuer: 'CN=Example Root CA',
      validFrom: '2023-06-01',
      validTo: '2025-06-01',
      status: 'Expired',
      basicConstraints: 'CA',
      keyUsage: 'Digital Signature',
      extendedKeyUsage: 'Server Auth'
    },
    {
      id: '3',
      subject: 'CN=Service API, O=Example Corp',
      issuer: 'CN=Example Intermediate CA',
      validFrom: '2024-04-12',
      validTo: '2026-04-12',
      status: 'Valid',
      basicConstraints: 'CA',
      keyUsage: 'Digital Signature',
      extendedKeyUsage: 'Server Auth'
    }
  ]);
  const [detailsOpen, setDetailsOpen] = useState<CertificateItem | null>(null);
  const [createOpen, setCreateOpen] = useState(false);

  const totalValid = useMemo(() => certificates.filter(c => c.status === 'Valid').length, [certificates]);
  const totalRevoked = useMemo(() => certificates.filter(c => c.status === 'Revoked').length, [certificates]);
  const totalExpired = useMemo(() => certificates.filter(c => c.status === 'Expired').length, [certificates]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleCreateCertificate = () => {
    setCreateOpen(true);
  };

  const handleCreateKeyPair = () => {
    toast('Create Key Pair – not implemented yet');
  };

  const handleAddUser = () => {
    toast('Add CA User – not implemented yet');
  };

  const handleView = (cert: CertificateItem) => {
    setDetailsOpen(cert);
  };

  const handleDownload = (cert: CertificateItem) => {
    const pem = `-----BEGIN CERTIFICATE-----\nMOCK_${cert.id}_BASE64_DATA\n-----END CERTIFICATE-----\n`;
    const blob = new Blob([pem], { type: 'application/x-pem-file' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${cert.subject.replace(/[^a-z0-9]+/gi, '_')}.pem`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
    toast.success('Download started');
  };

  const handleDelete = (cert: CertificateItem) => {
    setCertificates(prev => prev.filter(c => c.id !== cert.id));
    toast.success('Certificate deleted');
  };

  if (!user) {
    return <div>Loading...</div>;
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div className="header-title">
          <span className="material-symbols-outlined app-logo" aria-hidden>shield_person</span>
          <h1>Dashboard</h1>
        </div>
        <div className="user-info">
          <span>{user.email}</span>
          <button onClick={handleLogout} className="logout-button">
            Logout
          </button>
        </div>
      </header>

      <main className="dashboard-content">
        <div className="dashboard-actions">
          <button className="button-primary" onClick={handleCreateCertificate}>
            <span className="material-symbols-outlined" aria-hidden>auto_awesome</span>
            Create Certificate
          </button>
          <button className="button-tonal" onClick={handleCreateKeyPair}>
            <span className="material-symbols-outlined" aria-hidden>auto_awesome</span>
            Create Key Pair
          </button>
          <button className="button-tonal" onClick={handleAddUser}>
            <span className="material-symbols-outlined" aria-hidden>person_add</span>
            Add CA User
          </button>
        </div>

        <div className="dashboard-card">
          <h2>Certificates</h2>
          <div className="cert-summary">
            <span className="chip chip-valid">Valid: {totalValid}</span>
            <span className="chip chip-revoked">Revoked: {totalRevoked}</span>
            <span className="chip chip-expired">Expired: {totalExpired}</span>
          </div>
          <div className="cert-grid">
            {certificates.map(cert => (
              <div key={cert.id} className="cert-card surface-card">
                <div className="cert-header">
                  <h3 className="cert-title">{cert.subject}</h3>
                  <span className={`chip ${cert.status === 'Valid' ? 'chip-valid' : cert.status === 'Revoked' ? 'chip-revoked' : 'chip-expired'}`}>{cert.status}</span>
                </div>
                <div className="cert-meta">
                  <div><span className="meta-label">Issuer</span><span className="meta-value">{cert.issuer}</span></div>
                  <div><span className="meta-label">Valid from</span><span className="meta-value">{cert.validFrom}</span></div>
                  <div><span className="meta-label">Valid to</span><span className="meta-value">{cert.validTo}</span></div>
                </div>
                <div className="card-actions">
                  <button className="button-outlined" onClick={() => handleView(cert)}>
                    <span className="material-symbols-outlined" aria-hidden>visibility</span>
                    View
                  </button>
                  <button className="button-tonal" onClick={() => handleDownload(cert)}>
                    <span className="material-symbols-outlined" aria-hidden>download</span>
                    Download 
                  </button>
                  <button className="button-danger" onClick={() => handleDelete(cert)}>
                    <span className="material-symbols-outlined" aria-hidden>delete</span>
                    Revoke
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </main>
      {detailsOpen && (
        <CertificateDetailsModal certificate={detailsOpen} onClose={() => setDetailsOpen(null)} />
      )}
      {createOpen && (
        <CreateCertificateModal onClose={() => setCreateOpen(false)} onCreate={(c) => setCertificates(prev => [c, ...prev])} />
      )}
    </div>
  );
};

export default Dashboard;