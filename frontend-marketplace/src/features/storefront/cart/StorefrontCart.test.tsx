import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StorefrontCart } from './StorefrontCart';
import { CartPageClient } from './CartPageClient';
import { StorefrontCartProvider } from './StorefrontCartProvider';
import { CatalogContainer } from '../components/CatalogContainer';
import { cartService, CartResponse } from './services/cartService';
import { customerSessionService } from './services/customerSessionService';
import { ApiException } from '@/core/api/apiClient';

const pushMock = vi.fn();
vi.mock('next/navigation', () => ({
  usePathname: () => '/',
  useRouter: () => ({ push: pushMock }),
}));
vi.mock('./services/cartService', () => ({
  cartService: { get: vi.fn(), add: vi.fn(), update: vi.fn(), remove: vi.fn(), clear: vi.fn() },
}));
vi.mock('./services/customerSessionService', () => ({
  customerSessionService: { get: vi.fn(), login: vi.fn(), logout: vi.fn() },
}));
vi.mock('../services/storeService', () => ({
  storeService: {
    getStoreBySlugClient: vi.fn().mockResolvedValue({
      id: 'store-1',
      name: 'Toddy',
      slug: 'toddy',
      status: 'ACTIVE',
      deliveryEnabled: true,
      pickupEnabled: true,
      deliveryFeeAmount: 5,
    }),
    getPublicStoreSettingsClient: vi.fn().mockResolvedValue({
      cancellationPolicy: '',
      refundPolicy: '',
      requireCustomerDni: false,
      allowOrderNotes: true,
      onlinePaymentsEnabled: true,
      cashOnDeliveryEnabled: false,
      acceptsPosOnDelivery: false,
      minDeliveryAmount: 20,
      estimatedPrepTimeMinutes: 25,
    }),
  },
}));

const store = {
  id: 'store-1',
  name: 'Toddy',
  slug: 'toddy',
  status: 'ACTIVE' as const,
  deliveryEnabled: true,
  pickupEnabled: true,
};
const product = {
  id: 'product-1',
  name: 'Tacos',
  description: '',
  price: 12,
  category: 'Comida',
  variants: [
    {
      id: 'variant-1',
      presentationName: 'Unidad',
      priceAmount: 12,
      priceCurrency: 'PEN',
      tracksInventory: false,
      status: 'ACTIVE' as const,
    },
  ],
};
const empty: CartResponse = {
  id: null,
  storeId: store.id,
  items: [],
  totalItems: 0,
  totalAmount: 0,
  currency: 'PEN',
};
const filled: CartResponse = {
  ...empty,
  id: 'cart-1',
  items: [
    {
      variantId: 'variant-1',
      productId: product.id,
      productName: 'Tacos',
      presentationName: 'Unidad',
      quantity: 2,
      unitPriceAmount: 12,
      unitPriceCurrency: 'PEN',
      subtotalAmount: 24,
      available: true,
    },
  ],
  totalItems: 2,
  totalAmount: 24,
};

async function clickWhenEnabled(name: string) {
  await waitFor(
    () => {
      const button = screen.queryByRole('button', { name });
      if (!button || (button as HTMLButtonElement).disabled) {
        throw new Error(`botón "${name}" aún deshabilitado`);
      }
      fireEvent.click(button);
    },
    { timeout: 3000 }
  );
}

function setupNavbarAndCatalog() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <StorefrontCartProvider>
        <StorefrontCart />
        <CatalogContainer store={store} storeName={store.name} products={[product]} />
      </StorefrontCartProvider>
    </QueryClientProvider>
  );
}

function setupCartPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <StorefrontCartProvider>
        <CartPageClient />
      </StorefrontCartProvider>
    </QueryClientProvider>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.clear();
  vi.mocked(customerSessionService.get).mockResolvedValue({ authenticated: true });
  vi.mocked(cartService.get).mockResolvedValue(empty);
});

