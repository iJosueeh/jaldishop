import React from "react";
import { Container } from "@/shared/components/ui/Container";
import { MotionFade } from "@/shared/components/ui/MotionFade";
import { Card } from "@/shared/components/ui/Card";
import {
  Clock,
  MessageCircle,
  BarChart3,
  Smartphone,
  Check,
  Zap,
} from "lucide-react";
import { Badge } from "@/shared/components/ui/Badge";

export function BentoFeatures() {
  return (
    <section
      id="como-funciona"
      className="py-24 bg-linear-to-b from-warm-background-primary to-warm-background-secondary relative"
    >
      <Container size="lg">
        {/* Section Header */}
        <div className="text-center max-w-2xl mx-auto space-y-4 mb-16">
          <Badge variant="jade" size="md">
            Pilares Tecnológicos del Dominio
          </Badge>
          <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-warm-text">
            Las 4 superpotencias que protegen tu operación diaria
          </h2>
          <p className="text-base text-warm-text-muted">
            A diferencia de un e-commerce genérico, JaldiShop está diseñado
            desde la raíz para la realidad de las MYPE que preparan bajo pedido.
          </p>
        </div>

        {/* Bento Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Bento Item 1: Large Featured Card - Capacidad Dinámica */}
          <MotionFade delay={0.1} className="md:col-span-2">
            <Card className="p-8 h-full bg-white border-2 border-warm-border shadow-sm flex flex-col justify-between group hover:border-warm-primary-500 transition-all">
              <div className="space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-warm-background-primary text-warm-primary-500 border border-warm-border flex items-center justify-center">
                  <Clock className="w-6 h-6" />
                </div>
                <h3 className="text-2xl font-bold text-warm-text-primary">
                  Motor de Capacidad Operativa por Franjas Horarias
                </h3>
                <p className="text-sm text-warm-text-muted leading-relaxed max-w-xl">
                  Configura cuántos pedidos o productos puede procesar tu cocina
                  por bloque de tiempo. Cuando se llena el cupo, esa hora queda
                  deshabilitada automáticamente, evitando saturaciones y
                  retrasos.
                </p>
              </div>

              {/* Visual preview */}
              <div className="mt-8 p-4 rounded-2xl bg-warm-background-primary border border-warm-border grid grid-cols-3 gap-3">
                <div className="p-3 bg-white rounded-xl text-center shadow-xs border-2 border-jade-100">
                  <div className="text-[11px] font-semibold text-warm-text-muted">
                    10:00 AM
                  </div>
                  <div className="text-sm font-bold text-warm-text-primary">
                    4 cupos
                  </div>
                </div>
                <div className="p-3 bg-white rounded-xl text-center shadow-xs border-2 border-warm-border">
                  <div className="text-[11px] font-semibold text-warm-text-muted">
                    11:00 AM
                  </div>
                  <div className="text-sm font-bold text-warm-text-secondary">
                    1 cupo (Último)
                  </div>
                </div>
                <div className="p-3 bg-warm-background-secondary rounded-xl text-center opacity-60 border border-warm-border">
                  <div className="text-[11px] font-semibold text-warm-text-muted">
                    12:00 PM
                  </div>
                  <div className="text-sm font-bold text-warm-text-destructive">
                    Agotado
                  </div>
                </div>
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 2: Hold Transaccional 10m */}
          <MotionFade delay={0.2} className="md:col-span-1">
            <Card className="p-8 h-full bg-linear-to-br from-warm-primary-500 to-warm-primary-600 text-white shadow-xl shadow-warm-primary-500/20 flex flex-col justify-between border-2 border-warm-primary-600">
              <div className="space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-white/15 text-white flex items-center justify-center backdrop-blur-sm">
                  <Zap className="w-6 h-6 text-warm-secondary-500" />
                </div>
                <h3 className="text-2xl font-bold">
                  Hold Temporal de 10 Minutos
                </h3>
                <p className="text-sm text-warm-text-primary leading-relaxed">
                  Bloquea transaccionalmente el cupo en base de datos mientras
                  el comprador realiza la transferencia o pago. Cero doble
                  reserva.
                </p>
              </div>

              <div className="mt-8 pt-4 border-t border-white/20 flex items-center justify-between text-xs text-warm-text-primary">
                <span className="font-semibold">Reserva atómica</span>
                <span className="font-mono font-extrabold bg-white/20 px-3 py-1 rounded-xl text-warm-secondary-500">
                  10:00 min
                </span>
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 3: WhatsApp Bridge */}
          <MotionFade delay={0.3} className="md:col-span-1">
            <Card className="p-8 h-full bg-white border-2 border-warm-border shadow-sm flex flex-col justify-between hover:border-warm-primary-500 transition-all">
              <div className="space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-warm-background-primary text-warm-primary-500 border border-warm-border flex items-center justify-center">
                  <MessageCircle className="w-6 h-6" />
                </div>
                <h3 className="text-xl font-bold text-warm-text-primary">
                  WhatsApp Order Bridge
                </h3>
                <p className="text-sm text-warm-text-muted leading-relaxed">
                  Genera el mensaje del pedido formateado y validado
                  directamente al WhatsApp del comercio para resolver dudas o
                  compartir comprobantes al instante.
                </p>
              </div>

              <div className="mt-6 flex items-center gap-2 text-xs font-bold text-warm-primary-500">
                <Check className="w-4 h-4 text-warm-primary-500" /> Integración
                nativa con WhatsApp
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 4: Mobile & Next.js 15 App Router */}
          <MotionFade delay={0.4} className="md:col-span-1">
            <Card className="p-8 h-full bg-white border-2 border-warm-border shadow-sm flex flex-col justify-between hover:border-warm-primary-500 transition-all">
              <div className="space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-warm-background-primary text-warm-secondary-500 border border-warm-border flex items-center justify-center">
                  <Smartphone className="w-6 h-6" />
                </div>
                <h3 className="text-xl font-bold text-warm-text-primary">
                  Experiencia Mobile Instantánea
                </h3>
                <p className="text-sm text-warm-text-muted leading-relaxed">
                  Diseñado para abrir a toda velocidad desde un enlace en bio de
                  Instagram o estados de WhatsApp, sin lag ni tiempos de espera.
                </p>
              </div>

              <div className="mt-6 flex items-center gap-2 text-xs font-bold text-warm-secondary-500">
                <Check className="w-4 h-4 text-warm-secondary-500" /> Core Web
                Vitals optimizados
              </div>
            </Card>
          </MotionFade>

          {/* Bento Item 5: Panel MYPE Angular */}
          <MotionFade delay={0.5} className="md:col-span-1">
            <Card className="p-8 h-full bg-white border-2 border-warm-border shadow-sm flex flex-col justify-between hover:border-warm-primary-500 transition-all">
              <div className="space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-warm-background-primary text-warm-secondary-500 border border-warm-border flex items-center justify-center">
                  <BarChart3 className="w-6 h-6" />
                </div>
                <h3 className="text-xl font-bold text-warm-text-primary">
                  Sincronización con Panel MYPE
                </h3>
                <p className="text-sm text-warm-text-muted leading-relaxed">
                  Tu cocina recibe los pedidos en tiempo real en la app de
                  comerciante con notificaciones y control de estados de
                  preparación.
                </p>
              </div>

              <div className="mt-6 flex items-center gap-2 text-xs font-bold text-warm-secondary-500">
                <Check className="w-4 h-4 text-warm-secondary-500" /> Angular 20
                + Signals Reactivos
              </div>
            </Card>
          </MotionFade>
        </div>
      </Container>
    </section>
  );
}
