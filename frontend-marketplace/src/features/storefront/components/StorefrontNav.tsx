'use client';

import React, { useState } from 'react';
import { StoreCategory } from '../types/storefront.types';
import { motion } from 'framer-motion';

interface StorefrontNavProps {
  categories?: StoreCategory[];
  activeCategoryId?: string;
  onSelectCategory?: (id: string) => void;
}



export function StorefrontNav({
  categories = [],
  activeCategoryId,
  onSelectCategory,
}: StorefrontNavProps) {
  const [selected, setSelected] = useState('all');

  const handleSelect = (id: string) => {
    setSelected(id);
    if (onSelectCategory) {
      onSelectCategory(id);
    }
  };

  if (categories.length === 0) return null;

  return (
    <div className="sticky top-16 sm:top-20 z-30 bg-white/90 backdrop-blur-md border-y border-stone-200/70 py-2.5 shadow-2xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar py-0.5 relative">
          {categories.map((cat) => {
            const isSelected = (activeCategoryId ?? selected) === cat.id;

            return (
              <button
                key={cat.id}
                aria-pressed={isSelected}
                onClick={() => handleSelect(cat.id)}
                className={`relative px-4 py-2 rounded-full text-xs font-bold whitespace-nowrap transition-colors cursor-pointer select-none flex items-center gap-1.5 ${
                  isSelected ? 'text-white' : 'text-stone-700 hover:text-stone-900 bg-stone-50/80 hover:bg-stone-100/90 border border-stone-200/80'
                }`}
              >
                {/* Magic Tab Glider Animation */}
                {isSelected && (
                  <motion.div
                    layoutId="activeCategoryGlider"
                    transition={{ type: 'spring', stiffness: 400, damping: 30 }}
                    className="absolute inset-0 bg-[#005141] rounded-full shadow-sm shadow-[#005141]/20 -z-0"
                  />
                )}

                <span className="relative z-10">{cat.name}</span>

                {cat.itemCount !== undefined && (
                  <span
                    className={`relative z-10 text-[10px] font-bold px-2 py-0.5 rounded-full transition-colors ${
                      isSelected
                        ? 'bg-white/20 text-white'
                        : 'bg-white text-stone-600 border border-stone-200/70'
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
