'use client';

import React from 'react';
import Link from 'next/link';
import { useCustomerProfile } from '@/features/customer/hooks/useCustomerProfile';
import { useCustomerOrders } from '@/features/customer/hooks/useCustomerOrders';
import { useCustomerAnalytics } from '@/features/customer/hooks/useCustomerAnalytics';
import { ActiveOrderCard } from '@/features/customer/components/ActiveOrderCard';
import { AccountPageHeader } from '@/features/customer/components/AccountPageHeader';
import { AccountMetricTile } from '@/features/customer/components/AccountMetricTile';
import { Button } from '@/shared/components/ui/Button';
import { Badge } from '@/shared/components/ui/Badge';
import { Skeleton } from '@/shared/components/ui/Skeleton';
import { formatCurrency } from '@/shared/utils/formatters';
import {
  Package,
  Clock,
  Store,
  UserCheck,
  ArrowRight,
  ShieldAlert,
  HelpCircle,
  CheckCircle2,
  Phone,
  Mail,
  ShoppingBag,
} from 'lucide-react';

export function CustomerHomeView() {
  const { data: profile, isLoading: isProfileLoading } = useCustomerProfile();
  const {
    activeOrders,
    pastOrders,
    isLoading: isOrdersLoading,
  } = useCustomerOrders();
  const { summary: analyticsSummary, isLoading: isAnalyticsLoading } = useCustomerAnalytics();

  const isInitialLoading = isProfileLoading && !profile;

  if (isInitialLoading) {
    return (
      <div className="space-y-6 w-full" role="status" aria-busy="true" aria-label="Cargando resumen de cuenta">
        <div className="space-y-2 pb-5 border-b border-[#e7e0d6]">
          <Skeleton className="h-8 w-56 bg-[#f0eae0] rounded-lg" />
          <Skeleton className="h-4 w-80 bg-[#f0eae0] rounded-md" />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <Skeleton className="h-24 bg-[#f0eae0] rounded-xl" />
          <Skeleton className="h-24 bg-[#f0eae0] rounded-xl" />
          <Skeleton className="h-24 bg-[#f0eae0] rounded-xl" />
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_320px] gap-6">
          <Skeleton className="h-64 bg-[#f0eae0] rounded-xl" />
          <Skeleton className="h-64 bg-[#f0eae0] rounded-xl" />
        </div>
      </div>
    );
  }

  const hasPhone = Boolean(profile?.phone && profile.phone.trim().length > 0);
  const hasActiveOrders = activeOrders.length > 0;

  return (
    <div className="space-y-6 w-full">
      {/* 1. Saludo Personalizado (único h1) */}
      <AccountPageHeader
        title={`¡Hola, ${profile?.firstName || 'Comprador'}!`}
        description="Bienvenido a tu panel personal de compras locales y pedidos programados."
        actions={
          <Link href="/mi-jaldishop/perfil">
            <Button
              variant="outline"
              size="md"
              className="min-h-[44px] rounded-full border-[#e7e0d6] text-[#1c1917] hover:bg-[#f7f3ec] font-semibold"
              leftIcon={<UserCheck className="w-4 h-4 text-[#005141]" />}
            >
              Editar mis datos
            </Button>
          </Link>
        }
      />

      {/* 2. Franja de Métricas Clave */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <AccountMetricTile
          label={activeOrders.length === 1 ? 'Pedido activo' : 'Pedidos activos'}
          value={isOrdersLoading && !activeOrders.length ? '—' : activeOrders.length}
          icon={<Package className="w-4 h-4 text-[#005141]" />}
        />
        <AccountMetricTile
          label={pastOrders.length === 1 ? 'Pedido anterior' : 'Pedidos anteriores'}
          value={isOrdersLoading && !pastOrders.length ? '—' : pastOrders.length}
          icon={<Clock className="w-4 h-4 text-[#57534e]" />}
        />
        <AccountMetricTile
          label="Gasto (últimos 30 días)"
          value={
            isAnalyticsLoading && !analyticsSummary
              ? '—'
              : formatCurrency(Number(analyticsSummary?.spending?.totalSpentAmount ?? 0), 'PEN')
          }
          valueClassName="text-[#005141]"
          icon={<ShoppingBag className="w-4 h-4 text-[#005141]" />}
        />
      </div>

      {/* 3. Workspace Principal: Actividad (izq) + Rail de Cuenta (der) */}
      <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_320px] xl:grid-cols-[minmax(0,1fr)_340px] gap-6 items-start">
        {/* Columna Principal */}
        <div className="space-y-5 min-w-0">
          {isOrdersLoading && !activeOrders.length && !pastOrders.length ? (
            <div className="p-6 rounded-xl bg-white border border-[#e7e0d6] space-y-4 animate-pulse">
              <div className="h-6 w-48 bg-[#f0eae0] rounded-lg" />
              <div className="h-4 w-full bg-[#f7f3ec] rounded-md" />
              <div className="h-24 bg-[#f7f3ec] rounded-xl" />
            </div>
          ) : hasActiveOrders ? (
            <section aria-labelledby="active-order-heading" className="space-y-3">
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <span className="relative flex h-2.5 w-2.5" aria-hidden="true">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
                    <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500" />
                  </span>
                  <h2 id="active-order-heading" className="font-display text-lg font-bold text-[#1c1917] tabular-nums">
                    Tu pedido en curso ({activeOrders.length})
                  </h2>
                </div>
                <Link
                  href="/mi-jaldishop/pedidos"
                  className="text-xs font-semibold text-[#005141] hover:underline underline-offset-4"
                >
                  Ver todos los pedidos
                </Link>
              </div>

              <ActiveOrderCard order={activeOrders[0]} />

              {activeOrders.length > 1 && (
                <div className="px-4 py-3 rounded-xl bg-white border border-[#e7e0d6] text-xs text-[#57534e] flex items-center justify-between gap-3">
                  <span className="tabular-nums">
                    Tienes <strong className="text-[#1c1917]">{activeOrders.length - 1}</strong> pedido(s) adicional(es) en preparación.
                  </span>
                  <Link
                    href="/mi-jaldishop/pedidos"
                    className="font-bold text-[#005141] hover:underline underline-offset-4 shrink-0"
                  >
                    Ver en Mis Pedidos
                  </Link>
                </div>
              )}
            </section>
          ) : (
            <section className="p-6 rounded-xl bg-white border border-[#e7e0d6] space-y-5">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <Package className="w-5 h-5 text-[#005141] shrink-0" />
                  <div>
                    <h2 className="font-display text-lg font-bold text-[#1c1917]">
                      Tus Pedidos Programados
                    </h2>
                    <p className="text-xs text-[#57534e]">
                      Control de capacidad y entregas acordadas con cada comercio
                    </p>
                  </div>
                </div>
                <Badge variant="jade" size="sm" className="font-bold">
                  Cero Sobreventa
                </Badge>
              </div>

              <p className="text-sm text-[#57534e] leading-relaxed">
                En JaldiShop compras a negocios y emprendimientos locales que organizan su producción por turnos.
                Cada orden tiene una fecha y franja horaria asegurada para que recibas tu pedido a tiempo y recién preparado.
              </p>

              <div className="pt-4 border-t border-[#e7e0d6]/70 flex items-start gap-2.5 text-xs text-[#57534e]">
                <Clock className="w-4 h-4 text-[#005141] shrink-0 mt-0.5" />
                <p className="leading-relaxed">
                  <strong className="text-[#1c1917]">Coordinación directa:</strong> En la sección de pedidos podrás revisar los detalles de cada compra y acceder al contacto por WhatsApp del comerciante.
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-3 pt-1">
                <Link href="/mi-jaldishop/pedidos">
                  <Button
                    variant="primary"
                    size="md"
                    className="min-h-[44px] rounded-full px-6 font-bold"
                    rightIcon={<ArrowRight className="w-4 h-4" />}
                  >
                    Ver mis pedidos
                  </Button>
                </Link>
                <Link href="/#tiendas-destacadas">
                  <Button
                    variant="outline"
                    size="md"
                    className="min-h-[44px] rounded-full px-6 font-semibold border-[#e7e0d6] text-[#1c1917] hover:bg-[#f7f3ec]"
                    leftIcon={<Store className="w-4 h-4 text-[#005141]" />}
                  >
                    Explorar comercios locales
                  </Button>
                </Link>
              </div>
            </section>
          )}

          {/* Alerta de teléfono faltante */}
          {!hasPhone && (
            <div className="p-4 sm:p-5 rounded-xl bg-[#fffbeb] border border-[#fde68a] flex items-start gap-3">
              <ShieldAlert className="w-5 h-5 text-[#b45309] shrink-0 mt-0.5" />
              <div className="space-y-1 flex-1 text-xs">
                <p className="font-bold text-sm text-[#92400e]">
                  Añade un número celular para coordinar tus compras
                </p>
                <p className="text-[#92400e]/90 leading-relaxed">
                  Tener tu número registrado permite que el comercio te avise por WhatsApp cuando tu pedido esté listo para retiro o cuando el repartidor esté cerca.
                </p>
                <div className="pt-1">
                  <Link
                    href="/mi-jaldishop/perfil"
                    className="inline-flex items-center gap-1 font-bold text-[#005141] hover:underline underline-offset-4"
                  >
                    <span>Completar mi número de teléfono</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              </div>
            </div>
          )}

          {/* Garantías del Marketplace (una sola superficie dividida) */}
          <div className="rounded-xl bg-white border border-[#e7e0d6] grid grid-cols-1 sm:grid-cols-2 divide-y sm:divide-y-0 sm:divide-x divide-[#e7e0d6]/70">
            <div className="p-4 sm:p-5 flex items-start gap-3">
              <CheckCircle2 className="w-5 h-5 text-[#005141] shrink-0 mt-0.5" />
              <div>
                <h3 className="text-xs font-bold text-[#1c1917]">Horarios y Franjas Reales</h3>
                <p className="text-xs text-[#57534e] mt-0.5 leading-relaxed">
                  Los comercios fijan cupos de atención por hora para asegurar frescura y puntualidad.
                </p>
              </div>
            </div>

            <div className="p-4 sm:p-5 flex items-start gap-3">
              <Store className="w-5 h-5 text-[#ea580c] shrink-0 mt-0.5" />
              <div>
                <h3 className="text-xs font-bold text-[#1c1917]">Apoyo a Negocios Locales</h3>
                <p className="text-xs text-[#57534e] mt-0.5 leading-relaxed">
                  Comercio justo sin comisiones abusivas para los emprendedores de tu comunidad.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Rail Lateral Unificado (Ficha de Cliente + Estadísticas + Ayuda) */}
        <aside className="rounded-xl bg-white border border-[#e7e0d6] divide-y divide-[#e7e0d6]/80">
          {/* Ficha de Cliente */}
          <div className="p-5 space-y-4">
            <div className="flex items-center justify-between gap-2">
              <h2 className="text-xs font-bold uppercase tracking-wider text-[#57534e] flex items-center gap-2">
                <UserCheck className="w-4 h-4 text-[#005141]" />
                <span>Ficha de Cliente</span>
              </h2>
              <Link
                href="/mi-jaldishop/estadisticas"
                className="text-xs font-semibold text-[#005141] hover:underline underline-offset-4"
              >
                Estadísticas
              </Link>
            </div>

            <dl className="space-y-3 text-xs">
              <div>
                <dt className="text-[#78716c]">Nombre registrado</dt>
                <dd className="font-bold text-[#1c1917] text-sm mt-0.5">
                  {profile?.firstName} {profile?.lastName}
                </dd>
              </div>

              <div>
                <dt className="text-[#78716c] flex items-center gap-1.5">
                  <Mail className="w-3.5 h-3.5" aria-hidden="true" />
                  <span>Correo electrónico</span>
                </dt>
                <dd className="text-[#1c1917] font-medium break-all mt-0.5">{profile?.email}</dd>
              </div>

              <div>
                <dt className="text-[#78716c] flex items-center gap-1.5">
                  <Phone className="w-3.5 h-3.5" aria-hidden="true" />
                  <span>Teléfono para entregas</span>
                </dt>
                <dd
                  className={
                    hasPhone
                      ? 'text-[#1c1917] font-semibold tabular-nums mt-0.5'
                      : 'text-[#78716c] italic mt-0.5'
                  }
                >
                  {hasPhone ? profile?.phone : 'No registrado aún'}
                </dd>
              </div>
            </dl>

            <Link
              href="/mi-jaldishop/perfil"
              className="min-h-[44px] w-full inline-flex items-center justify-center gap-2 px-4 rounded-lg bg-[#faf7f2] hover:bg-[#f0eae0] border border-[#e7e0d6] text-xs text-[#1c1917] font-bold transition-colors"
            >
              <span>Actualizar datos</span>
              <ArrowRight className="w-3.5 h-3.5 text-[#57534e]" />
            </Link>
          </div>

          {/* Ayuda y Soporte */}
          <div className="p-5 space-y-2.5">
            <h2 className="text-xs font-bold uppercase tracking-wider text-[#57534e] flex items-center gap-2">
              <HelpCircle className="w-4 h-4 text-[#57534e]" />
              <span>Ayuda y Soporte</span>
            </h2>
            <p className="text-xs text-[#57534e] leading-relaxed">
              ¿Tienes dudas sobre tus pedidos o franjas horarias?
            </p>
            <div className="divide-y divide-[#e7e0d6]/70 border-t border-[#e7e0d6]/70">
              <Link
                href="/ayuda"
                className="min-h-[44px] flex items-center justify-between py-2 text-xs font-semibold text-[#1c1917] hover:text-[#005141] transition-colors"
              >
                <span>Centro de Ayuda</span>
                <ArrowRight className="w-3.5 h-3.5 text-[#78716c]" />
              </Link>
              <Link
                href="/libro-de-reclamaciones"
                className="min-h-[44px] flex items-center justify-between py-2 text-xs font-semibold text-[#1c1917] hover:text-[#005141] transition-colors"
              >
                <span>Libro de Reclamaciones</span>
                <ArrowRight className="w-3.5 h-3.5 text-[#78716c]" />
              </Link>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}
