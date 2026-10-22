'use client';

import React, { useState, useEffect } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import { Clock, ArrowUpRight } from 'lucide-react';
import { Badge } from '@/shared/components/ui/Badge';

export interface MarqueeItem {
  id: string;
  name: string;
  category: string;
  store: string;
  storeSlug: string;
  price: string;
  prepTime: string;
  badge: string;
  badgeVariant: 'jade' | 'terracotta' | 'amber';
  imageUrl: string;
}

export const DEFAULT_ROTATING_PHRASES: string[] = [
  'Capacidad operativa en tiempo real',
  'Pedidos y despachos bajo demanda',
  'Talleres, gastronomía y creadores locales',
  'Disponibilidad de slots al instante',
  'Creaciones por encargo sin sobreventa',
];

export const MARQUEE_ITEMS: MarqueeItem[] = [
  {
    id: 'm1',
    name: 'Croissant Francés de Mantequilla',
    category: 'Panadería Artesanal',
    store: 'Panadería Don Pepe',
    storeSlug: 'panaderia-don-pepe',
    price: 'S/ 8.50',
    prepTime: '15 min prep',
    badge: 'Horneado Hoy',
    badgeVariant: 'amber',
    imageUrl: 'https://images.unsplash.com/photo-1555507036-ab1f4038808a?q=80&w=600&auto=format&fit=crop',
  },
  {
    id: 'm2',
    name: 'Ramo Silvestre de Temporada',
    category: 'Floristería & Detalles',
    store: 'Floristería Botánica',
    storeSlug: 'panaderia-don-pepe',
    price: 'S/ 48.00',
    prepTime: '30 min armado',
    badge: 'Cupos Limitados',
    badgeVariant: 'terracotta',
    imageUrl: 'https://images.unsplash.com/photo-1561181286-d3fee7d55364?q=80&w=600&auto=format&fit=crop',
  },
  {
    id: 'm3',
    name: 'Cheesecake Horneado de Frutos Rojos',
    category: 'Pastelería Fina',
    store: 'Dulce Amor Repostería',
    storeSlug: 'dulce-amor',
    price: 'S/ 16.50',
    prepTime: '25 min prep',
    badge: 'Top Ventas',
    badgeVariant: 'amber',
    imageUrl: 'https://images.unsplash.com/photo-1533134242443-d4fd215305ad?q=80&w=600&auto=format&fit=crop',
  },
  {
    id: 'm4',
    name: 'Taza Cerámica Torneada a Mano',
    category: 'Taller & Artesanías',
    store: 'Taller Barro Vivo',
    storeSlug: 'panaderia-don-pepe',
    price: 'S/ 36.00',
    prepTime: 'Lote en torno',
    badge: 'Pieza Única',
    badgeVariant: 'jade',
    imageUrl: 'https://images.unsplash.com/photo-1610701596007-11502861dcfa?q=80&w=600&auto=format&fit=crop',
  },
  {
    id: 'm5',
    name: 'Cold Brew Embotellado de Altura',
    category: 'Cafetería de Especialidad',
    store: 'Café Villa Rica Barista',
    storeSlug: 'cafe-villa-rica',
    price: 'S/ 12.00',
    prepTime: '5 min despacho',
    badge: '100% Arábica',
    badgeVariant: 'jade',
    imageUrl: 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?q=80&w=600&auto=format&fit=crop',
  },
  {
    id: 'm6',
    name: 'Caja x6 Alfajores con Manjar de Olla',
    category: 'Dulces Criollos',
    store: 'Dulce Amor Repostería',
    storeSlug: 'dulce-amor',
    price: 'S/ 18.00',
    prepTime: '10 min prep',
    badge: 'Receta Casera',
    badgeVariant: 'amber',
    imageUrl: 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?q=80&w=600&auto=format&fit=crop',
  },
  {
    id: 'm7',
    name: 'Focaccia Crujiente con Romero y Oliva',
    category: 'Pizzas & Masas',
    store: 'La Focacceria Urbana',
    storeSlug: 'la-focacceria',
    price: 'S/ 18.00',
    prepTime: '15 min prep',
    badge: 'Receta Romana',
    badgeVariant: 'terracotta',
    imageUrl: 'https://images.unsplash.com/photo-1513104890138-7c749659a591?q=80&w=600&auto=format&fit=crop',
  },
  {
    id: 'm8',
    name: 'Tote Bag Textil Personalizado',
    category: 'Confección Bajo Pedido',
    store: 'Estudio Hilo & Trama',
    storeSlug: 'panaderia-don-pepe',
    price: 'S/ 42.00',
    prepTime: '1 día confección',
    badge: 'Hecho a Medida',
    badgeVariant: 'jade',
    imageUrl: 'https://images.unsplash.com/photo-1544816155-12df9643f363?q=80&w=600&auto=format&fit=crop',
  },
];

export interface CraftMarqueeSectionProps {
  title?: string;
  phrases?: string[];
  subtitle?: string;
  items?: MarqueeItem[];
}

