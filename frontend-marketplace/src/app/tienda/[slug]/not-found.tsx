import React from 'react';
import Link from 'next/link';
import { Store, ArrowLeft, Search } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { Button } from '@/shared/components/ui/Button';

export default function StoreNotFound() {
  return (
    <div className="min-h-[80vh] flex items-center justify-center py-20">
      <Container size="sm">
        <div className="text-center space-y-6 bg-white dark:bg-slate-900 p-8 sm:p-12 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-xl">
          <div className="w-16 h-16 rounded-3xl bg-rose-50 dark:bg-rose-950/60 text-rose-500 flex items-center justify-center mx-auto">
            <Store className="w-8 h-8" />
          </div>

          <div className="space-y-2">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white">
              Tienda no encontrada
            </h1>
            <p className="text-sm text-slate-500 dark:text-slate-400 max-w-sm mx-auto leading-relaxed">
              La tienda que estás buscando no existe, cambió de dirección o aún no ha completado su configuración inicial.
            </p>
          </div>

          <div className="pt-4 flex flex-col sm:flex-row items-center justify-center gap-3">
            <Link href="/" className="w-full sm:w-auto">
              <Button variant="primary" size="md" className="w-full" leftIcon={<ArrowLeft className="w-4 h-4" />}>
                Volver al Marketplace
              </Button>
            </Link>
            <Link href="/tienda/panaderia-don-pepe" className="w-full sm:w-auto">
              <Button variant="outline" size="md" className="w-full" leftIcon={<Search className="w-4 h-4" />}>
                Ver Tienda Demo
              </Button>
            </Link>
          </div>
        </div>
      </Container>
    </div>
  );
}
