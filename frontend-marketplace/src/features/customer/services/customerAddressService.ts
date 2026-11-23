import {
  CustomerAddress,
  CustomerAddressListResponse,
  CreateCustomerAddressPayload,
  UpdateCustomerAddressPayload,
} from '../types/customer.types';

function normalizeCustomerAddress(raw: CustomerAddress): CustomerAddress {
  const street = raw.streetAddress || raw.addressLine || '';
  return {
    ...raw,
    streetAddress: street,
    addressLine: street,
  };
}

function normalizePayload<T extends CreateCustomerAddressPayload | UpdateCustomerAddressPayload>(payload: T): T {
  const street = payload.streetAddress || payload.addressLine || '';
  return {
    ...payload,
    streetAddress: street,
    addressLine: street,
  };
}

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

    const data = await res.json();
    return {
      total: data.total ?? (Array.isArray(data.addresses) ? data.addresses.length : 0),
      maxAllowed: data.maxAllowed ?? 5,
      addresses: Array.isArray(data.addresses) ? data.addresses.map(normalizeCustomerAddress) : [],
    };
  },

  async createAddress(payload: CreateCustomerAddressPayload): Promise<CustomerAddress> {
    const res = await fetch('/api/customer/addresses', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(normalizePayload(payload)),
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No fue posible registrar la dirección.');
    }

    const raw = await res.json();
    return normalizeCustomerAddress(raw);
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
      body: JSON.stringify(normalizePayload(payload)),
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No fue posible actualizar la dirección.');
    }

    const raw = await res.json();
    return normalizeCustomerAddress(raw);
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

    const raw = await res.json();
    return normalizeCustomerAddress(raw);
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
