import type {
  CustomerReview,
  ReviewEligibility,
  CreateReviewPayload,
  UpdateReviewPayload,
} from '../types/customer.types';

export async function getCustomerReviews(): Promise<CustomerReview[]> {
  const response = await fetch('/api/customer/reviews', {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    cache: 'no-store',
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'Error al obtener tus opiniones');
  }

  return response.json();
}

export async function checkReviewEligibility(productId: string): Promise<ReviewEligibility> {
  const response = await fetch(`/api/customer/products/${encodeURIComponent(productId)}/reviews/eligibility`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    cache: 'no-store',
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'Error al verificar la elegibilidad');
  }

  return response.json();
}

export async function createReview(
  productId: string,
  payload: CreateReviewPayload
): Promise<CustomerReview> {
  const response = await fetch(`/api/customer/products/${encodeURIComponent(productId)}/reviews`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    cache: 'no-store',
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'Error al registrar la calificación');
  }

  return response.json();
}

export async function updateReview(
  productId: string,
  payload: UpdateReviewPayload
): Promise<CustomerReview> {
  const response = await fetch(`/api/customer/products/${encodeURIComponent(productId)}/reviews`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    cache: 'no-store',
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'Error al actualizar la calificación');
  }

  return response.json();
}
