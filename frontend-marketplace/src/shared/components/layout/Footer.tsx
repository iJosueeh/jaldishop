import React from 'react';
import Link from 'next/link';
import { Heart, ShieldCheck, Zap, MessageSquare, BookOpen, ArrowUpRight } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { JaldiShopLogo } from '@/shared/components/ui/JaldiShopLogo';
import { env } from '@/core/config/env';

export function Footer() {
  return (
    <footer className="border-t border-white/10 bg-[#0a0a09] pt-16 pb-12 text-[#a8a29e]">
      <Container size="lg">
        {/* Main Footer Grid (SaaS Enterprise Structure: 12 columns) */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-12 gap-10 lg:gap-8 pb-12 border-b border-white/10">
          {/* Brand info (Col 1: 4 cols) */}
          <div className="sm:col-span-2 lg:col-span-4 space-y-4">
            <Link href="/" className="inline-block">
              <JaldiShopLogo size="md" variant="light" />
            </Link>
            <p className="text-sm leading-relaxed max-w-sm text-[#a8a29e]">
              Vende por WhatsApp e Instagram sin enredos ni sobreventa. Acepta solo los pedidos que tu negocio puede preparar a tiempo y atiende a tus clientes con tranquilidad.
            </p>
            <div className="flex flex-wrap items-center gap-2 text-xs font-semibold pt-1">
              <span className="inline-flex items-center gap-1.5 bg-white/5 px-2.5 py-1 rounded-full border border-white/10 text-[#feae2c]">
                <Zap className="w-3.5 h-3.5" /> 10 min para pagar
              </span>
              <span className="inline-flex items-center gap-1.5 bg-white/5 px-2.5 py-1 rounded-full border border-white/10 text-emerald-400">
                <ShieldCheck className="w-3.5 h-3.5" /> Cero sobreventa
              </span>
              <span className="inline-flex items-center gap-1.5 bg-white/5 px-2.5 py-1 rounded-full border border-white/10 text-orange-400">
                <MessageSquare className="w-3.5 h-3.5" /> Pedidos por WhatsApp
              </span>
            </div>

            {/* System Status Indicator */}
            <div className="pt-2 flex items-center gap-2 text-xs text-stone-400 font-medium">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
                <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500" />
              </span>
              <span>Tiendas y pedidos activos en tiempo real</span>
            </div>
          </div>

          {/* Col 2: Rubros Comerciales Oficiales - store_categories (3 cols) */}
          <div className="space-y-3 lg:col-span-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-white">
              Explorar Tiendas
            </h4>
            <ul className="space-y-2.5 text-sm font-medium">
              <li>
                <Link href="/#tiendas-destacadas" className="hover:text-emerald-400 transition-colors">
                  Restaurantes & Cafeterías
                </Link>
              </li>
              <li>
                <Link href="/#tiendas-destacadas" className="hover:text-emerald-400 transition-colors">
                  Moda, Textil & Calzado
                </Link>
              </li>
              <li>
                <Link href="/#tiendas-destacadas" className="hover:text-emerald-400 transition-colors">
                  Hogar, Decoración & Cerámica
                </Link>
              </li>
              <li>
                <Link href="/#tiendas-destacadas" className="hover:text-emerald-400 transition-colors">
                  Salud, Belleza & Bienestar
                </Link>
              </li>
              <li>
                <Link href="/#tiendas-destacadas" className="hover:text-emerald-400 transition-colors">
                  Floristerías & Arreglos
                </Link>
              </li>
              <li>
                <Link href="/#tiendas-destacadas" className="hover:text-emerald-400 transition-colors">
                  Supermercado & Bodega Local
                </Link>
              </li>
            </ul>
          </div>

          {/* Col 3: Para Comerciantes MYPE (2 cols) */}
          <div className="space-y-3 lg:col-span-2">
            <h4 className="text-xs font-bold uppercase tracking-wider text-white">
              Para Comercios
            </h4>
            <ul className="space-y-2.5 text-sm font-medium">
              <li>
                <a
                  href={env.merchantUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-emerald-300 transition-colors font-bold text-[#feae2c] inline-flex items-center gap-1"
                >
                  <span>Portal MYPE</span>
                  <ArrowUpRight className="w-3.5 h-3.5" />
                </a>
              </li>
              <li>
                <a
                  href={env.merchantUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-emerald-400 transition-colors inline-flex items-center gap-1"
                >
                  <span>Crear mi Tienda</span>
                  <ArrowUpRight className="w-3.5 h-3.5 opacity-60" />
                </a>
              </li>
              <li>
                <Link href="/#caracteristicas" className="hover:text-emerald-400 transition-colors">
                  Horarios y Cupos
                </Link>
              </li>
              <li>
                <Link href="/#como-funciona" className="hover:text-emerald-400 transition-colors">
                  Cobros con Yape y Tarjetas
                </Link>
              </li>
              <li>
                <Link href="/#antes-despues" className="hover:text-emerald-400 transition-colors">
                  Venta por Chat vs JaldiShop
                </Link>
              </li>
            </ul>
          </div>

          {/* Col 4: Compañía, Soporte & Ayuda (3 cols) */}
          <div className="space-y-3 lg:col-span-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-white">
              Compañía
            </h4>
            <ul className="space-y-2.5 text-sm font-medium">
              <li>
                <Link href="/sobre-nosotros" className="hover:text-emerald-400 transition-colors">
                  Sobre Nosotros
                </Link>
              </li>
              <li>
                <Link href="/trabaja-con-nosotros" className="hover:text-emerald-400 transition-colors">
                  Trabaja con Nosotros
                </Link>
              </li>
              <li>
                <Link href="/ayuda" className="hover:text-emerald-400 transition-colors">
                  Centro de Ayuda & FAQ
                </Link>
              </li>
              <li>
                <a
                  href="mailto:soporte@jaldishop.com"
                  className="hover:text-emerald-400 transition-colors"
                >
                  Soporte & Mesa de Ayuda
                </a>
              </li>
              <li>
                <a
                  href="mailto:contacto@jaldishop.com"
                  className="hover:text-emerald-400 transition-colors"
                >
                  Contacto Corporativo
                </a>
              </li>
            </ul>
          </div>
        </div>

        {/* Legal & Compliance Strip (Perú Legal + INDECOPI + ARCO + Accesibilidad) */}
        <div className="pt-8 pb-4 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-[#a8a29e] border-b border-white/5">
          <div className="flex flex-wrap items-center justify-center sm:justify-start gap-4 sm:gap-6 font-medium">
            <Link href="/terminos" className="hover:text-emerald-400 transition-colors">
              Términos y Condiciones
            </Link>
            <span className="text-white/20 hidden sm:inline">•</span>
            <Link href="/privacidad" className="hover:text-emerald-400 transition-colors">
              Política de Privacidad
            </Link>
            <span className="text-white/20 hidden sm:inline">•</span>
            <Link href="/accesibilidad" className="hover:text-emerald-400 transition-colors">
              Accesibilidad Web
            </Link>
            <span className="text-white/20 hidden sm:inline">•</span>
            <Link
              href="/libro-de-reclamaciones"
              className="hover:text-amber-300 transition-colors inline-flex items-center gap-1.5 font-bold text-stone-200"
            >
              <BookOpen className="w-3.5 h-3.5 text-[#feae2c]" />
              <span>Libro de Reclamaciones</span>
            </Link>
          </div>

          <div className="text-[11px] text-[#78716c] text-center sm:text-right">
            <span>JaldiShop Plataformas Digitales S.A.C. · Lima, Perú</span>
          </div>
        </div>

        {/* Bottom credits */}
        <div className="pt-6 flex flex-col sm:flex-row items-center justify-between text-xs text-[#78716c] gap-4">
          <p suppressHydrationWarning>© {new Date().getFullYear()} JaldiShop. Plataforma de Capacidad y Pedidos para MYPE.</p>
          <p className="flex items-center gap-1.5 font-medium">
            Hecho con <Heart className="w-3.5 h-3.5 text-[#feae2c] fill-[#feae2c]" /> para negocios y creadores bajo pedido
          </p>
        </div>
      </Container>
    </footer>
  );
}
