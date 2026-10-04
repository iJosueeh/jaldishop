import React from 'react';
import Image from 'next/image';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { Card } from '@/shared/components/ui/Card';
import { Clock, MessageCircle, Smartphone, Check, Sparkles, ShieldCheck, CreditCard } from 'lucide-react';

export function BentoFeatures() {
  return (
    <section id="caracteristicas" className="py-20 sm:py-28 bg-[#0a0a09] text-white relative overflow-hidden scroll-mt-20 sm:scroll-mt-24">
      {/* Decorative Warm Ambient Glows */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-80 h-80 bg-[#005141]/15 blur-3xl rounded-full pointer-events-none" />
      <div className="absolute bottom-0 right-0 w-72 h-72 bg-[#feae2c]/8 blur-3xl rounded-full pointer-events-none" />

      <Container size="lg">
        {/* Section Header */}
        <div className="text-center max-w-2xl mx-auto space-y-3 sm:space-y-4 mb-12 sm:mb-16">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white/10 border border-white/10 text-emerald-400 text-xs font-bold backdrop-blur-md shadow-sm">
            <Sparkles className="w-3.5 h-3.5 text-emerald-400" />
            Herramientas para organizar tus pedidos
          </div>
          <h2 className="font-display text-2xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-white leading-tight">
            Más orden para tu negocio, de principio a fin
          </h2>
          <p className="text-sm sm:text-base lg:text-lg text-stone-300 max-w-xl mx-auto">
            Diseñado para panaderías de masa madre, reposterías de autor, cocinas ocultas y creadores bajo pedido.
          </p>
        </div>

        {/* Bento Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-5 sm:gap-6">
          {/* Bento Item 1: Large Featured Card - Capacidad Dinámica con Foto */}
          <MotionFade delay={0.1} className="lg:col-span-2">
            <Card className="group relative p-6 sm:p-8 h-full bg-[#141413] border border-emerald-500/30 hover:border-emerald-500/50 shadow-2xl rounded-3xl flex flex-col justify-between overflow-hidden transition-all duration-500 hover:-translate-y-1.5 hover:shadow-[0_20px_50px_rgba(0,81,65,0.25)]">
              {/* Background Artisanal Photo with Hover Zoom */}
              <div className="absolute inset-0 z-0">
                <Image
                  src="https://images.unsplash.com/photo-1517433670267-08bbd4be890f?q=80&w=1200&auto=format&fit=crop"
                  alt="Masa madre artesanal en preparación"
                  fill
                  sizes="(max-width: 1023px) 100vw, 66vw"
                  className="object-cover opacity-20 filter contrast-125 transition-transform duration-700 ease-out group-hover:scale-105"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-[#141413] via-[#141413]/80 to-transparent" />
              </div>

              {/* Diagonal Glass Shine sweep on hover */}
              <div className="absolute inset-0 -translate-x-full group-hover:translate-x-full transition-transform duration-1000 bg-gradient-to-r from-transparent via-white/15 to-transparent pointer-events-none z-10" />

              <div className="relative z-10 space-y-3.5 sm:space-y-4">
                <div className="flex items-center gap-3">
                <div className="w-12 h-12 shrink-0 rounded-2xl bg-white/10 border border-white/10 text-emerald-400 flex items-center justify-center shadow-inner backdrop-blur-md">
                  <Clock className="w-6 h-6" />
                </div>
                <span className="text-xs font-semibold text-emerald-300">Tu negocio marca el ritmo</span>
                </div>
                <h3 className="font-display text-2xl sm:text-3xl font-extrabold text-white leading-tight">
                  Recibe pedidos según tus horarios
                </h3>
                <p className="text-sm sm:text-base text-stone-200 leading-relaxed max-w-xl font-normal">
                  Define cuántos pedidos puedes preparar por día o por horario. Cuando se completa el límite, tus clientes pueden elegir otro horario disponible.
                </p>
              </div>

              {/* Visual preview with live capacity status */}
              <div className="relative z-10 mt-6 sm:mt-8">
              <p className="mb-3 text-xs text-stone-300">Ejemplo de horarios que ven tus clientes</p>
              <div className="p-3.5 sm:p-4 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-md grid grid-cols-1 sm:grid-cols-3 gap-2.5 sm:gap-3.5">
                <div className="p-3 bg-white/10 rounded-2xl flex sm:flex-col flex-wrap gap-2 items-center justify-between sm:justify-center text-left sm:text-center shadow-xs border border-emerald-500/40">
                  <div className="text-xs font-bold text-stone-300">10:00 - 11:00 AM</div>
                  <div className="text-sm font-extrabold text-emerald-400 sm:mt-1">4 cupos libres</div>
                </div>
                <div className="p-3 bg-white/10 rounded-2xl flex sm:flex-col flex-wrap gap-2 items-center justify-between sm:justify-center text-left sm:text-center shadow-xs border border-amber-500/40">
                  <div className="text-xs font-bold text-stone-300">11:00 - 12:00 PM</div>
                  <div className="text-sm font-extrabold text-[#feae2c] sm:mt-1">Último cupo</div>
                </div>
                <div className="p-3 bg-white/[0.03] rounded-2xl flex sm:flex-col flex-wrap gap-2 items-center justify-between sm:justify-center text-left sm:text-center border border-white/10">
                  <div className="text-xs font-bold text-stone-300">12:00 - 01:00 PM</div>
                  <div className="text-sm font-extrabold text-rose-300 sm:mt-1">Completo</div>
                </div>
              </div>
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 2: Hold Transaccional 10m con Anillo SVG */}
          <MotionFade delay={0.2} className="lg:col-span-1">
            <Card className="group relative p-6 sm:p-8 h-full bg-gradient-to-br from-[#005141] to-[#00382d] text-white shadow-2xl flex flex-col justify-between rounded-3xl border border-emerald-400/40 overflow-hidden transition-all duration-500 hover:-translate-y-1.5 hover:shadow-[0_20px_50px_rgba(0,81,65,0.35)]">
              {/* Diagonal Glass Shine sweep on hover */}
              <div className="absolute inset-0 -translate-x-full group-hover:translate-x-full transition-transform duration-1000 bg-gradient-to-r from-transparent via-white/15 to-transparent pointer-events-none z-10" />

              <div className="space-y-3.5 sm:space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-white/15 border border-white/20 text-white flex items-center justify-center backdrop-blur-sm shadow-inner">
                  <Clock className="w-6 h-6 text-[#feae2c]" />
                </div>
                <h3 className="font-display text-xl sm:text-2xl font-extrabold text-white">
                  Tiempo para iniciar el pago
                </h3>
                <p className="text-sm text-emerald-100 leading-relaxed font-normal">
                  Su horario se aparta durante 10 minutos al empezar la compra. Si la reserva vence sin iniciar el pago, el cupo vuelve a estar disponible.
                </p>
              </div>

              {/* Animated SVG Ring Pill */}
              <div className="mt-6 pt-4 border-t border-white/15 flex flex-wrap items-center justify-between gap-3 text-xs text-emerald-200">
                <span className="font-semibold flex items-center gap-1.5">
                  <ShieldCheck className="w-4 h-4 text-emerald-300" /> Cupo asegurado
                </span>

                <div className="flex items-center gap-2 bg-white/15 px-3 py-1.5 rounded-xl border border-white/20">
                  <span className="w-2 h-2 rounded-full bg-[#feae2c] animate-pulse" />
                  <span className="font-mono font-black text-xs text-white">10:00 min</span>
                </div>
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 3: Hub de Pagos (Mercado Pago + Billeteras) */}
          <MotionFade delay={0.3} className="lg:col-span-1">
            <Card className="group relative p-6 sm:p-8 h-full bg-[#141413] border border-white/10 hover:border-sky-500/40 shadow-2xl flex flex-col justify-between rounded-3xl overflow-hidden transition-all duration-500 hover:-translate-y-1.5 hover:shadow-[0_20px_50px_rgba(56,189,248,0.15)]">
              {/* Diagonal Glass Shine sweep on hover */}
              <div className="absolute inset-0 -translate-x-full group-hover:translate-x-full transition-transform duration-1000 bg-gradient-to-r from-transparent via-white/15 to-transparent pointer-events-none z-10" />

              <div className="space-y-3.5 sm:space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-white/10 border border-white/10 text-sky-400 flex items-center justify-center">
                  <CreditCard className="w-5 h-5" />
                </div>
                <h3 className="font-display text-xl font-extrabold text-white leading-snug lg:min-h-14">
                  Opciones para cobrar
                </h3>
                <p className="text-sm text-stone-200 leading-relaxed font-normal">
                  Ofrece pagos con tarjeta a través de Mercado Pago o con billeteras digitales como Yape y Plin.
                </p>
              </div>

              {/* Payment badges pill */}
              <div className="mt-6 min-h-12 grid grid-cols-2 gap-2 text-xs font-bold text-center">
                <div className="p-2.5 rounded-xl bg-sky-500/10 border border-sky-400/30 text-sky-300">
                  💳 Mercado Pago
                </div>
                <div className="p-2.5 rounded-xl bg-purple-500/10 border border-purple-400/30 text-purple-300">
                  🟣 Yape / Plin
                </div>
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 4: Catálogo Móvil en Bio */}
          <MotionFade delay={0.4} className="lg:col-span-1">
            <Card className="group relative p-6 sm:p-8 h-full bg-[#141413] border border-white/10 hover:border-orange-500/40 shadow-2xl flex flex-col justify-between rounded-3xl overflow-hidden transition-all duration-500 hover:-translate-y-1.5 hover:shadow-[0_20px_50px_rgba(234,88,12,0.15)]">
              {/* Diagonal Glass Shine sweep on hover */}
              <div className="absolute inset-0 -translate-x-full group-hover:translate-x-full transition-transform duration-1000 bg-gradient-to-r from-transparent via-white/15 to-transparent pointer-events-none z-10" />

              <div className="space-y-3.5 sm:space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-white/10 border border-white/10 text-orange-400 flex items-center justify-center">
                  <Smartphone className="w-5 h-5" />
                </div>
                <h3 className="font-display text-xl font-extrabold text-white leading-snug lg:min-h-14">
                  Un enlace para tu catálogo
                </h3>
                <p className="text-sm text-stone-200 leading-relaxed font-normal">
                  Añade tu tienda a tu perfil de Instagram o compártela por WhatsApp. Productos y horarios en un solo lugar, desde el celular.
                </p>
              </div>

              <div className="mt-6 min-h-12 flex items-center gap-2 text-xs font-bold text-orange-400 bg-white/5 border border-white/10 p-2.5 rounded-xl">
                <Check className="w-4 h-4 text-orange-400 shrink-0" /> Productos y horarios desde el celular
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 5: WhatsApp Bridge */}
          <MotionFade delay={0.5} className="lg:col-span-1">
            <Card className="group relative p-6 sm:p-8 h-full bg-[#141413] border border-white/10 hover:border-emerald-500/40 shadow-2xl flex flex-col justify-between rounded-3xl overflow-hidden transition-all duration-500 hover:-translate-y-1.5 hover:shadow-[0_20px_50px_rgba(0,81,65,0.25)]">
              {/* Diagonal Glass Shine sweep on hover */}
              <div className="absolute inset-0 -translate-x-full group-hover:translate-x-full transition-transform duration-1000 bg-gradient-to-r from-transparent via-white/15 to-transparent pointer-events-none z-10" />

              <div className="space-y-3.5 sm:space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-white/10 border border-white/10 text-emerald-400 flex items-center justify-center">
                  <MessageCircle className="w-5 h-5" />
                </div>
                <h3 className="font-display text-xl font-extrabold text-white leading-snug lg:min-h-14">
                  Menos mensajes para coordinar
                </h3>
                <p className="text-sm text-stone-200 leading-relaxed font-normal">
                  Coordina a partir de un resumen con productos, cantidades, horario y total. Tu cliente puede compartirlo contigo por WhatsApp.
                </p>
              </div>

              <div className="mt-6 min-h-12 flex items-center gap-2 text-xs font-bold text-emerald-400 bg-white/5 border border-white/10 p-2.5 rounded-xl">
                <Check className="w-4 h-4 text-emerald-400 shrink-0" /> Despachos coordinados sin enredos
              </div>
            </Card>
          </MotionFade>
        </div>
      </Container>
    </section>
  );
}
