'use client';

import React from 'react';
import { ShoppingBag, Plus } from 'lucide-react';
import { Card } from '@/shared/components/ui/Card';
import { Badge } from '@/shared/components/ui/Badge';
import { Button } from '@/shared/components/ui/Button';
import { formatCurrency } from '@/shared/utils/formatters';
import { toast } from 'sonner';

interface CatalogContainerProps {
  storeSlug?: string;
  storeName?: string;
  children?: React.ReactNode;
  cartSlot?: React.ReactNode;
  capacitySlot?: React.ReactNode;
}

// Sample initial products with appetizing warm retail theme
const SAMPLE_PRODUCTS = [
  {
    id: 'prod-01',
    name: 'Croissant Artesanal de Mantequilla',
    description: 'Hojaldrado tradicional con 100% mantequilla francesa, corteza dorada crujiente y miga aireada.',
    price: 8.5,
    category: 'Panadería',
    badge: 'Más Vendido',
    badgeVariant: 'amber' as const,
    imageBg: 'bg-[#fef3c7] text-[#92400e] border border-[#fde68a]',
    iconText: '🥐',
  },
  {
    id: 'prod-02',
    name: 'Pan de Masa Madre Rústico (800g)',
    description: 'Fermentación lenta de 24 horas, harina de trigo y centeno integral con masa madre viva.',
    price: 14.0,
    category: 'Panadería',
    badge: 'Masa Madre',
    badgeVariant: 'jade' as const,
    imageBg: 'bg-[#f0fdfa] text-[#005141] border border-[#ccfbf1]',
    iconText: '🥖',
  },
  {
    id: 'prod-03',
    name: 'Empanada Criolla de Lomo Saltado',
    description: 'Relleno jugoso de lomo de res saltado al wok con cebolla morada, tomate y ají amarillo.',
    price: 9.0,
    category: 'Salados',
    badge: 'Horneada Hoy',
    badgeVariant: 'terracotta' as const,
    imageBg: 'bg-[#fff7ed] text-[#ea580c] border border-[#fed7aa]',
    iconText: '🥟',
  },
  {
    id: 'prod-04',
    name: 'Cheesecake Horneado de Frutos Rojos',
    description: 'Base crocante de galleta de mantequilla, queso crema suave y coulis artesanal de frambuesas.',
    price: 16.5,
    category: 'Pastelería',
    badge: 'Favorito',
    badgeVariant: 'terracotta' as const,
    imageBg: 'bg-pink-50 text-pink-700 border border-pink-200',
    iconText: '🍰',
  },
  {
    id: 'prod-05',
    name: 'Caja de 6 Alfajores con Manjar de Olla',
    description: 'Masa delicada que se deshace en el paladar, abundante manjar blanco casero y coco fino.',
    price: 18.0,
    category: 'Pastelería',
    badge: 'Para Compartir',
    badgeVariant: 'amber' as const,
    imageBg: 'bg-[#fffbeb] text-[#92400e] border border-[#fef3c7]',
    iconText: '🍪',
  },
  {
    id: 'prod-06',
    name: 'Café Espresso Doble Blend Especial',
    description: 'Granos arábica seleccionados de Villa Rica con notas achocolatadas y acidez equilibrada.',
    price: 7.5,
    category: 'Bebidas',
    badge: '100% Arábica',
    badgeVariant: 'jade' as const,
    imageBg: 'bg-[#faf7f2] text-[#57534e] border border-[#e7e0d6]',
    iconText: '☕',
  },
];

