import React from 'react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { ShieldCheck, Zap, Users, Award } from 'lucide-react';

export function TrustMetrics() {
  const metrics = [
    {
      icon: <ShieldCheck className="w-5 h-5 text-[#34d399]" />,
      value: '0',
      label: 'Saturación o retrasos',
      sublabel: 'Tus horarios se cierran al llenarse el cupo',
    },
    {
      icon: <Zap className="w-5 h-5 text-[#feae2c]" />,
      value: '10 min',
      label: 'Para pagar por pedido',
      sublabel: 'El cupo se guarda mientras hacen la transferencia',
    },
    {
      icon: <Award className="w-5 h-5 text-[#38bdf8]" />,
      value: '100%',
      label: 'Entregas a tiempo',
      sublabel: 'Producción y despacho según tu capacidad real',
    },
    {
      icon: <Users className="w-5 h-5 text-[#fb923c]" />,
      value: '3x',
      label: 'Más ventas por chat',
      sublabel: 'Tus clientes piden directo sin mensajes eternos',
    },
  ];

  return (
    <section className="py-16 bg-[#0a0a09] text-white relative overflow-hidden">
      {/* Decorative Warm Ambient Glows */}
      <div className="absolute top-0 right-0 w-[400px] h-[400px] bg-[#005141]/20 blur-[140px] pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-[400px] h-[400px] bg-[#feae2c]/10 blur-[140px] pointer-events-none" />

      <Container size="lg">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
          {metrics.map((metric, idx) => (
            <MotionFade key={idx} delay={idx * 0.1} direction="up" className="text-center sm:text-left space-y-2">
              <div className="inline-flex items-center justify-center w-11 h-11 rounded-2xl bg-white/5 mb-1 backdrop-blur-sm">
                {metric.icon}
              </div>
              <div className="font-display text-3xl sm:text-4xl font-extrabold tracking-tight text-white">
                {metric.value}
              </div>
              <div className="text-sm font-bold text-stone-200">
                {metric.label}
              </div>
              <div className="text-xs text-stone-400">
                {metric.sublabel}
              </div>
            </MotionFade>
          ))}
        </div>
      </Container>
    </section>
  );
}





