import React from 'react';
import { Metadata } from 'next';
import { Briefcase, Code, Sparkles, Mail, Laptop, Heart, ArrowRight } from 'lucide-react';
import { CompanyLayout } from '@/features/company/components/CompanyLayout';
import { Card } from '@/shared/components/ui/Card';

export const metadata: Metadata = {
  title: 'Trabaja con Nosotros | JaldiShop',
  description:
    'Únete al equipo de JaldiShop. Diseñamos y construimos la infraestructura tecnológica para las micro y pequeñas empresas del Perú y Latinoamérica.',
};

export default function TrabajaConNosotrosPage() {
  return (
    <CompanyLayout
      title="Trabaja con Nosotros"
      subtitle="Buscamos personas apasionadas por el impacto social tangible, la excelencia técnica y las soluciones de comercio que transforman la vida de pequeños negocios."
      badgeText="Cultura & Talento"
      badgeIcon={<Briefcase className="w-3.5 h-3.5 text-[#feae2c]" />}
    >
      {/* 1. Nuestra Cultura */}
      <section className="space-y-4">
        <div className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-[#005141]">
          <Heart className="w-4 h-4 text-[#ea580c]" />
          <span>Nuestra Cultura</span>
        </div>
        <h2 className="font-display text-2xl sm:text-3xl font-extrabold text-[#1c1917]">
          Construyendo software con propósito para la economía real
        </h2>
        <p className="text-base text-stone-600 leading-relaxed">
          En JaldiShop no creamos software por vanidad tecnológica; resolvemos problemas reales para comerciantes que se levantan a las 4:00 a.m. a hornear pan o que pasan horas coordinando pedidos en WhatsApp. Valoramos el pragmatismo, el código limpio, la empatía con el usuario y la curiosidad intelectual.
        </p>
      </section>

      {/* 2. Lo que Valoramos (3 tarjetas) */}
      <section className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-3 shadow-xs">
          <div className="w-10 h-10 rounded-2xl bg-emerald-50 border border-emerald-200 text-[#005141] flex items-center justify-center">
            <Code className="w-5 h-5" />
          </div>
          <h4 className="font-display text-base font-extrabold text-[#1c1917]">
            Rigor y Buenas Prácticas
          </h4>
          <p className="text-sm text-stone-600 leading-relaxed">
            Arquitectura limpia, pruebas unitarias exhaustivas y código bien estructurado tanto en backend como en frontend.
          </p>
        </Card>

        <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-3 shadow-xs">
          <div className="w-10 h-10 rounded-2xl bg-amber-50 border border-amber-200 text-[#ea580c] flex items-center justify-center">
            <Laptop className="w-5 h-5" />
          </div>
          <h4 className="font-display text-base font-extrabold text-[#1c1917]">
            Flexibilidad y Autonomía
          </h4>
          <p className="text-sm text-stone-600 leading-relaxed">
            Trabajo orientado a resultados con horarios flexibles y respeto profundo por el tiempo de concentración profunda.
          </p>
        </Card>

        <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-3 shadow-xs">
          <div className="w-10 h-10 rounded-2xl bg-stone-100 border border-stone-200 text-stone-800 flex items-center justify-center">
            <Sparkles className="w-5 h-5" />
          </div>
          <h4 className="font-display text-base font-extrabold text-[#1c1917]">
            Crecimiento Continuo
          </h4>
          <p className="text-sm text-stone-600 leading-relaxed">
            Espacio para proponer mejoras, liderar módulos críticos y aprender continuamente sobre sistemas de alta concurrencia.
          </p>
        </Card>
      </section>

      {/* 3. Áreas de Oportunidad */}
      <section className="space-y-4 pt-4">
        <h3 className="font-display text-xl sm:text-2xl font-extrabold text-[#1c1917]">
          Perfiles que Frecuentemente Incorporamos
        </h3>
        <div className="space-y-3">
          <div className="p-5 bg-white rounded-2xl border border-stone-200/90 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <h5 className="font-bold text-stone-900 text-base">Desarrollo Frontend & Mobile Web (React / Next.js)</h5>
              <p className="text-xs text-stone-500 mt-0.5">Enfoque en accesibilidad, rendimiento de renderizado SSR/SSG y micro-interacciones táctiles.</p>
            </div>
            <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200 shrink-0 self-start sm:self-center">
              Remoto / Híbrido
            </span>
          </div>

          <div className="p-5 bg-white rounded-2xl border border-stone-200/90 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <h5 className="font-bold text-stone-900 text-base">Desarrollo Backend & Arquitectura Distribuida (Java / Spring Boot)</h5>
              <p className="text-xs text-stone-500 mt-0.5">Gestión de transacciones idempotentes, motores de capacidad y seguridad PCI-DSS.</p>
            </div>
            <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200 shrink-0 self-start sm:self-center">
              Remoto / Híbrido
            </span>
          </div>

          <div className="p-5 bg-white rounded-2xl border border-stone-200/90 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <h5 className="font-bold text-stone-900 text-base">Diseño de Experiencia y Producto (UI/UX)</h5>
              <p className="text-xs text-stone-500 mt-0.5">Interfaces amigables pensadas para usuarios de WhatsApp y dueños de pequeños comercios.</p>
            </div>
            <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200 shrink-0 self-start sm:self-center">
              Remoto / Híbrido
            </span>
          </div>
        </div>
      </section>

      {/* 4. Cómo Postular */}
      <section className="p-8 bg-stone-900 text-white rounded-3xl space-y-4">
        <h3 className="font-display text-xl font-extrabold text-white">
          ¿Te gustaría unirte a JaldiShop?
        </h3>
        <p className="text-sm text-stone-300 leading-relaxed max-w-2xl">
          Siempre nos entusiasma conocer a personas talentosas y comprometidas. Envíanos tu hoja de vida, enlace a tu perfil de LinkedIn o portafolio de proyectos a nuestro buzón de talento:
        </p>

        <div className="pt-2 flex flex-col sm:flex-row items-stretch sm:items-center gap-4">
          <a
            href="mailto:talento@jaldishop.com?subject=Postulaci%C3%B3n%20JaldiShop"
            className="inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-2xl bg-emerald-600 hover:bg-emerald-500 text-white font-extrabold text-sm transition-all shadow-lg"
          >
            <Mail className="w-4 h-4" />
            <span>talento@jaldishop.com</span>
            <ArrowRight className="w-4 h-4" />
          </a>
          <span className="text-xs text-stone-400 self-center">
            Revisamos todas las candidaturas y respondemos en un plazo máximo de 5 días útiles.
          </span>
        </div>
      </section>
    </CompanyLayout>
  );
}