export function CatalogContainer({
  children,
  cartSlot,
  capacitySlot,
}: CatalogContainerProps) {
  const handleAddToCart = (productName: string, price: number) => {
    toast.success(`¡${productName} añadido al pedido!`, {
      description: `Total parcial: ${formatCurrency(price)}. Cupo en franja protegido temporalmente.`,
    });
  };

  return (
    <div className="py-6">
      {/* Optional Top Slot for Capacity / Slot Reservation (Mia - FE-STORE-02) */}
      {capacitySlot && <div className="mb-8">{capacitySlot}</div>}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Main Catalog Area */}
        <div className="lg:col-span-8 space-y-6">
          <div className="flex items-center justify-between pb-2 border-b border-[#e7e0d6]">
            <div>
              <h2 className="text-xl font-extrabold text-[#1c1917]">
                Catálogo de Productos Frescos
              </h2>
              <p className="text-xs text-[#57534e]">
                Elige tus productos y selecciona tu franja horaria de entrega o retiro
              </p>
            </div>
            <span className="text-xs font-bold text-[#005141] bg-[#f0fdfa] px-3 py-1 rounded-full border border-[#ccfbf1]">
              6 opciones disponibles
            </span>
          </div>

          {/* Children Slot or Fallback Interactive Products Grid */}
          {children ? (
            children
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              {SAMPLE_PRODUCTS.map((prod) => (
                <Card
                  key={prod.id}
                  className="p-6 flex flex-col justify-between hover:shadow-lg transition-all bg-white border-2 border-[#e7e0d6] hover:border-[#005141] group"
                >
                  <div className="space-y-3.5">
                    {/* Visual Emoji Badge & Tag */}
                    <div className="flex items-start justify-between">
                      <div
                        className={`w-14 h-14 rounded-2xl ${prod.imageBg} flex items-center justify-center text-3xl shadow-xs`}
                      >
                        {prod.iconText}
                      </div>
                      <Badge variant={prod.badgeVariant} size="sm">
                        {prod.badge}
                      </Badge>
                    </div>

                    <div>
                      <h3 className="text-base font-bold text-[#1c1917] group-hover:text-[#005141] transition-colors">
                        {prod.name}
                      </h3>
                      <p className="text-xs text-[#57534e] mt-1.5 line-clamp-2 leading-relaxed">
                        {prod.description}
                      </p>
                    </div>
                  </div>

                  <div className="mt-6 pt-4 border-t border-[#e7e0d6] flex items-center justify-between">
                    <span className="text-lg font-extrabold text-[#1c1917] font-mono">
                      {formatCurrency(prod.price)}
                    </span>
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => handleAddToCart(prod.name, prod.price)}
                      leftIcon={<Plus className="w-4 h-4" />}
                    >
                      Añadir
                    </Button>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>

        {/* Sidebar Slot (Katherine - FE-STORE-03 Cart & Checkout summary) */}
        <div className="lg:col-span-4 sticky top-28">
          {cartSlot ? (
            cartSlot
          ) : (
            <Card className="p-6 bg-white border-2 border-[#e7e0d6] shadow-sm space-y-5">
              <div className="flex items-center justify-between pb-3.5 border-b border-[#e7e0d6]">
                <div className="flex items-center gap-2 text-sm font-extrabold text-[#1c1917]">
                  <ShoppingBag className="w-4 h-4 text-[#ea580c]" />
                  Resumen de tu Pedido
                </div>
                <Badge variant="amber" size="sm">
                  Hold 10m
                </Badge>
              </div>

              <div className="py-6 text-center text-[#57534e] space-y-2 bg-[#faf7f2] rounded-2xl p-4 border border-[#e7e0d6]">
                <p className="text-xs font-semibold text-[#1c1917]">Selecciona productos del menú para comenzar.</p>
                <p className="text-[11px] text-[#78716c] leading-relaxed">
                  Al iniciar el checkout, tu cupo de horneado o preparación quedará bloqueado por 10 minutos.
                </p>
              </div>

              <div className="pt-2 space-y-2.5">
                <div className="flex justify-between text-xs text-[#57534e]">
                  <span>Subtotal</span>
                  <span className="font-mono font-bold text-[#1c1917]">S/ 0.00</span>
                </div>
                <div className="flex justify-between text-xs text-[#57534e]">
                  <span>Delivery estimado</span>
                  <span className="font-mono font-bold text-[#1c1917]">S/ 5.00</span>
                </div>
                <div className="flex justify-between text-sm font-extrabold text-[#1c1917] pt-3 border-t border-[#e7e0d6]">
                  <span>Total estimado</span>
                  <span className="font-mono text-base text-[#005141]">S/ 0.00</span>
                </div>
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
