import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Currency, provideApi } from '../../../../generated/api';
import { CartState, MAX_QUANTITY } from '../../state/cart-state';
import { CheckoutPage } from './checkout-page';

describe('CheckoutPage cart controls', () => {
  let fixture: ComponentFixture<CheckoutPage>;
  let element: HTMLElement;
  let http: HttpTestingController;

  function button(name: string): HTMLButtonElement {
    const control = Array.from(element.querySelectorAll('button')).find(
      (candidate) =>
        (candidate.getAttribute('aria-label') ?? candidate.textContent?.trim()) === name,
    );
    if (!control) {
      throw new Error(`Missing button: ${name}`);
    }
    return control;
  }

  function click(name: string): void {
    button(name).click();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CheckoutPage],
      providers: [provideHttpClient(), provideApi('/api'), provideHttpClientTesting()],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CheckoutPage);
    element = fixture.nativeElement;
    fixture.detectChanges();
    http.expectOne('/api/products').flush({
      currency: Currency.Eur,
      items: [
        { id: 'APPLE', name: 'Apple', unitPrice: '0.30', offer: { quantity: 2, price: '0.45' } },
        { id: 'ORANGE', name: 'Orange', unitPrice: '0.80' },
      ],
      totalItems: 2,
    });
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('shows an empty cart and disables clearing it', () => {
    expect(element.textContent).toContain('Your cart is empty');
    expect(button('Clear cart').disabled).toBe(true);
  });

  it('adds products and updates the displayed quantity and total item count', () => {
    click('Add Apple to cart');
    click('Add Apple to cart');
    click('Add Orange to cart');

    const rows = element.querySelectorAll('ul[aria-label="Cart items"] li');
    expect(rows).toHaveLength(2);
    expect(rows[0].textContent).toContain('Apple');
    expect(rows[0].querySelector('[aria-label="Quantity of Apple"]')?.textContent).toBe('2');
    expect(element.querySelector('#cart-status')?.textContent).toContain('3 items in cart');
    expect(element.textContent).not.toContain('Your cart is empty');
    http.expectNone('/api/checkout');
  });

  it('increments, decrements and removes products through named buttons', () => {
    click('Add Apple to cart');
    click('Increase Apple quantity');
    expect(element.querySelector('[aria-label="Quantity of Apple"]')?.textContent).toBe('2');

    click('Decrease Apple quantity');
    expect(element.querySelector('#cart-status')?.textContent).toContain('1 item in cart');

    click('Decrease Apple quantity');
    expect(element.textContent).toContain('Your cart is empty');

    click('Add Orange to cart');
    click('Remove Orange from cart');
    expect(element.textContent).toContain('Your cart is empty');
  });

  it('clears all products at once', () => {
    click('Add Apple to cart');
    click('Add Orange to cart');
    click('Clear cart');

    expect(element.querySelectorAll('ul[aria-label="Cart items"] li')).toHaveLength(0);
    expect(element.querySelector('#cart-status')?.textContent).toContain('0 items in cart');
    expect(button('Clear cart').disabled).toBe(true);
  });

  it('disables adding and incrementing at the quantity limit and re-enables after decrement', () => {
    const state = fixture.debugElement.injector.get(CartState);
    state.setQuantity('APPLE', MAX_QUANTITY);
    fixture.detectChanges();

    expect(button('Add Apple to cart').disabled).toBe(true);
    expect(button('Increase Apple quantity').disabled).toBe(true);
    expect(element.textContent).toContain('Maximum quantity reached');
    expect(button('Add Orange to cart').disabled).toBe(false);

    click('Decrease Apple quantity');
    expect(button('Add Apple to cart').disabled).toBe(false);
    expect(button('Increase Apple quantity').disabled).toBe(false);
  });

  it('starts a fresh cart when the checkout page is recreated', () => {
    click('Add Apple to cart');
    fixture.destroy();
    fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    http.expectOne('/api/products').flush({ currency: Currency.Eur, items: [], totalItems: 0 });
    fixture.detectChanges();
    element = fixture.nativeElement;

    expect(element.textContent).toContain('Your cart is empty');
  });
});
