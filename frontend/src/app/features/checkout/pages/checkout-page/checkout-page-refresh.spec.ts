import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Currency, provideApi } from '../../../../generated/api';
import { CartState } from '../../state/cart-state';
import { CheckoutPage } from './checkout-page';

describe('CheckoutPage catalog reconciliation', () => {
  let fixture: ComponentFixture<CheckoutPage>;
  let element: HTMLElement;
  let http: HttpTestingController;
  let cart: CartState;
  const catalog = {
    catalogRevision: 'revision-1',
    currency: Currency.Eur,
    totalItems: 2,
    items: [
      { id: 'APPLE', name: 'Apple', unitPrice: '0.30' },
      { id: 'TEA', name: 'Tea', unitPrice: '2.00' },
    ],
  };
  const receipt = {
    catalogRevision: 'revision-1',
    currency: Currency.Eur,
    items: [],
    subtotal: '0.00',
    discount: '0.00',
    total: '0.00',
  };
  function button(name: string): HTMLButtonElement {
    const result = Array.from(element.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === name,
    );
    if (!result) throw new Error(`Missing button: ${name}`);
    return result;
  }
  function click(name: string): void {
    button(name).click();
    fixture.detectChanges();
  }
  function displayedReceipt(): Element | null {
    return element.querySelector('app-receipt');
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

  it('refresh clears a receipt immediately and preserves quantities for available products', () => {
    click('Calculate checkout');
    http.expectOne('/api/checkout').flush(receipt);
    fixture.detectChanges();
    expect(displayedReceipt()).not.toBeNull();
    click('Refresh products');
    expect(displayedReceipt()).toBeNull();
    expect(button('Calculate checkout').disabled).toBe(true);
    http.expectOne('/api/products').flush(catalog);
    fixture.detectChanges();
    cart.setQuantity('APPLE', 3);
    cart.setQuantity('TEA', 2);
    fixture.detectChanges();
    click('Refresh products');
    http.expectOne('/api/products').flush({
      ...catalog,
      catalogRevision: 'revision-2',
      items: [catalog.items[0]],
      totalItems: 1,
    });
    fixture.detectChanges();
    expect(cart.toCheckoutRequest().items).toEqual([{ productId: 'APPLE', quantity: 3 }]);
    expect(element.textContent).toContain('Removed unavailable products');
    expect(element.textContent).toContain('TEA');
    http.expectNone('/api/checkout');
  });
  it('discards a receipt from changed prices, refreshes, and requires explicit recalculation', () => {
    click('Calculate checkout');
    http.expectOne('/api/checkout').flush({ ...receipt, catalogRevision: 'revision-2' });
    fixture.detectChanges();
    expect(displayedReceipt()).toBeNull();
    expect(element.textContent).toContain('Catalog changed');
    expect(button('Calculate checkout').disabled).toBe(true);
    http.expectOne('/api/products').flush({ ...catalog, catalogRevision: 'revision-3' });
    fixture.detectChanges();
    expect(element.textContent).toContain('Products refreshed');
    http.expectNone('/api/checkout');
    click('Calculate checkout');
    http.expectOne('/api/checkout').flush({ ...receipt, catalogRevision: 'revision-3' });
    fixture.detectChanges();
    expect(displayedReceipt()).not.toBeNull();
  });
  it('does not announce refresh success on failure and preserves cart until a retry succeeds', () => {
    cart.setQuantity('APPLE', 2);
    fixture.detectChanges();
    click('Refresh products');
    http.expectOne('/api/products').error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(element.textContent).not.toContain('Products refreshed');
    expect(element.textContent).toContain('Could not load products');
    expect(cart.itemCount()).toBe(2);
    expect(button('Calculate checkout').disabled).toBe(true);
    click('Try again');
    http
      .expectOne('/api/products')
      .flush({ ...catalog, items: [], totalItems: 0, catalogRevision: 'revision-2' });
    fixture.detectChanges();
    expect(cart.itemCount()).toBe(0);
    expect(element.textContent).toContain('Removed unavailable products');
  });
  it('refresh is guarded while checkout is pending', () => {
    click('Calculate checkout');
    const refresh = button('Refresh products');
    expect(refresh.disabled).toBe(true);
    refresh.dispatchEvent(new MouseEvent('click'));
    http.expectNone('/api/products');
    http.expectOne('/api/checkout').flush(receipt);
    fixture.detectChanges();
  });
  it('ignores an obsolete mismatching checkout after a newer receipt succeeds', () => {
    click('Calculate checkout');
    const older = http.expectOne('/api/checkout');
    cart.setQuantity('APPLE', 1);
    fixture.detectChanges();
    click('Calculate checkout');
    http.expectOne('/api/checkout').flush(receipt);
    fixture.detectChanges();
    older.flush({ ...receipt, catalogRevision: 'obsolete' });
    fixture.detectChanges();
    http.expectNone('/api/products');
    expect(displayedReceipt()).not.toBeNull();
  });
  it('reconciles deleted products after an unknown-product checkout response', () => {
    cart.setQuantity('TEA', 1);
    fixture.detectChanges();
    click('Calculate checkout');
    http
      .expectOne('/api/checkout')
      .flush({ code: 'UNKNOWN_PRODUCT' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();
    http.expectOne('/api/products').flush({
      ...catalog,
      catalogRevision: 'revision-2',
      items: [catalog.items[0]],
      totalItems: 1,
    });
    fixture.detectChanges();
    expect(cart.itemCount()).toBe(0);
    expect(displayedReceipt()).toBeNull();
    expect(element.textContent).toContain('Removed unavailable products');
  });

  it('keeps a failed automatic refresh explicit and retries without resubmitting checkout', () => {
    cart.setQuantity('APPLE', 2);
    fixture.detectChanges();
    click('Calculate checkout');
    http.expectOne('/api/checkout').flush({ ...receipt, catalogRevision: 'revision-2' });
    http.expectOne('/api/products').error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(element.textContent).toContain('Catalog changed. Refresh failed');
    expect(element.textContent).not.toContain('Products refreshed');
    expect(cart.itemCount()).toBe(2);
    expect(displayedReceipt()).toBeNull();
    click('Try again');
    http.expectOne('/api/products').flush({ ...catalog, catalogRevision: 'revision-2' });
    fixture.detectChanges();
    http.expectNone('/api/checkout');
    click('Calculate checkout');
    http.expectOne('/api/checkout').flush({ ...receipt, catalogRevision: 'revision-2' });
    fixture.detectChanges();
    expect(displayedReceipt()).not.toBeNull();
    expect(element.textContent).not.toContain('Calculate checkout again');
  });

  it('guards duplicate refreshes and ignores a checkout completed during refresh', () => {
    click('Calculate checkout');
    const older = http.expectOne('/api/checkout');
    cart.setQuantity('APPLE', 1);
    fixture.detectChanges();
    const refresh = button('Refresh products');
    refresh.click();
    refresh.click();
    fixture.detectChanges();
    const request = http.expectOne('/api/products');
    older.flush({ ...receipt, catalogRevision: 'obsolete' });
    fixture.detectChanges();
    expect(displayedReceipt()).toBeNull();
    http.expectNone('/api/products');
    request.flush({ ...catalog, catalogRevision: 'revision-2' });
    fixture.detectChanges();
    expect(cart.itemCount()).toBe(1);
    expect(displayedReceipt()).toBeNull();
  });
});
