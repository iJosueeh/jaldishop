'use client';

import React, { useState } from 'react';
import { StoreCategory } from '../types/storefront.types';
import { motion } from 'framer-motion';

interface StorefrontNavProps {
  categories?: StoreCategory[];
  activeCategoryId?: string;
  onSelectCategory?: (id: string) => void;
}

const DEFAULT_CATEGORIES: StoreCategory[] = [
  { id: 'all', name: 'Todos los productos', slug: 'todos', itemCount: 6 },
  { id: 'destacados', name: '⭐ Más Populares', slug: 'destacados', itemCount: 3 },
  { id: 'panaderia', name: '🥐 Panes & Masas', slug: 'panes', itemCount: 2 },
  { id: 'pasteleria', name: '🍰 Pastelería & Dulces', slug: 'pasteles', itemCount: 2 },
  { id: 'bebidas', name: '☕ Cafetería & Bebidas', slug: 'bebidas', itemCount: 1 },
];

export function StorefrontNav({
  categories = DEFAULT_CATEGORIES,
  activeCategoryId = 'all',
  onSelectCategory,
}: StorefrontNavProps) {
  const [selected, setSelected] = useState(activeCategoryId);

  const handleSelect = (id: string) => {
    setSelected(id);
    if (onSelectCategory) {
      onSelectCategory(id);
    }
  };

  return (
    <div className="sticky top-16 sm:top-20 z-30 bg-[#faf7f2]/95 backdrop-blur-md border-y border-stone-200/80 py-3 shadow-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar py-0.5 relative">
          {categories.map((cat) => {
            const isSelected = selected === cat.id;
            return (
              <button
                key={cat.id}
                onClick={() => handleSelect(cat.id)}
                className={`relative px-4 py-2 rounded-2xl text-xs font-bold whitespace-nowrap transition-colors cursor-pointer select-none flex items-center gap-1.5 ${
                  isSelected ? 'text-white' : 'text-[#57534e] hover:text-[#1c1917] bg-white/80 border border-stone-200/80'
                }`}
              >
                {/* Magic Tab Glider Animation */}
                {isSelected && (
                  <motion.div
                    layoutId="activeCategoryGlider"
                    transition={{ type: 'spring', stiffness: 400, damping: 30 }}
                    className="absolute inset-0 bg-[#005141] rounded-2xl shadow-md shadow-[#005141]/20 -z-0"
                  />
                )}

                <span className="relative z-10">{cat.name}</span>

                {cat.itemCount !== undefined && (
                  <span
                    className={`relative z-10 text-[10px] font-bold px-2 py-0.5 rounded-full transition-colors ${
                      isSelected
                        ? 'bg-white/20 text-white'
                        : 'bg-[#faf7f2] text-[#78716c] border border-stone-200/70'
                    }`}
                  >
                    {cat.itemCount}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
