import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { CheckoutReceipt, Currency, provideApi } from '../../../../generated/api';
import { Cart } from '../../components/cart/cart';
import { ProductList } from '../../components/product-list/product-list';
import { CartState } from '../../state/cart-state';
import { CheckoutPage } from './checkout-page';

describe('CheckoutPage receipt lifecycle', () => {
  let fixture: ComponentFixture<CheckoutPage>;
  let element: HTMLElement;
  let http: HttpTestingController;
  let cart: CartState;

  const receipt: CheckoutReceipt = {
    catalogRevision: 'revision-1',
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

  function button(name: string): HTMLButtonElement {
    const control = Array.from(element.querySelectorAll('button')).find(
      (candidate) =>
        candidate.textContent?.trim() === name || candidate.getAttribute('aria-label') === name,
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

  function displayedReceipt(): Element | null {
    return element.querySelector('section[aria-labelledby="receipt-heading"]');
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CheckoutPage],
      providers: [provideHttpClient(), provideApi('/api'), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(CheckoutPage);
    element = fixture.nativeElement;
    http = TestBed.inject(HttpTestingController);
    cart = fixture.debugElement.injector.get(CartState);
    fixture.detectChanges();
    http.expectOne('/api/products').flush({
      catalogRevision: 'revision-1',
      currency: Currency.Eur,
      items: [
        { id: 'APPLE', name: 'Apple', unitPrice: '0.30', offer: { quantity: 2, price: '0.45' } },
        { id: 'BANANA', name: 'Banana', unitPrice: '0.50' },
      ],
      totalItems: 2,
    });
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('submits sorted quantities through the generated service and displays the returned receipt', () => {
    cart.setQuantity('APPLE', 3);
    fixture.detectChanges();
    click('Calculate checkout');
    const request = http.expectOne('/api/checkout');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ items: [{ productId: 'APPLE', quantity: 3 }] });
    expect(element.querySelector('#checkout-status')?.textContent).toContain('Calculating');
    expect(displayedReceipt()).toBeNull();

    request.flush(receipt);
    fixture.detectChanges();
    expect(displayedReceipt()?.textContent).toContain('EUR 0.75');
    expect(displayedReceipt()?.textContent).toContain('EUR 0.15');
    expect(cart.itemCount()).toBe(3);
    expect(button('Calculate checkout').disabled).toBe(false);
  });

  it('allows an explicit empty checkout and displays the zero receipt', () => {
    click('Calculate checkout');
    const request = http.expectOne('/api/checkout');
    expect(request.request.body).toEqual({ items: [] });
    request.flush({
      catalogRevision: 'revision-1',
      currency: Currency.Eur,
      items: [],
      subtotal: '0.00',
      discount: '0.00',
      total: '0.00',
    });
    fixture.detectChanges();

    expect(displayedReceipt()?.textContent).toContain('No items in this receipt');
    expect(displayedReceipt()?.textContent).toContain('EUR 0.00');
  });

  it('guards repeated submissions and all cart events while a request is pending', () => {
    cart.setQuantity('APPLE', 3);
    fixture.detectChanges();
    const calculate = button('Calculate checkout');
    calculate.click();
    calculate.click();
    fixture.detectChanges();
    const request = http.expectOne('/api/checkout');
    for (const name of [
      'Calculate checkout',
      'Add Apple to cart',
      'Add Banana to cart',
      'Increase Apple quantity',
      'Decrease Apple quantity',
      'Remove Apple from cart',
      'Clear cart',
    ]) {
      expect(button(name).disabled, name).toBe(true);
    }
    fixture.debugElement.query(By.directive(ProductList)).triggerEventHandler('add', 'BANANA');
    const cartView = fixture.debugElement.query(By.directive(Cart));
    cartView.triggerEventHandler('increment', 'APPLE');
    cartView.triggerEventHandler('decrement', 'APPLE');
    cartView.triggerEventHandler('remove', 'APPLE');
    cartView.triggerEventHandler('clear');
    expect(cart.toCheckoutRequest()).toEqual({ items: [{ productId: 'APPLE', quantity: 3 }] });
    request.flush(receipt);
    fixture.detectChanges();
    expect(button('Add Banana to cart').disabled).toBe(false);
  });

  it.each([
    'Add Banana to cart',
    'Increase Apple quantity',
    'Decrease Apple quantity',
    'Remove Apple from cart',
    'Clear cart',
  ])('invalidates a calculated receipt after %s', (action) => {
    cart.setQuantity('APPLE', 3);
    fixture.detectChanges();
    click('Calculate checkout');
    http.expectOne('/api/checkout').flush(receipt);
    fixture.detectChanges();
    expect(displayedReceipt()).not.toBeNull();

    click(action);
    expect(displayedReceipt()).toBeNull();
    expect(element.querySelector('#checkout-status')?.textContent).not.toContain(
      'Receipt calculated',
    );
    http.expectNone('/api/checkout');
  });

  it('preserves quantities on failure and retries without exposing backend details', () => {
    cart.setQuantity('APPLE', 3);
    fixture.detectChanges();
    click('Calculate checkout');
    http
      .expectOne('/api/checkout')
      .flush(
        { detail: 'private diagnostic' },
        { status: 500, statusText: 'Internal Server Error' },
      );
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Could not calculate checkout',
    );
    expect(element.textContent).not.toContain('private diagnostic');
    expect(cart.itemCount()).toBe(3);
    expect(button('Calculate checkout').disabled).toBe(false);
    expect(button('Add Banana to cart').disabled).toBe(false);
    click('Calculate checkout');
    const retry = http.expectOne('/api/checkout');
    expect(retry.request.body).toEqual({ items: [{ productId: 'APPLE', quantity: 3 }] });
    retry.flush(receipt);
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')).toBeNull();
    expect(displayedReceipt()?.textContent).toContain('EUR 0.75');
  });

  it('clears a checkout error when the cart changes', () => {
    click('Calculate checkout');
    http.expectOne('/api/checkout').error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')).not.toBeNull();
    click('Add Apple to cart');
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });

  it('discards a delayed receipt if the cart changes outside the guarded UI', () => {
    cart.setQuantity('APPLE', 3);
    fixture.detectChanges();
    click('Calculate checkout');
    const request = http.expectOne('/api/checkout');
    cart.increment('BANANA');
    request.flush(receipt);
    fixture.detectChanges();

    expect(displayedReceipt()).toBeNull();
    expect(cart.itemCount()).toBe(4);
    expect(button('Calculate checkout').disabled).toBe(false);
  });

  it.each(['success', 'error'])('ignores an older %s after a newer request succeeds', (outcome) => {
    cart.setQuantity('APPLE', 3);
    fixture.detectChanges();
    click('Calculate checkout');
    const older = http.expectOne('/api/checkout');

    cart.increment('BANANA');
    fixture.detectChanges();
    click('Calculate checkout');
    const newer = http.expectOne('/api/checkout');
    expect(newer.request.body.items).toEqual([
      { productId: 'APPLE', quantity: 3 },
      { productId: 'BANANA', quantity: 1 },
    ]);
    newer.flush({
      ...receipt,
      items: [
        ...receipt.items,
        {
          productId: 'BANANA',
          name: 'Banana',
          quantity: 1,
          unitPrice: '0.50',
          subtotal: '0.50',
          discount: '0.00',
          total: '0.50',
        },
      ],
      subtotal: '1.40',
      total: '1.25',
    });
    if (outcome === 'success') {
      older.flush(receipt);
    } else {
      older.error(new ProgressEvent('error'));
    }
    fixture.detectChanges();

    expect(displayedReceipt()?.textContent).toContain('EUR 1.25');
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });

  it('does not submit checkout before the catalog is ready', () => {
    fixture.destroy();
    fixture = TestBed.createComponent(CheckoutPage);
    fixture.detectChanges();
    element = fixture.nativeElement;
    const catalog = http.expectOne('/api/products');

    expect(button('Calculate checkout').disabled).toBe(true);
    button('Calculate checkout').dispatchEvent(new MouseEvent('click'));
    http.expectNone('/api/checkout');

    catalog.flush({
      catalogRevision: 'revision-1',
      currency: Currency.Eur,
      items: [],
      totalItems: 0,
    });
    fixture.detectChanges();
    expect(button('Calculate checkout').disabled).toBe(false);
  });

  it('hides the previous receipt during recalculation and after recalculation fails', () => {
    cart.setQuantity('APPLE', 3);
    fixture.detectChanges();
    click('Calculate checkout');
    http.expectOne('/api/checkout').flush(receipt);
    fixture.detectChanges();
    expect(displayedReceipt()).not.toBeNull();

    click('Calculate checkout');
    expect(displayedReceipt()).toBeNull();
    http.expectOne('/api/checkout').error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(displayedReceipt()).toBeNull();
    expect(cart.itemCount()).toBe(3);
  });

  it('rejects a late response even if quantities change back to their earlier values', () => {
    cart.setQuantity('APPLE', 3);
    fixture.detectChanges();
    click('Calculate checkout');
    const request = http.expectOne('/api/checkout');
    cart.increment('APPLE');
    cart.decrement('APPLE');
    request.flush(receipt);
    fixture.detectChanges();

    expect(cart.itemCount()).toBe(3);
    expect(displayedReceipt()).toBeNull();
  });

  it('cancels checkout requests when the page is destroyed', () => {
    click('Calculate checkout');
    const request = http.expectOne('/api/checkout');
    fixture.destroy();
    expect(request.cancelled).toBe(true);
  });
});
