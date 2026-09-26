import { TestBed } from '@angular/core/testing';
import { CheckoutReceipt, Currency } from '../../../../generated/api';
import { Receipt } from './receipt';

describe('Receipt', () => {
  const receipt: CheckoutReceipt = {
    currency: Currency.Eur,
    items: [
      {
        productId: 'APPLE',
        name: 'Apple',
        quantity: 3,
        unitPrice: '0.30',
        subtotal: '0.90',
        discount: '0.15',
        total: '0.75',
        appliedOffer: { quantity: 2, price: '0.45', applications: 1 },
      },
    ],
    subtotal: '0.90',
    discount: '0.15',
    total: '0.75',
  };

  async function render(value: CheckoutReceipt): Promise<HTMLElement> {
    await TestBed.configureTestingModule({ imports: [Receipt] }).compileComponents();
    const fixture = TestBed.createComponent(Receipt);
    fixture.componentRef.setInput('receipt', value);
    fixture.detectChanges();
    return fixture.nativeElement;
  }

  it('renders item quantities, unit prices, applied offers and server totals', async () => {
    const element = await render(receipt);
    const line = element.querySelector('li');
    expect(line?.textContent).toContain('Apple');
    expect(line?.textContent).toContain('3 × EUR 0.30');
    expect(line?.textContent).toContain('2 for EUR 0.45');
    expect(line?.textContent).toContain('applied 1 time');
    expect(line?.textContent).toContain('EUR 0.90');
    expect(line?.textContent).toContain('EUR 0.15');
    expect(line?.textContent).toContain('EUR 0.75');
    const totals = element.querySelector('dl[aria-label="Receipt totals"]');
    expect(totals?.textContent).toContain('Subtotal');
    expect(totals?.textContent).toContain('Savings');
    expect(totals?.textContent).toContain('Total');
    expect(
      Array.from(totals?.querySelectorAll('dd') ?? [], (item) => item.textContent?.trim()),
    ).toEqual(['EUR 0.90', 'EUR 0.15', 'EUR 0.75']);
  });

  it('renders an empty receipt with exact zero totals', async () => {
    const element = await render({
      currency: Currency.Eur,
      items: [],
      subtotal: '0.00',
      discount: '0.00',
      total: '0.00',
    });
    expect(element.textContent).toContain('No items in this receipt');
    expect(element.querySelectorAll('li')).toHaveLength(0);
    expect(Array.from(element.querySelectorAll('dd'), (item) => item.textContent?.trim())).toEqual([
      'EUR 0.00',
      'EUR 0.00',
      'EUR 0.00',
    ]);
  });

  it('preserves large decimal strings and omits offer text when no offer was applied', async () => {
    const amount = '9007199254740993.01';
    const element = await render({
      currency: Currency.Eur,
      items: [
        {
          productId: 'SPECIAL',
          name: 'Special',
          quantity: 1,
          unitPrice: amount,
          subtotal: amount,
          discount: '0.00',
          total: amount,
        },
      ],
      subtotal: amount,
      discount: '0.00',
      total: amount,
    });
    expect(element.querySelector('li')?.textContent).toContain(`1 × EUR ${amount}`);
    expect(element.textContent).not.toContain('applied');
    expect(element.querySelector('dl[aria-label="Receipt totals"]')?.textContent).toContain(
      `EUR ${amount}`,
    );
  });
});
