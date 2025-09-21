import React, { useState, useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { useAuth } from '../../contexts/AuthContext';
import toast from 'react-hot-toast';
import { apiService } from '../../services/api';
import type { Organisation } from '../../types/auth';

interface UserCreationFormData {
  email: string;
  password: string;
  confirmPassword: string;
  name: string;
  surname: string;
  organizationId: string;
}



interface UserCreationModalProps {
  onClose: () => void;
  onCreate: () => void;
}

function checkPasswordStrength(password: string): number {
  let score = 0;

  // Minimum 8 / 12 characters
  if (password.length >= 12) {
    score += 2;
  } else if (password.length >= 8) {
    score += 1;
  }

  // Uppercase letters
  if (/[A-Z]/.test(password)) {
    score++;
  }

  // Lowercase letters
  if (/[a-z]/.test(password)) {
    score++;
  }

  // Numbers
  if (/[0-9]/.test(password)) {
    score++;
  }

  // Special characters
  if (/[!@#\$%\^&*()_+\-=\[\]{};':"\\|,.<>?]/.test(password)) {
    score++;
  }

  // Passphrase (20+ chars)
  if (password.length >= 20) {
    score++;
  }

  return score;
}

function getPasswordStrengthInfo(strength: number) {
  if (strength < 3) {
    return { text: 'Weak', color: '#dc3545' }; // Red
  } else if (strength < 5) {
    return { text: 'Medium', color: '#ffc107' }; // Yellow
  } else if (strength < 7) {
    return { text: 'Strong', color: '#28a745' }; // Green
  } else {
    return { text: 'Very Strong', color: '#20c997' }; // Teal
  }
}

const UserCreationModal: React.FC<UserCreationModalProps> = ({ onClose, onCreate }) => {
  const [isLoading, setIsLoading] = useState(false);
  const [organisations, setOrganisations] = useState<Organisation[]>([]);
  const { registerCA } = useAuth();

  const {
    register,
    handleSubmit,
    watch,
    control,
    formState: { errors },
  } = useForm<UserCreationFormData>();

  const password = watch('password');

  useEffect(() => {
    const fetchOrganisations = async () => {
      try {
        const response = await apiService.getOrganisations();
        setOrganisations(response);
      } catch (error) {
        console.error('Failed to fetch organisations:', error);
        toast.error('Failed to load organisations');
      }
    };

    fetchOrganisations();
  }, []);

  const onSubmit = async (data: UserCreationFormData) => {
    setIsLoading(true);
    
    console.log('Form data:', data);
    console.log('Selected organization ID:', data.organizationId);
    
    const selectedOrganization = organisations.find(org => org.id.toString() === data.organizationId);
    
    if (!selectedOrganization) {
      toast.error('Please select a valid organization');
      setIsLoading(false);
      return;
    }
    
    try {
      const success = await registerCA({
        email: data.email,
        password: data.password,
        name: data.name,
        surname: data.surname,
        organization: selectedOrganization,
      });

      if (success.success) {
        toast.success('User created successfully!');
        onCreate();
        onClose();
      } else if (success.pwned) {
        console.log('User creation failed, password is pwned. Breach count: ' + success.breachCount);
        toast.error('User creation failed, password is pwned. Breach count: ' + success.breachCount);
      } else {
        toast.error('User creation failed, try again.');
      }
    } catch (error) {
      toast.error('User creation failed, try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="modal-backdrop" role="dialog" aria-modal="true">
      <div className="modal-card surface-card">
        <div className="modal-header">
          <h3 className="modal-title">Create CA User</h3>
          <button className="button-outlined" onClick={onClose} aria-label="Close">
            <span className="material-symbols-outlined" aria-hidden>close</span>
            Close
          </button>
        </div>
        <form className="modal-content form-grid" onSubmit={handleSubmit(onSubmit)}>
          <h4>User Information</h4>
          <label>
            <span className="meta-label">Email</span>
            <input
              type="email"
              {...register('email', {
                required: 'Email is required',
                pattern: {
                  value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                  message: 'Invalid email format',
                },
              })}
              placeholder="Enter email"
            />
            {errors.email && <span className="error">{errors.email.message}</span>}
          </label>

          <label>
            <span className="meta-label">Name</span>
            <input
              type="text"
              {...register('name', {
                required: 'Name is required',
                minLength: {
                  value: 2,
                  message: 'Name must have at least 2 characters',
                },
              })}
              placeholder="Enter name"
            />
            {errors.name && <span className="error">{errors.name.message}</span>}
          </label>

          <label>
            <span className="meta-label">Surname</span>
            <input
              type="text"
              {...register('surname', {
                required: 'Surname is required',
                minLength: {
                  value: 2,
                  message: 'Surname must have at least 2 characters',
                },
              })}
              placeholder="Enter surname"
            />
            {errors.surname && <span className="error">{errors.surname.message}</span>}
          </label>

          <label>
            <span className="meta-label">Organization</span>
            <Controller
              name="organizationId"
              control={control}
              rules={{ required: 'Organization is required' }}
              render={({ field }) => (
                <select
                  className="m3-select"
                  {...field}
                  value={field.value || ''}
                >
                  <option value="">Select organization</option>
                  {organisations.map((org) => (
                    <option key={org.id} value={org.id.toString()}>
                      {org.name} - {org.unit} ({org.country})
                    </option>
                  ))}
                </select>
              )}
            />
            {errors.organizationId && <span className="error">{errors.organizationId.message}</span>}
          </label>

          <h4>Security</h4>
          <label>
            <span className="meta-label">Password</span>
            <input
              type="password"
              {...register('password', {
                required: 'Password is required',
                minLength: {
                  value: 8,
                  message: 'Password must have at least 8 characters',
                },
                maxLength: {
                  value: 128,
                  message: 'Password must have at most 128 characters',
                },
                validate: (value) => {
                  const strength = checkPasswordStrength(value);
                  if (strength < 3) {
                    return false;
                  }
                  return true;
                },
              })}
              placeholder="Enter password"
            />
            {password && (
              <div className="password-strength-indicator">
                <div 
                  className="strength-bar"
                  style={{ 
                    backgroundColor: getPasswordStrengthInfo(checkPasswordStrength(password)).color,
                    width: `${Math.min((checkPasswordStrength(password) / 7) * 100, 100)}%`
                  }}
                ></div>
                <span 
                  className="strength-text"
                  style={{ color: getPasswordStrengthInfo(checkPasswordStrength(password)).color }}
                >
                  {getPasswordStrengthInfo(checkPasswordStrength(password)).text}
                </span>
              </div>
            )}
            {errors.password && <span className="error">{errors.password.message}</span>}
          </label>

          <label>
            <span className="meta-label">Confirm Password</span>
            <input
              type="password"
              {...register('confirmPassword', {
                required: 'Confirm password is required',
                validate: (value) =>
                  value === password || 'Passwords do not match',
              })}
              placeholder="Confirm password"
            />
            {errors.confirmPassword && <span className="error">{errors.confirmPassword.message}</span>}
          </label>

          <div className="modal-actions">
            <button type="button" className="button-outlined" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="button-primary" disabled={isLoading}>
              {isLoading ? 'Creating...' : 'Create User'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default UserCreationModal;
