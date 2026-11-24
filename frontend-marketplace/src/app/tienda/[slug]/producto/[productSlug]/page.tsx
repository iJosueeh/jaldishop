import { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { ProductDetailView } from '@/features/storefront/components/ProductDetailView';
import { storeService } from '@/features/storefront/services/storeService';

interface ProductDetailPageProps {
  params: Promise<{ slug: string; productSlug: string }>;
}

export const revalidate = 300;

export async function generateMetadata({ params }: ProductDetailPageProps): Promise<Metadata> {
  const { slug, productSlug } = await params;
  const product = await storeService.getStoreProductBySlug(slug, productSlug).catch(() => null);
  return product
    ? {
        title: `${product.name} | JaldiShop`,
        description: product.description,
        openGraph: {
          title: `${product.name} — JaldiShop`,
          description: product.description,
          images: product.imageUrl ? [{ url: product.imageUrl }] : undefined,
        },
      }
    : { title: 'Producto no encontrado | JaldiShop' };
}

export default async function ProductDetailPage({ params }: ProductDetailPageProps) {
  const { slug, productSlug } = await params;
  const [store, product] = await Promise.all([
    storeService.getStoreBySlug(slug),
    storeService.getStoreProductBySlug(slug, productSlug),
  ]);

  if (!store || !product) notFound();

  const [storeSettings, reviewsSummary, storeProducts] = await Promise.all([
    storeService.getPublicStoreSettings(store.id),
    storeService.getPublicProductReviews(product.id),
    storeService.getStoreProductsBySlug(slug),
  ]);

  const relatedProducts = storeService.selectRelatedProducts(storeProducts, product, 8);

  return (
    <ProductDetailView
      store={store}
      product={product}
      storeSettings={storeSettings}
      reviewsSummary={reviewsSummary}
      relatedProducts={relatedProducts}
    />
  );
}
