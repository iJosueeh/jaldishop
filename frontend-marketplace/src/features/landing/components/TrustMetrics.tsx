import React from 'react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { ShieldCheck, Clock, CreditCard, Sparkles } from 'lucide-react';

export function TrustMetrics() {
  const metrics = [
    {
      icon: <ShieldCheck className="w-5 h-5 text-[#34d399]" />,
      value: 'Tus',
      unit: 'límites',
      label: 'Horarios bajo control',
      sublabel: 'Decide cuántos pedidos aceptar por día o por horario. Al completarse, ese horario deja de estar disponible.',
      accentColor: 'from-[#34d399]/15 to-transparent',
      hoverBorder: 'hover:border-[#34d399]/40',
      iconBg: 'bg-[#34d399]/10 border-[#34d399]/25',
      badgeColor: 'text-[#34d399]',
    },
    {
      icon: <Clock className="w-5 h-5 text-[#feae2c]" />,
      value: '10 min',
      unit: 'de reserva',
      label: 'Tiempo para iniciar el pago',
      sublabel: 'Tu cliente conserva su horario durante 10 minutos para iniciar el pago con tranquilidad.',
      accentColor: 'from-[#feae2c]/15 to-transparent',
      hoverBorder: 'hover:border-[#feae2c]/40',
      iconBg: 'bg-[#feae2c]/10 border-[#feae2c]/25',
      badgeColor: 'text-[#feae2c]',
    },
    {
      icon: <CreditCard className="w-5 h-5 text-[#38bdf8]" />,
      value: 'Pagos',
      unit: 'a tu manera',
      label: 'Mercado Pago + Yape / Plin',
      sublabel: 'Cobros verificados con tarjetas Visa/Mastercard y billeteras móviles peruanas.',
      accentColor: 'from-[#38bdf8]/15 to-transparent',
      hoverBorder: 'hover:border-[#38bdf8]/40',
      iconBg: 'bg-[#38bdf8]/10 border-[#38bdf8]/25',
      badgeColor: 'text-[#38bdf8]',
    },
    {
      icon: <Sparkles className="w-5 h-5 text-[#fb923c]" />,
      value: 'Más',
      unit: 'orden',
      label: 'Organiza tus entregas',
      sublabel: 'Consulta los horarios elegidos por tus clientes para planificar la preparación y entrega de sus pedidos.',
      accentColor: 'from-[#fb923c]/15 to-transparent',
      hoverBorder: 'hover:border-[#fb923c]/40',
      iconBg: 'bg-[#fb923c]/10 border-[#fb923c]/25',
      badgeColor: 'text-[#fb923c]',
    },
  ];

  return (
    <section className="py-16 sm:py-20 bg-[#090b0c] text-white relative">
      {/* Subtle Ambient Background Warm & Cool Glows */}
      <div className="absolute top-0 right-0 w-80 h-80 bg-[#feae2c]/8 blur-3xl rounded-full pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-80 h-80 bg-[#34d399]/8 blur-3xl rounded-full pointer-events-none" />

      <Container size="lg">
        {/* Subtle Section Tag */}
        <div className="flex items-center justify-center mb-10">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-[#121618] border border-[#232b2f] text-xs font-medium text-stone-300 backdrop-blur-md">
            <span className="w-2 h-2 rounded-full bg-[#feae2c] animate-pulse" />
            <span>Pensado para panaderías, reposterías y negocios bajo pedido</span>
          </div>
        </div>

        {/* Multi-color balanced metrics grid with 12px cards as per DESIGN.md */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
          {metrics.map((metric, idx) => (
            <MotionFade
              key={idx}
              delay={idx * 0.1}
              direction="up"
              className={`group relative p-6 rounded-[12px] bg-[#121618] border border-[#1e2426] ${metric.hoverBorder} transition-all duration-300 flex flex-col justify-between hover:-translate-y-1 hover:shadow-xl overflow-hidden`}
            >
              {/* Subtle individual gradient hover glow */}
              <div
                className={`absolute inset-0 bg-gradient-to-br ${metric.accentColor} rounded-[12px] opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none`}
              />

              <div className="relative z-10 space-y-3.5">
                <div className={`inline-flex items-center justify-center w-11 h-11 rounded-[10px] ${metric.iconBg} border shadow-inner group-hover:scale-105 transition-transform duration-300`}>
                  {metric.icon}
                </div>
                <div className="flex items-baseline gap-2">
                  <span className="font-display text-2xl sm:text-3xl font-semibold tracking-tight text-white">
                    {metric.value}
                  </span>
                  <span className={`text-[11px] font-semibold uppercase tracking-wider ${metric.badgeColor}`}>
                    {metric.unit}
                  </span>
                </div>
                <div className="text-sm font-semibold text-white leading-snug">
                  {metric.label}
                </div>
              </div>

              <div className="relative z-10 text-xs text-[#a1a1aa] mt-4 pt-3.5 border-t border-[#1e2426] leading-relaxed">
                {metric.sublabel}
              </div>
            </MotionFade>
          ))}
        </div>
      </Container>
    </section>
  );
}
