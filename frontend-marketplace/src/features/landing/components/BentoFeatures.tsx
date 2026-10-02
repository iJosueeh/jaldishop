import React from 'react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { Card } from '@/shared/components/ui/Card';
import { Clock, MessageCircle, BarChart3, Smartphone, Check, Zap } from 'lucide-react';
import { Badge } from '@/shared/components/ui/Badge';

export function BentoFeatures() {
  return (
    <section id="como-funciona" className="py-24 bg-gradient-to-b from-[#faf7f2] via-[#f7f3ec] to-[#faf7f2] relative">
      <Container size="lg">
        {/* Section Header */}
        <div className="text-center max-w-2xl mx-auto space-y-4 mb-16">
          <Badge variant="jade" size="md">
            Pilares Tecnológicos del Dominio
          </Badge>
          <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-[#1c1917]">
            Las 4 superpotencias que protegen tu operación diaria
          </h2>
          <p className="text-base text-[#57534e]">
            A diferencia de un e-commerce genérico, JaldiShop está diseñado desde la raíz para la realidad de las MYPE que preparan bajo pedido.
          </p>
        </div>

        {/* Bento Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Bento Item 1: Large Featured Card - Capacidad Dinámica */}
          <MotionFade delay={0.1} className="md:col-span-2">
            <Card className="p-8 h-full bg-white border-2 border-[#e7e0d6] shadow-sm flex flex-col justify-between group hover:border-[#005141] transition-all">
              <div className="space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-[#f0fdfa] text-[#005141] border border-[#ccfbf1] flex items-center justify-center">
                  <Clock className="w-6 h-6" />
                </div>
                <h3 className="text-2xl font-extrabold text-[#1c1917]">
                  Motor de Capacidad Operativa por Franjas Horarias
                </h3>
                <p className="text-sm text-[#57534e] leading-relaxed max-w-xl font-medium">
                  Configura cuántos pedidos o productos puede procesar tu cocina por bloque de tiempo. Cuando se llena el cupo, esa hora queda deshabilitada automáticamente, evitando saturaciones y retrasos.
                </p>
              </div>

              {/* Visual preview */}
              <div className="mt-8 p-4 rounded-2xl bg-[#faf7f2] border-2 border-[#e7e0d6] grid grid-cols-3 gap-3.5">
                <div className="p-3.5 bg-white rounded-2xl text-center shadow-xs border-2 border-[#99f6e4]">
                  <div className="text-xs font-bold text-[#57534e]">10:00 AM</div>
                  <div className="text-sm font-extrabold text-[#005141] mt-0.5">4 cupos</div>
                </div>
                <div className="p-3.5 bg-white rounded-2xl text-center shadow-xs border-2 border-[#fed7aa]">
                  <div className="text-xs font-bold text-[#57534e]">11:00 AM</div>
                  <div className="text-sm font-extrabold text-[#ea580c] mt-0.5">1 cupo (Último)</div>
                </div>
                <div className="p-3.5 bg-[#f7f3ec] rounded-2xl text-center opacity-70 border-2 border-[#e7e0d6]">
                  <div className="text-xs font-bold text-[#78716c]">12:00 PM</div>
                  <div className="text-sm font-extrabold text-[#b91c1c] mt-0.5">Agotado</div>
                </div>
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 2: Hold Transaccional 10m */}
          <MotionFade delay={0.2} className="md:col-span-1">
            <Card className="p-8 h-full bg-[#005141] text-white shadow-xl shadow-[#005141]/20 flex flex-col justify-between border-2 border-[#166a57]">
              <div className="space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-white/15 text-white flex items-center justify-center backdrop-blur-sm">
                  <Zap className="w-6 h-6 text-[#feae2c]" />
                </div>
                <h3 className="text-2xl font-extrabold">
                  Hold Temporal de 10 Minutos
                </h3>
                <p className="text-sm text-[#ccfbf1] leading-relaxed font-normal">
                  Bloquea transaccionalmente el cupo en base de datos mientras el comprador realiza la transferencia o pago. Cero doble reserva.
                </p>
              </div>

              <div className="mt-8 pt-4 border-t border-white/20 flex items-center justify-between text-xs text-[#ccfbf1]">
                <span className="font-bold">Reserva atómica</span>
                <span className="font-mono font-extrabold bg-white/20 px-3 py-1 rounded-xl text-[#feae2c]">
                  10:00 min
                </span>
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 3: WhatsApp Bridge */}
          <MotionFade delay={0.3} className="md:col-span-1">
            <Card className="p-8 h-full bg-white border-2 border-[#e7e0d6] shadow-sm flex flex-col justify-between hover:border-[#005141] transition-all">
              <div className="space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-[#f0fdfa] text-[#005141] border border-[#ccfbf1] flex items-center justify-center">
                  <MessageCircle className="w-6 h-6" />
                </div>
                <h3 className="text-xl font-extrabold text-[#1c1917]">
                  WhatsApp Order Bridge
                </h3>
                <p className="text-sm text-[#57534e] leading-relaxed font-medium">
                  Genera el mensaje del pedido formateado y validado directamente al WhatsApp del comercio para resolver dudas o compartir comprobantes al instante.
                </p>
              </div>

              <div className="mt-6 flex items-center gap-2 text-xs font-bold text-[#005141] bg-[#f0fdfa] p-2.5 rounded-xl border border-[#ccfbf1]">
                <Check className="w-4 h-4 text-[#005141]" /> Integración nativa con WhatsApp
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 4: Mobile & Next.js 15 App Router */}
          <MotionFade delay={0.4} className="md:col-span-1">
            <Card className="p-8 h-full bg-white border-2 border-[#e7e0d6] shadow-sm flex flex-col justify-between hover:border-[#005141] transition-all">
              <div className="space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-[#fff7ed] text-[#ea580c] border border-[#fed7aa] flex items-center justify-center">
                  <Smartphone className="w-6 h-6" />
                </div>
                <h3 className="text-xl font-extrabold text-[#1c1917]">
                  Experiencia Mobile Instantánea
                </h3>
                <p className="text-sm text-[#57534e] leading-relaxed font-medium">
                  Diseñado para abrir a toda velocidad desde un enlace en bio de Instagram o estados de WhatsApp, sin lag ni tiempos de espera.
                </p>
              </div>

              <div className="mt-6 flex items-center gap-2 text-xs font-bold text-[#ea580c] bg-[#fff7ed] p-2.5 rounded-xl border border-[#fed7aa]">
                <Check className="w-4 h-4 text-[#ea580c]" /> Core Web Vitals optimizados
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 5: Panel MYPE Angular */}
          <MotionFade delay={0.5} className="md:col-span-1">
            <Card className="p-8 h-full bg-white border-2 border-[#e7e0d6] shadow-sm flex flex-col justify-between hover:border-[#005141] transition-all">
              <div className="space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-[#fef3c7] text-[#92400e] border border-[#fde68a] flex items-center justify-center">
                  <BarChart3 className="w-6 h-6" />
                </div>
                <h3 className="text-xl font-extrabold text-[#1c1917]">
                  Sincronización con Panel MYPE
                </h3>
                <p className="text-sm text-[#57534e] leading-relaxed font-medium">
                  Tu cocina recibe los pedidos en tiempo real en la app de comerciante con notificaciones y control de estados de preparación.
                </p>
              </div>

              <div className="mt-6 flex items-center gap-2 text-xs font-bold text-[#92400e] bg-[#fef3c7] p-2.5 rounded-xl border border-[#fde68a]">
                <Check className="w-4 h-4 text-[#92400e]" /> Angular 20 + Signals Reactivos
              </div>
            </Card>
          </MotionFade>
        </div>
      </Container>
    </section>
  );
}
