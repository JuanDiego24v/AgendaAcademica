import { ReactNode } from 'react';
import Sidebar from './Sidebar';

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <>
      <Sidebar />
      <main className="main">{children}</main>
    </>
  );
}
