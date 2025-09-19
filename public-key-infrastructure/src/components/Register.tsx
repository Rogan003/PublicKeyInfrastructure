import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { useAuth } from '../contexts/AuthContext';
import toast from 'react-hot-toast';
import '../styles/Auth.css';
import { apiService } from '../services/api';
import type { Organisation } from '../types/auth';

interface RegisterFormData {
  email: string;
  password: string;
  confirmPassword: string;
  name: string;
  surname: string;
  organizationId: string;
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


const Register: React.FC = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [organisations, setOrganisations] = useState<Organisation[]>([]);
  const { register: registerUser } = useAuth();
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<RegisterFormData>();

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

  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    
    try {

      const selectedOrganization = organisations.find(org => org.id.toString() === data.organizationId);

      const success = await registerUser({
        email: data.email,
        password: data.password,
        name: data.name,
        surname: data.surname,
        organization: selectedOrganization,
      });
 
      if (success.success) {
        toast.success('Registration successful!');
        navigate('/login');
      } else if (success.pwned) {
        console.log('Registration failed, password is pwned. Breach count: ' + success.breachCount);
        toast.error('Registration failed, password is pwned. Breach count: ' + success.breachCount);
      } else {
        toast.error('Registration failed, try again.');
      }
    } catch (error) {
      toast.error('Registration failed, try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2>Registration</h2>
        <form onSubmit={handleSubmit(onSubmit)} className="auth-form auth-form--two-col">
          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              type="email"
              id="email"
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
          </div>

          <div className="form-group">
            <label htmlFor="name">Name</label>
            <input
              type="text"
              id="name"
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
          </div>

          <div className="form-group">
            <label htmlFor="surname">Surname</label>
            <input
              type="text"
              id="surname"
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
          </div>

          <div className="form-group">
            <label htmlFor="organizationId">Organization</label>
            <select
              id="organizationId"
              {...register('organizationId', {
                required: 'Organization is required',
              })}
            >
              <option value="">Select organization</option>
              {organisations.map((org) => (
                <option key={org.id} value={org.id}>
                  {org.name} - {org.unit} ({org.country})
                </option>
              ))}
            </select>
            {errors.organizationId && <span className="error">{errors.organizationId.message}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              type="password"
              id="password"
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
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">Confirm password</label>
            <input
              type="password"
              id="confirmPassword"
              {...register('confirmPassword', {
                required: 'Confirm password is required',
                validate: (value) =>
                  value === password || 'Passwords do not match',
              })}
              placeholder="Confirm password"
            />
            {errors.confirmPassword && <span className="error">{errors.confirmPassword.message}</span>}
          </div>

          <button type="submit" className="auth-button" disabled={isLoading}>
            {isLoading ? 'Registration...' : 'Register'}
          </button>
        </form>

        <div className="auth-links">
          <p>
            Already have an account? <Link to="/login">Login</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Register;