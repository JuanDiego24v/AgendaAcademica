import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import ChatWidget from './components/ChatWidget';
import Login from './pages/Login';
import Register from './pages/Register';
import Home from './pages/Home';
import Cursos from './pages/Cursos';
import Examenes from './pages/Examenes';
import Calendario from './pages/Calendario';
import ChangePassword from './pages/ChangePassword';
import Perfil from './pages/Perfil';

function GlobalWidgets() {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <ChatWidget /> : null;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/home" element={<ProtectedRoute><Home /></ProtectedRoute>} />
          <Route path="/cursos" element={<ProtectedRoute><Cursos /></ProtectedRoute>} />
          <Route path="/examenes" element={<ProtectedRoute><Examenes /></ProtectedRoute>} />
          <Route path="/calendario" element={<ProtectedRoute><Calendario /></ProtectedRoute>} />
          <Route path="/change-password" element={<ProtectedRoute><ChangePassword /></ProtectedRoute>} />
          <Route path="/perfil" element={<ProtectedRoute><Perfil /></ProtectedRoute>} />
          <Route path="*" element={<Navigate to="/home" replace />} />
        </Routes>
        <GlobalWidgets />
      </BrowserRouter>
    </AuthProvider>
  );
}
