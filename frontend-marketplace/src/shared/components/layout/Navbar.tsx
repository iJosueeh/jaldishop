'use client';

import React, { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { motion, AnimatePresence } from 'framer-motion';
import { Menu, X, ArrowUpRight, ArrowRight, HelpCircle, Sliders, ArrowLeftRight, Store, User, ChevronDown, Search } from 'lucide-react';
import { Container } from '@/shared/components/ui/Container';
import { JaldiShopLogo } from '@/shared/components/ui/JaldiShopLogo';
import { NavbarSearch } from './NavbarSearch';
import { MobileSearchModal } from './MobileSearchModal';
import { env } from '@/core/config/env';

export function Navbar() {
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isMobileSearchOpen, setIsMobileSearchOpen] = useState(false);
  const [isAuthOpen, setIsAuthOpen] = useState(false);
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

  return (
    <>
      <header
        className={`fixed top-0 left-0 right-0 z-40 transition-all duration-300 ${
          isScrolled
            ? 'bg-[#141413]/95 backdrop-blur-md shadow-xl shadow-black/40 py-2.5'
            : 'bg-transparent py-4'
        }`}
      >
        <Container size="lg">
          <div className="flex items-center justify-between h-14 sm:h-16 gap-3">
            {/* Brand Logo Oficial */}
            <Link href="/" className="group shrink-0">
              <JaldiShopLogo size="md" variant="light" />
            </Link>

            {/* Clean Real-time Search Input (Desktop) */}
            <div className="hidden md:flex items-center flex-1 max-w-xs lg:max-w-sm ml-2">
              <NavbarSearch className="w-full" />
            </div>

            {/* Desktop Navigation Links */}
            <nav className="hidden xl:flex items-center gap-6 text-sm font-semibold text-stone-200">
              <Link
                href="#como-funciona"
                className="hover:text-white transition-colors"
              >
                ¿Cómo funciona?
              </Link>
              <Link
                href="#simulador"
                className="hover:text-white transition-colors"
              >
                Simulador
              </Link>
              <Link
                href="#antes-despues"
                className="hover:text-white transition-colors"
              >
                Antes vs Después
              </Link>
              <Link
                href="#tiendas-destacadas"
                className="hover:text-white transition-colors"
              >
                Tiendas
              </Link>
            </nav>

            {/* Unified Role-Based Auth Dropdown (Desktop) */}
            <div className="hidden sm:block relative" ref={dropdownRef}>
              <button
                type="button"
                onClick={() => setIsAuthOpen((prev) => !prev)}
                className="inline-flex items-center justify-center gap-2 px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold text-white bg-gradient-to-r from-[#d9532f] to-[#feae2c] hover:from-[#c24422] hover:to-[#ea9c22] shadow-lg shadow-[#d9532f]/20 hover:shadow-[#d9532f]/35 transition-all duration-200 cursor-pointer"
                aria-expanded={isAuthOpen}
                aria-haspopup="true"
              >
                <User className="w-4 h-4 text-white" />
                <span>Iniciar Sesión</span>
                <ChevronDown
                  className={`w-3.5 h-3.5 text-white/90 transition-transform duration-200 ${
                    isAuthOpen ? 'rotate-180' : ''
                  }`}
                />
              </button>

              {/* Role Selection Dropdown Menu */}
              <AnimatePresence>
                {isAuthOpen && (
                  <motion.div
                    initial={{ opacity: 0, y: 8, scale: 0.96 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 8, scale: 0.96 }}
                    transition={{ duration: 0.16, ease: 'easeOut' }}
                    className="absolute right-0 top-full mt-2.5 w-80 rounded-2xl bg-[#121211]/98 backdrop-blur-2xl border border-white/12 shadow-2xl shadow-black/90 overflow-hidden z-50 text-white"
                  >
                    {/* Top Ambient Gradient Line */}
                    <div className="h-[2px] w-full bg-gradient-to-r from-[#d9532f] via-[#feae2c] to-[#005141]" />

                    <div className="p-2">
                      <div className="px-3 py-1.5 flex items-center justify-between text-[11px] font-semibold text-stone-400 border-b border-white/5 mb-1.5 pb-1">
                        <span>¿Cómo deseas ingresar?</span>
                        <span className="text-[10px] text-stone-300 bg-white/10 px-2 py-0.5 rounded-full">
                          Acceso Seguro
                        </span>
                      </div>

                      <div className="space-y-1.5">
                        {/* Cliente Option */}
                        <Link
                          href="/tienda/panaderia-don-pepe"
                          onClick={() => setIsAuthOpen(false)}
                          className="flex items-start gap-3 p-3 rounded-xl border border-white/5 hover:border-emerald-500/30 bg-white/[0.02] hover:bg-emerald-950/20 transition-all group cursor-pointer"
                        >
                          <div className="w-9 h-9 rounded-xl bg-emerald-500/15 border border-emerald-500/25 flex items-center justify-center shrink-0 mt-0.5 group-hover:scale-105 transition-transform">
                            <User className="w-4 h-4 text-emerald-400" />
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between">
                              <div className="flex items-center gap-1.5">
                                <span className="text-xs font-bold text-stone-100 group-hover:text-emerald-300 transition-colors">
                                  Soy Cliente
                                </span>
                                <span className="text-[10px] px-1.5 py-0.2 rounded bg-emerald-500/20 text-emerald-300 font-semibold">
                                  Comprador
                                </span>
                              </div>
                              <ArrowRight className="w-3.5 h-3.5 text-stone-600 group-hover:text-emerald-400 group-hover:translate-x-0.5 transition-all" />
                            </div>
                            <p className="text-[11px] text-stone-400 mt-1 leading-relaxed">
                              Sigue el estado de tus pedidos en vivo y explora tiendas locales.
                            </p>
                          </div>
                        </Link>

                        {/* Comerciante Option */}
                        <a
                          href={env.merchantUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          onClick={() => setIsAuthOpen(false)}
                          className="flex items-start gap-3 p-3 rounded-xl border border-white/5 hover:border-[#d9532f]/30 bg-white/[0.02] hover:bg-orange-950/20 transition-all group cursor-pointer"
                        >
                          <div className="w-9 h-9 rounded-xl bg-[#d9532f]/15 border border-[#d9532f]/25 flex items-center justify-center shrink-0 mt-0.5 group-hover:scale-105 transition-transform">
                            <Store className="w-4 h-4 text-[#feae2c]" />
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between">
                              <div className="flex items-center gap-1.5">
                                <span className="text-xs font-bold text-stone-100 group-hover:text-[#feae2c] transition-colors">
                                  Soy Comerciante
                                </span>
                                <span className="text-[10px] px-1.5 py-0.2 rounded bg-[#d9532f]/20 text-[#feae2c] font-semibold">
                                  Negocio ↗
                                </span>
                              </div>
                              <ArrowUpRight className="w-3.5 h-3.5 text-stone-600 group-hover:text-[#feae2c] group-hover:translate-x-0.5 transition-all" />
                            </div>
                            <p className="text-[11px] text-stone-400 mt-1 leading-relaxed">
                              Gestiona tus límites de capacidad, despachos y catálogo de productos.
                            </p>
                          </div>
                        </a>
                      </div>

                      {/* Footer */}
                      <div className="mt-2 pt-2 border-t border-white/5 px-2 flex items-center justify-between text-[11px]">
                        <span className="text-stone-400">¿Tienes un comercio local?</span>
                        <a
                          href={env.merchantUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          onClick={() => setIsAuthOpen(false)}
                          className="font-semibold text-[#feae2c] hover:text-[#ffd276] transition-colors"
                        >
                          Únete como vendedor →
                        </a>
                      </div>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            {/* Mobile Action Triggers (Search & Menu) */}
            <div className="flex items-center gap-1.5 md:hidden">
              <button
                type="button"
                onClick={() => setIsMobileSearchOpen(true)}
                className="p-2.5 rounded-2xl transition-all text-white hover:bg-white/15 bg-white/10 backdrop-blur-md cursor-pointer"
                aria-label="Buscar tiendas o productos"
              >
                <Search className="w-5 h-5 text-white" />
              </button>
              <button
                type="button"
                onClick={() => setIsMobileMenuOpen(true)}
                className="p-2.5 rounded-2xl transition-all text-white hover:bg-white/15 bg-white/10 backdrop-blur-md cursor-pointer"
                aria-label="Abrir Menú"
              >
                <Menu className="w-5 h-5 text-white" />
              </button>
            </div>
          </div>
        </Container>
      </header>

      {/* Option 3: Modern Side-over Drawer with Backdrop Blur */}
      <AnimatePresence>
        {isMobileMenuOpen && (
          <div className="fixed inset-0 z-50 md:hidden flex justify-end">
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
              className="relative z-10 w-[85vw] max-w-sm h-full bg-[#0a0a09]/98 backdrop-blur-2xl border-l border-white/10 shadow-2xl shadow-black/90 flex flex-col justify-between overflow-y-auto text-white"
            >
              {/* Drawer Top Header */}
              <div className="flex items-center justify-between p-5 border-b border-white/10 shrink-0">
                <Link href="/" onClick={() => setIsMobileMenuOpen(false)}>
                  <JaldiShopLogo size="sm" variant="light" />
                </Link>
                <button
                  onClick={() => setIsMobileMenuOpen(false)}
                  className="p-2 rounded-xl bg-white/10 hover:bg-white/15 text-white transition-colors cursor-pointer"
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
                  className="w-full flex items-center justify-between px-3.5 py-2.5 rounded-2xl bg-white/[0.08] hover:bg-white/15 border border-white/12 text-stone-300 hover:text-white transition-all text-xs font-semibold cursor-pointer"
                >
                  <div className="flex items-center gap-2.5">
                    <Search className="w-4 h-4 text-[#feae2c]" />
                    <span>Buscar tiendas o productos...</span>
                  </div>
                  <ArrowRight className="w-3.5 h-3.5 text-stone-500" />
                </button>

                {/* Vertical Navigation Links */}
                <nav className="flex flex-col space-y-1.5 pt-1">
                  {[
                    {
                      href: '#como-funciona',
                      icon: <HelpCircle className="w-4 h-4 text-emerald-400" />,
                      title: '¿Cómo funciona?',
                    },
                    {
                      href: '#simulador',
                      icon: <Sliders className="w-4 h-4 text-[#feae2c]" />,
                      title: 'Simulador de Capacidad',
                    },
                    {
                      href: '#antes-despues',
                      icon: <ArrowLeftRight className="w-4 h-4 text-orange-400" />,
                      title: 'Antes vs Después',
                    },
                    {
                      href: '#tiendas-destacadas',
                      icon: <Store className="w-4 h-4 text-emerald-400" />,
                      title: 'Tiendas Populares',
                    },
                  ].map((item) => (
                    <Link
                      key={item.href}
                      href={item.href}
                      onClick={() => setIsMobileMenuOpen(false)}
                      className="flex items-center justify-between p-3 rounded-2xl bg-white/[0.03] hover:bg-white/10 hover:text-white transition-all text-stone-200 text-sm font-bold group"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-xl bg-white/5 flex items-center justify-center">
                          {item.icon}
                        </div>
                        <span>{item.title}</span>
                      </div>
                      <ArrowRight className="w-4 h-4 text-stone-500 group-hover:text-[#feae2c] group-hover:translate-x-0.5 transition-all" />
                    </Link>
                  ))}
                </nav>
              </div>

              {/* Drawer Footer Auth Actions */}
              <div className="p-5 border-t border-white/10 bg-black/40 flex flex-col gap-2 shrink-0">
                <div className="text-[11px] font-bold tracking-wider text-stone-400 uppercase px-1 pb-1">
                  Acceso a la plataforma
                </div>

                {/* Cliente Action */}
                <Link
                  href="/tienda/panaderia-don-pepe"
                  onClick={() => setIsMobileMenuOpen(false)}
                  className="flex items-center justify-between p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-white hover:bg-emerald-500/15 transition-all group"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-emerald-500/20 flex items-center justify-center">
                      <User className="w-4 h-4 text-emerald-400" />
                    </div>
                    <div className="text-left">
                      <div className="text-xs font-bold text-emerald-300">Soy Cliente</div>
                      <div className="text-[11px] text-stone-300">Mis pedidos y compras</div>
                    </div>
                  </div>
                  <ArrowRight className="w-4 h-4 text-emerald-400 group-hover:translate-x-0.5 transition-transform" />
                </Link>

                {/* Comerciante Action */}
                <a
                  href={env.merchantUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center justify-between p-3 rounded-xl bg-[#d9532f]/10 border border-[#d9532f]/20 text-white hover:bg-[#d9532f]/15 transition-all group"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-[#d9532f]/20 flex items-center justify-center">
                      <Store className="w-4 h-4 text-[#feae2c]" />
                    </div>
                    <div className="text-left">
                      <div className="text-xs font-bold text-[#feae2c]">Soy Comerciante</div>
                      <div className="text-[11px] text-stone-300">Panel y gestión de ventas</div>
                    </div>
                  </div>
                  <ArrowUpRight className="w-4 h-4 text-[#feae2c] group-hover:translate-x-0.5 transition-transform" />
                </a>
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
