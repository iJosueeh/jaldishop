'use client';

import React, { useState } from 'react';
import { StoreCategory } from '../types/storefront.types';

interface StorefrontNavProps {
  categories?: StoreCategory[];
  activeCategoryId?: string;
  onSelectCategory?: (id: string) => void;
}

const DEFAULT_CATEGORIES: StoreCategory[] = [
  { id: 'all', name: 'Todos los productos', slug: 'todos', itemCount: 6 },
  { id: 'destacados', name: '⭐ Más Populares', slug: 'destacados', itemCount: 3 },
  { id: 'panaderia', name: 'Panes & Masas', slug: 'panes', itemCount: 2 },
  { id: 'pasteleria', name: 'Pastelería & Dulces', slug: 'pasteles', itemCount: 2 },
  { id: 'bebidas', name: 'Cafetería & Bebidas', slug: 'bebidas', itemCount: 1 },
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
    <div className="sticky top-16 sm:top-20 z-30 bg-[#faf7f2]/95 backdrop-blur-md border-y border-[#e7e0d6] py-3 shadow-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-2.5 overflow-x-auto no-scrollbar py-0.5">
          {categories.map((cat) => {
            const isSelected = selected === cat.id;
            return (
              <button
                key={cat.id}
                onClick={() => handleSelect(cat.id)}
                className={`px-4 py-2 rounded-2xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
                  isSelected
                    ? 'bg-[#005141] text-white shadow-sm shadow-[#005141]/20'
                    : 'bg-white text-[#57534e] hover:text-[#1c1917] border border-[#e7e0d6] hover:border-[#a8a29e]'
                }`}
              >
                {cat.name}
                {cat.itemCount !== undefined && (
                  <span
                    className={`ml-1.5 text-[10px] font-bold px-2 py-0.5 rounded-full ${
                      isSelected
                        ? 'bg-white/20 text-white'
                        : 'bg-[#faf7f2] text-[#57534e] border border-[#e7e0d6]'
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
