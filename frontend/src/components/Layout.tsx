import { ReactNode } from 'react';
import Sidebar from './Sidebar';
import ChatWidget from './ChatWidget';

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <>
      <Sidebar />
      <main className="main">{children}</main>
      <ChatWidget />
    </>
  );
}
