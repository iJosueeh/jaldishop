'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { Search, Sparkles, Menu, X, ArrowUpRight } from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { Container } from '@/shared/components/ui/Container';
import { JaldiShopLogo } from '@/shared/components/ui/JaldiShopLogo';
import { CommandSearchModal } from './CommandSearchModal';
import { env } from '@/core/config/env';

export function Navbar() {
  const [isScrolled, setIsScrolled] = useState(false);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };

    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setIsSearchOpen((prev) => !prev);
      }
    };

    window.addEventListener('scroll', handleScroll);
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      window.removeEventListener('scroll', handleScroll);
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  return (
    <>
      <header
        className={`fixed top-0 left-0 right-0 z-40 transition-all duration-300 ${
          isScrolled
            ? 'bg-[#faf7f2]/95 backdrop-blur-md border-b border-[#e7e0d6] shadow-sm shadow-[#1c1917]/5 py-2'
            : 'bg-black/20 backdrop-blur-sm border-b border-white/10 py-3.5'
        }`}
      >
        <Container size="lg">
          <div className="flex items-center justify-between h-14 sm:h-16">
            {/* Brand Logo Oficial */}
            <Link href="/" className="group">
              <JaldiShopLogo size="md" variant={isScrolled ? 'dark' : 'light'} />
            </Link>

            {/* Quick Search Trigger (⌘K) */}
            <div className="hidden md:flex items-center">
              <button
                onClick={() => setIsSearchOpen(true)}
                className={`flex items-center gap-3 px-4 py-2 text-xs rounded-2xl transition-all shadow-xs cursor-pointer w-72 justify-between ${
                  isScrolled
                    ? 'text-[#57534e] bg-white/90 hover:bg-white border border-[#e7e0d6] hover:border-[#a8a29e]'
                    : 'text-stone-200 bg-white/10 hover:bg-white/15 border border-white/20 backdrop-blur-md'
                }`}
              >
                <div className="flex items-center gap-2">
                  <Search className={`w-3.5 h-3.5 ${isScrolled ? 'text-[#a8a29e]' : 'text-stone-300'}`} />
                  <span className="font-medium">Buscar panadería, sushi, pastelería...</span>
                </div>
                <kbd
                  className={`px-2 py-0.5 text-[10px] font-mono rounded-md font-semibold border ${
                    isScrolled
                      ? 'bg-[#f7f3ec] border-[#e7e0d6] text-[#57534e]'
                      : 'bg-white/10 border-white/20 text-stone-200'
                  }`}
                >
                  ⌘K
                </kbd>
              </button>
            </div>

            {/* Desktop Navigation Links */}
            <nav
              className={`hidden lg:flex items-center gap-6 text-sm font-semibold transition-colors ${
                isScrolled ? 'text-[#57534e]' : 'text-stone-200'
              }`}
            >
              <Link
                href="#como-funciona"
                className={`transition-colors ${isScrolled ? 'hover:text-[#005141]' : 'hover:text-white'}`}
              >
                ¿Cómo funciona?
              </Link>
              <Link
                href="#simulador"
                className={`transition-colors ${isScrolled ? 'hover:text-[#005141]' : 'hover:text-white'}`}
              >
                Simulador
              </Link>
              <Link
                href="#antes-despues"
                className={`transition-colors ${isScrolled ? 'hover:text-[#005141]' : 'hover:text-white'}`}
              >
                Antes vs Después
              </Link>
              <Link
                href="#tiendas-destacadas"
                className={`transition-colors ${isScrolled ? 'hover:text-[#005141]' : 'hover:text-white'}`}
              >
                Tiendas
              </Link>
            </nav>

            {/* Merchant Portal & Demo CTAs */}
            <div className="hidden sm:flex items-center gap-2.5">
              <a
                href={env.merchantUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                <Button
                  variant="outline"
                  size="sm"
                  className={
                    !isScrolled
                      ? 'bg-white/10 text-white border-white/30 hover:bg-white/20'
                      : ''
                  }
                  rightIcon={<ArrowUpRight className={`w-3.5 h-3.5 ${isScrolled ? 'text-[#57534e]' : 'text-stone-200'}`} />}
                >
                  Soy Comerciante
                </Button>
              </a>
              <Link href="/tienda/panaderia-don-pepe">
                <Button variant="terracotta" size="sm" leftIcon={<Sparkles className="w-3.5 h-3.5" />}>
                  Probar Demo
                </Button>
              </Link>
            </div>

            {/* Mobile Menu & Search Actions */}
            <div className="flex items-center gap-2 md:hidden">
              <button
                onClick={() => setIsSearchOpen(true)}
                className={`p-2.5 rounded-2xl border transition-all ${
                  isScrolled
                    ? 'text-[#1c1917] hover:bg-[#f7f3ec] border-[#e7e0d6]'
                    : 'text-white hover:bg-white/10 border-white/20'
                }`}
                aria-label="Buscar"
              >
                <Search className="w-5 h-5" />
              </button>
              <button
                onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                className={`p-2.5 rounded-2xl border transition-all ${
                  isScrolled
                    ? 'text-[#1c1917] hover:bg-[#f7f3ec] border-[#e7e0d6]'
                    : 'text-white hover:bg-white/10 border-white/20'
                }`}
                aria-label="Menú"
              >
                {isMobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
              </button>
            </div>
          </div>
        </Container>


        {/* Mobile menu dropdown */}
        {isMobileMenuOpen && (
          <div className="md:hidden bg-[#faf7f2] border-b border-[#e7e0d6] px-6 py-6 space-y-4 shadow-xl animate-in slide-in-from-top-4 duration-200">
            <nav className="flex flex-col space-y-3.5 text-base font-bold text-[#1c1917]">
              <Link
                href="#como-funciona"
                onClick={() => setIsMobileMenuOpen(false)}
                className="hover:text-[#005141]"
              >
                ¿Cómo funciona?
              </Link>
              <Link
                href="#antes-despues"
                onClick={() => setIsMobileMenuOpen(false)}
                className="hover:text-[#005141]"
              >
                Antes vs Después (El Secreto de JaldiShop)
              </Link>
              <Link
                href="#tiendas-destacadas"
                onClick={() => setIsMobileMenuOpen(false)}
                className="hover:text-[#005141]"
              >
                Tiendas Populares
              </Link>
              <Link
                href="#beneficios"
                onClick={() => setIsMobileMenuOpen(false)}
                className="hover:text-[#005141]"
              >
                Beneficios MYPE
              </Link>
            </nav>
            <div className="pt-4 border-t border-[#e7e0d6] flex flex-col gap-3">
              <a
                href={env.merchantUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="w-full"
              >
                <Button variant="outline" size="md" className="w-full justify-center">
                  Panel de Comerciante (Angular)
                </Button>
              </a>
              <Link href="/tienda/panaderia-don-pepe" onClick={() => setIsMobileMenuOpen(false)}>
                <Button variant="terracotta" size="md" className="w-full justify-center">
                  Probar Tienda Demo
                </Button>
              </Link>
            </div>
          </div>
        )}
      </header>

      {/* Global Command Search Modal */}
      <CommandSearchModal
        isOpen={isSearchOpen}
        onClose={() => setIsSearchOpen(false)}
      />
    </>
  );
}
