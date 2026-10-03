import React from 'react';
import { Metadata } from 'next';
import { Target, ShieldCheck, HeartHandshake, Zap, ArrowRight, Store, Users, CheckCircle2 } from 'lucide-react';
import { CompanyLayout } from '@/features/company/components/CompanyLayout';
import { Card } from '@/shared/components/ui/Card';
import { env } from '@/core/config/env';

export const metadata: Metadata = {
  title: 'Sobre Nosotros | JaldiShop',
  description:
    'Conoce la historia, propósito y equipo detrás de JaldiShop. La plataforma de capacidad operativa y pedidos diseñada para MYPEs bajo demanda.',
};

export default function SobreNosotrosPage() {
  return (
    <CompanyLayout
      title="Sobre JaldiShop"
      subtitle="Revolucionando el comercio bajo pedido para micro y pequeñas empresas. Eliminamos la sobreventa y sincronizamos pedidos con la capacidad real del negocio."
      badgeText="Nuestra Misión"
    >
      {/* 1. Origen del Problema */}
      <section className="space-y-4">
        <div className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-[#005141]">
          <Target className="w-4 h-4 text-[#ea580c]" />
          <span>El Desafío que Resolvemos</span>
        </div>
        <h2 className="font-display text-2xl sm:text-3xl font-extrabold text-[#1c1917]">
          El comercio tradicional evalúa stock; nosotros evaluamos capacidad de cumplimiento
        </h2>
        <p className="text-base text-stone-600 leading-relaxed">
          Millones de micro y pequeñas empresas (pastelerías, talleres artesanales, cocinas ocultas, floristerías y creadores independientes) venden todos los días mediante WhatsApp e Instagram. Su principal obstáculo no es la falta de clientes, sino el colapso operativo: aceptar más pedidos de los que humanamente pueden preparar y entregar a tiempo.
        </p>
        <p className="text-base text-stone-600 leading-relaxed">
          Un e-commerce convencional solo verifica si hay productos guardados, ignorando el tiempo de horneado, el armado a mano y los horarios de entrega. <strong>JaldiShop nació para dar tranquilidad a los negocios</strong>: organiza los pedidos por horarios de entrega, asegurando que cada cliente reciba su orden a tiempo y sin sobreventa.
        </p>
      </section>

      {/* 2. Cuatro Pilares Fundamentales (Grid 2x2) */}
      <section className="space-y-6 pt-4">
        <h3 className="font-display text-xl sm:text-2xl font-extrabold text-[#1c1917]">
          Nuestros Principios Operativos
        </h3>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
          <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-3 shadow-xs">
            <div className="w-10 h-10 rounded-2xl bg-emerald-50 border border-emerald-200 text-[#005141] flex items-center justify-center">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <h4 className="font-display text-lg font-extrabold text-[#1c1917]">
              Cero Sobreventa Garantizada
            </h4>
            <p className="text-sm text-stone-600 leading-relaxed">
              Cuando los cupos de un horario se completan, la tienda cierra ese horario de forma automática. Ningún cliente compra lo que el negocio no puede cumplir.
            </p>
          </Card>

          <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-3 shadow-xs">
            <div className="w-10 h-10 rounded-2xl bg-amber-50 border border-amber-200 text-[#ea580c] flex items-center justify-center">
              <Zap className="w-5 h-5" />
            </div>
            <h4 className="font-display text-lg font-extrabold text-[#1c1917]">
              10 Minutos para Pagar con Calma
            </h4>
            <p className="text-sm text-stone-600 leading-relaxed">
              Apartamos tu pedido durante diez minutos mientras realizas tu pago con Yape, Plin o tarjeta, evitando que alguien más tome tu lugar.
            </p>
          </Card>

          <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-3 shadow-xs">
            <div className="w-10 h-10 rounded-2xl bg-stone-100 border border-stone-200 text-stone-800 flex items-center justify-center">
              <HeartHandshake className="w-5 h-5" />
            </div>
            <h4 className="font-display text-lg font-extrabold text-[#1c1917]">
              Empoderamiento para la MYPE
            </h4>
            <p className="text-sm text-stone-600 leading-relaxed">
              Herramientas de nivel empresarial adaptadas a la realidad de la microempresa peruana y latinoamericana, sin contratos forzosos ni comisiones abusivas.
            </p>
          </Card>

          <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-3 shadow-xs">
            <div className="w-10 h-10 rounded-2xl bg-emerald-50 border border-emerald-200 text-[#005141] flex items-center justify-center">
              <Users className="w-5 h-5" />
            </div>
            <h4 className="font-display text-lg font-extrabold text-[#1c1917]">
              Comercio Local con Identidad
            </h4>
            <p className="text-sm text-stone-600 leading-relaxed">
              Cada tienda conserva su identidad de marca, sus recetas, sus técnicas artesanales y su relación directa por WhatsApp con su propia comunidad de clientes.
            </p>
          </Card>
        </div>
      </section>

      {/* 3. Quiénes Somos / Entidad Legal */}
      <section className="p-8 bg-stone-100 rounded-3xl border border-stone-200/90 space-y-4">
        <h3 className="font-display text-xl font-extrabold text-[#1c1917]">
          Estructura Corporativa y Compromiso Legal
        </h3>
        <p className="text-sm text-stone-600 leading-relaxed">
          JaldiShop es una plataforma operada por <strong>JaldiShop Plataformas Digitales S.A.C.</strong> (RUC 20612345678), constituida en Lima, República del Perú. Nuestro equipo combina experiencia en ingeniería de software, arquitectura de sistemas distribuidos y gestión comercial de microempresas.
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2 text-xs font-semibold text-stone-700">
          <div className="flex items-center gap-2 bg-white px-3.5 py-2.5 rounded-xl border border-stone-200">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>Sede en Lima, Perú</span>
          </div>
          <div className="flex items-center gap-2 bg-white px-3.5 py-2.5 rounded-xl border border-stone-200">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>Ley 29571 & Ley 29733</span>
          </div>
          <div className="flex items-center gap-2 bg-white px-3.5 py-2.5 rounded-xl border border-stone-200">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>Soporte Local Dedicado</span>
          </div>
        </div>
      </section>

      {/* 4. Call to Action */}
      <section className="p-8 bg-[#005141] text-white rounded-3xl flex flex-col sm:flex-row items-center justify-between gap-6 shadow-xl">
        <div className="space-y-2 text-center sm:text-left">
          <h4 className="font-display text-xl font-extrabold">¿Tienes un negocio que trabaja bajo pedido?</h4>
          <p className="text-sm text-emerald-100 max-w-md">
            Digitaliza tu catálogo, activa tu control de franjas horarias y empieza a vender con total tranquilidad.
          </p>
        </div>
        <a
          href={env.merchantUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-2 px-6 py-3 rounded-2xl bg-white text-[#005141] font-extrabold text-sm hover:bg-stone-100 transition-all shadow-md shrink-0"
        >
          <Store className="w-4 h-4" />
          <span>Crear mi Tienda Gratis</span>
          <ArrowRight className="w-4 h-4" />
        </a>
      </section>
    </CompanyLayout>
  );
}
