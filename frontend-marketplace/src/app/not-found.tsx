import React from 'react';
import Link from 'next/link';
import { ShoppingBag, ArrowLeft } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { Button } from '@/shared/components/ui/Button';

export default function NotFound() {
  return (
    <div className="min-h-[85vh] flex items-center justify-center py-20">
      <Container size="sm">
        <div className="text-center space-y-6 bg-white dark:bg-slate-900 p-8 sm:p-12 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-xl">
          <div className="w-16 h-16 rounded-3xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 flex items-center justify-center mx-auto">
            <ShoppingBag className="w-8 h-8" />
          </div>

          <div className="space-y-2">
            <span className="text-xs font-mono font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/50 px-3 py-1 rounded-full">
              ERROR 404
            </span>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white pt-2">
              Página no encontrada
            </h1>
            <p className="text-sm text-slate-500 dark:text-slate-400 max-w-sm mx-auto leading-relaxed">
              Lo sentimos, la página o recurso que buscas no se encuentra disponible.
            </p>
          </div>

          <div className="pt-4 flex justify-center">
            <Link href="/">
              <Button variant="primary" size="md" leftIcon={<ArrowLeft className="w-4 h-4" />}>
                Regresar al Inicio
              </Button>
            </Link>
          </div>
        </div>
      </Container>
    </div>
  );
}
