export interface Theme {
  '--black': string;
  '--black-soft': string;
  '--black-card': string;
  '--black-hover': string;
  '--orange': string;
  '--orange-glow': string;
  '--orange-dim': string;
  '--orange-pale': string;
  '--orange-line': string;
  '--white': string;
  '--muted': string;
  '--border': string;
}

export const THEMES: Record<string, Theme> = {
  dark: {
    '--black': '#0a0a0a',
    '--black-soft': '#111111',
    '--black-card': '#161616',
    '--black-hover': '#1e1e1e',
    '--orange': '#E87620',
    '--orange-glow': '#ff8c2a',
    '--orange-dim': '#b85c14',
    '--orange-pale': 'rgba(232,118,32,0.08)',
    '--orange-line': 'rgba(232,118,32,0.25)',
    '--white': '#f0ede8',
    '--muted': '#6a6560',
    '--border': 'rgba(255,255,255,0.06)',
  },
  'purple-light': {
    '--black': '#fafafa',
    '--black-soft': '#f0eef9',
    '--black-card': '#ffffff',
    '--black-hover': '#ede9f8',
    '--orange': '#8B5CF6',
    '--orange-glow': '#a78bfa',
    '--orange-dim': '#7c3aed',
    '--orange-pale': 'rgba(139,92,246,0.08)',
    '--orange-line': 'rgba(139,92,246,0.25)',
    '--white': '#1a1025',
    '--muted': '#7b6f8a',
    '--border': 'rgba(0,0,0,0.08)',
  },
};

export function applyTheme(themeKey: string): void {
  const theme = THEMES[themeKey] ?? THEMES['dark'];
  const root = document.documentElement;
  Object.entries(theme).forEach(([key, value]) => {
    root.style.setProperty(key, value);
  });
}
