/**
 * Application environment configuration
 */

export const env = {
  apiUrl: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1',
  merchantUrl: process.env.NEXT_PUBLIC_MERCHANT_URL || 'https://negocios-jaldishop.pages.dev',
  siteUrl: process.env.NEXT_PUBLIC_SITE_URL || 'http://localhost:3000',
  appName: 'JaldiShop',
  appDescription: 'Plataforma de gestión de pedidos y control inteligente de capacidad para MYPE.',
};
