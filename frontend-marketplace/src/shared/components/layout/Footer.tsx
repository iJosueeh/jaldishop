import React from 'react';
import Link from 'next/link';
import { Heart, ShieldCheck, Zap, MessageSquare } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { JaldiShopLogo } from '@/shared/components/ui/JaldiShopLogo';
import { env } from '@/core/config/env';

export function Footer() {
  return (
    <footer className="border-t border-white/10 bg-[#0a0a09] pt-16 pb-12 text-[#a8a29e]">
      <Container size="lg">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-10 pb-12 border-b border-white/10">
          {/* Brand info */}
          <div className="md:col-span-2 space-y-4">
            <Link href="/" className="inline-block">
              <JaldiShopLogo size="md" variant="light" />
            </Link>
            <p className="text-sm leading-relaxed max-w-sm text-[#a8a29e]">
              La plataforma de comercio con control inteligente de capacidad operativa para MYPE. Cero sobreventa, pedidos organizados y clientes satisfechos.
            </p>
            <div className="flex flex-wrap items-center gap-3 text-xs font-semibold">
              <span className="inline-flex items-center gap-1.5 bg-white/5 px-3 py-1 rounded-full border border-white/10 text-[#feae2c]">
                <Zap className="w-3.5 h-3.5" /> 10 min para pagar
              </span>
              <span className="inline-flex items-center gap-1.5 bg-white/5 px-3 py-1 rounded-full border border-white/10 text-emerald-400">
                <ShieldCheck className="w-3.5 h-3.5" /> Capacidad Real
              </span>
              <span className="inline-flex items-center gap-1.5 bg-white/5 px-3 py-1 rounded-full border border-white/10 text-orange-400">
                <MessageSquare className="w-3.5 h-3.5" /> WhatsApp Directo
              </span>
            </div>
          </div>

          {/* Navigation Links */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-white">
              Explorar Tiendas
            </h4>
            <ul className="space-y-2.5 text-sm font-medium">
              <li>
                <Link href="#tiendas-destacadas" className="hover:text-emerald-400 transition-colors">
                  Tiendas Populares
                </Link>
              </li>
              <li>
                <Link href="#como-funciona" className="hover:text-emerald-400 transition-colors">
                  ¿Cómo Funciona?
                </Link>
              </li>
              <li>
                <Link href="/tienda/panaderia-don-pepe" className="hover:text-emerald-400 transition-colors">
                  Tienda Demo (Panadería)
                </Link>
              </li>
              <li>
                <Link href="/tienda/dulce-amor" className="hover:text-emerald-400 transition-colors">
                  Tienda Demo (Repostería)
                </Link>
              </li>
            </ul>
          </div>

          {/* Merchant & Resources */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-white">
              Para Comerciantes
            </h4>
            <ul className="space-y-2.5 text-sm font-medium">
              <li>
                <a
                  href={env.merchantUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-emerald-300 transition-colors font-bold text-[#feae2c]"
                >
                  Panel de Comerciante ↗
                </a>
              </li>
              <li>
                <Link href="#simulador" className="hover:text-emerald-400 transition-colors">
                  Simulador de Capacidad
                </Link>
              </li>
              <li>
                <Link href="#antes-despues" className="hover:text-emerald-400 transition-colors">
                  Antes vs Después
                </Link>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom credits */}
        <div className="pt-8 flex flex-col sm:flex-row items-center justify-between text-xs text-[#78716c] gap-4">
          <p>© {new Date().getFullYear()} JaldiShop. Plataforma de Capacidad y Pedidos para MYPE.</p>
          <p className="flex items-center gap-1.5 font-medium">
            Hecho con <Heart className="w-3.5 h-3.5 text-[#feae2c] fill-[#feae2c]" /> para negocios y emprendimientos bajo pedido
          </p>
        </div>
      </Container>
    </footer>
  );
}
