import { useRef, useState } from 'react';
import client from '../api/client';

interface ChatMessage { text: string; sender: 'bot' | 'user'; }

export default function ChatWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([
    { text: '¡Hola! Soy tu asistente IA. Puedes preguntarme sobre la app o subir un PDF de sílabo para importar el curso y sus exámenes automáticamente.', sender: 'bot' },
  ]);
  const [input, setInput] = useState('');
  const [uploading, setUploading] = useState(false);
  const bodyRef = useRef<HTMLDivElement>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  const scrollToBottom = () => {
    setTimeout(() => { if (bodyRef.current) bodyRef.current.scrollTop = bodyRef.current.scrollHeight; }, 50);
  };

  const send = async () => {
    const text = input.trim();
    if (!text) return;
    setMessages(m => [...m, { text, sender: 'user' }]);
    setInput('');
    try {
      const { data } = await client.post<{ respuesta: string; updated: boolean }>(
        '/api/ia/chatbot/ask',
        { mensaje: text, historial: messages },
      );
      setMessages(m => [...m, { text: data.respuesta, sender: 'bot' }]);
      if (data.updated) window.location.reload();
    } catch {
      setMessages(m => [...m, { text: 'Error al conectar con la IA.', sender: 'bot' }]);
    }
    scrollToBottom();
  };

  const handlePdf = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploading(true);
    setMessages(m => [...m, { text: `📄 ${file.name}`, sender: 'user' }]);
    setMessages(m => [...m, { text: 'Procesando sílabo, un momento...', sender: 'bot' }]);
    scrollToBottom();

    try {
      const form = new FormData();
      form.append('file', file);
      const { data } = await client.post<{ message: string; updated: boolean }>(
        '/api/ia/silabo/importar',
        form,
        { headers: { 'Content-Type': 'multipart/form-data' } },
      );
      setMessages(m => {
        const copy = [...m];
        copy[copy.length - 1] = { text: data.message, sender: 'bot' };
        return copy;
      });
      if (data.updated) setTimeout(() => window.location.reload(), 1500);
    } catch (err: unknown) {
      const serverMsg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      const displayMsg = serverMsg || 'Error al procesar el PDF. Verifica que sea un sílabo con texto legible.';
      setMessages(m => {
        const copy = [...m];
        copy[copy.length - 1] = { text: displayMsg, sender: 'bot' };
        return copy;
      });
    } finally {
      setUploading(false);
      if (fileRef.current) fileRef.current.value = '';
    }
    scrollToBottom();
  };

  return (
    <div className="chat-widget">
      <button className="chat-btn" onClick={() => setOpen(o => !o)}>💬</button>
      {open && (
        <div className="chat-window">
          <div className="chat-header">
            <span>Asistente IA</span>
            <button className="chat-close" onClick={() => setOpen(false)}>✕</button>
          </div>
          <div className="chat-body" ref={bodyRef}>
            {messages.map((m, i) => (
              <div key={i} className={`chat-message ${m.sender}`}>{m.text}</div>
            ))}
          </div>
          <div className="chat-input-area">
            <input
              ref={fileRef}
              type="file"
              accept=".pdf"
              style={{ display: 'none' }}
              onChange={handlePdf}
            />
            <button
              className="chat-pdf"
              onClick={() => fileRef.current?.click()}
              disabled={uploading}
              title="Subir sílabo PDF"
            >📎</button>
            <input
              type="text" className="chat-input" placeholder="Escribe tu duda..."
              value={input} onChange={e => setInput(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && send()}
              disabled={uploading}
            />
            <button className="chat-submit" onClick={send} disabled={uploading}>➤</button>
          </div>
          <div className="chat-disclaimer">IA solo para entorno web · puede cometer errores</div>
        </div>
      )}
    </div>
  );
}
