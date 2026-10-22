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
      <div className="bg-[#0f1416]/95 backdrop-blur-md p-4 rounded-[10px] border border-emerald-500/30 text-white shadow-xl min-h-36 flex flex-col justify-between gap-3">
        <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
          <span className="font-semibold flex items-center gap-1.5 text-emerald-400">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            Cupos del negocio
          </span>
          <span className="text-xs font-mono bg-white/10 px-2 py-0.5 rounded border border-white/10 text-stone-200">
            10:00 - 11:00 AM
          </span>
        </div>
        <div className="w-full bg-white/10 h-1.5 rounded-full overflow-hidden">
          <div className="bg-emerald-400 h-full w-[80%] rounded-full" />
        </div>
        <div className="flex flex-wrap items-center justify-between gap-2 text-xs font-mono text-stone-300">
          <span>12/15 pedidos</span>
          <span className="text-emerald-400 font-bold">¡3 cupos libres!</span>
        </div>
      </div>
    ),
  },
  {
    number: '02',
    badge: 'Cupo reservado 10 min',
    badgeVariant: 'amber',
    title: 'Tus clientes eligen su horario',
    description:
      'Al empezar la compra, tu cliente elige un horario disponible. Su cupo queda reservado durante 10 minutos para iniciar el pago con tranquilidad.',
    imageUrl:
      'https://images.unsplash.com/photo-1578985545062-69928b1d9587?q=80&w=900&auto=format&fit=crop',
    imageAlt: 'Pastelería y repostería artesanal en empaque fino',
    hudContent: (
      <div className="bg-[#14120e]/95 backdrop-blur-md p-4 rounded-[10px] border border-[#feae2c]/35 text-white shadow-xl min-h-36 flex flex-col justify-between gap-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-[8px] bg-[#feae2c]/15 border border-[#feae2c]/30 flex items-center justify-center text-[#feae2c]">
              <Clock className="w-4 h-4 animate-spin-slow" />
            </div>
            <div>
              <div className="text-[10px] font-bold text-[#feae2c] uppercase tracking-wider">Cupo apartado</div>
              <div className="text-sm font-mono font-bold text-white">09:59 restantes</div>
            </div>
          </div>
          <span className="text-xs bg-[#feae2c]/15 text-[#feae2c] px-2.5 py-0.5 rounded-full font-semibold border border-[#feae2c]/30">
            Reservado
          </span>
        </div>
        {/* Payment badges pill */}
        <div className="pt-2 border-t border-white/10 flex flex-wrap items-center justify-between gap-2 text-xs font-medium text-stone-300">
          <span className="flex items-center gap-1 text-white">
            <CreditCard className="w-3.5 h-3.5 text-[#38bdf8]" /> <span className="text-[#38bdf8] font-semibold">Mercado Pago</span>
          </span>
          <span className="text-[#c084fc] font-mono text-xs font-bold bg-[#c084fc]/10 px-2 py-0.5 rounded border border-[#c084fc]/20">Yape / Plin</span>
        </div>
      </div>
    ),
  },
  {
    number: '03',
    badge: 'Pedido claro por chat',
    badgeVariant: 'terracotta',
    title: 'Coordina con un resumen del pedido',
    description:
      'Tu cliente puede compartir por WhatsApp un resumen con los productos, cantidades y horario elegido para coordinar la entrega contigo.',
    imageUrl:
      'https://images.unsplash.com/photo-1556910103-1c02745aae4d?q=80&w=900&auto=format&fit=crop',
    imageAlt: 'Mesa de trabajo y pedidos empaquetados ordenadamente',
    hudContent: (
      <div className="bg-[#0e1613]/95 backdrop-blur-md p-4 rounded-[10px] border border-emerald-500/30 text-white shadow-xl min-h-36 flex flex-col justify-between gap-3">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-[8px] bg-emerald-500/15 border border-emerald-500/30 text-emerald-400 flex items-center justify-center">
              <MessageSquare className="w-4 h-4" />
            </div>
            <div>
              <div className="text-sm font-semibold text-white">Resumen para WhatsApp</div>
              <div className="text-xs text-stone-400">Pedido #ORD-104</div>
            </div>
          </div>
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
        </div>
        <p className="text-xs text-stone-200 leading-relaxed bg-white/5 p-2.5 rounded-[8px] border border-white/10">
          2 × Pan artesanal · Recojo 10:00 – 11:00 AM
        </p>
      </div>
    ),
  },
];

