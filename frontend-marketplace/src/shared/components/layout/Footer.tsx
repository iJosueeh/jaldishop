import React from 'react';
import Link from 'next/link';
import { Heart, ShieldCheck, Zap, MessageSquare } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { JaldiShopLogo } from '@/shared/components/ui/JaldiShopLogo';
import { env } from '@/core/config/env';

export function Footer() {
  return (
    <footer className="border-t border-[#e7e0d6] bg-[#faf7f2] pt-16 pb-12 text-[#57534e]">
      <Container size="lg">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-10 pb-12 border-b border-[#e7e0d6]">
          {/* Brand info */}
          <div className="md:col-span-2 space-y-4">
            <Link href="/" className="inline-block">
              <JaldiShopLogo size="md" />
            </Link>
            <p className="text-sm leading-relaxed max-w-sm text-[#57534e]">
              La plataforma de comercio con control inteligente de capacidad operativa para MYPE. Cero sobreventa, pedidos organizados y clientes satisfechos.
            </p>
            <div className="flex flex-wrap items-center gap-4 text-xs font-bold text-[#005141]">
              <span className="inline-flex items-center gap-1.5 bg-[#f0fdfa] px-2.5 py-1 rounded-full border border-[#ccfbf1]">
                <Zap className="w-3.5 h-3.5 text-[#feae2c]" /> Hold 10 min
              </span>
              <span className="inline-flex items-center gap-1.5 bg-[#f0fdfa] px-2.5 py-1 rounded-full border border-[#ccfbf1]">
                <ShieldCheck className="w-3.5 h-3.5 text-[#005141]" /> Capacidad Real
              </span>
              <span className="inline-flex items-center gap-1.5 bg-[#f0fdfa] px-2.5 py-1 rounded-full border border-[#ccfbf1]">
                <MessageSquare className="w-3.5 h-3.5 text-[#ea580c]" /> WhatsApp Bridge
              </span>
            </div>
          </div>

          {/* Navigation Links */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-[#1c1917]">
              Explorar Tiendas
            </h4>
            <ul className="space-y-2.5 text-sm font-medium">
              <li>
                <Link href="#tiendas-destacadas" className="hover:text-[#005141] transition-colors">
                  Tiendas Populares
                </Link>
              </li>
              <li>
                <Link href="#como-funciona" className="hover:text-[#005141] transition-colors">
                  ¿Cómo Funciona?
                </Link>
              </li>
              <li>
                <Link href="/tienda/panaderia-don-pepe" className="hover:text-[#005141] transition-colors">
                  Tienda Demo (Panadería)
                </Link>
              </li>
              <li>
                <Link href="/tienda/dulce-amor" className="hover:text-[#005141] transition-colors">
                  Tienda Demo (Repostería)
                </Link>
              </li>
            </ul>
          </div>

          {/* Merchant & Tech */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-[#1c1917]">
              Para Comerciantes
            </h4>
            <ul className="space-y-2.5 text-sm font-medium">
              <li>
                <a
                  href={env.merchantUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-[#005141] transition-colors font-bold text-[#ea580c]"
                >
                  Panel MYPE (Angular) ↗
                </a>
              </li>
              <li>
                <a
                  href="https://github.com/iJosueeh/jaldishop"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-[#005141] transition-colors"
                >
                  GitHub Repositorio
                </a>
              </li>
              <li>
                <Link href="#antes-despues" className="hover:text-[#005141] transition-colors">
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
            Hecho con <Heart className="w-3.5 h-3.5 text-[#ea580c] fill-[#ea580c]" /> para las pequeñas empresas gastronómicas
          </p>
        </div>
      </Container>
    </footer>
  );
}
