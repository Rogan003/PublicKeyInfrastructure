import React from 'react';
import type { CertificateItem } from '../../types/certificates';

interface CertificateDetailsModalProps {
  certificate: CertificateItem | null;
  onClose: () => void;
}

const CertificateDetailsModal: React.FC<CertificateDetailsModalProps> = ({ certificate, onClose }) => {
  if (!certificate) return null;

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <div className="modal-card surface-card">
        <div className="modal-header">
          <h3 className="modal-title">Certificate details</h3>
          <button className="button-outlined" onClick={onClose} aria-label="Close">
            <span className="material-symbols-outlined" aria-hidden>close</span>
            Close
          </button>
        </div>
        <div className="modal-content">
          <div className="cert-meta">
            <h4>Basic</h4>
            <div><span className="meta-label">Subject</span><span className="meta-value">{certificate.subject}</span></div>
            <div><span className="meta-label">Issuer</span><span className="meta-value">{certificate.issuer}</span></div>
            <div><span className="meta-label">Valid from</span><span className="meta-value">{certificate.validFrom}</span></div>
            <div><span className="meta-label">Valid to</span><span className="meta-value">{certificate.validTo}</span></div>
            <div><span className="meta-label">Status</span><span className="meta-value">{certificate.status}</span></div>
            <h4>Extensions</h4>
            <div><span className="meta-label">Basic constraints</span><span className="meta-value">{certificate.basicConstraints}</span></div>
            <div><span className="meta-label">Key usage</span><span className="meta-value">{certificate.keyUsage}</span></div>
            <div><span className="meta-label">Extended key usage</span><span className="meta-value">{certificate.extendedKeyUsage}</span></div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CertificateDetailsModal;


