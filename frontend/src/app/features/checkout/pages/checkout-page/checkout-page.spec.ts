import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Currency, ProductCatalog, provideApi } from '../../../../generated/api';
import { CheckoutPage } from './checkout-page';

describe('CheckoutPage catalog', () => {
  let fixture: ComponentFixture<CheckoutPage>;
  let http: HttpTestingController;
  let element: HTMLElement;

  const catalog: ProductCatalog = {
    catalogRevision: 'revision-1',
    currency: Currency.Eur,
    items: [
      { id: 'APPLE', name: 'Apple', unitPrice: '0.30', offer: { quantity: 2, price: '0.45' } },
      { id: 'ORANGE', name: 'Orange', unitPrice: '0.80' },
    ],
    totalItems: 2,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CheckoutPage],
      providers: [provideHttpClient(), provideApi('/api'), provideHttpClientTesting()],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CheckoutPage);
    element = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('announces loading until the catalog arrives', () => {
    expect(element.querySelector('[role="status"]')?.textContent).toContain('Loading products');
    const request = http.expectOne('/api/products');
    expect(request.request.method).toBe('GET');

    request.flush(catalog);
    fixture.detectChanges();

    expect(element.textContent).not.toContain('Loading products');
    expect(element.querySelector('[role="status"]')?.textContent).toContain('2 products available');
  });

  it('renders product names, exact server prices and offers in a semantic list', () => {
    http.expectOne('/api/products').flush(catalog);
    fixture.detectChanges();

    const products = element.querySelectorAll('ul li');
    expect(products).toHaveLength(2);
    expect(products[0].textContent).toContain('Apple');
    expect(products[0].textContent).toContain('EUR 0.30');
    expect(products[0].textContent).toContain('2 for EUR 0.45');
    expect(products[1].textContent).toContain('Orange');
    expect(products[1].textContent).toContain('EUR 0.80');
    expect(products[1].textContent).not.toContain('for EUR');
  });

  it('announces a single available product naturally', () => {
    http.expectOne('/api/products').flush({ ...catalog, items: [catalog.items[0]], totalItems: 1 });
    fixture.detectChanges();

    expect(element.querySelector('[role="status"]')?.textContent?.trim()).toBe(
      '1 product available',
    );
  });

  it('ignores a second retry click while the replacement request is pending', () => {
    http.expectOne('/api/products').error(new ProgressEvent('error'));
    fixture.detectChanges();

    const retry = element.querySelector<HTMLButtonElement>(
      'section[aria-labelledby="products-heading"] button',
    );
    retry?.click();
    retry?.click();

    http.expectOne('/api/products').flush(catalog);
    fixture.detectChanges();
    expect(element.textContent).toContain('Apple');
  });

  it('shows an empty catalog without product rows', () => {
    http
      .expectOne('/api/products')
      .flush({ catalogRevision: 'revision-1', currency: Currency.Eur, items: [], totalItems: 0 });
    fixture.detectChanges();

    expect(element.querySelector('[role="status"]')?.textContent).toContain(
      'No products available',
    );
    expect(element.querySelectorAll('li')).toHaveLength(0);
  });

  it('announces a safe error and retries through the generated service', () => {
    http.expectOne('/api/products').flush('private failure detail', {
      status: 500,
      statusText: 'Internal Server Error',
    });
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Could not load products',
    );
    expect(element.textContent).not.toContain('private failure detail');
    const retry = element.querySelector<HTMLButtonElement>(
      'section[aria-labelledby="products-heading"] button',
    );
    expect(retry?.textContent).toContain('Try again');
    retry?.click();
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')).toBeNull();
    expect(
      element.querySelector<HTMLButtonElement>(
        'section[aria-labelledby="products-heading"] button',
      ),
    ).toBeNull();
    expect(element.querySelector('[role="status"]')?.textContent).toContain('Loading products');
    http.expectOne('/api/products').flush(catalog);
    fixture.detectChanges();

    expect(element.textContent).toContain('Apple');
  });

  it('allows another retry after a network failure', () => {
    http.expectOne('/api/products').error(new ProgressEvent('error'));
    fixture.detectChanges();
    element
      .querySelector<HTMLButtonElement>('section[aria-labelledby="products-heading"] button')
      ?.click();
    http.expectOne('/api/products').error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain(
      'Could not load products',
    );
    expect(
      element.querySelector<HTMLButtonElement>('section[aria-labelledby="products-heading"] button')
        ?.textContent,
    ).toContain('Try again');
  });

  it('cancels an in-flight catalog request when the page is destroyed', () => {
    const request = http.expectOne('/api/products');
    fixture.destroy();

    expect(request.cancelled).toBe(true);
  });
});
