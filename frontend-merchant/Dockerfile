FROM node:20-alpine AS build

WORKDIR /app

# Optimización de caché de capas: copiar solo manifiestos de paquetes
COPY package*.json ./

# Instalación limpia y determinista de dependencias
RUN npm ci

# Copiar el código fuente del proyecto
COPY . .

# Compilación optimizada para producción
RUN npm run build

FROM nginx:1.27-alpine AS production

# Copiar configuración personalizada de Nginx para SPA (Angular)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar los archivos estáticos generados en la etapa de build
COPY --from=build /app/dist/frontend-merchant/browser /usr/share/nginx/html

# Exponer el puerto HTTP estándar
EXPOSE 80

# Comando para iniciar Nginx en primer plano
CMD ["nginx", "-g", "daemon off;"]
