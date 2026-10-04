'use client';

import Link from 'next/link';

export default function StoreLoadError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <section className="mx-auto max-w-2xl px-6 py-32 text-center">
      <h1 className="font-display text-3xl font-bold text-stone-900">No pudimos cargar esta tienda</h1>
      <p className="mt-4 text-base leading-relaxed text-stone-600">La información del negocio no pudo consultarse en este momento. Esto no significa que la tienda haya dejado de existir.</p>
      <button onClick={reset} className="mt-6 rounded-xl bg-[#005141] px-6 py-3 font-semibold text-white">Volver a intentar</button>
      <Link href="/" className="mt-4 block text-sm font-semibold text-[#005141]">Volver a JaldiShop</Link>
    </section>
  );
}
