import React from 'react';
import Image from 'next/image';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { ShieldCheck, Clock, CreditCard, MessageSquare, Sparkles, CheckCircle2 } from 'lucide-react';
import { Badge } from '@/shared/components/ui/Badge';

interface StepCard {
  number: string;
  badge: string;
  badgeVariant: 'jade' | 'terracotta' | 'amber';
  title: string;
  description: string;
  imageUrl: string;
  imageAlt: string;
  hudContent: React.ReactNode;
}

const STEPS: StepCard[] = [
  {
    number: '01',
    badge: 'Capacidad Inteligente',
    badgeVariant: 'jade',
    title: 'Configura tu límite real de cocina',
    description:
      'Define cuántos pedidos puede atender tu taller por día o franja horaria. JaldiShop bloquea automáticamente cuando alcanzas el 100%, eliminando la sobreventa.',
    imageUrl:
      'https://images.unsplash.com/photo-1509440159596-0249088772ff?q=80&w=900&auto=format&fit=crop',
    imageAlt: 'Hogazas de pan artesanal saliendo del horno',
    hudContent: (
      <div className="bg-[#141413]/90 backdrop-blur-md p-3.5 rounded-2xl border border-white/20 text-white shadow-xl space-y-2">
        <div className="flex items-center justify-between text-xs">
          <span className="font-extrabold flex items-center gap-1.5 text-emerald-400">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            Cocina & Taller Activo
          </span>
          <span className="text-[10px] font-mono bg-white/10 px-2 py-0.5 rounded-md text-stone-300">
            10:00 - 11:00 AM
          </span>
        </div>
        <div className="w-full bg-white/10 h-1.5 rounded-full overflow-hidden">
          <div className="bg-emerald-400 h-full w-[80%] rounded-full" />
        </div>
        <div className="flex items-center justify-between text-[11px] font-mono text-stone-300">
          <span>12/15 pedidos</span>
          <span className="text-emerald-400 font-bold">¡3 cupos libres!</span>
        </div>
      </div>
    ),
  },
  {
    number: '02',
    badge: 'Hold 10 min & Pasarela',
    badgeVariant: 'amber',
    title: 'Tus clientes reservan y pagan protegidos',
    description:
      'Al iniciar el checkout, tu cliente cuenta con 10 minutos de reserva garantizada para pagar con Mercado Pago, tarjetas de crédito/débito o billeteras Yape y Plin.',
    imageUrl:
      'https://images.unsplash.com/photo-1578985545062-69928b1d9587?q=80&w=900&auto=format&fit=crop',
    imageAlt: 'Pastelería y repostería artesanal en empaque fino',
    hudContent: (
      <div className="bg-[#141413]/90 backdrop-blur-md p-3.5 rounded-2xl border border-white/20 text-white shadow-xl space-y-2.5">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-[#feae2c]/20 flex items-center justify-center text-[#feae2c]">
              <Clock className="w-4 h-4 animate-spin-slow" />
            </div>
            <div>
              <div className="text-[10px] font-extrabold text-[#feae2c] uppercase">Cupo apartado</div>
              <div className="text-xs font-mono font-black text-white">09:59 restantes</div>
            </div>
          </div>
          <span className="text-[10px] bg-emerald-500/20 text-emerald-300 px-2 py-0.5 rounded-full font-bold border border-emerald-500/30">
            Blindado
          </span>
        </div>
        {/* Payment badges pill */}
        <div className="pt-1.5 border-t border-white/10 flex items-center justify-between text-[10px] font-bold text-stone-300">
          <span className="flex items-center gap-1">
            <CreditCard className="w-3 h-3 text-[#38bdf8]" /> Mercado Pago & Tarjetas
          </span>
          <span className="text-purple-300 font-mono">Yape / Plin</span>
        </div>
      </div>
    ),
  },
  {
    number: '03',
    badge: 'Despacho Impecable',
    badgeVariant: 'terracotta',
    title: 'Órdenes ordenadas directo a tu WhatsApp',
    description:
      'Recibe el pedido estructurado con número de orden, comprobante verificado y hora de entrega. Tu equipo cocina en paz y tu cliente recibe puntual.',
    imageUrl:
      'https://images.unsplash.com/photo-1556910103-1c02745aae4d?q=80&w=900&auto=format&fit=crop',
    imageAlt: 'Mesa de trabajo y pedidos empaquetados ordenadamente',
    hudContent: (
      <div className="bg-[#141413]/90 backdrop-blur-md p-3.5 rounded-2xl border border-white/20 text-white shadow-xl space-y-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-[#005141] text-white flex items-center justify-center">
              <MessageSquare className="w-4 h-4" />
            </div>
            <div>
              <div className="text-xs font-black text-white">WhatsApp Directo ⚡</div>
              <div className="text-[10px] text-stone-400">Orden #ORD-104 Confirmada</div>
            </div>
          </div>
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
        </div>
        <p className="text-[10px] text-stone-300 leading-tight bg-white/5 p-2 rounded-xl border border-white/10">
          “2x Pedido artesanal • Franja 10:00 AM • Pagado con Mercado Pago / Yape”
        </p>
      </div>
    ),
  },
];

export function HowItWorksSection() {
  return (
    <section id="como-funciona" className="py-24 bg-[#0a0a09] text-white relative overflow-hidden scroll-mt-20 sm:scroll-mt-24">
      {/* Subtle Warm Background Glows */}
      <div className="absolute top-0 right-1/4 w-96 h-96 bg-[#005141]/20 blur-3xl rounded-full pointer-events-none" />
      <div className="absolute bottom-0 left-1/4 w-96 h-96 bg-[#ea580c]/15 blur-3xl rounded-full pointer-events-none" />

      <Container size="lg" className="relative z-10 space-y-16">
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto space-y-4">
          <MotionFade delay={0.1}>
            <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-white/10 text-emerald-400 text-xs font-bold backdrop-blur-md border border-white/10 shadow-sm">
              <Sparkles className="w-3.5 h-3.5 text-[#feae2c]" />
              Tradición Artesanal + Tecnología Inteligente
            </div>
          </MotionFade>

          <MotionFade delay={0.2}>
            <h2 className="font-display text-3xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-white leading-tight">
              De la masa al despacho en tres momentos impecables.
            </h2>
          </MotionFade>

          <MotionFade delay={0.3}>
            <p className="text-base sm:text-lg text-stone-300 leading-relaxed">
              Sin procesos complicados ni cajas de texto interminables. Así es como JaldiShop protege tu cocina y sincroniza tus ventas digitales.
            </p>
          </MotionFade>
        </div>

        {/* 3 Editorial Visual Cards with Hover Effects */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {STEPS.map((step, idx) => (
            <MotionFade key={step.number} delay={0.2 + idx * 0.15}>
              <div className="group relative bg-[#141413] rounded-[32px] border border-white/10 hover:border-emerald-500/40 transition-all duration-500 overflow-hidden flex flex-col h-full shadow-2xl hover:shadow-[0_20px_50px_rgba(0,81,65,0.25)] hover:-translate-y-2">
                {/* Visual Image Header with Rich Hover Zoom & Glass Shine */}
                <div className="relative h-64 sm:h-72 w-full overflow-hidden bg-stone-900">
                  <Image
                    src={step.imageUrl}
                    alt={step.imageAlt}
                    fill
                    sizes="(max-width: 768px) 100vw, 33vw"
                    className="object-cover transition-transform duration-700 ease-out group-hover:scale-110 filter brightness-[0.92] group-hover:brightness-100"
                  />
                  {/* Subtle Gradient Overlay */}
                  <div className="absolute inset-0 bg-gradient-to-t from-[#141413] via-[#141413]/40 to-transparent transition-opacity duration-500" />

                  {/* Diagonal Glass Shine sweep on hover */}
                  <div className="absolute inset-0 -translate-x-full group-hover:translate-x-full transition-transform duration-1000 bg-gradient-to-r from-transparent via-white/20 to-transparent pointer-events-none z-10" />

                  {/* Top Step Number Pill */}
                  <div className="absolute top-4 left-4 z-10 flex items-center gap-2">
                    <span className="w-8 h-8 rounded-full bg-white/20 backdrop-blur-md text-white font-mono font-black text-xs flex items-center justify-center border border-white/20 shadow-md">
                      {step.number}
                    </span>
                    <Badge variant={step.badgeVariant} size="sm" className="font-bold shadow-md">
                      {step.badge}
                    </Badge>
                  </div>

                  {/* Floating HUD Card at Bottom of Image */}
                  <div className="absolute bottom-4 left-4 right-4 z-10 transform transition-transform duration-300 group-hover:-translate-y-1.5">
                    {step.hudContent}
                  </div>
                </div>

                {/* Card Content Text */}
                <div className="p-6 sm:p-7 flex-1 flex flex-col justify-between space-y-4">
                  <div className="space-y-2.5">
                    <h3 className="text-xl font-bold text-white group-hover:text-emerald-300 transition-colors leading-snug">
                      {step.title}
                    </h3>
                    <p className="text-xs sm:text-sm text-stone-400 leading-relaxed">
                      {step.description}
                    </p>
                  </div>

                  <div className="pt-4 border-t border-white/10 flex items-center gap-2 text-xs font-bold text-stone-400 group-hover:text-white transition-colors">
                    <ShieldCheck className="w-4 h-4 text-emerald-400" />
                    <span>Control automático 24/7</span>
                  </div>
                </div>
              </div>
            </MotionFade>
          ))}
        </div>
      </Container>
    </section>
  );
}
