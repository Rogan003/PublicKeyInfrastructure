import React from 'react';
import { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import '../styles/Dashboard.css';
import toast from 'react-hot-toast';
import type {Certificate} from '../types/certificates';
import CertificateDetailsModal from './modals/CertificateDetails';
import CreateCertificateModal from './modals/CreateCertificate';
import UserCreationModal from './modals/UserCreation';
import CreateOrganisationModal from './modals/CreateOrganisation';
import CSRRequestModal from './modals/CSRRequest';

const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [certificates, setCertificates] = useState<Certificate[]>(() => []);
  const [detailsOpen, setDetailsOpen] = useState<Certificate | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [userCreationOpen, setUserCreationOpen] = useState(false);
  const [organisationCreationOpen, setOrganisationCreationOpen] = useState(false);
  const [csrOpen, setCsrOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleCreateCertificate = () => {
    setCreateOpen(true);
  };

  const handleAddUser = () => {
    setUserCreationOpen(true);
  };

  const handleCreateOrganisation = () => {
    setOrganisationCreationOpen(true);
  };

  const handleOrganisationCreated = () => {
    toast.success('Organisation created successfully');
    setOrganisationCreationOpen(false);
  };

  const handleView = (cert: Certificate) => {
    setDetailsOpen(cert);
  };

  const handleDownload = (cert: Certificate) => {
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

  const handleDelete = (cert: Certificate) => {
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

          <button className="button-tonal" onClick={() => setCsrOpen(true)}>
            <span className="material-symbols-outlined" aria-hidden>file_upload</span>
            Request EE Certificate (CSR)
          </button>

          <button className="button-tonal" onClick={handleAddUser}>
            <span className="material-symbols-outlined" aria-hidden>person_add</span>
            Add CA User
          </button>

          {/* <button className="button-tonal" onClick={handleCreateKeyPair}>
            <span className="material-symbols-outlined" aria-hidden>auto_awesome</span>
            Create Key Pair
          </button> */}

           <button className="button-tonal" onClick={handleCreateOrganisation}>
            <span className="material-symbols-outlined" aria-hidden>auto_awesome</span>
            Create Organisation
          </button> 
        </div>

        <div className="dashboard-card">
          <h2>Certificates</h2>
          <div className="cert-grid">
            {certificates.map(cert => (
              <div key={cert.id} className="cert-card surface-card">
                <div className="cert-header">
                  <h3 className="cert-title">{cert.subject}</h3>
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

                  <button disabled={user.role !== 'ADMIN'} className="button-danger" onClick={() => handleDelete(cert)}>
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
        <CreateCertificateModal onClose={() => setCreateOpen(false)} />
      )}
      {userCreationOpen && (
        <UserCreationModal onClose={() => setUserCreationOpen(false)} onCreate={() => {}} />
      )}
      {organisationCreationOpen && (
        <CreateOrganisationModal onClose={() => setOrganisationCreationOpen(false)} onCreate={handleOrganisationCreated} />
      )}
      {csrOpen && (
        <CSRRequestModal
          onClose={() => setCsrOpen(false)}
          onIssued={(res) => {
            // Optionally push into visible list to reflect issuance
            const c: CertificateItem = {
              id: res.certificateId,
              subject: 'New EE Certificate',
              issuer: 'Selected CA',
              commonName: '',
              givenName: '',
              surname: '',
              organizationName: '',
              organizationUnit: '',
              country: '',
              email: '',
              owner: user.email,
              validFrom: '',
              validTo: '',
              status: 'Pending',
              basicConstraints: '',
              keyUsage: '',
              extendedKeyUsage: '',
              certificateType: 'End-Entity'
            };
            setCertificates(prev => [c, ...prev]);
          }}
        />
      )}
    </div>
  );
};

export default Dashboard;