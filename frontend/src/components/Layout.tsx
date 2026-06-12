import { type ReactNode } from 'react';
import Sidebar from './Sidebar';
import WelcomeModal from './WelcomeModal';
import { useAuth } from '../context/AuthContext';

export default function Layout({ children }: { children: ReactNode }) {
  const { periodoActivo, cargandoPeriodo } = useAuth();

  return (
    <>
      <Sidebar />
      <main className="main">{children}</main>
      {!cargandoPeriodo && !periodoActivo && <WelcomeModal />}
    </>
  );
}
