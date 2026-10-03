import type { Metadata, Viewport } from 'next';
import { Plus_Jakarta_Sans, Outfit } from 'next/font/google';
import './globals.css';
import { QueryProvider } from '@/core/providers/QueryProvider';
import { Navbar } from '@/shared/components/layout/Navbar';
import { HashScrollHandler } from '@/shared/components/layout/HashScrollHandler';
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
  title: 'JaldiShop | Pedidos sin sobreventa para pequeños negocios',
  description:
    'Vende por WhatsApp e Instagram aceptando solo los pedidos que puedes preparar a tiempo. 10 minutos para pagar y cero sobreventa para tu negocio.',
  keywords: [
    'JaldiShop',
    'pedidos online',
    'horarios de entrega',
    'MYPE',
    'WhatsApp pedidos',
    'tienda online',
    'pastelerías',
    'talleres artesanales',
    'floristerías',
    'comercio local',
  ],
  authors: [{ name: 'JaldiShop Team' }],
  openGraph: {
    title: 'JaldiShop — Pedidos organizados y sin sobreventa para tu negocio',
    description:
      'Acepta solo los pedidos que puedes entregar a tiempo. Horarios claros para tus clientes y pedidos directos a tu WhatsApp.',
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
    <html
      lang="es"
      className={`${plusJakartaSans.variable} ${outfit.variable} scroll-smooth`}
      data-scroll-behavior="smooth"
    >
      <body className="font-sans antialiased min-h-screen flex flex-col justify-between selection:bg-emerald-500 selection:text-white">
        <QueryProvider>
          <HashScrollHandler />
          <Navbar />
          <main className="flex-1">{children}</main>
          <Toaster richColors position="top-right" />
        </QueryProvider>
      </body>
    </html>
  );
}

