import React, { useState } from 'react';
import type { CertificateItem } from '../../types/certificates';

interface CreateCertificateModalProps {
  onClose: () => void;
  onCreate: (cert: CertificateItem) => void;
}

const CreateCertificateModal: React.FC<CreateCertificateModalProps> = ({ onClose, onCreate }) => {
  const [commonName, setCommonName] = useState('');
  const [givenName, setGivenName] = useState('');
  const [surname, setSurname] = useState('');
  const [organizationName, setOrganizationName] = useState('');
  const [organizationUnit, setOrganizationUnit] = useState('');
  const [country, setCountry] = useState('');
  const [email, setEmail] = useState('');
  const [owner, setOwner] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [certificateType, setCertificateType] = useState('RootCA');
  const [issuerCA, setIssuerCA] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!commonName || !givenName || !surname || !organizationName || !startDate || !endDate) return;
    if (certificateType !== 'RootCA' && !issuerCA) return;
    
    const newCert: CertificateItem = {
      id: `${Date.now()}`,
      subject: `${givenName} ${surname} (${commonName})`,
      issuer: certificateType === 'RootCA' ? 'Self-signed' : issuerCA,
      commonName,
      givenName,
      surname,
      organizationName,
      organizationUnit,
      country,
      email,
      owner,
      validFrom: startDate,
      validTo: endDate,
      status: 'Pending',
      basicConstraints: '',
      keyUsage: '',
      extendedKeyUsage: '',
      certificateType
    };
    onCreate(newCert);
    onClose();
  };

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <div className="modal-card surface-card">
        <div className="modal-header">
          <h3 className="modal-title">Create Certificate</h3>
          <button className="button-outlined" onClick={onClose} aria-label="Close">
            <span className="material-symbols-outlined" aria-hidden>close</span>
            Close
          </button>
        </div>
        <form className="modal-content" onSubmit={handleSubmit}>
          <div className="form-grid-two-columns">
            <div className="form-column">
              <label>
                <span className="meta-label">Common name</span>
                <input 
                  type="text" 
                  value={commonName} 
                  onChange={(e) => setCommonName(e.target.value)} 
                  placeholder="Enter common name"
                  required
                />
              </label>
              <label>
                <span className="meta-label">Given name</span>
                <input 
                  type="text" 
                  value={givenName} 
                  onChange={(e) => setGivenName(e.target.value)} 
                  placeholder="Enter given name"
                  required
                />
              </label>
              <label>
                <span className="meta-label">Organization unit</span>
                <input 
                  type="text" 
                  value={organizationUnit} 
                  onChange={(e) => setOrganizationUnit(e.target.value)} 
                  placeholder="Enter organization unit"
                />
              </label>
              <label>
                <span className="meta-label">Email</span>
                <input 
                  type="email" 
                  value={email} 
                  onChange={(e) => setEmail(e.target.value)} 
                  placeholder="Enter email address"
                />
              </label>
              <label>
                <span className="meta-label">Start Date</span>
                <input 
                  type="date" 
                  value={startDate} 
                  onChange={(e) => setStartDate(e.target.value)} 
                  required
                />
              </label>
            </div>
            <div className="form-column">
              <label>
                <span className="meta-label">Surname</span>
                <input 
                  type="text" 
                  value={surname} 
                  onChange={(e) => setSurname(e.target.value)} 
                  placeholder="Enter surname"
                  required
                />
              </label>
              <label>
                <span className="meta-label">Organization name</span>
                <input 
                  type="text" 
                  value={organizationName} 
                  onChange={(e) => setOrganizationName(e.target.value)} 
                  placeholder="Enter organization name"
                  required
                />
              </label>
              <label>
                <span className="meta-label">Country</span>
                <input 
                  type="text" 
                  value={country} 
                  onChange={(e) => setCountry(e.target.value)} 
                  placeholder="Enter country"
                />
              </label>
              <label>
                <span className="meta-label">Owner</span>
                <select className="m3-select" value={owner} onChange={(e) => setOwner(e.target.value)}>
                  <option value="">Select owner</option>
                  <option value="owner">owner - organisation</option>
                </select>
              </label>
              <label>
                <span className="meta-label">End Date</span>
                <input 
                  type="date" 
                  value={endDate} 
                  onChange={(e) => setEndDate(e.target.value)} 
                  required
                />
              </label>
            </div>
          </div>
          
          <div className="form-bottom-section">
            <label>
              <span className="meta-label">Certificate type</span>
              <select 
                className="m3-select" 
                value={certificateType} 
                onChange={(e) => {
                  setCertificateType(e.target.value);
                  if (e.target.value === 'RootCA') {
                    setIssuerCA('');
                  }
                }}
              >
                <option value="RootCA">RootCA</option>
                <option value="IntermediateCA">IntermediateCA</option>
                <option value="EndEntity">EndEntity</option>
              </select>
            </label>
            
            {certificateType !== 'RootCA' && (
              <label>
                <span className="meta-label">Issuer CA</span>
                <select 
                  className="m3-select" 
                  value={issuerCA} 
                  onChange={(e) => setIssuerCA(e.target.value)}
                  required
                >
                  <option value="">Select issuer CA</option>
                  <option value="Root CA Certificate">Root CA Certificate</option>
                  <option value="Intermediate CA 1">Intermediate CA 1</option>
                  <option value="Intermediate CA 2">Intermediate CA 2</option>
                  <option value="Company Root CA">Company Root CA</option>
                </select>
              </label>
            )}
          </div>
          
          <div className="modal-actions">
            <button type="submit" className="button-primary">
              Create
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateCertificateModal;


