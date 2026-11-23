import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  getCustomerReviews,
  checkReviewEligibility,
  createReview,
  updateReview,
  deleteReview,
} from './customerReviewService';

describe('customerReviewService', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('getCustomerReviews fetches and returns reviews list', async () => {
    const mockReviews = [{ id: 'rev-1', rating: 5, comment: 'Genial' }];
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => mockReviews,
    });

    const result = await getCustomerReviews();
    expect(result).toEqual(mockReviews);
    expect(globalThis.fetch).toHaveBeenCalledWith('/api/customer/reviews', expect.any(Object));
  });

  it('checkReviewEligibility fetches review eligibility for a product', async () => {
    const mockEligibility = { eligible: true, alreadyReviewed: false };
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => mockEligibility,
    });

    const result = await checkReviewEligibility('p-123');
    expect(result).toEqual(mockEligibility);
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/customer/products/p-123/reviews/eligibility',
      expect.any(Object)
    );
  });

  it('createReview sends POST payload and returns created review', async () => {
    const mockReview = { id: 'rev-2', rating: 4, comment: 'Muy bueno' };
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => mockReview,
    });

    const result = await createReview('p-123', { rating: 4, comment: 'Muy bueno' });
    expect(result).toEqual(mockReview);
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/customer/products/p-123/reviews',
      expect.objectContaining({ method: 'POST' })
    );
  });

  it('updateReview sends PUT payload and returns updated review', async () => {
    const mockReview = { id: 'rev-2', rating: 5, comment: 'Excelente' };
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => mockReview,
    });

    const result = await updateReview('p-123', { rating: 5, comment: 'Excelente' });
    expect(result).toEqual(mockReview);
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/customer/products/p-123/reviews',
      expect.objectContaining({ method: 'PUT' })
    );
  });

  it('deleteReview sends DELETE request and resolves successfully on ok', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
    });

    await expect(deleteReview('p-123')).resolves.toBeUndefined();
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/customer/products/p-123/reviews',
      expect.objectContaining({ method: 'DELETE' })
    );
  });

  it('deleteReview throws error when response is not ok', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({ message: 'No se puede eliminar la reseña' }),
    });

    await expect(deleteReview('p-123')).rejects.toThrow('No se puede eliminar la reseña');
  });
});
