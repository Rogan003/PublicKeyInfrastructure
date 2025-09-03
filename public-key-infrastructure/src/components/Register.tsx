import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { useAuth } from '../contexts/AuthContext';
import toast from 'react-hot-toast';
import '../styles/Auth.css';

interface RegisterFormData {
  email: string;
  password: string;
  confirmPassword: string;
  name: string;
  surname: string;
  organization: string;
}

const Register: React.FC = () => {
  const [isLoading, setIsLoading] = useState(false);
  const { register: registerUser } = useAuth();
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<RegisterFormData>();

  const password = watch('password');

  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    
    try {
      const success = await registerUser({
        email: data.email,
        password: data.password,
        name: data.name,
        surname: data.surname,
        organization: data.organization,
      });
      
      if (success) {
        toast.success('Uspešno ste se registrovali!');
        navigate('/dashboard');
      } else {
        toast.error('Neuspešna registracija. Pokušajte ponovo.');
      }
    } catch (error) {
      toast.error('Greška pri registraciji. Pokušajte ponovo.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2>Registracija</h2>
        <form onSubmit={handleSubmit(onSubmit)} className="auth-form">
          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              type="email"
              id="email"
              {...register('email', {
                required: 'Email je obavezan',
                pattern: {
                  value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                  message: 'Neispravan email format',
                },
              })}
              placeholder="Unesite email"
            />
            {errors.email && <span className="error">{errors.email.message}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="name">Ime</label>
            <input
              type="text"
              id="name"
              {...register('name', {
                required: 'Ime je obavezno',
                minLength: {
                  value: 2,
                  message: 'Ime mora imati najmanje 2 karaktera',
                },
              })}
              placeholder="Unesite ime"
            />
            {errors.name && <span className="error">{errors.name.message}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="surname">Prezime</label>
            <input
              type="text"
              id="surname"
              {...register('surname', {
                required: 'Prezime je obavezno',
                minLength: {
                  value: 2,
                  message: 'Prezime mora imati najmanje 2 karaktera',
                },
              })}
              placeholder="Unesite prezime"
            />
            {errors.surname && <span className="error">{errors.surname.message}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="organization">Organizacija</label>
            <input
              type="text"
              id="organization"
              {...register('organization', {
                required: 'Organizacija je obavezna',
              })}
              placeholder="Unesite organizaciju"
            />
            {errors.organization && <span className="error">{errors.organization.message}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="password">Lozinka</label>
            <input
              type="password"
              id="password"
              {...register('password', {
                required: 'Lozinka je obavezna',
                minLength: {
                  value: 6,
                  message: 'Lozinka mora imati najmanje 6 karaktera',
                },
              })}
              placeholder="Unesite lozinku"
            />
            {errors.password && <span className="error">{errors.password.message}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">Potvrda lozinke</label>
            <input
              type="password"
              id="confirmPassword"
              {...register('confirmPassword', {
                required: 'Potvrda lozinke je obavezna',
                validate: (value) =>
                  value === password || 'Lozinke se ne poklapaju',
              })}
              placeholder="Potvrdite lozinku"
            />
            {errors.confirmPassword && <span className="error">{errors.confirmPassword.message}</span>}
          </div>

          <button type="submit" className="auth-button" disabled={isLoading}>
            {isLoading ? 'Registracija...' : 'Registrujte se'}
          </button>
        </form>

        <div className="auth-links">
          <p>
            Već imate nalog? <Link to="/login">Prijavite se</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Register;