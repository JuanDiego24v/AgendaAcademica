import { useRef, useState } from 'react';
import client from '../api/client';

interface ChatMessage { text: string; sender: 'bot' | 'user'; }

export default function ChatWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([
    { text: '¡Hola! Soy tu asistente IA. ¿En qué puedo ayudarte?', sender: 'bot' },
  ]);
  const [input, setInput] = useState('');
  const bodyRef = useRef<HTMLDivElement>(null);

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
    setTimeout(() => { if (bodyRef.current) bodyRef.current.scrollTop = bodyRef.current.scrollHeight; }, 50);
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
              type="text" className="chat-input" placeholder="Escribe tu duda..."
              value={input} onChange={e => setInput(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && send()}
            />
            <button className="chat-submit" onClick={send}>➤</button>
          </div>
          <div className="chat-disclaimer">IA solo para entorno web · puede cometer errores</div>
        </div>
      )}
    </div>
  );
}
