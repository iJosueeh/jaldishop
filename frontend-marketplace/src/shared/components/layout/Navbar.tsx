'use client';

import React, { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Menu,
  X,
  ArrowUpRight,
  ArrowRight,
  HelpCircle,
  Sliders,
  ArrowLeftRight,
  Store,
  User,
  ChevronDown,
  Search,
  LogOut,
  Package,
  LayoutDashboard,
  UserCheck,
  Heart,
} from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { JaldiShopLogo } from '@/shared/components/ui/JaldiShopLogo';
import { NavbarSearch } from './NavbarSearch';
import { MobileSearchModal } from './MobileSearchModal';
import { env } from '@/core/config/env';
import { StorefrontCart } from '@/features/storefront/cart/StorefrontCart';
import { useCustomerSession } from '@/features/customer/hooks/useCustomerSession';

export function Navbar() {
  const pathname = usePathname();
  const isHome = pathname === '/';
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isMobileSearchOpen, setIsMobileSearchOpen] = useState(false);
  const [isAuthOpen, setIsAuthOpen] = useState(false);

  const { isAuthenticated, user, logout } = useCustomerSession();

  const loginHref =
    pathname && pathname !== '/login' && pathname !== '/registro'
      ? `/login?redirect=${encodeURIComponent(pathname)}`
      : '/login';
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Handle click outside to close auth dropdown
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent | TouchEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsAuthOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsAuthOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('touchstart', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('touchstart', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  // Lock body scroll when mobile drawer is open
  useEffect(() => {
    if (isMobileMenuOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isMobileMenuOpen]);

  // En las pantallas de autenticación (/login, /registro), no renderizar la barra superior fixed para evitar sobreposiciones
  if (pathname === '/login' || pathname === '/registro') {
    return null;
  }

  const userInitial = user?.firstName ? user.firstName.charAt(0).toUpperCase() : 'C';
  const userDisplayName = user?.firstName || 'Mi JaldiShop';

  return (
    <>
      <header
        className={`fixed top-0 left-0 right-0 z-40 transition-all duration-300 ${
          isHome
            ? isScrolled
              ? 'bg-[#072720]/90 backdrop-blur-md shadow-xl shadow-black/40 py-2.5 border-b border-[#1e2c31]'
              : 'bg-transparent py-4'
            : 'bg-[#072720]/95 backdrop-blur-md shadow-md py-2.5 sm:py-3 border-b border-[#1e2c31]'
        }`}
      >
        <Container size="full" className="max-w-[1440px]">
          <div className="flex items-center h-14 sm:h-16 gap-3 md:gap-6">
            {/* Brand Logo Oficial */}
            <Link href="/" className="group shrink-0">
              <JaldiShopLogo size="md" variant="light" className="max-[360px]:gap-2 max-[360px]:[&_img]:w-7 max-[360px]:[&_span]:text-base" />
            </Link>

            {/* Clean Real-time Search Input (Desktop) */}
            <div className="hidden md:flex min-w-0 flex-1 items-center max-w-sm">
              <NavbarSearch className="w-full" />
            </div>

            {/* Desktop Navigation Links */}
            <nav className="hidden min-[1440px]:flex shrink-0 items-center gap-6 whitespace-nowrap text-sm font-medium text-[#99b3ad]">
              <Link
                href="/#como-funciona"
                className="hover:text-[#36f4a4] transition-colors"
              >
                ¿Cómo funciona?
              </Link>
              <Link
                href="/#caracteristicas"
                className="hover:text-[#36f4a4] transition-colors"
              >
                Herramientas para tu negocio
              </Link>
              <Link
                href="/#antes-despues"
                className="hover:text-[#36f4a4] transition-colors"
              >
                Antes vs Después
              </Link>
              <Link
                href="/#tiendas-destacadas"
                className="hover:text-[#36f4a4] transition-colors"
              >
                Comercios
              </Link>
            </nav>

            <div className="ml-auto flex shrink-0 items-center gap-2 sm:gap-3">
            <div className="shrink-0"><StorefrontCart /></div>

            {/* Role-Based / Customer-Aware Auth Dropdown (Desktop) */}
            <div className="hidden md:block relative" ref={dropdownRef}>
              {isAuthenticated ? (
                /* Authenticated Customer Chip Trigger */
                <button
                  type="button"
                  onClick={() => setIsAuthOpen((prev) => !prev)}
                  className="inline-flex items-center justify-center gap-2 pl-2 pr-3.5 py-1.5 rounded-full text-xs sm:text-sm font-semibold text-white bg-[#061a1c] hover:bg-[#072720] border border-[#1e2c31] hover:border-[#36f4a4]/50 shadow-md shadow-black/30 transition-all duration-200 cursor-pointer"
                  aria-expanded={isAuthOpen}
                  aria-haspopup="true"
                  aria-label="Menú de cuenta de cliente"
                >
                  <div className="w-6 h-6 rounded-full bg-[#005141] text-[#36f4a4] font-bold text-xs flex items-center justify-center border border-[#1e2c31] shrink-0">
                    {userInitial}
                  </div>
                  <span className="max-w-[120px] truncate text-xs font-bold text-stone-100">
                    {userDisplayName}
                  </span>
                  <ChevronDown
                    className={`w-3.5 h-3.5 text-[#99b3ad] transition-transform duration-200 ${
                      isAuthOpen ? 'rotate-180' : ''
                    }`}
                  />
                </button>
              ) : (
                /* Unauthenticated Trigger */
                <button
                  type="button"
                  onClick={() => setIsAuthOpen((prev) => !prev)}
                  className="inline-flex items-center justify-center gap-2 px-5 py-2 rounded-full text-xs sm:text-sm font-semibold text-[#02090a] bg-white hover:bg-stone-100 shadow-md shadow-black/30 hover:scale-[1.02] transition-all duration-200 cursor-pointer"
                  aria-expanded={isAuthOpen}
                  aria-haspopup="true"
                >
                  <User className="w-4 h-4 text-[#02090a]" />
                  <span>Iniciar Sesión</span>
                  <ChevronDown
                    className={`w-3.5 h-3.5 text-[#02090a]/80 transition-transform duration-200 ${
                      isAuthOpen ? 'rotate-180' : ''
                    }`}
                  />
                </button>
              )}

              {/* Role Selection or Customer Menu Dropdown */}
              <AnimatePresence>
                {isAuthOpen && (
                  <motion.div
                    initial={{ opacity: 0, y: 8, scale: 0.96 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 8, scale: 0.96 }}
                    transition={{ duration: 0.16, ease: 'easeOut' }}
                    className={`absolute right-0 top-full mt-2.5 rounded-[14px] bg-white border border-stone-200/90 shadow-2xl shadow-stone-900/15 overflow-hidden z-50 text-stone-900 ${
                      isAuthenticated ? 'w-64' : 'w-84'
                    }`}
                  >
                    {isAuthenticated ? (
                      /* Authenticated Dropdown Menu */
                      <div className="p-2 space-y-1">
                        {/* Header informativo */}
                        <div className="px-3 py-2 bg-stone-50 rounded-xl border border-stone-100 mb-1">
                          <p className="text-xs font-bold text-stone-900 truncate">
                            {user?.firstName ? `${user.firstName} ${user.lastName || ''}`.trim() : 'Cliente JaldiShop'}
                          </p>
                          <p className="text-[11px] text-stone-500 truncate mt-0.5">
                            {user?.email || 'Sesión activa'}
                          </p>
                        </div>

                        {/* Navigation Links */}
                        <div className="space-y-0.5">
                          <Link
                            href="/mi-jaldishop"
                            onClick={() => setIsAuthOpen(false)}
                            className="flex items-center gap-2.5 px-3 py-2 text-xs font-semibold text-stone-700 hover:text-[#005141] hover:bg-emerald-50/60 rounded-lg transition-colors"
                          >
                            <LayoutDashboard className="w-4 h-4 text-[#005141]" />
                            <span>Mi JaldiShop (Inicio)</span>
                          </Link>

                          <Link
                            href="/mi-jaldishop/pedidos"
                            onClick={() => setIsAuthOpen(false)}
                            className="flex items-center gap-2.5 px-3 py-2 text-xs font-semibold text-stone-700 hover:text-[#005141] hover:bg-emerald-50/60 rounded-lg transition-colors"
                          >
                            <Package className="w-4 h-4 text-[#005141]" />
                            <span>Mis Pedidos</span>
                          </Link>

                          <Link
                            href="/mi-jaldishop/favoritos"
                            onClick={() => setIsAuthOpen(false)}
                            className="flex items-center gap-2.5 px-3 py-2 text-xs font-semibold text-stone-700 hover:text-[#005141] hover:bg-emerald-50/60 rounded-lg transition-colors"
                          >
                            <Heart className="w-4 h-4 text-[#005141]" />
                            <span>Mis Favoritos</span>
                          </Link>

                          <Link
                            href="/mi-jaldishop/perfil"
                            onClick={() => setIsAuthOpen(false)}
                            className="flex items-center gap-2.5 px-3 py-2 text-xs font-semibold text-stone-700 hover:text-[#005141] hover:bg-emerald-50/60 rounded-lg transition-colors"
                          >
                            <UserCheck className="w-4 h-4 text-[#005141]" />
                            <span>Datos de mi Cuenta</span>
                          </Link>
                        </div>

                        {/* Logout Separator */}
                        <div className="mt-1 pt-1 border-t border-stone-100">
                          <button
                            type="button"
                            onClick={() => {
                              setIsAuthOpen(false);
                              logout();
                            }}
                            className="w-full flex items-center gap-2.5 px-3 py-2 text-xs font-semibold text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer text-left"
                          >
                            <LogOut className="w-4 h-4 text-rose-500" />
                            <span>Cerrar Sesión</span>
                          </button>
                        </div>
                      </div>
                    ) : (
                      /* Unauthenticated Role Selection */
                      <div className="p-3">
                        <div className="px-2 py-1.5 flex items-center justify-between text-[11px] font-semibold text-stone-500 border-b border-stone-100 mb-2 pb-2">
                          <span className="font-bold text-stone-800">Acceso a JaldiShop</span>
                          <span className="text-[10px] text-[#005141] bg-emerald-50 border border-emerald-200/80 px-2 py-0.5 rounded-full font-bold">
                            Comercios MYPE
                          </span>
                        </div>

                        <div className="space-y-2">
                          {/* Cliente Option */}
                          <Link
                            href={loginHref}
                            onClick={() => setIsAuthOpen(false)}
                            className="flex items-start gap-3 p-3 rounded-[12px] border border-stone-200/80 hover:border-[#005141]/50 bg-stone-50/70 hover:bg-emerald-50/40 transition-all group cursor-pointer"
                          >
                            <div className="w-9 h-9 rounded-[8px] bg-emerald-100/70 text-[#005141] flex items-center justify-center shrink-0 mt-0.5 group-hover:scale-105 transition-transform">
                              <User className="w-4 h-4" />
                            </div>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center justify-between">
                                <span className="text-xs font-bold text-stone-900 group-hover:text-[#005141] transition-colors">
                                  Soy Cliente
                                </span>
                                <ArrowRight className="w-3.5 h-3.5 text-stone-400 group-hover:text-[#005141] group-hover:translate-x-0.5 transition-all" />
                              </div>
                              <p className="text-[11px] text-stone-500 mt-0.5 leading-relaxed">
                                Explora comercios y emprendimientos locales, y realiza tus pedidos en línea.
                              </p>
                            </div>
                          </Link>

                          {/* Comerciante Option */}
                          <a
                            href={env.merchantUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            onClick={() => setIsAuthOpen(false)}
                            className="flex items-start gap-3 p-3 rounded-[12px] border border-stone-200/80 hover:border-[#ea580c]/50 bg-stone-50/70 hover:bg-orange-50/40 transition-all group cursor-pointer"
                          >
                            <div className="w-9 h-9 rounded-[8px] bg-orange-100/70 text-[#ea580c] flex items-center justify-center shrink-0 mt-0.5 group-hover:scale-105 transition-transform">
                              <Store className="w-4 h-4" />
                            </div>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center justify-between">
                                <span className="text-xs font-bold text-stone-900 group-hover:text-[#ea580c] transition-colors">
                                  Soy Comerciante (MYPE)
                                </span>
                                <ArrowUpRight className="w-3.5 h-3.5 text-stone-400 group-hover:text-[#ea580c] group-hover:translate-x-0.5 transition-all" />
                              </div>
                              <p className="text-[11px] text-stone-500 mt-0.5 leading-relaxed">
                                Accede al panel de tu negocio para gestionar cupos de atención, pedidos y catálogo.
                              </p>
                            </div>
                          </a>
                        </div>

                        {/* Footer */}
                        <div className="mt-2.5 pt-2 border-t border-stone-100 px-2 flex items-center justify-between text-[11px]">
                          <span className="text-stone-500">¿Tienes un comercio local?</span>
                          <a
                            href={env.merchantUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            onClick={() => setIsAuthOpen(false)}
                            className="font-bold text-[#005141] hover:underline transition-colors"
                          >
                            Registra tu tienda →
                          </a>
                        </div>
                      </div>
                    )}
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            {/* Mobile Action Triggers (Search & Menu) */}
            <div className="flex items-center gap-2 min-[1440px]:hidden">
              <button
                type="button"
                onClick={() => setIsMobileSearchOpen(true)}
                className="flex h-10 w-10 items-center justify-center rounded-xl border border-white/20 text-white hover:bg-white/10 transition-colors cursor-pointer md:hidden"
                aria-label="Buscar tiendas o productos"
              >
                <Search className="w-5 h-5 text-white" />
              </button>
              <button
                type="button"
                onClick={() => setIsMobileMenuOpen(true)}
                className="flex h-10 w-10 items-center justify-center rounded-xl border border-white/20 text-white hover:bg-white/10 transition-colors cursor-pointer"
                aria-label="Abrir Menú"
              >
                <Menu className="w-5 h-5 text-white" />
              </button>
            </div>
            </div>
          </div>
        </Container>
      </header>

      {/* Modern Side-over Drawer with Backdrop Blur */}
      <AnimatePresence>
        {isMobileMenuOpen && (
          <div className="fixed inset-0 z-50 min-[1440px]:hidden flex justify-end">
            {/* Backdrop Blur Overlay */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
              onClick={() => setIsMobileMenuOpen(false)}
              className="fixed inset-0 bg-black/70 backdrop-blur-sm"
            />

            {/* Slide-over Drawer Panel */}
            <motion.div
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ type: 'spring', damping: 28, stiffness: 280 }}
              className="relative z-10 w-[85vw] max-w-sm h-full bg-white border-l border-stone-200/90 shadow-2xl shadow-stone-900/20 flex flex-col justify-between overflow-y-auto text-stone-900"
            >
              {/* Drawer Top Header */}
              <div className="flex items-center justify-between p-5 border-b border-stone-100 shrink-0">
                <Link href="/" onClick={() => setIsMobileMenuOpen(false)}>
                  <JaldiShopLogo size="sm" variant="dark" />
                </Link>
                <button
                  onClick={() => setIsMobileMenuOpen(false)}
                  className="p-2 rounded-full bg-stone-100 hover:bg-stone-200 text-stone-700 transition-colors cursor-pointer"
                  aria-label="Cerrar menú"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Drawer Content: Search Trigger & Nav Links */}
              <div className="p-5 space-y-5 flex-1 flex flex-col">
                {/* Search Trigger Button in Drawer */}
                <button
                  type="button"
                  onClick={() => {
                    setIsMobileMenuOpen(false);
                    setIsMobileSearchOpen(true);
                  }}
                  className="w-full flex items-center justify-between px-4 py-2.5 rounded-full bg-stone-100 hover:bg-stone-200/80 border border-stone-200/80 text-stone-700 transition-all text-xs font-semibold cursor-pointer shadow-2xs"
                >
                  <div className="flex items-center gap-2.5">
                    <Search className="w-4 h-4 text-[#ea580c]" />
                    <span>Buscar tiendas o productos...</span>
                  </div>
                  <ArrowRight className="w-3.5 h-3.5 text-stone-400" />
                </button>

                {/* Vertical Navigation Links */}
                <nav className="flex flex-col space-y-1.5 pt-1">
                  {[
                    {
                      href: '/#como-funciona',
                      icon: <HelpCircle className="w-4 h-4 text-[#005141]" />,
                      title: '¿Cómo funciona?',
                    },
                    {
                      href: '/#caracteristicas',
                      icon: <Sliders className="w-4 h-4 text-[#ea580c]" />,
                      title: 'Herramientas para tu negocio',
                    },
                    {
                      href: '/#antes-despues',
                      icon: <ArrowLeftRight className="w-4 h-4 text-[#feae2c]" />,
                      title: 'Antes vs Después',
                    },
                    {
                      href: '/#tiendas-destacadas',
                      icon: <Store className="w-4 h-4 text-[#005141]" />,
                      title: 'Comercios Destacados',
                    },
                  ].map((item) => (
                    <Link
                      key={item.href}
                      href={item.href}
                      onClick={() => setIsMobileMenuOpen(false)}
                      className="flex items-center justify-between p-3 rounded-[12px] bg-stone-50/70 hover:bg-stone-100 text-stone-800 transition-all text-sm font-bold border border-stone-200/60 group"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-[8px] bg-white border border-stone-200/80 flex items-center justify-center shadow-2xs">
                          {item.icon}
                        </div>
                        <span>{item.title}</span>
                      </div>
                      <ArrowRight className="w-4 h-4 text-stone-400 group-hover:text-[#005141] group-hover:translate-x-0.5 transition-all" />
                    </Link>
                  ))}
                </nav>
              </div>

              {/* Drawer Footer Auth Actions */}
              <div className="p-5 border-t border-stone-200/90 bg-stone-50/70 flex flex-col gap-2 shrink-0">
                {isAuthenticated ? (
                  /* Authenticated Customer Mobile Profile Block */
                  <div className="space-y-3">
                    <div className="flex items-center gap-3 p-3 bg-white border border-stone-200/90 rounded-xl shadow-2xs">
                      <div className="w-9 h-9 rounded-full bg-[#005141] text-[#36f4a4] font-bold text-sm flex items-center justify-center shrink-0">
                        {userInitial}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="text-xs font-bold text-stone-900 truncate">
                          {user?.firstName ? `${user.firstName} ${user.lastName || ''}`.trim() : 'Cliente JaldiShop'}
                        </div>
                        <div className="text-[11px] text-stone-500 truncate">
                          {user?.email}
                        </div>
                      </div>
                    </div>

                    <div className="grid grid-cols-3 gap-2">
                      <Link
                        href="/mi-jaldishop"
                        onClick={() => setIsMobileMenuOpen(false)}
                        className="flex items-center justify-center gap-1.5 p-2.5 rounded-xl bg-white border border-stone-200 text-xs font-bold text-stone-800 hover:border-[#005141] transition-colors"
                      >
                        <LayoutDashboard className="w-3.5 h-3.5 text-[#005141]" />
                        <span>Inicio</span>
                      </Link>
                      <Link
                        href="/mi-jaldishop/pedidos"
                        onClick={() => setIsMobileMenuOpen(false)}
                        className="flex items-center justify-center gap-1.5 p-2.5 rounded-xl bg-white border border-stone-200 text-xs font-bold text-stone-800 hover:border-[#005141] transition-colors"
                      >
                        <Package className="w-3.5 h-3.5 text-[#005141]" />
                        <span>Pedidos</span>
                      </Link>
                      <Link
                        href="/mi-jaldishop/favoritos"
                        onClick={() => setIsMobileMenuOpen(false)}
                        className="flex items-center justify-center gap-1.5 p-2.5 rounded-xl bg-white border border-stone-200 text-xs font-bold text-stone-800 hover:border-[#005141] transition-colors"
                      >
                        <Heart className="w-3.5 h-3.5 text-[#005141]" />
                        <span>Favoritos</span>
                      </Link>
                    </div>

                    <button
                      type="button"
                      onClick={() => {
                        setIsMobileMenuOpen(false);
                        logout();
                      }}
                      className="w-full flex items-center justify-center gap-2 p-2.5 rounded-xl bg-white border border-rose-200 hover:bg-rose-50 text-xs font-bold text-rose-600 transition-colors cursor-pointer"
                    >
                      <LogOut className="w-3.5 h-3.5 text-rose-500" />
                      <span>Cerrar Sesión</span>
                    </button>
                  </div>
                ) : (
                  /* Unauthenticated Mobile Auth Options */
                  <>
                    <div className="text-[11px] font-bold tracking-wider text-stone-500 uppercase px-1 pb-1">
                      Acceso a la plataforma
                    </div>

                    {/* Cliente Action */}
                    <Link
                      href={loginHref}
                      onClick={() => setIsMobileMenuOpen(false)}
                      className="flex items-center justify-between p-3 rounded-[12px] bg-white border border-stone-200/90 hover:border-[#005141]/50 text-stone-900 transition-all shadow-2xs group"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-[8px] bg-emerald-100/70 text-[#005141] flex items-center justify-center">
                          <User className="w-4 h-4" />
                        </div>
                        <div className="text-left">
                          <div className="text-xs font-bold text-stone-900 group-hover:text-[#005141] transition-colors">Soy Cliente</div>
                          <div className="text-[11px] text-stone-500">Explorar comercios y emprendimientos</div>
                        </div>
                      </div>
                      <ArrowRight className="w-4 h-4 text-stone-400 group-hover:text-[#005141] group-hover:translate-x-0.5 transition-transform" />
                    </Link>

                    {/* Comerciante Action */}
                    <a
                      href={env.merchantUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex items-center justify-between p-3 rounded-[12px] bg-white border border-stone-200/90 hover:border-[#ea580c]/50 text-stone-900 transition-all shadow-2xs group"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-[8px] bg-orange-100/70 text-[#ea580c] flex items-center justify-center">
                          <Store className="w-4 h-4" />
                        </div>
                        <div className="text-left">
                          <div className="text-xs font-bold text-stone-900 group-hover:text-[#ea580c] transition-colors">Soy Comerciante (MYPE)</div>
                          <div className="text-[11px] text-stone-500">Panel de gestión de pedidos y cupos</div>
                        </div>
                      </div>
                      <ArrowUpRight className="w-4 h-4 text-stone-400 group-hover:text-[#ea580c] group-hover:translate-x-0.5 transition-transform" />
                    </a>
                  </>
                )}
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Fullscreen Mobile Search Overlay (X / Twitter style) */}
      <MobileSearchModal
        isOpen={isMobileSearchOpen}
        onClose={() => setIsMobileSearchOpen(false)}
      />
    </>
  );
}
