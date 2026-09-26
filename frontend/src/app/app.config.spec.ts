import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { appConfig } from './app.config';
import { CheckoutService, Currency, ProductsService } from './generated/api';

describe('Application HTTP configuration', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [...appConfig.providers, provideHttpClientTesting()],
    });
  });

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('loads products using the generated service and one relative API prefix', () => {
    const response = { currency: Currency.Eur, items: [], totalItems: 0 };
    const received = vi.fn();
    TestBed.inject(ProductsService).getProducts().subscribe(received);
    const request = TestBed.inject(HttpTestingController).expectOne('/api/products');

    expect(request.request.method).toBe('GET');
    request.flush(response);
    expect(received).toHaveBeenCalledExactlyOnceWith(response);
  });

  it('posts quantities and preserves monetary response strings through the generated service', () => {
    const payload = { items: [{ productId: 'APPLE', quantity: 3 }] };
    const response = {
      currency: Currency.Eur,
      items: [],
      subtotal: '0.90',
      discount: '0.15',
      total: '0.75',
    };
    const received = vi.fn();
    TestBed.inject(CheckoutService).calculateCheckout(payload).subscribe(received);
    const request = TestBed.inject(HttpTestingController).expectOne('/api/checkout');

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush(response);
    expect(received).toHaveBeenCalledExactlyOnceWith(response);
  });
});