export function HowItWorksSection() {
  return (
    <section id="como-funciona" className="py-24 bg-[#090b0c] text-white relative scroll-mt-20 sm:scroll-mt-24">
      {/* Single Upper Ambient Glow (The lower seam is unified with ComparisonSection) */}
      <div className="absolute top-0 right-1/4 w-[500px] h-[500px] bg-[#34d399]/8 blur-3xl rounded-full pointer-events-none" />

      <Container size="lg" className="relative z-10 space-y-16">
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto space-y-4">
          <MotionFade delay={0.1}>
            <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-[#121618] text-[#feae2c] text-xs font-semibold backdrop-blur-md border border-[#232b2f] shadow-sm">
              <Sparkles className="w-3.5 h-3.5 text-[#feae2c]" />
              Hecho para negocios bajo pedido
            </div>
          </MotionFade>

          <MotionFade delay={0.2}>
            <h2 className="font-display text-3xl sm:text-4xl lg:text-5xl font-light tracking-tight text-white leading-tight">
              Del catálogo a la entrega, en tres pasos.
            </h2>
          </MotionFade>

          <MotionFade delay={0.3}>
            <p className="text-base sm:text-lg text-[#a1a1aa] leading-relaxed max-w-xl mx-auto">
              Comparte tu catálogo digital, ofrece horarios disponibles y organiza los pedidos de tu negocio.
            </p>
          </MotionFade>
        </div>

        {/* 3 Step Cards with 12px borders as per DESIGN.md */}
        <ol aria-label="Pasos para organizar tus pedidos" className="grid grid-cols-1 lg:grid-cols-3 gap-6 sm:gap-8">
          {STEPS.map((step, idx) => (
            <li key={step.number} className="min-w-0">
              <MotionFade delay={0.2 + idx * 0.15} className="h-full">
                <div className="group relative bg-[#121618] rounded-[12px] border border-[#1e2426] hover:border-stone-500 transition-all duration-300 overflow-hidden flex flex-col h-full shadow-lg hover:-translate-y-1">
                  {/* Visual Image Header */}
                  <div className="relative h-72 sm:h-80 lg:h-72 w-full shrink-0 overflow-hidden bg-stone-900">
                    <Image
                      src={step.imageUrl}
                      alt={step.imageAlt}
                      fill
                      sizes="(max-width: 1023px) 100vw, 33vw"
                      className="object-cover transition-transform duration-700 ease-out group-hover:scale-105 filter brightness-[0.92] group-hover:brightness-100"
                    />
                    {/* Dark gradient overlay */}
                    <div className="absolute inset-0 bg-gradient-to-t from-[#121618] via-[#121618]/40 to-transparent" />

                    {/* Top Step Number Pill */}
                    <div className="absolute top-4 left-4 right-4 z-10 flex items-center gap-2">
                      <span className="px-3 h-8 shrink-0 rounded-full bg-black/75 backdrop-blur-md text-stone-300 font-mono font-medium text-xs flex items-center justify-center border border-white/10 shadow-md">
                        Paso {step.number}
                      </span>
                      <Badge variant={step.badgeVariant} size="sm" className="font-semibold shadow-md">
                        {step.badge}
                      </Badge>
                    </div>

                    {/* Floating HUD Card at Bottom of Image */}
                    <div className="absolute bottom-4 left-4 right-4 z-10 transform transition-transform duration-300 group-hover:-translate-y-1">
                      {step.hudContent}
                    </div>
                  </div>

                  {/* Card Content Text */}
                  <div className="p-6 sm:p-7 flex-1 flex flex-col justify-between space-y-4">
                    <div className="space-y-2">
                      <h3 className="text-xl font-semibold text-white group-hover:text-[#feae2c] transition-colors leading-snug">
                        {step.title}
                      </h3>
                      <p className="text-sm text-[#a1a1aa] leading-relaxed">
                        {step.description}
                      </p>
                    </div>

                    <div className="pt-4 border-t border-[#1e2426] flex items-center gap-2 text-xs font-medium text-stone-400 group-hover:text-white transition-colors">
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
