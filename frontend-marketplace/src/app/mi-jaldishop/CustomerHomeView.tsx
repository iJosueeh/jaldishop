'use client';

import React from 'react';
import Link from 'next/link';
import { useCustomerProfile } from '@/features/customer/hooks/useCustomerProfile';
import { useCustomerOrders } from '@/features/customer/hooks/useCustomerOrders';
import { ActiveOrderCard } from '@/features/customer/components/ActiveOrderCard';
import { Button } from '@/shared/components/ui/Button';
import { Badge } from '@/shared/components/ui/Badge';
import { Skeleton } from '@/shared/components/ui/Skeleton';
import {
  Package,
  Clock,
  Store,
  UserCheck,
  ArrowRight,
  ShieldAlert,
  HelpCircle,
  Sparkles,
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

  const isInitialLoading = isProfileLoading && !profile;

  if (isInitialLoading) {
    return (
      <div className="space-y-8 w-full">
        <Skeleton className="h-8 w-64 bg-stone-200 rounded-xl" />
        <Skeleton className="h-4 w-96 bg-stone-200 rounded-lg" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 pt-4">
          <Skeleton className="h-64 lg:col-span-2 bg-stone-200 rounded-2xl" />
          <Skeleton className="h-64 lg:col-span-1 bg-stone-200 rounded-2xl" />
        </div>
      </div>
    );
  }

  const hasPhone = Boolean(profile?.phone && profile.phone.trim().length > 0);
  const hasActiveOrders = activeOrders.length > 0;

  return (
    <div className="space-y-8 w-full">
      {/* 1. Saludo Personalizado y Acceso Rápido */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-stone-200/80">
        <div>
          <h2 className="font-display text-2xl sm:text-3xl font-bold text-stone-900 tracking-tight flex items-center gap-2">
            <span>¡Hola, {profile?.firstName || 'Comprador'}!</span>
            <Sparkles className="w-5 h-5 text-[#005141]" />
          </h2>
          <p className="text-sm text-stone-600 mt-1 leading-relaxed">
            Bienvenido a tu panel personal de compras locales y pedidos programados.
          </p>
        </div>

        <Link href="/mi-jaldishop/perfil">
          <Button
            variant="outline"
            size="md"
            className="rounded-full border-stone-300 text-stone-700 hover:bg-stone-50 font-semibold"
            leftIcon={<UserCheck className="w-4 h-4 text-[#005141]" />}
          >
            Editar mis datos
          </Button>
        </Link>
      </div>

      {/* 2. Grid de Actividad y Contenido (Full Width 3-Column Layout) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        {/* Columna Principal (2 Columnas) */}
        <div className="lg:col-span-2 space-y-6">
          {/* Si está cargando pedidos por primera vez en segundo plano sin cache previa */}
          {isOrdersLoading && !activeOrders.length && !pastOrders.length ? (
            <div className="p-6 sm:p-8 rounded-2xl bg-white border border-stone-200/90 shadow-xs space-y-4 animate-pulse">
              <div className="h-6 w-48 bg-stone-200 rounded-lg" />
              <div className="h-4 w-full bg-stone-100 rounded-md" />
              <div className="h-24 bg-stone-100 rounded-xl" />
            </div>
          ) : hasActiveOrders ? (
            /* Si tiene pedidos activos: Hero con ActiveOrderCard */
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="relative flex h-2.5 w-2.5">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
                    <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500" />
                  </span>
                  <h3 className="font-display text-lg font-bold text-stone-900">
                    Tu pedido en curso ({activeOrders.length})
                  </h3>
                </div>
                <Link
                  href="/mi-jaldishop/pedidos"
                  className="text-xs font-semibold text-[#005141] hover:underline"
                >
                  Ver todos los pedidos
                </Link>
              </div>

              {/* Render first active order hero */}
              <ActiveOrderCard order={activeOrders[0]} />

              {/* If more than 1 active order, list subsequent ones */}
              {activeOrders.length > 1 && (
                <div className="p-4 rounded-xl bg-stone-50 border border-stone-200 text-xs text-stone-600 flex items-center justify-between">
                  <span>
                    Tienes <strong>{activeOrders.length - 1}</strong> pedido(s) adicional(es) en preparación.
                  </span>
                  <Link
                    href="/mi-jaldishop/pedidos"
                    className="font-bold text-[#005141] hover:underline"
                  >
                    Ver en Mis Pedidos
                  </Link>
                </div>
              )}
            </div>
          ) : (
            /* Card Principal Informativa cuando NO hay pedidos activos */
            <div className="p-6 sm:p-8 rounded-2xl bg-white border border-stone-200/90 shadow-xs space-y-6">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-3.5">
                  <div className="w-11 h-11 rounded-xl bg-emerald-50 border border-emerald-200/60 text-[#005141] flex items-center justify-center shrink-0">
                    <Package className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="font-display text-lg sm:text-xl font-bold text-stone-900">
                      Tus Pedidos Programados
                    </h3>
                    <p className="text-xs text-stone-500">
                      Control de capacidad y entregas acordadas con cada comercio
                    </p>
                  </div>
                </div>
                <Badge variant="jade" size="sm" className="font-bold">
                  Cero Sobreventa
                </Badge>
              </div>

              <p className="text-sm text-stone-600 leading-relaxed">
                En JaldiShop compras a negocios y emprendimientos locales que organizan su producción por turnos.
                Cada orden tiene una fecha y franja horaria asegurada para que recibas tu pedido a tiempo y recién preparado.
              </p>

              <div className="p-4 rounded-xl bg-[#faf7f2] border border-stone-200/80 flex items-start gap-3.5 text-xs text-stone-600">
                <Clock className="w-4 h-4 text-[#005141] shrink-0 mt-0.5" />
                <div>
                  <span className="font-bold text-stone-800">Coordinación directa:</span> En la sección de pedidos podrás revisar los detalles de cada compra y acceder al contacto directo por WhatsApp del comerciante.
                </div>
              </div>

              <div className="flex flex-wrap items-center gap-3 pt-2">
                <Link href="/mi-jaldishop/pedidos">
                  <Button
                    variant="primary"
                    size="md"
                    className="rounded-full px-6 font-bold shadow-md shadow-[#005141]/10"
                    rightIcon={<ArrowRight className="w-4 h-4" />}
                  >
                    Ver mis pedidos
                  </Button>
                </Link>
                <Link href="/#tiendas-destacadas">
                  <Button
                    variant="outline"
                    size="md"
                    className="rounded-full px-6 font-semibold border-stone-300 text-stone-700 hover:bg-stone-50"
                    leftIcon={<Store className="w-4 h-4 text-[#005141]" />}
                  >
                    Explorar comercios locales
                  </Button>
                </Link>
              </div>
            </div>
          )}

          {/* Banner Amigable de Alerta de Celular */}
          {!hasPhone && (
            <div className="p-5 sm:p-6 rounded-2xl bg-amber-50 border border-amber-200/90 flex items-start gap-4 shadow-2xs">
              <div className="w-9 h-9 rounded-xl bg-amber-100 text-[#b45309] flex items-center justify-center shrink-0 mt-0.5">
                <ShieldAlert className="w-5 h-5" />
              </div>
              <div className="space-y-1.5 flex-1 text-xs">
                <p className="font-bold text-sm text-amber-950">
                  Añade un número celular para coordinar tus compras
                </p>
                <p className="text-amber-800 leading-relaxed">
                  Tener tu número registrado permite que la pastelería o taller artesanal te avise por WhatsApp cuando tu pedido esté listo para retiro o cuando el repartidor esté cerca.
                </p>
                <div className="pt-1.5">
                  <Link
                    href="/mi-jaldishop/perfil"
                    className="inline-flex items-center gap-1 font-bold text-[#005141] hover:underline"
                  >
                    <span>Completar mi número de teléfono</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              </div>
            </div>
          )}

          {/* Garantías del Marketplace */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="p-5 rounded-xl bg-white border border-stone-200/90 shadow-2xs flex items-start gap-3">
              <CheckCircle2 className="w-5 h-5 text-[#005141] shrink-0 mt-0.5" />
              <div>
                <h4 className="text-xs font-bold text-stone-900">Horarios y Franjas Reales</h4>
                <p className="text-xs text-stone-500 mt-0.5 leading-relaxed">
                  Los comercios fijan cupos de atención por hora para asegurar la frescura y puntualidad.
                </p>
              </div>
            </div>

            <div className="p-5 rounded-xl bg-white border border-stone-200/90 shadow-2xs flex items-start gap-3">
              <Store className="w-5 h-5 text-[#ea580c] shrink-0 mt-0.5" />
              <div>
                <h4 className="text-xs font-bold text-stone-900">Apoyo a Negocios Locales</h4>
                <p className="text-xs text-stone-500 mt-0.5 leading-relaxed">
                  Comercio justo sin comisiones abusivas para los emprendedores de tu comunidad.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Columna Lateral (1 Columna) */}
        <div className="space-y-6">
          {/* Métricas Reales del Comprador */}
          <div className="p-5 rounded-2xl bg-white border border-stone-200/90 shadow-xs">
            <h3 className="text-xs font-bold uppercase tracking-wider text-stone-500 mb-3 flex items-center gap-2">
              <ShoppingBag className="w-4 h-4 text-[#005141]" />
              <span>Resumen de compras</span>
            </h3>
            <div className="grid grid-cols-2 gap-3 text-center">
              <div className="p-3 bg-stone-50 rounded-xl">
                <span className="block text-2xl font-bold text-stone-900">
                  {isOrdersLoading && !activeOrders.length ? '-' : activeOrders.length}
                </span>
                <span className="text-[11px] text-stone-500 font-medium">
                  {activeOrders.length === 1 ? 'Activo' : 'Activos'}
                </span>
              </div>
              <div className="p-3 bg-stone-50 rounded-xl">
                <span className="block text-2xl font-bold text-stone-900">
                  {isOrdersLoading && !pastOrders.length ? '-' : pastOrders.length}
                </span>
                <span className="text-[11px] text-stone-500 font-medium">
                  {pastOrders.length === 1 ? 'Anterior' : 'Anteriores'}
                </span>
              </div>
            </div>
          </div>

          {/* Ficha Resumen de Cuenta */}
          <div className="p-6 rounded-2xl bg-white border border-stone-200/90 shadow-xs space-y-4">
            <h3 className="text-xs font-bold uppercase tracking-wider text-stone-500 flex items-center gap-2">
              <UserCheck className="w-4 h-4 text-[#005141]" />
              <span>Ficha de Cliente</span>
            </h3>

            <div className="space-y-3.5 pt-1 text-xs">
              <div className="pb-3 border-b border-stone-100">
                <span className="text-stone-400 block mb-0.5">Nombre registrado</span>
                <span className="font-bold text-stone-900 text-sm">
                  {profile?.firstName} {profile?.lastName}
                </span>
              </div>

              <div className="pb-3 border-b border-stone-100">
                <span className="text-stone-400 flex items-center gap-1.5 mb-0.5">
                  <Mail className="w-3.5 h-3.5 text-stone-400" />
                  <span>Correo electrónico</span>
                </span>
                <span className="text-stone-700 font-medium break-all">{profile?.email}</span>
              </div>

              <div>
                <span className="text-stone-400 flex items-center gap-1.5 mb-0.5">
                  <Phone className="w-3.5 h-3.5 text-stone-400" />
                  <span>Teléfono para entregas</span>
                </span>
                <span className={hasPhone ? 'text-stone-900 font-semibold' : 'text-stone-400 italic'}>
                  {hasPhone ? profile?.phone : 'No registrado aún'}
                </span>
              </div>

              <div className="pt-2">
                <Link
                  href="/mi-jaldishop/perfil"
                  className="w-full inline-flex items-center justify-center gap-2 py-2 px-4 rounded-xl bg-stone-50 hover:bg-stone-100 border border-stone-200 text-xs text-stone-800 font-bold transition-colors"
                >
                  <span>Actualizar datos</span>
                  <ArrowRight className="w-3 h-3 text-stone-400" />
                </Link>
              </div>
            </div>
          </div>

          {/* Ayuda y Asistencia */}
          <div className="p-6 rounded-2xl bg-white border border-stone-200/90 shadow-xs space-y-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-stone-500 flex items-center gap-2">
              <HelpCircle className="w-4 h-4 text-stone-400" />
              <span>Ayuda y Soporte</span>
            </h3>

            <p className="text-xs text-stone-500 leading-relaxed">
              ¿Tienes preguntas sobre el proceso de compra o la política de franjas horarias?
            </p>

            <div className="space-y-2 pt-2 text-xs">
              <Link
                href="/ayuda"
                className="flex items-center justify-between p-3 rounded-xl bg-stone-50 hover:bg-emerald-50/50 border border-stone-200/80 text-stone-700 hover:text-[#005141] transition-all font-semibold"
              >
                <span>Centro de Ayuda</span>
                <ArrowRight className="w-3.5 h-3.5 text-stone-400" />
              </Link>
              <Link
                href="/libro-de-reclamaciones"
                className="flex items-center justify-between p-3 rounded-xl bg-stone-50 hover:bg-emerald-50/50 border border-stone-200/80 text-stone-700 hover:text-[#005141] transition-all font-semibold"
              >
                <span>Libro de Reclamaciones</span>
                <ArrowRight className="w-3.5 h-3.5 text-stone-400" />
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
