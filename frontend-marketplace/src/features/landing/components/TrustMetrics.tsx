import React from 'react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { ShieldCheck, Clock, CreditCard, Sparkles } from 'lucide-react';

export function TrustMetrics() {
  const metrics = [
    {
      icon: <ShieldCheck className="w-5 h-5 text-[#34d399]" />,
      value: '0',
      unit: 'sobreventas',
      label: 'Control de Capacidad Real',
      sublabel: 'Bloqueo automático de franjas y días cuando la capacidad llega al 100%.',
      accentColor: 'from-emerald-500/20 to-transparent',
      hoverBorder: 'group-hover:border-emerald-500/40',
    },
    {
      icon: <Clock className="w-5 h-5 text-[#feae2c]" />,
      value: '10 min',
      unit: 'de reserva',
      label: 'Hold Transaccional Seguro',
      sublabel: 'Cupo protegido en base de datos mientras el cliente realiza el pago con calma.',
      accentColor: 'from-amber-500/20 to-transparent',
      hoverBorder: 'group-hover:border-amber-500/40',
    },
    {
      icon: <CreditCard className="w-5 h-5 text-[#38bdf8]" />,
      value: 'Pasarela',
      unit: '& Billeteras',
      label: 'Mercado Pago + Yape / Plin',
      sublabel: 'Cobros verificados con tarjetas Visa/Mastercard y billeteras móviles peruanas.',
      accentColor: 'from-sky-500/20 to-transparent',
      hoverBorder: 'group-hover:border-sky-500/40',
    },
    {
      icon: <Sparkles className="w-5 h-5 text-[#fb923c]" />,
      value: '100%',
      unit: 'puntualidad',
      label: 'Despachos sin Saturación',
      sublabel: 'Tu equipo cocina en armonía y tus clientes reciben sus pedidos siempre a la hora.',
      accentColor: 'from-orange-500/20 to-transparent',
      hoverBorder: 'group-hover:border-orange-500/40',
    },
  ];

  return (
    <section className="py-16 sm:py-20 bg-[#0a0a09] text-white relative overflow-hidden">
      {/* Decorative Warm Ambient Glows */}
      <div className="absolute top-0 right-0 w-72 h-72 bg-[#005141]/20 blur-3xl rounded-full pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-72 h-72 bg-[#feae2c]/12 blur-3xl rounded-full pointer-events-none" />

      <Container size="lg">
        {/* Subtle Section Tag */}
        <div className="flex items-center justify-center mb-8">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white/10 border border-white/10 text-xs font-semibold text-stone-300 backdrop-blur-md">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>Resultados probados para panaderías, reposterías y talleres bajo pedido</span>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
          {metrics.map((metric, idx) => (
            <MotionFade
              key={idx}
              delay={idx * 0.1}
              direction="up"
              className={`group relative p-6 rounded-3xl bg-white/[0.04] hover:bg-white/[0.08] border border-white/10 ${metric.hoverBorder} transition-all duration-300 backdrop-blur-md flex flex-col justify-between hover:-translate-y-1.5 hover:shadow-2xl overflow-hidden`}
            >
              {/* Diagonal Glass Shine sweep on hover */}
              <div className="absolute inset-0 -translate-x-full group-hover:translate-x-full transition-transform duration-1000 bg-gradient-to-r from-transparent via-white/10 to-transparent pointer-events-none z-10" />

              {/* Subtle gradient hover glow */}
              <div
                className={`absolute inset-0 bg-gradient-to-br ${metric.accentColor} rounded-3xl opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none`}
              />

              <div className="relative z-10 space-y-3">
                <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-white/10 border border-white/15 shadow-inner backdrop-blur-sm group-hover:scale-105 transition-transform duration-300">
                  {metric.icon}
                </div>
                <div className="flex items-baseline gap-2">
                  <span className="font-display text-2xl sm:text-3xl font-black tracking-tight text-white">
                    {metric.value}
                  </span>
                  <span className="text-[11px] font-bold text-stone-400 uppercase tracking-wider">
                    {metric.unit}
                  </span>
                </div>
                <div className="text-sm font-bold text-stone-100 leading-snug">
                  {metric.label}
                </div>
              </div>

              <div className="relative z-10 text-xs text-stone-400 mt-4 pt-3.5 border-t border-white/10 leading-relaxed">
                {metric.sublabel}
              </div>
            </MotionFade>
          ))}
        </div>
      </Container>
    </section>
  );
}