export function CraftMarqueeSection({
  title,
  phrases = DEFAULT_ROTATING_PHRASES,
  subtitle = 'Pasa el cursor para pausar y ver slots de preparación de cada negocio',
  items = MARQUEE_ITEMS,
}: CraftMarqueeSectionProps) {
  const [phraseIndex, setPhraseIndex] = useState(0);
  const [isFading, setIsFading] = useState(false);

  useEffect(() => {
    if (title || !phrases.length) return;

    const interval = setInterval(() => {
      setIsFading(true);
      const timeout = setTimeout(() => {
        setPhraseIndex((prev) => (prev + 1) % phrases.length);
        setIsFading(false);
      }, 250);

      return () => clearTimeout(timeout);
    }, 3500);

    return () => clearInterval(interval);
  }, [title, phrases]);

  const duplicatedItems = [...items, ...items];
  const activeTitle = title || phrases[phraseIndex] || DEFAULT_ROTATING_PHRASES[0];

  return (
    <section className="py-10 bg-[#090b0c] relative">
      {/* Subtle Section Title Ribbon */}
      <div className="max-w-7xl mx-auto px-6 mb-5 flex items-center justify-between">
        <div className="flex items-center gap-2.5 text-xs font-semibold text-white uppercase tracking-wider">
          {/* Live pulsing badge in Amber & Jade */}
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-[#161c1e] border border-[#232b2f] text-emerald-400 text-[10px] font-semibold tracking-wide">
            <span className="relative flex h-1.5 w-1.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
              <span className="relative inline-flex rounded-full h-1.5 w-1.5 bg-emerald-500" />
            </span>
            <span>EN VIVO</span>
          </span>

          {/* Dynamic rotating heading */}
          <span
            className={`transition-all duration-300 text-xs font-semibold text-white uppercase tracking-wider ${
              isFading ? 'opacity-0 translate-y-0.5' : 'opacity-100 translate-y-0'
            }`}
          >
            {activeTitle}
          </span>

          <span className="text-stone-700 hidden sm:inline">•</span>
          <span className="text-[11px] font-normal text-stone-400 normal-case hidden sm:inline">
            {subtitle}
          </span>
        </div>

        <Link
          href="/#tiendas-destacadas"
          className="text-xs font-semibold text-[#feae2c] hover:text-[#ea580c] inline-flex items-center gap-1 transition-all"
        >
          <span>Ver todas las tiendas</span>
          <ArrowUpRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      {/* Marquee Track Container with smooth lateral gradient masks */}
      <div className="relative w-full overflow-hidden before:pointer-events-none before:absolute before:inset-y-0 before:left-0 before:w-16 sm:before:w-28 before:bg-gradient-to-r before:from-[#090b0c] before:to-transparent before:z-10 after:pointer-events-none after:absolute after:inset-y-0 after:right-0 after:w-16 sm:after:w-28 after:bg-gradient-to-l after:from-[#090b0c] after:to-transparent after:z-10">
        <div className="animate-marquee gap-5 flex items-center py-2 px-4">
          {duplicatedItems.map((item, index) => (
            <Link
              key={`${item.id}-${index}`}
              href={`/tienda/${item.storeSlug}`}
              className="group shrink-0 block"
            >
              <div className="w-72 sm:w-80 bg-[#121618] rounded-[12px] p-3.5 border border-[#1e2426] shadow-sm transition-all duration-300 hover:border-stone-500 hover:-translate-y-1 cursor-pointer relative overflow-hidden">
                {/* Product Photo with Rich Food Contrast */}
                <div className="relative h-40 w-full rounded-[10px] overflow-hidden bg-stone-900 mb-3">
                  <Image
                    src={item.imageUrl}
                    alt={item.name}
                    fill
                    sizes="320px"
                    className="object-cover transition-transform duration-700 ease-out group-hover:scale-105"
                  />
                  {/* Subtle dark vignette */}
                  <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-transparent to-black/20" />

                  {/* Top Badge (Varied by Product: Amber, Terracotta, Jade) */}
                  <div className="absolute top-2.5 left-2.5 z-10">
                    <Badge variant={item.badgeVariant} size="sm" className="shadow-xs font-semibold text-[10px]">
                      {item.badge}
                    </Badge>
                  </div>

                  {/* Prep Time pill in Amber */}
                  <div className="absolute bottom-2.5 right-2.5 z-10 bg-black/75 backdrop-blur-md px-2 py-0.5 rounded-full border border-white/10 text-[10px] font-medium text-white flex items-center gap-1">
                    <Clock className="w-3 h-3 text-[#feae2c]" />
                    <span>{item.prepTime}</span>
                  </div>
                </div>

                {/* Product Info */}
                <div className="space-y-1">
                  <div className="flex items-center justify-between text-[11px] text-stone-400 font-medium">
                    <span>{item.category}</span>
                    <span className="text-stone-300 font-medium group-hover:text-white transition-colors">
                      {item.store}
                    </span>
                  </div>

                  <h4 className="text-sm font-semibold text-white group-hover:text-[#feae2c] transition-colors truncate">
                    {item.name}
                  </h4>

                  <div className="flex items-center justify-between pt-1 border-t border-[#1e2426] mt-2">
                    <span className="font-mono font-semibold text-sm text-white">
                      {item.price}
                    </span>
                    <span className="text-[11px] font-bold text-[#ea580c] flex items-center gap-0.5 group-hover:translate-x-0.5 transition-transform">
                      <span>Pedir</span>
                      <ArrowUpRight className="w-3 h-3" />
                    </span>
                  </div>
                </div>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}