describe('Storefront navbar cart link & /carrito page', () => {
  it('navbar cart links directly to /carrito without opening a modal dialog', async () => {
    vi.mocked(cartService.get).mockResolvedValue(filled);
    localStorage.setItem('jaldishop-active-cart-store', JSON.stringify(store));
    setupNavbarAndCatalog();

    const cartLink = await screen.findByRole('link', { name: 'Carrito, 2 productos' });
    expect(cartLink).toHaveAttribute('href', '/carrito');
    fireEvent.click(cartLink);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('adds a real variant from catalog and updates navbar cart count', async () => {
    vi.mocked(cartService.add).mockResolvedValue(filled);
    setupNavbarAndCatalog();
    await waitFor(() => expect(cartService.get).toHaveBeenCalledWith(store.id));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Añadir' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Añadir' }));
    await waitFor(() => expect(cartService.add).toHaveBeenCalledWith(store.id, 'variant-1', 1));
    expect(await screen.findByRole('link', { name: 'Carrito, 2 productos' })).toBeInTheDocument();
  });

  it('requires customer login inside /carrito interface when unauthenticated', async () => {
    vi.mocked(customerSessionService.get).mockResolvedValue({ authenticated: false });
    setupCartPage();
    expect(await screen.findByText(/Inicia sesión para ver tus carritos/i)).toBeInTheDocument();
    const loginLink = screen.getByRole('link', { name: 'Ingresar como cliente' });
    expect(loginLink).toHaveAttribute('href', '/login?redirect=/carrito');
    const registerLink = screen.getByRole('link', { name: '¿No tienes cuenta? Regístrate aquí' });
    expect(registerLink).toHaveAttribute('href', '/registro?redirect=/carrito');
  });

  it('shows empty left column and zeroed right summary when there are no items', async () => {
    setupCartPage();
    expect(await screen.findByText('No hay ningún elemento en tu carrito')).toBeInTheDocument();
    expect(screen.getByText('Sin tienda seleccionada')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Continuar al checkout/i })).toBeDisabled();
  });

  it('shows all store carts with corner checkboxes, switches selection, and navigates to /checkout', async () => {
    const store2 = { id: 'store-2', name: 'La Miga', slug: 'la-miga' };
    localStorage.setItem('jaldishop-active-cart-store', JSON.stringify(store));
    localStorage.setItem('jaldishop-recent-cart-stores', JSON.stringify([store, store2]));

    vi.mocked(cartService.get).mockImplementation(async (id) =>
      id === store2.id
        ? {
            ...empty,
            storeId: store2.id,
            id: 'cart-2',
            items: [
              {
                variantId: 'v-2',
                productId: 'p-2',
                productName: 'Pan',
                presentationName: 'Bolsa',
                quantity: 1,
                unitPriceAmount: 8,
                unitPriceCurrency: 'PEN',
                subtotalAmount: 8,
                available: true,
              },
            ],
            totalItems: 1,
            totalAmount: 8,
          }
        : filled
    );

    setupCartPage();
    expect(await screen.findByText('Tacos — Unidad')).toBeInTheDocument();
    expect(await screen.findByText('Pan — Bolsa')).toBeInTheDocument();

    // Select second store via its corner checkbox
    const store2Checkbox = await screen.findByRole('checkbox', {
      name: /Seleccionar La Miga para checkout/i,
    });
    fireEvent.click(store2Checkbox);
    await waitFor(() => expect(store2Checkbox).toBeChecked());

    // Proceed to checkout for selected store
    fireEvent.click(screen.getByRole('button', { name: /Continuar al checkout/i }));
    expect(pushMock).toHaveBeenCalledWith('/checkout?store=la-miga');
  });

  it('updates and removes items inside /carrito', async () => {
    localStorage.setItem('jaldishop-active-cart-store', JSON.stringify(store));
    localStorage.setItem('jaldishop-recent-cart-stores', JSON.stringify([store]));
    vi.mocked(cartService.get).mockResolvedValue(filled);
    vi.mocked(cartService.update).mockResolvedValue({
      ...filled,
      items: [{ ...filled.items[0], quantity: 1 }],
      totalItems: 1,
      totalAmount: 12,
    });
    vi.mocked(cartService.remove).mockResolvedValue(empty);

    setupCartPage();
    await clickWhenEnabled('Quitar una unidad de Tacos — Unidad');
    await waitFor(() => expect(cartService.update).toHaveBeenCalledWith(store.id, 'variant-1', 1));
    await clickWhenEnabled('Quitar una unidad de Tacos — Unidad');
    await waitFor(() => expect(cartService.remove).toHaveBeenCalledWith(store.id, 'variant-1'));
  });

  it('preserves server state after rejected update and disables checkout when item is unavailable', async () => {
    localStorage.setItem('jaldishop-active-cart-store', JSON.stringify(store));
    localStorage.setItem('jaldishop-recent-cart-stores', JSON.stringify([store]));
    vi.mocked(cartService.get).mockResolvedValue({
      ...filled,
      items: [{ ...filled.items[0], available: false }],
    });
    vi.mocked(cartService.update).mockRejectedValue(
      new ApiException(409, 'Conflict', undefined, 'La presentación no está disponible.')
    );

    setupCartPage();
    expect(await screen.findByText(/No disponible — retira este ítem/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Continuar al checkout/i })).toBeDisabled();
  });

  it('paginates store carts at 3 per page and displays essential store summary info', async () => {
    const stores = [
      { id: 'store-1', name: 'Toddy', slug: 'toddy' },
      { id: 'store-2', name: 'La Miga', slug: 'la-miga' },
      { id: 'store-3', name: 'Café Sur', slug: 'cafe-sur' },
      { id: 'store-4', name: 'Flor Verde', slug: 'flor-verde' },
    ];
    localStorage.setItem('jaldishop-active-cart-store', JSON.stringify(stores[0]));
    localStorage.setItem('jaldishop-recent-cart-stores', JSON.stringify(stores));

    vi.mocked(cartService.get).mockImplementation(async (id) => ({
      ...empty,
      storeId: id,
      id: `cart-${id}`,
      items: [
        {
          variantId: `var-${id}`,
          productId: `prod-${id}`,
          productName: `Producto ${id}`,
          presentationName: 'Estándar',
          quantity: 1,
          unitPriceAmount: 15,
          unitPriceCurrency: 'PEN',
          subtotalAmount: 15,
          available: true,
        },
      ],
      totalItems: 1,
      totalAmount: 15,
    }));

    setupCartPage();
    expect(await screen.findByText('Producto store-1 — Estándar')).toBeInTheDocument();
    expect(screen.getByText('Producto store-3 — Estándar')).toBeInTheDocument();
    expect(screen.queryByText('Producto store-4 — Estándar')).not.toBeInTheDocument();

    // Essential summary info visible (service fee + delivery + prep time)
    expect(screen.getByText('Tarifa de servicio')).toBeInTheDocument();
    expect(await screen.findByText(/25 min/i)).toBeInTheDocument();

    // Navigate to page 2
    fireEvent.click(screen.getByRole('button', { name: 'Página 2' }));
    expect(await screen.findByText('Producto store-4 — Estándar')).toBeInTheDocument();
    expect(screen.queryByText('Producto store-1 — Estándar')).not.toBeInTheDocument();
  });
});
