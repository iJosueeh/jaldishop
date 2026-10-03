import React from 'react';
import { Metadata } from 'next';
import Link from 'next/link';
import { HelpCircle, ShoppingBag, Store, MessageCircle, Mail, Clock, ShieldCheck, ArrowRight } from 'lucide-react';
import { CompanyLayout } from '@/features/company/components/CompanyLayout';
import { Card } from '@/shared/components/ui/Card';
import { env } from '@/core/config/env';

export const metadata: Metadata = {
  title: 'Centro de Ayuda & Preguntas Frecuentes | JaldiShop',
  description:
    'Encuentra respuestas a dudas frecuentes sobre compras, reservas de capacidad de 10 minutos, pagos con Yape/Plin y gestión de tiendas en JaldiShop.',
};

export default function AyudaPage() {
  return (
    <CompanyLayout
      title="Centro de Ayuda & FAQ"
      subtitle="Todo lo que necesitas saber para comprar con tranquilidad, reservar tu cupo de entrega o administrar tu tienda con control inteligente de capacidad."
      badgeText="Soporte & Ayuda"
      badgeIcon={<HelpCircle className="w-3.5 h-3.5 text-[#feae2c]" />}
    >
      {/* 1. FAQ Compradores */}
      <section className="space-y-6">
        <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-[#005141]">
          <ShoppingBag className="w-4 h-4 text-[#ea580c]" />
          <span>Para Compradores y Clientes</span>
        </div>
        <h2 className="font-display text-2xl font-extrabold text-[#1c1917]">
          Preguntas Frecuentes sobre tus Pedidos
        </h2>

        <div className="space-y-4">
          <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-2 shadow-xs">
            <h4 className="font-display text-base font-extrabold text-[#1c1917] flex items-center gap-2">
              <Clock className="w-4 h-4 text-[#ea580c] shrink-0" />
              <span>¿Qué es la reserva temporal de 10 minutos?</span>
            </h4>
            <p className="text-sm text-stone-600 leading-relaxed pl-6">
              Cuando seleccionas un horario de entrega o recojo y avanzas al checkout, JaldiShop bloquea ese cupo exclusivamente para ti durante <strong>10 minutos</strong>. Esto te garantiza que nadie más tomará tu horario mientras realizas el pago en tu aplicación bancaria o billetera digital.
            </p>
          </Card>

          <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-2 shadow-xs">
            <h4 className="font-display text-base font-extrabold text-[#1c1917] flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>¿Qué medios de pago puedo utilizar?</span>
            </h4>
            <p className="text-sm text-stone-600 leading-relaxed pl-6">
              Aceptamos pagos móviles instantáneos mediante <strong>Yape y Plin</strong> (escaneando el código QR oficial de la tienda), transferencias bancarias directas y tarjetas de débito/crédito según los canales habilitados por cada comercio.
            </p>
          </Card>

          <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-2 shadow-xs">
            <h4 className="font-display text-base font-extrabold text-[#1c1917] flex items-center gap-2">
              <MessageCircle className="w-4 h-4 text-[#005141] shrink-0" />
              <span>¿Cómo recibo el seguimiento de mi pedido?</span>
            </h4>
            <p className="text-sm text-stone-600 leading-relaxed pl-6">
              Al confirmar tu compra, el sistema te redirige a WhatsApp con tu comprobante y código único de orden preformateado. Además, puedes consultar el estado en tiempo real (<em>Confirmado → En Preparación → En Camino → Completado</em>) desde el portal web.
            </p>
          </Card>
        </div>
      </section>

      {/* 2. FAQ Comerciantes */}
      <section className="space-y-6 pt-4">
        <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-[#ea580c]">
          <Store className="w-4 h-4 text-[#005141]" />
          <span>Para Comerciantes y Emprendedores</span>
        </div>
        <h2 className="font-display text-2xl font-extrabold text-[#1c1917]">
          Gestión de Tienda y Capacidad Operativa
        </h2>

        <div className="space-y-4">
          <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-2 shadow-xs">
            <h4 className="font-display text-base font-extrabold text-[#1c1917]">
              ¿Cómo evita JaldiShop la sobreventa en mi negocio?
            </h4>
            <p className="text-sm text-stone-600 leading-relaxed">
              En tu Panel de Control configuras cuántos pedidos puede preparar tu taller o cocina por franja de tiempo (ej. 4 pedidos de 10:00 a 11:00 AM). En cuanto se alcanza ese límite, la franja horaria se inhabilita automáticamente para los clientes, protegiendo tus tiempos de entrega.
            </p>
          </Card>

          <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-2 shadow-xs">
            <h4 className="font-display text-base font-extrabold text-[#1c1917]">
              ¿Qué rubros comerciales pueden usar JaldiShop?
            </h4>
            <p className="text-sm text-stone-600 leading-relaxed">
              Cualquier micro o pequeña empresa que opere bajo pedido: panaderías y pastelerías de autor, restaurantes, floristerías, talleres de cerámica y artesanías, servicios de catering, confección textil personalizada y tiendas de regalos con horarios de despacho.
            </p>
          </Card>

          <Card className="p-6 bg-white border border-stone-200/90 rounded-3xl space-y-2 shadow-xs">
            <h4 className="font-display text-base font-extrabold text-[#1c1917]">
              ¿Cómo accedo a mi panel de administración?
            </h4>
            <p className="text-sm text-stone-600 leading-relaxed">
              Puedes ingresar en cualquier momento desde el{' '}
              <a
                href={env.merchantUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="text-[#005141] font-bold underline hover:text-[#00382d]"
              >
                Portal del Comerciante
              </a>{' '}
              con tu correo registrado y contraseña para monitorear pedidos en vivo y actualizar tus límites diarios.
            </p>
          </Card>
        </div>
      </section>

      {/* 3. Canales de Contacto Directo */}
      <section className="p-8 bg-stone-900 text-white rounded-3xl space-y-4">
        <h3 className="font-display text-xl font-extrabold text-white">
          ¿Tienes una consulta específica o requieres asistencia?
        </h3>
        <p className="text-sm text-stone-300 leading-relaxed max-w-2xl">
          Nuestro equipo de soporte humano está a tu disposición para ayudarte con cualquier inconveniente técnico o duda comercial:
        </p>

        <div className="pt-2 flex flex-col sm:flex-row items-stretch sm:items-center gap-4">
          <a
            href="mailto:soporte@jaldishop.com"
            className="inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-2xl bg-emerald-600 hover:bg-emerald-500 text-white font-extrabold text-sm transition-all shadow-lg"
          >
            <Mail className="w-4 h-4" />
            <span>soporte@jaldishop.com</span>
            <ArrowRight className="w-4 h-4" />
          </a>

          <Link
            href="/libro-de-reclamaciones"
            className="inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-2xl bg-white/10 hover:bg-white/15 text-white font-bold text-sm transition-all border border-white/10"
          >
            <span>Libro de Reclamaciones</span>
          </Link>
        </div>
      </section>
    </CompanyLayout>
  );
}
