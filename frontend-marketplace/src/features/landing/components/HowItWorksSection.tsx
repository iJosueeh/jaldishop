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
    badge: 'Tú defines los cupos',
    badgeVariant: 'jade',
    title: 'Decide cuántos pedidos aceptar',
    description:
      'Define cuántos pedidos puedes preparar por día o por horario. Al completarse los cupos, ese horario deja de estar disponible para nuevas compras.',
    imageUrl:
      'https://images.unsplash.com/photo-1509440159596-0249088772ff?q=80&w=900&auto=format&fit=crop',
    imageAlt: 'Hogazas de pan artesanal saliendo del horno',
    hudContent: (
      <div className="bg-[#141413]/95 backdrop-blur-md p-4 rounded-2xl border border-white/25 text-white shadow-xl min-h-36 flex flex-col justify-between gap-3">
        <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
          <span className="font-extrabold flex items-center gap-1.5 text-emerald-400">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            Cupos del negocio
          </span>
          <span className="text-xs font-mono bg-white/10 px-2 py-1 rounded-md text-stone-200">
            10:00 - 11:00 AM
          </span>
        </div>
        <div className="w-full bg-white/10 h-1.5 rounded-full overflow-hidden">
          <div className="bg-emerald-400 h-full w-[80%] rounded-full" />
        </div>
        <div className="flex flex-wrap items-center justify-between gap-2 text-xs font-mono text-stone-200">
          <span>12/15 pedidos</span>
          <span className="text-emerald-400 font-bold">¡3 cupos libres!</span>
        </div>
      </div>
    ),
  },
  {
    number: '02',
    badge: 'Cupo reservado',
    badgeVariant: 'amber',
    title: 'Tus clientes eligen su horario',
    description:
      'Al empezar la compra, tu cliente elige un horario disponible. Su cupo queda reservado durante 10 minutos para iniciar el pago.',
    imageUrl:
      'https://images.unsplash.com/photo-1578985545062-69928b1d9587?q=80&w=900&auto=format&fit=crop',
    imageAlt: 'Pastelería y repostería artesanal en empaque fino',
    hudContent: (
      <div className="bg-[#141413]/95 backdrop-blur-md p-4 rounded-2xl border border-white/25 text-white shadow-xl min-h-36 flex flex-col justify-between gap-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-[#feae2c]/20 flex items-center justify-center text-[#feae2c]">
              <Clock className="w-4 h-4 animate-spin-slow" />
            </div>
            <div>
              <div className="text-xs font-extrabold text-[#feae2c] uppercase">Cupo apartado</div>
              <div className="text-sm font-mono font-black text-white">09:59 restantes</div>
            </div>
          </div>
          <span className="text-xs bg-emerald-500/20 text-emerald-300 px-2 py-1 rounded-full font-bold border border-emerald-500/30">
            Reservado
          </span>
        </div>
        {/* Payment badges pill */}
        <div className="pt-2 border-t border-white/10 flex flex-wrap items-center justify-between gap-2 text-xs font-bold text-stone-200">
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
    badge: 'Pedido claro',
    badgeVariant: 'terracotta',
    title: 'Coordina con un resumen del pedido',
    description:
      'Tu cliente puede compartir por WhatsApp un resumen con los productos, cantidades y horario elegido para coordinar contigo.',
    imageUrl:
      'https://images.unsplash.com/photo-1556910103-1c02745aae4d?q=80&w=900&auto=format&fit=crop',
    imageAlt: 'Mesa de trabajo y pedidos empaquetados ordenadamente',
    hudContent: (
      <div className="bg-[#141413]/95 backdrop-blur-md p-4 rounded-2xl border border-white/25 text-white shadow-xl min-h-36 flex flex-col justify-between gap-3">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-[#005141] text-white flex items-center justify-center">
              <MessageSquare className="w-4 h-4" />
            </div>
            <div>
              <div className="text-sm font-black text-white">Resumen para WhatsApp</div>
              <div className="text-xs text-stone-300">Pedido #ORD-104</div>
            </div>
          </div>
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
        </div>
        <p className="text-xs text-stone-200 leading-relaxed bg-white/5 p-2.5 rounded-xl border border-white/10">
          2 × Pan artesanal · Recojo 10:00 – 11:00 AM
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
              Hecho para negocios bajo pedido
            </div>
          </MotionFade>

          <MotionFade delay={0.2}>
            <h2 className="font-display text-3xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-white leading-tight">
              Del catálogo a la entrega, en tres pasos.
            </h2>
          </MotionFade>

          <MotionFade delay={0.3}>
            <p className="text-base sm:text-lg text-stone-300 leading-relaxed">
              Comparte tu catálogo digital, ofrece horarios disponibles y organiza los pedidos de tu negocio.
            </p>
          </MotionFade>
        </div>

        {/* 3 Editorial Visual Cards with Hover Effects */}
        <ol aria-label="Pasos para organizar tus pedidos" className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {STEPS.map((step, idx) => (
            <li key={step.number} className="min-w-0">
            <MotionFade delay={0.2 + idx * 0.15} className="h-full">
              <div className="group relative bg-[#141413] rounded-[32px] border border-white/10 hover:border-emerald-500/40 transition-all duration-500 overflow-hidden flex flex-col h-full shadow-2xl hover:shadow-[0_20px_50px_rgba(0,81,65,0.25)] hover:-translate-y-2">
                {/* Visual Image Header with Rich Hover Zoom & Glass Shine */}
                <div className="relative h-72 sm:h-80 lg:h-72 w-full shrink-0 overflow-hidden bg-stone-900">
                  <Image
                    src={step.imageUrl}
                    alt={step.imageAlt}
                    fill
                    sizes="(max-width: 1023px) 100vw, 33vw"
                    className="object-cover transition-transform duration-700 ease-out group-hover:scale-110 filter brightness-[0.92] group-hover:brightness-100"
                  />
                  {/* Subtle Gradient Overlay */}
                  <div className="absolute inset-0 bg-gradient-to-t from-[#141413] via-[#141413]/40 to-transparent transition-opacity duration-500" />

                  {/* Diagonal Glass Shine sweep on hover */}
                  <div className="absolute inset-0 -translate-x-full group-hover:translate-x-full transition-transform duration-1000 bg-gradient-to-r from-transparent via-white/20 to-transparent pointer-events-none z-10" />

                  {/* Top Step Number Pill */}
                  <div className="absolute top-4 left-4 right-4 z-10 flex items-center gap-2">
                    <span className="px-3 h-9 shrink-0 rounded-full bg-black/50 backdrop-blur-md text-white font-mono font-black text-xs flex items-center justify-center border border-white/25 shadow-md">
                      Paso {step.number}
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
                    <h3 className="text-xl lg:min-h-14 font-bold text-white group-hover:text-emerald-300 transition-colors leading-snug">
                      {step.title}
                    </h3>
                    <p className="text-sm text-stone-300 leading-relaxed">
                      {step.description}
                    </p>
                  </div>

                  <div className="pt-4 border-t border-white/10 flex items-center gap-2 text-xs font-bold text-stone-400 group-hover:text-white transition-colors">
                    <ShieldCheck className="w-4 h-4 text-emerald-400" />
                    <span>Pedidos según tus cupos</span>
                  </div>
                </div>
              </div>
            </MotionFade>
            </li>
          ))}
        </ol>
      </Container>
    </section>
  );
}
