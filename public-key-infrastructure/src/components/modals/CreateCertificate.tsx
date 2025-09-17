import React, { useState } from 'react';
import type { CertificateItem } from '../../types/certificates';

interface CreateCertificateModalProps {
  onClose: () => void;
  onCreate: (cert: CertificateItem) => void;
}

const CreateCertificateModal: React.FC<CreateCertificateModalProps> = ({ onClose, onCreate }) => {
  const [subject, setSubject] = useState('');
  const [issuer, setIssuer] = useState('');
  const [validFrom, setValidFrom] = useState('');
  const [validTo, setValidTo] = useState('');
  const [basicConstraints, setBasicConstraints] = useState('');
  const [keyUsage, setKeyUsage] = useState('');
  const [extendedKeyUsage, setExtendedKeyUsage] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!subject || !issuer || !validFrom || !validTo) return;
    const newCert: CertificateItem = {
      id: `${Date.now()}`,
      subject,
      issuer,
      validFrom,
      validTo,
      status: 'Valid',
      basicConstraints ,
      keyUsage,
      extendedKeyUsage
    };
    onCreate(newCert);
    onClose();
  };

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <div className="modal-card surface-card">
        <div className="modal-header">
          <h3 className="modal-title">Create certificate</h3>
          <button className="button-outlined" onClick={onClose} aria-label="Close">
            <span className="material-symbols-outlined" aria-hidden>close</span>
            Close
          </button>
        </div>
        <form className="modal-content form-grid" onSubmit={handleSubmit}>
          <h4>Basic</h4>
          <label>
            <span className="meta-label">Subject</span>
            <select className="m3-select" value={subject} onChange={(e) => setSubject(e.target.value)}>
              <option value="User">User</option>
              <option value="Server">Server</option>
              <option value="Client">Client</option>
            </select>
          </label>
          <label>
            <span className="meta-label">Issuer CA</span>
            <select className="m3-select" value={issuer} onChange={(e) => setIssuer(e.target.value)}>
              <option value="Certificate 1">Certificate 1</option>
              <option value="Certificate 2">Certificate 2</option>
              <option value="Certificate 3">Certificate 3</option>
            </select>
          </label>
          <label>
            <span className="meta-label">Valid from</span>
            <input type="date" value={validFrom} onChange={(e) => setValidFrom(e.target.value)} />
          </label>
          <label>
            <span className="meta-label">Valid to</span>
            <input type="date" value={validTo} onChange={(e) => setValidTo(e.target.value)} />
          </label>
          <h4>Extensions</h4>
          <label>
            <span className="meta-label">Basic constraints</span>
            <input type="text" value={basicConstraints} onChange={(e) => setBasicConstraints(e.target.value)} placeholder='Add basic constraints'/>
          </label>
          <label>
            <span className="meta-label">Key usage</span>
            <input type="text" value={keyUsage} onChange={(e) => setKeyUsage(e.target.value)} placeholder='Add key usage'/>
          </label>
          <label>
            <span className="meta-label">Extended key usage</span>
            <input type="text" value={extendedKeyUsage} onChange={(e) => setExtendedKeyUsage(e.target.value)} placeholder='Add extended key usage'/>
          </label>
          <div className="modal-actions">
            <button type="button" className="button-outlined" onClick={onClose}>
              Cancel
            </button>
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


