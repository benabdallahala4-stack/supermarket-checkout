import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Currency, provideApi } from '../../../../generated/api';
import { CheckoutPage } from './checkout-page';

describe('CheckoutPage keyboard focus', () => {
  let fixture: ComponentFixture<CheckoutPage>;
  let element: HTMLElement;
  let http: HttpTestingController;
  const catalog = {
    catalogRevision: 'revision-1',
    currency: Currency.Eur,
    totalItems: 1,
    items: [{ id: 'APPLE', name: 'Apple', unitPrice: '0.30' }],
  };
  function button(name: string): HTMLButtonElement {
    const result = Array.from(element.querySelectorAll('button')).find(
      (b) => (b.getAttribute('aria-label') ?? b.textContent?.trim()) === name,
    );
    if (!result) {
      throw new Error(`Missing button: ${name}`);
    }
    return result;
  }
  function activate(name: string): void {
    button(name).focus();
    button(name).click();
    fixture.detectChanges();
  }
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CheckoutPage],
      providers: [provideHttpClient(), provideApi('/api'), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(CheckoutPage);
    element = fixture.nativeElement;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne('/api/products').flush(catalog);
    fixture.detectChanges();
  });
  afterEach(() => {
    try {
      http.verify();
    } finally {
      TestBed.resetTestingModule();
    }
  });

  it('moves focus to the persistent product heading when refresh removes its button', () => {
    activate('Refresh products');
    const request = http.expectOne('/api/products');
    expect(document.activeElement).toBe(element.querySelector('#products-heading'));
    request.flush(catalog);
    fixture.detectChanges();
    expect(document.activeElement).toBe(element.querySelector('#products-heading'));
  });
  it('keeps focus on the product heading during a retry', () => {
    activate('Refresh products');
    http.expectOne('/api/products').error(new ProgressEvent('error'));
    fixture.detectChanges();
    activate('Try again');
    const request = http.expectOne('/api/products');
    expect(document.activeElement).toBe(element.querySelector('#products-heading'));
    request.flush(catalog);
  });
  it.each(['Remove Apple from cart', 'Decrease Apple quantity', 'Clear cart'])(
    'moves focus to the cart heading after %s removes a control',
    (action) => {
      activate('Add Apple to cart');
      activate(action);
      expect(document.activeElement).toBe(element.querySelector('#cart-heading'));
      expect(element.textContent).toContain('Your cart is empty');
    },
  );
  it('keeps focus on decrement when the row remains', () => {
    activate('Add Apple to cart');
    activate('Add Apple to cart');
    activate('Decrease Apple quantity');
    expect(document.activeElement).toBe(button('Decrease Apple quantity'));
  });
  it('uses a persistent focus target while checkout disables the submit button', () => {
    activate('Calculate checkout');
    const request = http.expectOne('/api/checkout');
    expect(document.activeElement).toBe(element.querySelector('#checkout-status'));
    request.flush({
      catalogRevision: 'revision-1',
      currency: Currency.Eur,
      items: [],
      subtotal: '0.00',
      discount: '0.00',
      total: '0.00',
    });
    fixture.detectChanges();
    expect(document.activeElement).toBe(element.querySelector('#checkout-status'));
  });
});
