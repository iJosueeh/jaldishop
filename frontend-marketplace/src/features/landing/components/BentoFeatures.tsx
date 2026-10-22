import React from 'react';
import Image from 'next/image';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { Card } from '@/shared/components/ui/Card';
import { Clock, MessageCircle, Smartphone, Check, Sparkles, ShieldCheck, CreditCard } from 'lucide-react';

export function BentoFeatures() {
  return (
    <section id="caracteristicas" className="pt-8 pb-20 sm:pt-12 sm:pb-28 bg-[#090b0c] text-white relative scroll-mt-20 sm:scroll-mt-24">
      {/* 1. Seamless Blueprint Dot Grid continuing from Comparison */}
      <div className="absolute inset-0 bg-grid-dark pointer-events-none opacity-35 [mask-image:radial-gradient(ellipse_at_center,black_45%,transparent_85%)]" />

      {/* 2. Unified Single Seam Light between ComparisonSection and BentoFeatures */}
      <div className="absolute top-0 -translate-y-1/2 right-12 w-[600px] h-[550px] bg-[#10b981]/15 blur-[140px] rounded-full pointer-events-none" />
      <div className="absolute top-1/3 left-1/2 -translate-x-1/2 w-[600px] h-[350px] bg-[#feae2c]/10 blur-[130px] rounded-full pointer-events-none" />
      <div className="absolute bottom-0 left-10 w-[450px] h-[450px] bg-[#38bdf8]/10 blur-[130px] rounded-full pointer-events-none" />

      <Container size="lg" className="relative z-10">
        {/* Section Header */}
        <div className="text-center max-w-2xl mx-auto space-y-3 sm:space-y-4 mb-12 sm:mb-16">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-[#141a1d] border border-amber-500/25 text-[#feae2c] text-xs font-semibold backdrop-blur-md shadow-lg shadow-amber-950/20">
            <Sparkles className="w-3.5 h-3.5 text-[#feae2c]" />
            Herramientas para organizar tus pedidos
          </div>
          <h2 className="font-display text-2xl sm:text-4xl lg:text-5xl font-light tracking-tight text-white leading-tight">
            Más orden para tu negocio, de principio a fin
          </h2>
          <p className="text-sm sm:text-base lg:text-lg text-[#a1a1aa] max-w-xl mx-auto">
            Diseñado para panaderías de masa madre, reposterías de autor, cocinas ocultas y creadores bajo pedido.
          </p>
        </div>

        {/* Bento Grid with rich functional colors & 12px cards */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-5 sm:gap-6">
          {/* Bento Item 1: Large Featured Card - Capacidad Dinámica */}
          <MotionFade delay={0.1} className="lg:col-span-2">
            <Card className="group relative p-6 sm:p-8 h-full bg-[#121619]/90 backdrop-blur-md border border-white/10 hover:border-emerald-500/50 shadow-xl rounded-[12px] flex flex-col justify-between overflow-hidden transition-all duration-300 hover:-translate-y-1">
              {/* Background Artisanal Photo */}
              <div className="absolute inset-0 z-0">
                <Image
                  src="https://images.unsplash.com/photo-1517433670267-08bbd4be890f?q=80&w=1200&auto=format&fit=crop"
                  alt="Masa madre artesanal en preparación"
                  fill
                  sizes="(max-width: 1023px) 100vw, 66vw"
                  className="object-cover opacity-15 filter contrast-125 transition-transform duration-700 ease-out group-hover:scale-105"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-[#121619] via-[#121619]/80 to-transparent" />
              </div>

              <div className="relative z-10 space-y-3.5 sm:space-y-4">
                <div className="flex items-center gap-3">
                  <div className="w-11 h-11 shrink-0 rounded-[10px] bg-emerald-500/10 border border-emerald-500/25 text-emerald-400 flex items-center justify-center shadow-inner">
                    <Clock className="w-5 h-5" />
                  </div>
                  <span className="text-xs font-semibold text-emerald-400">Tu negocio marca el ritmo</span>
                </div>
                <h3 className="font-display text-2xl sm:text-3xl font-semibold text-white leading-tight">
                  Recibe pedidos según tus horarios
                </h3>
                <p className="text-sm sm:text-base text-[#a1a1aa] leading-relaxed max-w-xl font-normal">
                  Define cuántos pedidos puedes preparar por día o por horario. Cuando se completa el límite, tus clientes pueden elegir otro horario disponible.
                </p>
              </div>

              {/* Visual preview with multi-color capacity status */}
              <div className="relative z-10 mt-6 sm:mt-8">
                <p className="mb-3 text-xs text-stone-400 font-medium">Ejemplo de horarios que ven tus clientes</p>
                <div className="p-3.5 sm:p-4 rounded-[10px] bg-black/60 border border-white/10 backdrop-blur-md grid grid-cols-1 sm:grid-cols-3 gap-2.5 sm:gap-3.5">
                  <div className="p-3 bg-[#121619] rounded-[8px] flex sm:flex-col flex-wrap gap-2 items-center justify-between sm:justify-center text-left sm:text-center border border-emerald-500/40">
                    <div className="text-xs font-medium text-stone-300">10:00 - 11:00 AM</div>
                    <div className="text-sm font-bold text-emerald-400 sm:mt-1">4 cupos libres</div>
                  </div>
                  <div className="p-3 bg-[#121619] rounded-[8px] flex sm:flex-col flex-wrap gap-2 items-center justify-between sm:justify-center text-left sm:text-center border border-[#feae2c]/40">
                    <div className="text-xs font-medium text-stone-300">11:00 - 12:00 PM</div>
                    <div className="text-sm font-bold text-[#feae2c] sm:mt-1">Último cupo</div>
                  </div>
                  <div className="p-3 bg-black/40 rounded-[8px] flex sm:flex-col flex-wrap gap-2 items-center justify-between sm:justify-center text-left sm:text-center border border-white/10">
                    <div className="text-xs font-medium text-stone-500">12:00 - 01:00 PM</div>
                    <div className="text-sm font-medium text-rose-300 sm:mt-1">Completo</div>
                  </div>
                </div>
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 2: Hold Transaccional 10m (Warm Amber Focus) */}
          <MotionFade delay={0.2} className="lg:col-span-1">
            <Card className="group relative p-6 sm:p-8 h-full bg-[#18140e]/95 backdrop-blur-md text-white shadow-xl flex flex-col justify-between rounded-[12px] border border-[#feae2c]/35 overflow-hidden transition-all duration-300 hover:border-[#feae2c]/60 hover:-translate-y-1 shadow-[0_10px_35px_rgba(254,174,44,0.08)]">
              <div className="space-y-3.5 sm:space-y-4">
                <div className="w-11 h-11 rounded-[10px] bg-[#feae2c]/15 border border-[#feae2c]/30 text-[#feae2c] flex items-center justify-center">
                  <Clock className="w-5 h-5 text-[#feae2c]" />
                </div>
                <h3 className="font-display text-xl sm:text-2xl font-semibold text-white">
                  Tiempo para iniciar el pago
                </h3>
                <p className="text-sm text-stone-300 leading-relaxed font-normal">
                  Su horario se aparta durante 10 minutos al empezar la compra. Si la reserva vence sin iniciar el pago, el cupo vuelve a estar disponible.
                </p>
              </div>

              {/* Animated Warm Ring Pill */}
              <div className="mt-6 pt-4 border-t border-white/10 flex flex-wrap items-center justify-between gap-3 text-xs text-stone-300">
                <span className="font-medium flex items-center gap-1.5 text-white">
                  <ShieldCheck className="w-4 h-4 text-[#feae2c]" /> Cupo asegurado
                </span>

                <div className="flex items-center gap-2 bg-black/60 px-3 py-1.5 rounded-full border border-[#feae2c]/35">
                  <span className="w-2 h-2 rounded-full bg-[#feae2c] animate-pulse" />
                  <span className="font-mono font-bold text-xs text-white">10:00 min</span>
                </div>
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 3: Hub de Pagos (Sky Blue & Purple) */}
          <MotionFade delay={0.3} className="lg:col-span-1">
            <Card className="group relative p-6 sm:p-8 h-full bg-[#121619]/90 backdrop-blur-md border border-white/10 hover:border-sky-400/50 shadow-xl flex flex-col justify-between rounded-[12px] overflow-hidden transition-all duration-300 hover:-translate-y-1">
              <div className="space-y-3.5 sm:space-y-4">
                <div className="w-11 h-11 rounded-[10px] bg-sky-500/10 border border-sky-400/25 text-[#38bdf8] flex items-center justify-center">
                  <CreditCard className="w-5 h-5" />
                </div>
                <h3 className="font-display text-xl font-semibold text-white leading-snug lg:min-h-14">
                  Opciones para cobrar
                </h3>
                <p className="text-sm text-[#a1a1aa] leading-relaxed font-normal">
                  Ofrece pagos con tarjeta a través de Mercado Pago o con billeteras digitales peruanas como Yape y Plin.
                </p>
              </div>

              <div className="mt-6 min-h-12 grid grid-cols-2 gap-2 text-xs font-semibold text-center">
                <div className="p-2.5 rounded-[8px] bg-sky-500/10 border border-sky-400/30 text-[#38bdf8] font-bold">
                  💳 Mercado Pago
                </div>
                <div className="p-2.5 rounded-[8px] bg-purple-500/10 border border-purple-400/30 text-[#c084fc] font-bold">
                  🟣 Yape / Plin
                </div>
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 4: Catálogo Móvil en Bio (Warm Orange / Terracotta) */}
          <MotionFade delay={0.4} className="lg:col-span-1">
            <Card className="group relative p-6 sm:p-8 h-full bg-[#121619]/90 backdrop-blur-md border border-white/10 hover:border-orange-500/50 shadow-xl flex flex-col justify-between rounded-[12px] overflow-hidden transition-all duration-300 hover:-translate-y-1">
              <div className="space-y-3.5 sm:space-y-4">
                <div className="w-11 h-11 rounded-[10px] bg-orange-500/10 border border-orange-500/25 text-[#ea580c] flex items-center justify-center">
                  <Smartphone className="w-5 h-5" />
                </div>
                <h3 className="font-display text-xl font-semibold text-white leading-snug lg:min-h-14">
                  Un enlace para tu catálogo
                </h3>
                <p className="text-sm text-[#a1a1aa] leading-relaxed font-normal">
                  Añade tu tienda a tu perfil de Instagram o compártela por WhatsApp. Productos y horarios en un solo lugar, desde el celular.
                </p>
              </div>

              <div className="mt-6 min-h-12 flex items-center gap-2 text-xs font-semibold text-[#ea580c] bg-orange-500/10 border border-orange-500/25 p-2.5 rounded-[8px]">
                <Check className="w-4 h-4 text-[#ea580c] shrink-0" /> Productos y horarios desde el celular
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 5: WhatsApp Bridge (Emerald WhatsApp) */}
          <MotionFade delay={0.5} className="lg:col-span-1">
            <Card className="group relative p-6 sm:p-8 h-full bg-[#121619]/90 backdrop-blur-md border border-white/10 hover:border-emerald-500/50 shadow-xl flex flex-col justify-between rounded-[12px] overflow-hidden transition-all duration-300 hover:-translate-y-1">
              <div className="space-y-3.5 sm:space-y-4">
                <div className="w-11 h-11 rounded-[10px] bg-emerald-500/10 border border-emerald-500/25 text-emerald-400 flex items-center justify-center">
                  <MessageCircle className="w-5 h-5" />
                </div>
                <h3 className="font-display text-xl font-semibold text-white leading-snug lg:min-h-14">
                  Menos mensajes para coordinar
                </h3>
                <p className="text-sm text-[#a1a1aa] leading-relaxed font-normal">
                  Coordina a partir de un resumen con productos, cantidades, horario y total. Tu cliente puede compartirlo contigo por WhatsApp.
                </p>
              </div>

              <div className="mt-6 min-h-12 flex items-center gap-2 text-xs font-semibold text-emerald-400 bg-emerald-500/10 border border-emerald-500/25 p-2.5 rounded-[8px]">
                <Check className="w-4 h-4 text-emerald-400 shrink-0" /> Despachos coordinados sin enredos
              </div>
            </Card>
          </MotionFade>
        </div>
      </Container>
    </section>
  );
}
