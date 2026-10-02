import React from 'react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { Card } from '@/shared/components/ui/Card';
import { XCircle, CheckCircle2, AlertTriangle, Sparkles } from 'lucide-react';
import { Badge } from '@/shared/components/ui/Badge';

export function ComparisonSection() {
  return (
    <section id="antes-despues" className="py-16 sm:py-24 bg-[#faf7f2] relative overflow-hidden">
      <Container size="lg">
        {/* Section Header */}
        <div className="text-center max-w-2xl mx-auto space-y-3 sm:space-y-4 mb-10 sm:mb-16">
          <Badge variant="terracotta" size="md">
            La Transformación Operativa
          </Badge>
          <h2 className="font-display text-2xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight text-[#1c1917] leading-tight sm:leading-snug">
            ¿Por qué vender por chat tradicional te está costando dinero y clientes?
          </h2>
          <p className="text-sm sm:text-base lg:text-lg text-[#57534e] max-w-xl mx-auto">
            Compara el dolor diario de coordinar pedidos manualmente frente a la tranquilidad de un sistema con capacidad inteligente.
          </p>
        </div>

        {/* Comparison Cards Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 sm:gap-8 items-stretch">
          {/* Left Card: The Chaos of WhatsApp */}
          <MotionFade delay={0.1} direction="up">
            <Card className="p-5 sm:p-7 lg:p-8 h-full bg-white shadow-md relative overflow-hidden flex flex-col justify-between rounded-3xl border-0">
              <div className="space-y-5 sm:space-y-6">
                {/* Header with responsive wrapping */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-2 border-b border-rose-50 sm:border-transparent">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center font-bold shrink-0">
                      <AlertTriangle className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="font-display text-base sm:text-lg font-bold text-[#1c1917]">Venta por Chat Desordenado</h3>
                      <p className="text-xs text-rose-600 font-semibold">El dolor tradicional de las MYPE</p>
                    </div>
                  </div>
                  <div className="self-start sm:self-auto">
                    <Badge variant="danger" size="sm">
                      Caos Operativo
                    </Badge>
                  </div>
                </div>

                <ul className="space-y-3.5 sm:space-y-4 text-xs sm:text-sm text-[#57534e]">
                  <li className="flex items-start gap-3 p-2.5 sm:p-0 rounded-2xl bg-rose-50/40 sm:bg-transparent">
                    <XCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
                    <div className="leading-relaxed">
                      <strong className="text-[#1c1917] block font-bold mb-0.5">Sobreventa en horas pico:</strong>
                      Aceptas 10 pedidos para la misma hora y tu equipo o taller no da abasto, provocando quejas y clientes molestos.
                    </div>
                  </li>
                  <li className="flex items-start gap-3 p-2.5 sm:p-0 rounded-2xl bg-rose-50/40 sm:bg-transparent">
                    <XCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
                    <div className="leading-relaxed">
                      <strong className="text-[#1c1917] block font-bold mb-0.5">Pedidos traspapelados:</strong>
                      El cliente envía comprobantes por chat, notas en papel y audios que se pierden en el historial.
                    </div>
                  </li>
                  <li className="flex items-start gap-3 p-2.5 sm:p-0 rounded-2xl bg-rose-50/40 sm:bg-transparent">
                    <XCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
                    <div className="leading-relaxed">
                      <strong className="text-[#1c1917] block font-bold mb-0.5">Disputas de stock y turnos:</strong>
                      Dos clientes piden el último producto o detalle al mismo tiempo y tienes que cancelar a uno de ellos.
                    </div>
                  </li>
                  <li className="flex items-start gap-3 p-2.5 sm:p-0 rounded-2xl bg-rose-50/40 sm:bg-transparent">
                    <XCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
                    <div className="leading-relaxed">
                      <strong className="text-[#1c1917] block font-bold mb-0.5">Pérdida de horas valiosas:</strong>
                      Repetir el catálogo, los precios y los métodos de pago 50 veces al día por mensajes manuales.
                    </div>
                  </li>
                </ul>
              </div>

              <div className="mt-6 sm:mt-8 p-3.5 sm:p-4 bg-rose-50/80 rounded-2xl text-xs text-rose-700 font-semibold text-center leading-relaxed">
                ❌ Pérdida promedio del 25% de clientes por mala experiencia y retrasos
              </div>
            </Card>
          </MotionFade>

          {/* Right Card: The Order with JaldiShop */}
          <MotionFade delay={0.2} direction="up">
            <Card className="p-5 sm:p-7 lg:p-8 h-full bg-[#f0fdfa] shadow-lg shadow-[#005141]/5 relative overflow-hidden flex flex-col justify-between rounded-3xl border-0">
              <div className="space-y-5 sm:space-y-6">
                {/* Header with responsive wrapping */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-2 border-b border-teal-50 sm:border-transparent">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-2xl bg-[#005141] text-white flex items-center justify-center font-bold shadow-xs shrink-0">
                      <Sparkles className="w-5 h-5 text-[#feae2c]" />
                    </div>
                    <div>
                      <h3 className="font-display text-base sm:text-lg font-bold text-[#1c1917]">Capa de Orden JaldiShop</h3>
                      <p className="text-xs text-[#005141] font-bold">Control de Capacidad Inteligente</p>
                    </div>
                  </div>
                  <div className="self-start sm:self-auto">
                    <Badge variant="jade" size="sm">
                      Solución Definitiva
                    </Badge>
                  </div>
                </div>

                <ul className="space-y-3.5 sm:space-y-4 text-xs sm:text-sm text-[#57534e]">
                  <li className="flex items-start gap-3 p-2.5 sm:p-0 rounded-2xl bg-white/70 sm:bg-transparent shadow-2xs sm:shadow-none">
                    <CheckCircle2 className="w-5 h-5 text-[#005141] shrink-0 mt-0.5" />
                    <div className="leading-relaxed">
                      <strong className="text-[#1c1917] block font-bold mb-0.5">Límites exactos por franja horaria:</strong>
                      Tu tienda solo acepta los pedidos que tu negocio puede preparar a tiempo. Al llenarse, se bloquea sola.
                    </div>
                  </li>
                  <li className="flex items-start gap-3 p-2.5 sm:p-0 rounded-2xl bg-white/70 sm:bg-transparent shadow-2xs sm:shadow-none">
                    <CheckCircle2 className="w-5 h-5 text-[#005141] shrink-0 mt-0.5" />
                    <div className="leading-relaxed">
                      <strong className="text-[#1c1917] block font-bold mb-0.5">Tiempo de gracia de 10 minutos:</strong>
                      El cupo queda asegurado mientras el cliente realiza el pago, impidiendo la doble venta.
                    </div>
                  </li>
                  <li className="flex items-start gap-3 p-2.5 sm:p-0 rounded-2xl bg-white/70 sm:bg-transparent shadow-2xs sm:shadow-none">
                    <CheckCircle2 className="w-5 h-5 text-[#005141] shrink-0 mt-0.5" />
                    <div className="leading-relaxed">
                      <strong className="text-[#1c1917] block font-bold mb-0.5">WhatsApp Order Bridge automatizado:</strong>
                      Los pedidos llegan con formato claro, productos, montos, dirección y horario de entrega exacto.
                    </div>
                  </li>
                  <li className="flex items-start gap-3 p-2.5 sm:p-0 rounded-2xl bg-white/70 sm:bg-transparent shadow-2xs sm:shadow-none">
                    <CheckCircle2 className="w-5 h-5 text-[#005141] shrink-0 mt-0.5" />
                    <div className="leading-relaxed">
                      <strong className="text-[#1c1917] block font-bold mb-0.5">Panel MYPE en tiempo real:</strong>
                      Tu equipo ve en pantalla la cola de pedidos ordenados por prioridad y horario de entrega.
                    </div>
                  </li>
                </ul>
              </div>

              <div className="mt-6 sm:mt-8 p-3.5 sm:p-4 bg-[#ccfbf1]/60 rounded-2xl text-xs text-[#005141] font-bold text-center leading-relaxed">
                ✨ 99.8% de entregas puntuales y 3x más recomendaciones de tus clientes
              </div>
            </Card>
          </MotionFade>
        </div>
      </Container>
    </section>
  );
}

