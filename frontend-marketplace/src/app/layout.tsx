import type { Metadata, Viewport } from 'next';
import { Plus_Jakarta_Sans, Outfit } from 'next/font/google';
import './globals.css';
import { QueryProvider } from '@/core/providers/QueryProvider';
import { Navbar } from '@/shared/components/layout/Navbar';
import { Toaster } from 'sonner';

const plusJakartaSans = Plus_Jakarta_Sans({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-sans',
});

const outfit = Outfit({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-display',
});

export const metadata: Metadata = {
  title: 'JaldiShop | Control Inteligente de Capacidad y Pedidos para MYPE',
  description:
    'Plataforma de pedidos y gestión de capacidad para micro y pequeñas empresas gastronómicas y artesanales. Vende por WhatsApp e Instagram con cero sobreventa.',
  keywords: [
    'JaldiShop',
    'pedidos online',
    'capacidad operativa',
    'MYPE',
    'WhatsApp pedidos',
    'tienda online',
    'panaderías',
    'dark kitchens',
    'repostería',
  ],
  authors: [{ name: 'JaldiShop Team' }],
  openGraph: {
    title: 'JaldiShop — Cero Sobreventa para tu Negocio Gastronómico',
    description:
      'Sincroniza tus pedidos con la capacidad real de tu cocina. Bloqueo automático de franjas y reserva de 10 minutos.',
    type: 'website',
    locale: 'es_PE',
  },
};

export const viewport: Viewport = {
  themeColor: '#059669',
  width: 'device-width',
  initialScale: 1,
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es" className={`${plusJakartaSans.variable} ${outfit.variable} scroll-smooth`}>
      <body className="font-sans antialiased min-h-screen flex flex-col justify-between selection:bg-emerald-500 selection:text-white">
        <QueryProvider>
          <Navbar />
          <main className="flex-1">{children}</main>
          <Toaster richColors position="top-right" />
        </QueryProvider>
      </body>
    </html>
  );
}

