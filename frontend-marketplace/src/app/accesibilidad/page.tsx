import React from 'react';
import { Metadata } from 'next';
import { Eye, CheckCircle2, Shield, Keyboard, Smartphone, Mail, ArrowRight } from 'lucide-react';
import { CompanyLayout } from '@/features/company/components/CompanyLayout';
import { Card } from '@/shared/components/ui/Card';

export const metadata: Metadata = {
  title: 'Declaración de Accesibilidad Web | JaldiShop',
  description:
    'Conoce el compromiso de JaldiShop con la accesibilidad digital, los estándares WCAG 2.1 AA y el diseño inclusivo para todos los usuarios.',
};

export default function AccesibilidadPage() {
  return (
    <CompanyLayout
      title="Declaración de Accesibilidad"
      subtitle="Compromiso formal con la inclusión digital, el acceso universal y el cumplimiento de las pautas internacionales WCAG 2.1 Nivel AA en toda la plataforma."
      badgeText="Inclusión Digital"
      badgeIcon={<Eye className="w-3.5 h-3.5 text-[#feae2c]" />}
    >
      {/* 1. Compromiso */}
      <section className="space-y-4">
        <div className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-[#005141]">
          <Shield className="w-4 h-4 text-[#005141]" />
          <span>Acceso Universal para Todos</span>
        </div>
        <h2 className="font-display text-2xl sm:text-3xl font-extrabold text-[#1c1917]">
          Tecnología diseñada para ser utilizada por cualquier persona, sin barreras
        </h2>
        <p className="text-base text-stone-600 leading-relaxed">
          En <strong>JaldiShop</strong> creemos que el comercio electrónico y las herramientas operativas deben ser accesibles para todas las personas, independientemente de sus capacidades visuales, auditivas, motoras o cognitivas. Trabajamos continuamente para que tanto compradores como comerciantes puedan navegar, ordenar y administrar sus tiendas de forma fluida y autónoma.
        </p>
      </section>

      {/* 2. Medidas de Accesibilidad Implementadas */}
      <section className="space-y-6 pt-2">
        <h3 className="font-display text-xl sm:text-2xl font-extrabold text-[#1c1917]">
          Criterios Técnicos y Estándares Aplicados
        </h3>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
          <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-3 shadow-xs">
            <div className="w-10 h-10 rounded-2xl bg-emerald-50 border border-emerald-200 text-[#005141] flex items-center justify-center">
              <Eye className="w-5 h-5" />
            </div>
            <h4 className="font-display text-base font-extrabold text-[#1c1917]">
              Contraste y Jerarquía Visual
            </h4>
            <p className="text-sm text-stone-600 leading-relaxed">
              Paletas de color diseñadas con ratios de contraste superiores a <strong>4.5:1</strong> para texto normal y <strong>3:1</strong> para elementos gráficos grandes, garantizando legibilidad en cualquier condición de iluminación.
            </p>
          </Card>

          <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-3 shadow-xs">
            <div className="w-10 h-10 rounded-2xl bg-amber-50 border border-amber-200 text-[#ea580c] flex items-center justify-center">
              <Keyboard className="w-5 h-5" />
            </div>
            <h4 className="font-display text-base font-extrabold text-[#1c1917]">
              Navegación Total por Teclado
            </h4>
            <p className="text-sm text-stone-600 leading-relaxed">
              Todos los elementos interactivos (menús, botones de compra, selección de franjas horarias y formularios) cuentan con indicadores de foco visibles y pueden operarse íntegramente mediante la tecla Tab y Enter.
            </p>
          </Card>

          <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-3 shadow-xs">
            <div className="w-10 h-10 rounded-2xl bg-stone-100 border border-stone-200 text-stone-800 flex items-center justify-center">
              <CheckCircle2 className="w-5 h-5" />
            </div>
            <h4 className="font-display text-base font-extrabold text-[#1c1917]">
              Semántica Web y Lectores de Pantalla
            </h4>
            <p className="text-sm text-stone-600 leading-relaxed">
              Estructura HTML5 semántica con roles ARIA, textos alternativos (<code className="text-xs bg-stone-200 px-1 py-0.5 rounded">alt</code>) en todas las imágenes de productos y notificaciones en vivo (<code className="text-xs bg-stone-200 px-1 py-0.5 rounded">aria-live</code>) para alertas dinámicas.
            </p>
          </Card>

          <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-3 shadow-xs">
            <div className="w-10 h-10 rounded-2xl bg-emerald-50 border border-emerald-200 text-[#005141] flex items-center justify-center">
              <Smartphone className="w-5 h-5" />
            </div>
            <h4 className="font-display text-base font-extrabold text-[#1c1917]">
              Diseño Táctil y Responsive
            </h4>
            <p className="text-sm text-stone-600 leading-relaxed">
              Áreas de toque ampliadas (mínimo 44x44 píxeles en dispositivos móviles) que facilitan la interacción a personas con destreza motora reducida o que usan pantallas pequeñas.
            </p>
          </Card>
        </div>
      </section>

      {/* 3. Marco Normativo */}
      <section className="p-8 bg-stone-100 rounded-3xl border border-stone-200/90 space-y-3">
        <h4 className="font-display text-lg font-extrabold text-[#1c1917]">
          Estándares Internacionales de Referencia
        </h4>
        <p className="text-sm text-stone-600 leading-relaxed">
          Nuestras directrices de desarrollo toman como referencia las <strong>Pautas de Accesibilidad para el Contenido Web (WCAG 2.1 Nivel AA)</strong> publicadas por el World Wide Web Consortium (W3C), así como los lineamientos de accesibilidad digital establecidos por el Estado Peruano para plataformas de comercio electrónico.
        </p>
      </section>

      {/* 4. Canal de Asistencia */}
      <section className="p-8 bg-[#005141] text-white rounded-3xl space-y-4 shadow-xl">
        <h3 className="font-display text-xl font-extrabold text-white">
          ¿Encontraste alguna barrera de accesibilidad?
        </h3>
        <p className="text-sm text-emerald-100 leading-relaxed max-w-2xl">
          La accesibilidad es un proceso continuo. Si experimentas alguna dificultad para acceder a algún contenido, realizar una compra o navegar en nuestra plataforma, por favor escríbenos a nuestro equipo de accesibilidad. Atenderemos tu caso con máxima prioridad:
        </p>
        <div className="pt-2">
          <a
            href="mailto:accesibilidad@jaldishop.com?subject=Reporte%20de%20Accesibilidad"
            className="inline-flex items-center gap-2 px-6 py-3.5 rounded-2xl bg-white text-[#005141] font-extrabold text-sm hover:bg-stone-100 transition-all shadow-md"
          >
            <Mail className="w-4 h-4" />
            <span>accesibilidad@jaldishop.com</span>
            <ArrowRight className="w-4 h-4" />
          </a>
        </div>
      </section>
    </CompanyLayout>
  );
}
