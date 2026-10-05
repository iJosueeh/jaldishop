import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StorefrontCart } from './StorefrontCart';
import { StorefrontCartProvider } from './StorefrontCartProvider';
import { CatalogContainer } from '../components/CatalogContainer';
import { cartService, CartResponse } from './services/cartService';
import { customerSessionService } from './services/customerSessionService';
import { ApiException } from '@/core/api/apiClient';

vi.mock('next/navigation', () => ({ usePathname: () => '/', useRouter: () => ({ push: vi.fn() }) }));
vi.mock('./services/cartService', () => ({ cartService: { get: vi.fn(), add: vi.fn(), update: vi.fn(), remove: vi.fn(), clear: vi.fn() } }));
vi.mock('./services/customerSessionService', () => ({ customerSessionService: { get: vi.fn(), login: vi.fn(), logout: vi.fn() } }));
const store = { id: 'store-1', name: 'Toddy', slug: 'toddy', status: 'ACTIVE' as const, deliveryEnabled: true, pickupEnabled: true };
const product = { id: 'product-1', name: 'Tacos', description: '', price: 12, category: 'Comida', variants: [{ id: 'variant-1', presentationName: 'Unidad', priceAmount: 12, priceCurrency: 'PEN', tracksInventory: false, status: 'ACTIVE' as const }] };
const empty: CartResponse = { id: null, storeId: store.id, items: [], totalItems: 0, totalAmount: 0, currency: 'PEN' };
const filled: CartResponse = { ...empty, id: 'cart-1', items: [{ variantId: 'variant-1', productId: product.id, productName: 'Tacos', presentationName: 'Unidad', quantity: 2, unitPriceAmount: 12, unitPriceCurrency: 'PEN', subtotalAmount: 24, available: true }], totalItems: 2, totalAmount: 24 };
function setup() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<QueryClientProvider client={client}><StorefrontCartProvider><StorefrontCart /><CatalogContainer store={store} storeName={store.name} products={[product]} /></StorefrontCartProvider></QueryClientProvider>);
}
beforeEach(() => {
  vi.clearAllMocks(); localStorage.clear();
  vi.mocked(customerSessionService.get).mockResolvedValue({ authenticated: true });
  vi.mocked(cartService.get).mockResolvedValue(empty);
});
describe('Backend storefront cart', () => {
  it('adds a real variant and displays the server quantity and total', async () => {
    vi.mocked(cartService.add).mockResolvedValue(filled);
    setup();
    await waitFor(() => expect(cartService.get).toHaveBeenCalledWith(store.id));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Añadir' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Añadir' }));
    await waitFor(() => expect(cartService.add).toHaveBeenCalledWith(store.id, 'variant-1', 1));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Carrito, 2 productos' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Carrito, 2 productos' }));
    expect(screen.getAllByText(/24.00/).length).toBeGreaterThan(0);
    expect(screen.getByText('Entrega y horarios')).toBeInTheDocument();
  });
  it('updates, removes and clears using variant IDs and server responses', async () => {
    vi.mocked(cartService.get).mockResolvedValue(filled);
    vi.mocked(cartService.update).mockResolvedValue({ ...filled, items: [{ ...filled.items[0], quantity: 1 }], totalItems: 1, totalAmount: 12 });
    vi.mocked(cartService.remove).mockResolvedValue(empty);
    setup();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Carrito, 2 productos' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Carrito, 2 productos' }));
    fireEvent.click(screen.getByRole('button', { name: 'Quitar una unidad de Tacos — Unidad' }));
    await waitFor(() => expect(cartService.update).toHaveBeenCalledWith(store.id, 'variant-1', 1));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Carrito, 1 producto' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Quitar una unidad de Tacos — Unidad' }));
    await waitFor(() => expect(cartService.remove).toHaveBeenCalledWith(store.id, 'variant-1'));
    await waitFor(() => expect(screen.queryByText('Entrega y horarios')).not.toBeInTheDocument());
  });
  it('requires customer authentication without fabricating a saved item', async () => {
    vi.mocked(customerSessionService.get).mockResolvedValue({ authenticated: false });
    setup();
    await waitFor(() => expect(customerSessionService.get).toHaveBeenCalled());
    await waitFor(() => expect(screen.getByRole('button', { name: 'Añadir' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Añadir' }));
    await screen.findByRole('button', { name: 'Ingresar como cliente' });
    expect(cartService.add).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Carrito, 0 productos' })).toBeInTheDocument();
  });
  it('preserves the server selection after a rejected update', async () => {
    vi.mocked(cartService.get).mockResolvedValue(filled);
    vi.mocked(cartService.update).mockRejectedValue(new ApiException(409, 'Conflict', undefined, 'La presentación no está disponible.'));
    setup();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Carrito, 2 productos' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Carrito, 2 productos' }));
    fireEvent.click(screen.getByRole('button', { name: 'Añadir una unidad de Tacos — Unidad' }));
    await screen.findByRole('alert');
    expect(screen.getByRole('button', { name: 'Carrito, 2 productos' })).toBeInTheDocument();
  });
  it('clears the server cart without reserving capacity', async () => {
    vi.mocked(cartService.get).mockResolvedValue(filled);
    vi.mocked(cartService.clear).mockResolvedValue(empty);
    setup();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Carrito, 2 productos' })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Carrito, 2 productos' }));
    fireEvent.click(screen.getByText('Vaciar carrito'));
    await waitFor(() => expect(cartService.clear).toHaveBeenCalledWith(store.id));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Carrito, 0 productos' })).toBeInTheDocument());
  });
  it('resumes the selected variant after customer login succeeds', async () => {
    vi.mocked(customerSessionService.get).mockResolvedValue({ authenticated: false });
    vi.mocked(customerSessionService.login).mockResolvedValue({ authenticated: true });
    vi.mocked(cartService.add).mockResolvedValue(filled);
    vi.mocked(cartService.get).mockResolvedValue(filled);
    setup();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Añadir' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Añadir' }));
    fireEvent.change(await screen.findByLabelText('Correo'), { target: { value: 'customer@example.test' } });
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'test-password' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Ingresar como cliente' }).closest('form')!);
    await waitFor(() => expect(cartService.add).toHaveBeenCalledWith(store.id, 'variant-1', 1));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Carrito, 2 productos' })).toBeInTheDocument());
  });
  it('closes with Escape and returns focus to the trigger', async () => {
    setup();
    const trigger = screen.getByRole('button', { name: 'Carrito, 0 productos' });
    fireEvent.click(trigger); fireEvent.keyDown(document, { key: 'Escape' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });
});
