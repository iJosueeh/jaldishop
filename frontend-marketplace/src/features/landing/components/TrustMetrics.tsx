import React from 'react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { ShieldCheck, Zap, Users, Award } from 'lucide-react';

export function TrustMetrics() {
  const metrics = [
    {
      icon: <ShieldCheck className="w-5 h-5 text-[#99f6e4]" />,
      value: '0%',
      label: 'Sobreventa',
      sublabel: 'Bloqueo automático al agotar cupos',
    },
    {
      icon: <Zap className="w-5 h-5 text-[#feae2c]" />,
      value: '10 min',
      label: 'Hold Temporal',
      sublabel: 'Garantía de stock mientras pagan',
    },
    {
      icon: <Award className="w-5 h-5 text-[#ccfbf1]" />,
      value: '99.8%',
      label: 'Puntualidad en Entregas',
      sublabel: 'Gracias a franjas horarias exactas',
    },
    {
      icon: <Users className="w-5 h-5 text-[#fed7aa]" />,
      value: '3x',
      label: 'Más Conversión',
      sublabel: 'Frente a pedidos por chat desordenado',
    },
  ];

  return (
    <section className="py-16 bg-[#005141] text-white border-y border-[#003d31] relative overflow-hidden">
      {/* Decorative Warm Ambient Glows */}
      <div className="absolute top-0 right-0 w-[400px] h-[400px] bg-[#feae2c]/10 blur-[120px] pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-[400px] h-[400px] bg-[#ea580c]/10 blur-[120px] pointer-events-none" />

      <Container size="lg">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
          {metrics.map((metric, idx) => (
            <MotionFade key={idx} delay={idx * 0.1} direction="up" className="text-center sm:text-left space-y-2">
              <div className="inline-flex items-center justify-center w-11 h-11 rounded-2xl bg-white/10 border border-white/15 mb-1 backdrop-blur-sm">
                {metric.icon}
              </div>
              <div className="text-3xl sm:text-4xl font-extrabold tracking-tight text-white font-mono">
                {metric.value}
              </div>
              <div className="text-sm font-bold text-[#ccfbf1]">
                {metric.label}
              </div>
              <div className="text-xs text-white/70">
                {metric.sublabel}
              </div>
            </MotionFade>
          ))}
        </div>
      </Container>
    </section>
  );
}
