'use client';

import Link from 'next/link';
import { ArrowRight } from 'lucide-react';
import { toast } from 'sonner';
import { ProductItem, PublicStore } from '../../types/storefront.types';
import { ProductCard } from '../catalog/ProductCard';
import { useStorefrontCart } from '../../cart/StorefrontCartProvider';

interface RelatedStoreProductsProps {
  store: PublicStore;
  products: ProductItem[];
}

export function RelatedStoreProducts({ store, products }: RelatedStoreProductsProps) {
  const { items, selectStore, changeQuantity, pending } = useStorefrontCart();

  if (products.length === 0) return null;

  const getProductCartQuantity = (product: ProductItem) => {
    const variantIds = new Set(product.variants?.map((v) => v.id) ?? []);
    return items.reduce((sum, entry) => {
      if (entry.product.id === product.id || (entry.product.variantId && variantIds.has(entry.product.variantId))) {
        return sum + entry.quantity;
      }
      return sum;
    }, 0);
  };

  const handleAddRelated = async (item: ProductItem) => {
    if (!store.slug) return;
    selectStore({ id: store.id, name: store.name, slug: store.slug });
    await changeQuantity(item, 1);
    toast.success(`${item.name} añadido al carrito`);
  };

  return (
    <section aria-labelledby="related-products-heading" className="space-y-5 pt-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-[11px] font-bold uppercase tracking-wider text-[#005141]">
            Más de {store.name}
          </p>
          <h2 id="related-products-heading" className="font-display text-2xl font-extrabold text-stone-950">
            Productos relacionados
          </h2>
        </div>
        <Link
          href={`/tienda/${store.slug}`}
          className="inline-flex items-center gap-1.5 text-xs font-bold text-[#005141] hover:underline"
        >
          <span>Ver catálogo completo</span>
          <ArrowRight className="h-3.5 w-3.5" />
        </Link>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {products.map((related) => (
          <ProductCard
            key={related.id}
            storeSlug={store.slug}
            product={related}
            onAdd={handleAddRelated}
            currentQuantity={getProductCartQuantity(related)}
            pending={pending}
          />
        ))}
      </div>
    </section>
  );
}
