import { render, screen, fireEvent } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ReviewModal } from './ReviewModal';
import { useCustomerReviews } from '../hooks/useCustomerReviews';

vi.mock('../hooks/useCustomerReviews', () => ({
  useCustomerReviews: vi.fn(),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('ReviewModal Component', () => {
  const mockCreateReview = vi.fn();
  const mockUpdateReview = vi.fn();
  const mockOnClose = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useCustomerReviews).mockReturnValue({
      reviews: [],
      isLoading: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
      createReview: mockCreateReview,
      isCreating: false,
      updateReview: mockUpdateReview,
      isUpdating: false,
    });
  });

  it('renders modal with product name and stars when open', () => {
    render(
      <ReviewModal
        isOpen={true}
        onClose={mockOnClose}
        productId="prod-123"
        productName="Mesa de Centro Rústica"
      />
    );

    expect(screen.getByText('Calificar producto')).toBeInTheDocument();
    expect(screen.getByText('Mesa de Centro Rústica')).toBeInTheDocument();
    expect(screen.getByText('Compra verificada')).toBeInTheDocument();
  });

  it('submits new review when submitting the form', async () => {
    mockCreateReview.mockResolvedValueOnce({});

    render(
      <ReviewModal
        isOpen={true}
        onClose={mockOnClose}
        productId="prod-123"
        productName="Mesa de Centro Rústica"
      />
    );

    const textarea = screen.getByPlaceholderText(/¿Qué tal fue la calidad/i);
    fireEvent.change(textarea, { target: { value: 'Gran calidad y atención rápida.' } });

    const submitBtn = screen.getByText('Publicar opinión');
    fireEvent.click(submitBtn);

    expect(mockCreateReview).toHaveBeenCalledWith({
      productId: 'prod-123',
      payload: {
        rating: 5,
        comment: 'Gran calidad y atención rápida.',
      },
    });
  });

  it('submits updated review when in editing mode', async () => {
    mockUpdateReview.mockResolvedValueOnce({});

    render(
      <ReviewModal
        isOpen={true}
        onClose={mockOnClose}
        productId="prod-123"
        productName="Mesa de Centro Rústica"
        initialRating={4}
        initialComment="Comentario anterior"
        isEditing={true}
      />
    );

    expect(screen.getByText('Editar opinión')).toBeInTheDocument();

    const submitBtn = screen.getByText('Guardar cambios');
    fireEvent.click(submitBtn);

    expect(mockUpdateReview).toHaveBeenCalledWith({
      productId: 'prod-123',
      payload: {
        rating: 4,
        comment: 'Comentario anterior',
      },
    });
  });
});
