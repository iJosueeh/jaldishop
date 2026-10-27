import {
  CustomerAddress,
  CustomerAddressListResponse,
  CreateCustomerAddressPayload,
  UpdateCustomerAddressPayload,
} from '../types/customer.types';

export const customerAddressService = {
  async getAddresses(): Promise<CustomerAddressListResponse> {
    const res = await fetch('/api/customer/addresses', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      cache: 'no-store',
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No fue posible cargar tus direcciones.');
    }

    return res.json();
  },

  async createAddress(payload: CreateCustomerAddressPayload): Promise<CustomerAddress> {
    const res = await fetch('/api/customer/addresses', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No fue posible registrar la dirección.');
    }

    return res.json();
  },

  async updateAddress(
    id: string,
    payload: UpdateCustomerAddressPayload
  ): Promise<CustomerAddress> {
    const res = await fetch(`/api/customer/addresses/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No fue posible actualizar la dirección.');
    }

    return res.json();
  },

  async setDefaultAddress(id: string): Promise<CustomerAddress> {
    const res = await fetch(`/api/customer/addresses/${encodeURIComponent(id)}/default`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(
        error.message || 'No fue posible cambiar la dirección predeterminada.'
      );
    }

    return res.json();
  },

  async deleteAddress(id: string): Promise<void> {
    const res = await fetch(`/api/customer/addresses/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No fue posible eliminar la dirección.');
    }
  },
};
