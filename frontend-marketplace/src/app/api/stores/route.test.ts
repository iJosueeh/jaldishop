import { beforeEach, describe, expect, it, vi } from 'vitest';
import { GET } from './route';
import { NextRequest } from 'next/server';

const fetchMock = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('fetch', fetchMock);
});

describe('Public Stores Paginated BFF Endpoint', () => {
  it('returns paginated slice of stores from upstream', async () => {
    const mockStores = Array.from({ length: 12 }).map((_, i) => ({
      id: `store-${i + 1}`,
      name: `Tienda ${i + 1}`,
      slug: `tienda-${i + 1}`,
      status: 'ACTIVE',
    }));

    fetchMock.mockResolvedValue(new Response(JSON.stringify(mockStores), { status: 200 }));

    const req = new NextRequest('http://localhost:3000/api/stores?page=2&pageSize=9');
    const response = await GET(req);
    expect(response.status).toBe(200);

    const body = await response.json();
    expect(body.total).toBe(12);
    expect(body.page).toBe(2);
    expect(body.pageSize).toBe(9);
    expect(body.totalPages).toBe(2);
    expect(body.items).toHaveLength(3);
    expect(body.items[0].id).toBe('store-10');
  });

  it('returns 503 when upstream fails', async () => {
    fetchMock.mockRejectedValue(new Error('Network down'));

    const req = new NextRequest('http://localhost:3000/api/stores');
    const response = await GET(req);
    expect(response.status).toBe(503);
  });
});
