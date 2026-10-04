/**
 * Application environment configuration
 */

export const env = {
  apiUrl:
    process.env.NEXT_PUBLIC_API_URL ||
    (process.env.NODE_ENV === "production"
      ? "https://jaldishop-api.onrender.com/api/v1"
      : "http://localhost:8080/api/v1"),
  merchantUrl:
    process.env.NEXT_PUBLIC_MERCHANT_URL ||
    "https://negocios-jaldishop.pages.dev",
  siteUrl:
    process.env.NEXT_PUBLIC_SITE_URL ||
    (process.env.NODE_ENV === "production"
      ? "https://www.jaldishop.net"
      : "http://localhost:3000"),
  appName: "JaldiShop",
  appDescription:
    "Plataforma de gestión de pedidos y control inteligente de capacidad para MYPE.",
};
