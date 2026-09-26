import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { appConfig } from './app.config';
import { Currency } from './generated/api';

describe('App', () => {
  it('renders the checkout application heading and catalog in its main landmark', async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [...appConfig.providers, provideHttpClientTesting()],
    }).compileComponents();

    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/products').flush({ currency: Currency.Eur, items: [], totalItems: 0 });
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('main h1')?.textContent).toBe('Supermarket Checkout');
    expect(element.querySelector('main h2')?.textContent).toBe('Products');
    expect(element.textContent).toContain('No products available');
    http.verify();
  });
});
