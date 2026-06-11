import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const navItems = [
  { to: '/home',            icon: '⊟', label: 'Dashboard' },
  { to: '/cursos',          icon: '◈', label: 'Cursos' },
  { to: '/examenes',        icon: '✦', label: 'Exámenes' },
  { to: '/calendario',      icon: '◫', label: 'Calendario' },
];

export default function Sidebar() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="sidebar">
      <div className="sidebar-logo">AA</div>

      {navItems.map(({ to, icon, label }) => (
        <NavLink
          key={to}
          to={to}
          className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
        >
          <span className="nav-icon">{icon}</span>
          <span className="nav-label">{label}</span>
        </NavLink>
      ))}

      <NavLink
        to="/change-password"
        className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
        style={{ marginTop: 'auto' }}
      >
        <span className="nav-icon">⚙</span>
        <span className="nav-label">Contraseña</span>
      </NavLink>

      <button className="nav-item" onClick={handleLogout}>
        <span className="nav-icon">⏻</span>
        <span className="nav-label">Logout</span>
      </button>
    </nav>
  );
}
