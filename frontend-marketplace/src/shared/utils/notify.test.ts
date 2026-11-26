import { describe, it, expect, vi, beforeEach } from 'vitest';
import { toast } from 'sonner';
import { notify, notifyKeys } from './notify';

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    info: vi.fn(),
    warning: vi.fn(),
    error: vi.fn(),
    dismiss: vi.fn(),
  },
}));

describe('notify', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('delegates to sonner with severity-calibrated durations and deterministic ids', () => {
    notify.success('Añadido', { id: notifyKeys.cartAdd('p-1') });
    expect(toast.success).toHaveBeenCalledWith('Añadido', {
      id: 'cart-add-p-1',
      duration: 3500,
    });

    notify.info('Inicia sesión', { id: notifyKeys.favorite('p-2') });
    expect(toast.info).toHaveBeenCalledWith('Inicia sesión', {
      id: 'favorite-p-2',
      duration: 3500,
    });

    notify.warning('Pedido cancelado', { id: notifyKeys.orderStatus('ORD-9') });
    expect(toast.warning).toHaveBeenCalledWith('Pedido cancelado', {
      id: 'order-status-ORD-9',
      duration: 4500,
    });

    notify.error('Error de red', { id: notifyKeys.network() });
    expect(toast.error).toHaveBeenCalledWith('Error de red', {
      id: 'network-error',
      duration: 6500,
    });
  });

  it('allows overriding default duration and dismissing by id', () => {
    notify.error('Temporal', { id: 'tmp', duration: 2000 });
    expect(toast.error).toHaveBeenCalledWith('Temporal', {
      id: 'tmp',
      duration: 2000,
    });

    notify.dismiss('tmp');
    expect(toast.dismiss).toHaveBeenCalledWith('tmp');
  });
});
