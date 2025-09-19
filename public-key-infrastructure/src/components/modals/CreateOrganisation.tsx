import React, { useState } from 'react';
import type { Organisation } from '../../types/auth';
import { apiService } from '../../services/api';

interface CreateOrganisationModalProps {
  onClose: () => void;
  onCreate: (organisation: Organisation) => void;
}

const CreateOrganisationModal: React.FC<CreateOrganisationModalProps> = ({ onClose, onCreate }) => {
  const [name, setName] = useState('');
  const [unit, setUnit] = useState('');
  const [country, setCountry] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !unit || !country) return;

    setIsLoading(true);
    try {
      const newOrganisation = await apiService.createOrganisation({
        name,
        unit,
        country
      });
      onCreate(newOrganisation);
      onClose();
    } catch (error) {
      console.error('Error creating organisation:', error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <div className="modal-card surface-card">
        <div className="modal-header">
          <h3 className="modal-title">Create Organisation</h3>
          <button className="button-outlined" onClick={onClose} aria-label="Close">
            <span className="material-symbols-outlined" aria-hidden>close</span>
            Close
          </button>
        </div>
        <form className="modal-content form-grid" onSubmit={handleSubmit}>
          <h4>Organisation Details</h4>
          <label>
            <span className="meta-label">Name *</span>
            <input 
              type="text" 
              value={name} 
              onChange={(e) => setName(e.target.value)} 
              placeholder="Enter organisation name"
              required
            />
          </label>
          <label>
            <span className="meta-label">Unit *</span>
            <input 
              type="text" 
              value={unit} 
              onChange={(e) => setUnit(e.target.value)} 
              placeholder="Enter unit name"
              required
            />
          </label>
          <label>
            <span className="meta-label">Country *</span>
            <input 
              type="text" 
              value={country} 
              onChange={(e) => setCountry(e.target.value)} 
              placeholder="Enter country code (e.g., US, UK, DE)"
              required
            />
          </label>
          <div className="modal-actions">
            <button type="button" className="button-outlined" onClick={onClose} disabled={isLoading}>
              Cancel
            </button>
            <button type="submit" className="button-primary" disabled={isLoading || !name || !unit || !country}>
              {isLoading ? 'Creating...' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateOrganisationModal;
