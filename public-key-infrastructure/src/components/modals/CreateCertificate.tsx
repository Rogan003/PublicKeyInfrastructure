import React, {useEffect, useState} from 'react';
import type {CACertificateInfo, CreateCertificate, Subject} from '../../types/certificates';
import {apiService} from "../../services/api.ts";
import {useAuth} from "../../contexts/AuthContext.tsx";

interface CreateCertificateModalProps {
  onClose: () => void;
}

const CreateCertificateModal: React.FC<CreateCertificateModalProps> = ({ onClose }) => {
    const { user } = useAuth();
  const [commonName, setCommonName] = useState('');
  const [givenName, setGivenName] = useState('');
  const [surname, setSurname] = useState('');
  const [organizationName, setOrganizationName] = useState('');
  const [organizationUnit, setOrganizationUnit] = useState('');
  const [country, setCountry] = useState('');
  const [email, setEmail] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [certificateType, setCertificateType] = useState('RootCA');
  const [issuerCA, setIssuerCA] = useState<number>(-1);
  const [caCertificates, setCACertificates] = useState<CACertificateInfo[]>([]);
  const [selectedExtensions, setSelectedExtensions] = useState<string[]>([]);

  const availableExtensions = [
    { value: 'KEY_USAGE', label: 'Key Usage - digitalSignature | keyEncipherment' },
    { value: 'EXT_KEY_SERVER', label: 'Extended Key Usage - Server Auth' },
    { value: 'EXT_KEY_CLIENT', label: 'Extended Key Usage - Client Auth' },
    { value: 'SAN', label: 'Subject Alternative Name' }
  ];

  // Handle extension selection with validation
  const handleExtensionChange = (extensionValue: string, checked: boolean) => {
    if (checked) {
        setSelectedExtensions([...selectedExtensions, extensionValue]);
    } else {
        setSelectedExtensions(selectedExtensions.filter(ext => ext !== extensionValue));
    }
  };

  useEffect(() => {
      apiService.getAllCACertificates().then(setCACertificates)
      if (user?.role === "CERTIFICATE_AUTHORITY") {
          setCertificateType("IntermediateCA");
      } else {
          setCertificateType("EndEntity");
      }
  }, [])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!commonName || !givenName || !surname || !organizationName || !startDate || !endDate) return;
    if (certificateType !== 'RootCA' && !issuerCA) return;

    const subject: Subject = {
        commonName,
        surname,
        givenName,
        organizationName,
        organizationUnit,
        country,
        email
    };

    const issuerCertificateId = issuerCA === -1 ? undefined : issuerCA


    const newCert: CreateCertificate = {
        issuerCertificateId,
        subject,
        startDate: new Date(startDate),
        endDate: new Date(endDate),
        extensions: selectedExtensions,
        ownerId: user?.id ?? -1
    };

    if (certificateType === 'RootCA') {
        newCert.extensions.push("BASIC_CA");
        setSelectedExtensions([...selectedExtensions, "BASIC_CA"]);
        apiService.createRootCertificate(newCert);
    } else if (certificateType === 'IntermediateCA') {
        newCert.extensions.push("BASIC_CA");
        apiService.createIntermediateCertificate(newCert);
    } else {
        newCert.extensions.push("BASIC_END");
        apiService.createEndEntityCertificate(newCert);
    }

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
          {/* Subject Information Section */}
          <div className="form-section">
            <h4 className="section-title">
              <span className="material-symbols-outlined">person</span>
              Subject Information
            </h4>
            <div className="form-grid-two-columns">
              <div className="form-column">
                <label>
                  <span className="meta-label">Common name *</span>
                  <input 
                    type="text" 
                    value={commonName} 
                    onChange={(e) => setCommonName(e.target.value)} 
                    placeholder="Enter common name"
                    required
                  />
                </label>
                <label>
                  <span className="meta-label">Given name *</span>
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
              </div>
              <div className="form-column">
                <label>
                  <span className="meta-label">Surname *</span>
                  <input 
                    type="text" 
                    value={surname} 
                    onChange={(e) => setSurname(e.target.value)} 
                    placeholder="Enter surname"
                    required
                  />
                </label>
                <label>
                  <span className="meta-label">Organization name *</span>
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
              </div>
            </div>
          </div>

          {/* Certificate Configuration Section */}
          <div className="form-section">
            <h4 className="section-title">
              <span className="material-symbols-outlined">settings</span>
              Certificate Configuration
            </h4>
            <div className="form-grid-two-columns">
              <div className="form-column">
                <label>
                  <span className="meta-label">Certificate type *</span>
                  <select 
                    className="m3-select" 
                    value={certificateType} 
                    onChange={(e) => {
                      setCertificateType(e.target.value);
                      if (e.target.value === 'RootCA') {
                        setIssuerCA(-1);
                      }
                    }}
                  >
                      {
                          user?.role === "ADMIN" &&
                          <option value="RootCA">Root CA</option>
                      }
                      {
                          (user?.role === "ADMIN" || user?.role === "CERTIFICATE_AUTHORITY") &&
                          <option value="IntermediateCA">Intermediate CA</option>
                      }
                    <option value="EndEntity">End Entity</option>
                  </select>
                </label>
                <label>
                  <span className="meta-label">Start Date *</span>
                  <input 
                    type="date" 
                    value={startDate} 
                    onChange={(e) => setStartDate(e.target.value)} 
                    required
                  />
                </label>
              </div>
              <div className="form-column">
                {certificateType !== 'RootCA' && (
                  <label>
                    <span className="meta-label">Issuer CA *</span>
                    <select 
                      className="m3-select" 
                      value={issuerCA} 
                      onChange={(e) => setIssuerCA(Number(e.target.value))}
                      required
                    >
                      <option value="-1">Select issuer CA</option>
                      {caCertificates.map((ca) => (
                        <option key={ca.id} value={ca.id}>
                          {ca.name}
                        </option>
                      ))}
                    </select>
                  </label>
                )}
                <label>
                  <span className="meta-label">End Date *</span>
                  <input 
                    type="date" 
                    value={endDate} 
                    onChange={(e) => setEndDate(e.target.value)} 
                    required
                  />
                </label>
              </div>
            </div>
          </div>
          
          {/* Extensions Section */}
          <div className="form-section">
            <h4 className="section-title">
              <span className="material-symbols-outlined">extension</span>
              Certificate Extensions
            </h4>
            <div className="extensions-grid">
              {availableExtensions.map((extension) => {
                const isSelected = selectedExtensions.includes(extension.value);
                
                return (
                  <label 
                    key={extension.value} 
                    className={`checkbox-label`}
                  >
                    <input
                      type="checkbox"
                      checked={isSelected}
                      onChange={(e) => handleExtensionChange(extension.value, e.target.checked)}
                    />
                    <span className="checkbox-text">{extension.label}</span>
                  </label>
                );
              })}
            </div>
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


