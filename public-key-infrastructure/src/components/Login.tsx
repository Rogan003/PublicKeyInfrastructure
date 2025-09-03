import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { useAuth } from '../contexts/AuthContext';
import toast from 'react-hot-toast';
import '../styles/Auth.css';

interface LoginFormData {
  email: string;
  password: string;
}

const Login: React.FC = () => {
  const [isLoading, setIsLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>();

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);
    
    try {
      const success = await login(data.email, data.password);
      
      if (success) {
        toast.success('Uspešno ste se prijavili!');
        navigate('/dashboard');
      } else {
        toast.error('Neuspešna prijava. Proverite email i lozinku.');
      }
    } catch (error) {
      toast.error('Greška pri prijavi. Pokušajte ponovo.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2>Prijava</h2>
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

          <button type="submit" className="auth-button" disabled={isLoading}>
            {isLoading ? 'Prijavljivanje...' : 'Prijavi se'}
          </button>
        </form>

        <div className="auth-links">
          <p>
            Nemate nalog? <Link to="/register">Registrujte se</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;