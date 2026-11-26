import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { PeruPhoneInput } from './PeruPhoneInput';

describe('PeruPhoneInput', () => {
  it('renders +51 prefix and strips +51 when user pastes full international number', () => {
    const onChange = vi.fn();
    render(
      <div>
        <label htmlFor="tel">Celular</label>
        <PeruPhoneInput id="tel" value="+51987654321" onChange={onChange} />
      </div>
    );

    expect(screen.getByText('+51')).toBeInTheDocument();
    const input = screen.getByLabelText('Celular');
    expect(input).toHaveValue('987654321');

    fireEvent.change(input, { target: { value: '+51 912 345 678' } });
    expect(onChange).toHaveBeenCalledWith('912345678');
  });

  it('displays accessible error message when error prop is provided', () => {
    render(
      <div>
        <label htmlFor="tel">Celular</label>
        <PeruPhoneInput
          id="tel"
          value="123"
          onChange={() => {}}
          error="Número inválido"
        />
      </div>
    );

    const input = screen.getByLabelText('Celular');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('alert')).toHaveTextContent('Número inválido');
  });
});
