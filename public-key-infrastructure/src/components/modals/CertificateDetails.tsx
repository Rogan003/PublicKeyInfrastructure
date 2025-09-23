import React from 'react';
import type {Certificate} from '../../types/certificates';

interface CertificateDetailsModalProps {
  certificate: Certificate | null;
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
            <h4>Extensions</h4>
              {/* TODO: Add extensions here */}
          </div>
        </div>
      </div>
    </div>
  );
};

export default CertificateDetailsModal;


