import React, {useEffect} from 'react';
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
import type {IssueCertificateResponse} from "../types/csr.ts";
import RevocationReason from "./modals/RevocationReason.tsx";
import {apiService} from "../services/api.ts";
import CRLViewer from "./modals/CRLViewer.tsx";

const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [certificates, setCertificates] = useState<Certificate[]>(() => []);
  const [detailsOpen, setDetailsOpen] = useState<Certificate | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [userCreationOpen, setUserCreationOpen] = useState(false);
  const [organisationCreationOpen, setOrganisationCreationOpen] = useState(false);
  const [csrOpen, setCsrOpen] = useState(false);
  const [revokeOpen, setRevokeOpen] = useState<Certificate | null>(null);
  const [crlOpen, setCrlOpen] = useState(false);
    const [downloadingCertId, setDownloadingCertId] = useState<number | null>(null);

  const handleRevokeCertificate = (cert: Certificate) => {
    setRevokeOpen(cert); // Open revocation modal for this certificate
  };

  const handleRevokeConfirmed = async (reason: string, comment?: string) => {
    try {
        if (!revokeOpen) return;
        await apiService.revokeCertificate(revokeOpen.id, { reason, comment });
        setCertificates(prev => prev.filter(c => c.id !== revokeOpen.id));
        toast.success('Certificate revoked successfully');

    } catch (error: any) {
        toast.error(error?.message || 'Failed to revoke the certificate');

    } finally {
        setRevokeOpen(null);
    }
  };

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

    const handleDownloadWithFormat = async (cert: Certificate, format: 'JKS' | 'PKCS12') => {
        try {
            const password = 'changeit'; // You might want to prompt user for this

            // Download keystore in the requested format
            const response = await apiService.downloadKeystoreWithFormat(cert.id, format, password);
            const keystoreBlob = response.data;

            // Extract filename from Content-Disposition header
            const contentDisposition = response.headers['content-disposition'];
            const extension = format === 'JKS' ? '.jks' : '.p12';
            let fileName = `keystore_${cert.id}${extension}`;

            if (contentDisposition) {
                const fileNameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                if (fileNameMatch && fileNameMatch[1]) {
                    fileName = fileNameMatch[1].replace(/['"]/g, '');
                }
            }

            const keystoreUrl = URL.createObjectURL(keystoreBlob);
            const keystoreLink = document.createElement('a');
            keystoreLink.href = keystoreUrl;
            keystoreLink.download = fileName;
            document.body.appendChild(keystoreLink);
            keystoreLink.click();
            keystoreLink.remove();
            URL.revokeObjectURL(keystoreUrl);

            toast.success(`Keystore downloaded successfully in ${format} format`);
            setDownloadingCertId(null);

        } catch (error: unknown) {
            console.error('Keystore download error:', error);
            toast.error((error as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Error downloading keystore');
            setDownloadingCertId(null);
        }
    };

    const handleDownload = (cert: Certificate) => {
        // Toggle format selection for this certificate
        setDownloadingCertId(downloadingCertId === cert.id ? null : cert.id);
    };


    useEffect(() => {
      apiService.getAllCertificates().then(setCertificates)
  }, [detailsOpen, createOpen, userCreationOpen, organisationCreationOpen]);

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
            {
                user.role != 'REGULAR_USER' &&
                <button className="button-primary" onClick={handleCreateCertificate}>
                    <span className="material-symbols-outlined" aria-hidden>auto_awesome</span>
                    Create Certificate
                </button>
            }
            {
                user.role === 'REGULAR_USER' &&
                <button className="button-tonal" onClick={() => setCsrOpen(true)}>
                    <span className="material-symbols-outlined" aria-hidden>file_upload</span>
                    Request EE Certificate (CSR)
                </button>
            }
            {
                user.role === 'ADMIN' &&
                <button className="button-tonal" onClick={handleAddUser}>
                    <span className="material-symbols-outlined" aria-hidden>person_add</span>
                    Add CA User
                </button>
            }

          {/* <button className="button-tonal" onClick={handleCreateKeyPair}>
            <span className="material-symbols-outlined" aria-hidden>auto_awesome</span>
            Create Key Pair
          </button> */}
            {
                user.role === 'ADMIN' &&
                <button className="button-tonal" onClick={handleCreateOrganisation}>
                    <span className="material-symbols-outlined" aria-hidden>auto_awesome</span>
                    Create Organisation
                </button>
            }
            <button className="button-tonal" onClick={() => setCrlOpen(true)}>
                <span className="material-symbols-outlined" aria-hidden>list_alt</span>
                View CRL
            </button>
        </div>

          <div className="dashboard-card">
              <h2>Certificates</h2>
              <div className="cert-grid">
                  {certificates.map(cert => (
                      <div key={cert.id} className="cert-card surface-card">
                          <div className="cert-header">
                              <h3
                                  className="cert-title"
                                  style={{
                                      overflowWrap: 'anywhere',
                                      wordBreak: 'break-word',
                                      whiteSpace: 'pre-wrap',
                                      lineHeight: 1.35,
                                      margin: 0,
                                  }}
                              >
                                  {cert.subject}
                              </h3>
                          </div>
                          <div
                              className="cert-meta"
                              style={{
                                  display: 'grid',
                                  gridTemplateColumns: 'minmax(110px, 160px) 1fr',
                                  columnGap: 12,
                                  rowGap: 8,
                                  alignItems: 'start',
                              }}
                          >
                              <span className="meta-label">Issuer</span>
                              <span
                                  className="meta-value"
                                  style={{
                                      overflowWrap: 'anywhere',
                                      wordBreak: 'break-word',
                                      whiteSpace: 'pre-wrap',
                                      lineHeight: 1.35,
                                  }}
                                  title={cert.issuer}
                              >
                    {cert.issuer}
                  </span>

                              <span className="meta-label">Valid from</span>
                              <span
                                  className="meta-value"
                                  style={{ overflowWrap: 'anywhere', wordBreak: 'break-word' }}
                              >
                             {cert.validFrom}
                              </span>

                              <span className="meta-label">Valid to</span>
                              <span
                                  className="meta-value"
                                  style={{ overflowWrap: 'anywhere', wordBreak: 'break-word' }}
                              >
                              {cert.validTo}
                              </span>

                              {
                                  (cert.keystoreFormat == 'PKCS12' || cert.keystoreFormat == 'JKS') &&
                                  <><span className="meta-label">Keystore Format</span><span
                                      className="meta-value"
                                      style={{overflowWrap: 'anywhere', wordBreak: 'break-word'}}
                                  >
                                      {cert.keystoreFormat}
                                  </span></>
                              }
                          </div>
                          <div className="card-actions">
                              <button className="button-outlined" onClick={() => handleView(cert)}>
                                  <span className="material-symbols-outlined" aria-hidden>visibility</span>
                                  View
                              </button>

                              <div style={{ position: 'relative', display: 'inline-block' }}>
                                  <button className="button-tonal" onClick={() => handleDownload(cert)}>
                                      <span className="material-symbols-outlined" aria-hidden>download</span>
                                      Download
                                  </button>

                                  {downloadingCertId === cert.id && (
                                      <div style={{
                                          position: 'absolute',
                                          top: '100%',
                                          left: 0,
                                          marginTop: '4px',
                                          backgroundColor: 'var(--surface-container-high, #fff)',
                                          border: '1px solid var(--outline-variant, #ccc)',
                                          borderRadius: '8px',
                                          boxShadow: '0 4px 8px rgba(0,0,0,0.1)',
                                          zIndex: 10,
                                          minWidth: '120px',
                                          overflow: 'hidden'
                                      }}>
                                          <button
                                              onClick={() => handleDownloadWithFormat(cert, 'PKCS12')}
                                              style={{
                                                  display: 'block',
                                                  width: '100%',
                                                  padding: '10px 16px',
                                                  border: 'none',
                                                  background: 'transparent',
                                                  textAlign: 'left',
                                                  cursor: 'pointer',
                                                  fontSize: '14px',
                                                  transition: 'background-color 0.2s'
                                              }}
                                              onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'var(--surface-container-highest, #f0f0f0)'}
                                              onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                                          >
                                              PKCS12 (.p12)
                                          </button>
                                          <button
                                              onClick={() => handleDownloadWithFormat(cert, 'JKS')}
                                              style={{
                                                  display: 'block',
                                                  width: '100%',
                                                  padding: '10px 16px',
                                                  border: 'none',
                                                  background: 'transparent',
                                                  textAlign: 'left',
                                                  cursor: 'pointer',
                                                  fontSize: '14px',
                                                  transition: 'background-color 0.2s'
                                              }}
                                              onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'var(--surface-container-highest, #f0f0f0)'}
                                              onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                                          >
                                              JKS (.jks)
                                          </button>
                                      </div>
                                  )}
                              </div>



                              <button className="button-danger" onClick={() => handleRevokeCertificate(cert)}>
                                  <span className="material-symbols-outlined" aria-hidden>delete</span>
                                  Revoke
                              </button>
                          </div>
                      </div>
                  ))}
              </div>
          </div>

      </main>
      {revokeOpen && (
        <RevocationReason
            onClose={() => setRevokeOpen(null)}
            onConfirm={handleRevokeConfirmed}
        />
      )}
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
          onIssued={(res: IssueCertificateResponse) => {
              const newCertificate: Certificate = {
                  id: res.id,
                  subject: res.subjectDN,
                  issuer: res.issuerDN,
                  validFrom: res.validFrom,
                  validTo: res.validTo,
                  extensions: res.extensions,
              };

              setCertificates((prev) => [newCertificate, ...prev]);
          }}

        />
      )}
      {crlOpen && (
         <CRLViewer onClose={() => setCrlOpen(false)} />
      )}

    </div>
  );
};

export default Dashboard;