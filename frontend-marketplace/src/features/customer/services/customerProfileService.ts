import { apiClient } from '@/core/api/apiClient';
import { UserProfile, UpdateProfilePayload } from '../types/customer.types';

export const customerProfileService = {
  /**
   * Fetches customer profile from BFF endpoint
   */
  getProfile: () =>
    apiClient<UserProfile>('/api/customer/profile', {
      baseUrl: '',
      cache: 'no-store',
    }),

  /**
   * Updates customer profile fields (firstName, lastName, phone)
   */
  updateProfile: (data: UpdateProfilePayload) =>
    apiClient<UserProfile>('/api/customer/profile', {
      baseUrl: '',
      method: 'PUT',
      body: JSON.stringify(data),
    }),
};
