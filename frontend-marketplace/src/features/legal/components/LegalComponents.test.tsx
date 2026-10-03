import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { LegalTldrCard } from './LegalTldrCard';
import { LegalPrintButton } from './LegalPrintButton';
import { LegalSection } from './LegalSection';
import { LegalTableOfContents } from './LegalTableOfContents';
import { LegalLayout } from './LegalLayout';
import { TERMS_DOCUMENT } from '../constants/termsContent';

describe('Legal Visual Components (Paso 2)', () => {
  it('renders LegalTldrCard with title, summary, and key takeaway points', () => {
    const mockTldr = {
      title: 'Resumen de prueba',
      summary: 'Esta es una explicación clara sin jerga.',
      keyPoints: ['Punto 1', 'Punto 2'],
    };

    render(<LegalTldrCard tldr={mockTldr} />);

    expect(screen.getByText('En pocas palabras (Resumen)')).toBeInTheDocument();
    expect(screen.getByText('Resumen de prueba')).toBeInTheDocument();
    expect(screen.getByText('Esta es una explicación clara sin jerga.')).toBeInTheDocument();
    expect(screen.getByText('Punto 1')).toBeInTheDocument();
    expect(screen.getByText('Punto 2')).toBeInTheDocument();
  });

  it('renders LegalPrintButton and triggers window.print when clicked', () => {
    const printSpy = vi.fn();
    window.print = printSpy;

    render(<LegalPrintButton />);

    const button = screen.getByLabelText('Imprimir o guardar en PDF este documento legal');
    expect(button).toBeInTheDocument();

    fireEvent.click(button);
    expect(printSpy).toHaveBeenCalledTimes(1);
  });

  it('renders LegalSection with clause title, TLDR, and callouts/tables if present', () => {
    const sampleClause = TERMS_DOCUMENT.clauses[0];
    render(<LegalSection clause={sampleClause} index={0} />);

    expect(screen.getByText(sampleClause.title)).toBeInTheDocument();
    expect(screen.getByText(sampleClause.tldr.title)).toBeInTheDocument();
  });

  it('renders LegalTableOfContents with all clause links and fires onSelectClause', () => {
    class MockIntersectionObserver {
      observe = vi.fn();
      unobserve = vi.fn();
      disconnect = vi.fn();
    }
    window.IntersectionObserver = MockIntersectionObserver as unknown as typeof IntersectionObserver;

    const onSelectClauseMock = vi.fn();

    render(
      <LegalTableOfContents
        clauses={TERMS_DOCUMENT.clauses}
        activeClauseIndex={0}
        onSelectClause={onSelectClauseMock}
        viewMode="tabs"
      />
    );

    expect(screen.getByText('Tabla de Contenidos')).toBeInTheDocument();
    TERMS_DOCUMENT.clauses.forEach((c) => {
      const titleToFind = c.shortTitle || c.title;
      const elements = screen.getAllByText(titleToFind);
      expect(elements.length).toBeGreaterThanOrEqual(1);
    });

    // Clicking the second clause triggers onSelectClause with index 1
    const secondClauseTitle = TERMS_DOCUMENT.clauses[1].shortTitle || TERMS_DOCUMENT.clauses[1].title;
    const clauseButtons = screen.getAllByText(secondClauseTitle);
    fireEvent.click(clauseButtons[0]);
    expect(onSelectClauseMock).toHaveBeenCalledWith(1);
  });

  it('renders LegalLayout with tab navigation, progress indicator, and next/previous controls', () => {
    class MockIntersectionObserver {
      observe = vi.fn();
      unobserve = vi.fn();
      disconnect = vi.fn();
    }
    window.IntersectionObserver = MockIntersectionObserver as unknown as typeof IntersectionObserver;
    window.scrollTo = vi.fn();

    render(<LegalLayout document={TERMS_DOCUMENT} />);

    // Starts on Clause 1 (appears in top indicator and bottom toolbar)
    expect(screen.getAllByText(/Cláusula 1 de 7/i)[0]).toBeInTheDocument();
    expect(screen.getByText('Por Cláusulas')).toBeInTheDocument();
    expect(screen.getByText('Ver Completo')).toBeInTheDocument();

    // The first clause title should be rendered
    expect(screen.getAllByText(TERMS_DOCUMENT.clauses[0].title)[0]).toBeInTheDocument();

    // Advance to next clause
    const nextButton = screen.getByText('Siguiente').closest('button');
    expect(nextButton).toBeInTheDocument();
    expect(nextButton).not.toBeDisabled();

    fireEvent.click(nextButton!);

    // Should now display Clause 2
    expect(screen.getAllByText(/Cláusula 2 de 7/i)[0]).toBeInTheDocument();

    // Switch to continuous document mode
    const continuousButton = screen.getByLabelText('Ver documento completo continuo');
    fireEvent.click(continuousButton);

    expect(screen.getByText('Documento Completo')).toBeInTheDocument();
    expect(screen.getByText('Volver al inicio del documento')).toBeInTheDocument();
  });
});
