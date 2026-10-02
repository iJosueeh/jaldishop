import React from 'react';
import { Container } from '@/shared/components/ui/Container';
import { MotionFade } from '@/shared/components/ui/MotionFade';
import { Card } from '@/shared/components/ui/Card';
import { XCircle, CheckCircle2, AlertTriangle, Sparkles } from 'lucide-react';
import { Badge } from '@/shared/components/ui/Badge';

export function ComparisonSection() {
  return (
    <section id="antes-despues" className="py-24 bg-linear-to-b from-warm-muted via-[#faf7f2] to-[#faf7f2] relative overflow-hidden">
      <Container size="lg">
        {/* Section Header */}
        <div className="text-center max-w-2xl mx-auto space-y-4 mb-16">
          <Badge variant="terracotta" size="md">
            La Transformación Operativa
          </Badge>
          <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-warm-text">
            ¿Por qué vender por chat tradicional te está costando dinero y clientes?
          </h2>
          <p className="text-base text-warm-text-muted">
            Compara el dolor diario de coordinar pedidos manualmente frente a la tranquilidad de un sistema con capacidad inteligente.
          </p>
        </div>

        {/* Comparison Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-stretch">
          {/* Left Card: The Chaos of WhatsApp */}
          <MotionFade delay={0.1} direction="right">
            <Card className="p-8 h-full bg-white border-2 border-rose-200/80 shadow-sm relative overflow-hidden flex flex-col justify-between">
              <div className="space-y-6">
                <div className="flex items-center justify-between pb-4 border-b border-rose-100">
                  <div className="flex items-center gap-2.5">
                    <div className="w-10 h-10 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center font-bold">
                      <AlertTriangle className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="text-lg font-bold text-warm-text">Venta por Chat Desordenado</h3>
                      <p className="text-xs text-rose-600 font-semibold">El dolor tradicional de las MYPE</p>
                    </div>
                  </div>
                  <Badge variant="danger" size="sm">
                    Caos Operativo
                  </Badge>
                </div>

                <ul className="space-y-4 text-sm text-warm-text-muted">
                  <li className="flex items-start gap-3">
                    <XCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
                    <div>
                      <strong className="text-warm-text block">Sobreventa en horas pico:</strong>
                      Aceptas 10 pedidos para la misma hora y tu cocina no da abasto, provocando quejas y clientes molestos.
                    </div>
                  </li>
                  <li className="flex items-start gap-3">
                    <XCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
                    <div>
                      <strong className="text-warm-text block">Pedidos traspapelados:</strong>
                      El cliente envía comprobantes por chat, notas en papel y audios que se pierden en el historial.
                    </div>
                  </li>
                  <li className="flex items-start gap-3">
                    <XCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
                    <div>
                      <strong className="text-warm-text block">Disputas de stock:</strong>
                      Dos clientes piden el último pastel al mismo tiempo y tienes que cancelar a uno de ellos.
                    </div>
                  </li>
                  <li className="flex items-start gap-3">
                    <XCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
                    <div>
                      <strong className="text-warm-text block">Pérdida de horas valiosas:</strong>
                      Repetir el menú, los precios y los métodos de pago 50 veces al día por mensajes manuales.
                    </div>
                  </li>
                </ul>
              </div>

              <div className="mt-8 p-4 bg-rose-50/60 rounded-2xl border border-rose-100 text-xs text-rose-700 font-medium text-center">
                ❌ Pérdida promedio del 25% de clientes por mala experiencia y retrasos
              </div>
            </Card>
          </MotionFade>

          {/* Right Card: The Order with JaldiShop */}
          <MotionFade delay={0.2} direction="left">
            <Card className="p-8 h-full bg-jade-50 border-2 border-jade-900 shadow-lg shadow-jade-900/10 relative overflow-hidden flex flex-col justify-between">
              <div className="space-y-6">
                <div className="flex items-center justify-between pb-4 border-b border-jade-100">
                  <div className="flex items-center gap-2.5">
                    <div className="w-10 h-10 rounded-2xl bg-jade-900 text-white flex items-center justify-center font-bold">
                      <Sparkles className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="text-lg font-bold text-warm-text">Capa de Orden JaldiShop</h3>
                      <p className="text-xs text-warm-text-primary font-bold">Control de Capacidad Inteligente</p>
                    </div>
                  </div>
                  <Badge variant="jade" size="sm">
                    Solución Definitiva
                  </Badge>
                </div>

                <ul className="space-y-4 text-sm text-warm-text-muted">
                  <li className="flex items-start gap-3">
                    <CheckCircle2 className="w-5 h-5 text-warm-primary-500 shrink-0 mt-0.5" />
                    <div>
                      <strong className="text-warm-text block">Límites exactos por franja horaria:</strong>
                      Tu tienda solo acepta los pedidos que tu equipo puede preparar a tiempo. Al llenarse, se bloquea sola.
                    </div>
                  </li>
                  <li className="flex items-start gap-3">
                    <CheckCircle2 className="w-5 h-5 text-warm-primary-500 shrink-0 mt-0.5" />
                    <div>
                      <strong className="text-warm-text block">Hold transaccional de 10 minutos:</strong>
                      El cupo queda asegurado mientras el cliente realiza el pago, impidiendo la doble venta.
                    </div>
                  </li>
                  <li className="flex items-start gap-3">
                    <CheckCircle2 className="w-5 h-5 text-jade-900 shrink-0 mt-0.5" />
                    <div>
                      <strong className="text-warm-text block">WhatsApp Order Bridge automatizado:</strong>
                      Los pedidos llegan con formato claro, productos, montos, dirección y franja de entrega exacta.
                    </div>
                  </li>
                  <li className="flex items-start gap-3">
                    <CheckCircle2 className="w-5 h-5 text-jade-900 shrink-0 mt-0.5" />
                    <div>
                      <strong className="text-warm-text block">Panel MYPE en tiempo real:</strong>
                      Tu cocina ve en pantalla la cola de pedidos ordenados por prioridad y horario de entrega.
                    </div>
                  </li>
                </ul>
              </div>

              <div className="mt-8 p-4 bg-jade-100/60 rounded-2xl border border-[#99f6e4] text-xs text-jade-900 font-bold text-center">
                ✨ 99.8% de entregas puntuales y 3x más recomendaciones de tus clientes
              </div>
            </Card>
          </MotionFade>
        </div>
      </Container>
    </section>
  );
}
