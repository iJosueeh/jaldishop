import { render, screen, fireEvent } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CancelOrderModal } from './CancelOrderModal';

describe('CancelOrderModal Component', () => {
  const defaultProps = {
    isOpen: true,
    onClose: vi.fn(),
    orderNumber: 'ORD-20261113-0001',
    storeName: 'Panadería Central',
    totalAmount: 45.5,
    currency: 'PEN',
    onConfirm: vi.fn().mockResolvedValue(undefined),
    isCancelling: false,
    error: null,
    onClearError: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('does not render when isOpen is false', () => {
    const { container } = render(
      <CancelOrderModal {...defaultProps} isOpen={false} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders order number, store name, amount and informative text without immediate refund promises', () => {
    render(<CancelOrderModal {...defaultProps} />);

    expect(screen.getByText('¿Deseas cancelar este pedido?')).toBeInTheDocument();
    expect(screen.getByText(/ORD-20261113-0001/)).toBeInTheDocument();
    expect(screen.getByText(/Panadería Central/)).toBeInTheDocument();
    expect(screen.getByText(/S\/ 45.50/)).toBeInTheDocument();

    const refundText = screen.getByText(/operaciones de compensación y reembolso se tramitarán/i);
    expect(refundText).toBeInTheDocument();
    expect(screen.queryByText(/reembolso inmediato/i)).not.toBeInTheDocument();
  });

  it('confirmación descartada: clicking Mantener pedido or X calls onClose and does NOT call onConfirm', () => {
    render(<CancelOrderModal {...defaultProps} />);

    const closeBtn = screen.getByRole('button', { name: /mantener pedido/i });
    fireEvent.click(closeBtn);

    expect(defaultProps.onClose).toHaveBeenCalledTimes(1);
    expect(defaultProps.onConfirm).not.toHaveBeenCalled();

    const xBtn = screen.getByLabelText('Cerrar modal');
    fireEvent.click(xBtn);
    expect(defaultProps.onClose).toHaveBeenCalledTimes(2);
    expect(defaultProps.onConfirm).not.toHaveBeenCalled();
  });

  it('submits optional reason when filled and calls onConfirm', async () => {
    render(<CancelOrderModal {...defaultProps} />);

    const textarea = screen.getByLabelText(/motivo de la cancelación/i);
    fireEvent.change(textarea, { target: { value: 'Compré por error un producto repetido.' } });

    const submitBtn = screen.getByRole('button', { name: /confirmar cancelación/i });
    fireEvent.click(submitBtn);

    expect(defaultProps.onConfirm).toHaveBeenCalledWith('Compré por error un producto repetido.');
  });

  it('submits undefined when reason is blank', async () => {
    render(<CancelOrderModal {...defaultProps} />);

    const submitBtn = screen.getByRole('button', { name: /confirmar cancelación/i });
    fireEvent.click(submitBtn);

    expect(defaultProps.onConfirm).toHaveBeenCalledWith(undefined);
  });

  it('motivo de más de 255 caracteres: shows validation warning and disables submit button', () => {
    render(<CancelOrderModal {...defaultProps} />);

    const textarea = screen.getByLabelText(/motivo de la cancelación/i);
    const longText = 'x'.repeat(256);
    fireEvent.change(textarea, { target: { value: longText } });

    expect(screen.getByText('Supera el límite permitido')).toBeInTheDocument();
    expect(screen.getByText('256/255')).toBeInTheDocument();

    const submitBtn = screen.getByRole('button', { name: /confirmar cancelación/i });
    expect(submitBtn).toBeDisabled();

    // Trying to submit does nothing
    fireEvent.click(submitBtn);
    expect(defaultProps.onConfirm).not.toHaveBeenCalled();
  });

  it('solicitud duplicada: blocks repeated submission and disables buttons while isCancelling', () => {
    render(<CancelOrderModal {...defaultProps} isCancelling={true} />);

    expect(screen.getByText('Cancelando pedido...')).toBeInTheDocument();

    const submitBtn = screen.getByRole('button', { name: /cancelando pedido/i });
    expect(submitBtn).toBeDisabled();

    const cancelBtn = screen.getByRole('button', { name: /mantener pedido/i });
    expect(cancelBtn).toBeDisabled();

    fireEvent.click(submitBtn);
    expect(defaultProps.onConfirm).not.toHaveBeenCalled();

    fireEvent.click(cancelBtn);
    expect(defaultProps.onClose).not.toHaveBeenCalled();
  });

  it('renders error banner when error is present and clears on typing', () => {
    const onClearError = vi.fn();
    render(
      <CancelOrderModal
        {...defaultProps}
        onClearError={onClearError}
        error="El pedido ya no puede ser cancelado porque se encuentra en preparación."
      />
    );

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(
      screen.getByText('El pedido ya no puede ser cancelado porque se encuentra en preparación.')
    ).toBeInTheDocument();

    onClearError.mockClear();

    const textarea = screen.getByLabelText(/motivo de la cancelación/i);
    fireEvent.change(textarea, { target: { value: 'Nuevo texto' } });

    expect(onClearError).toHaveBeenCalledTimes(1);
  });
});
