import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App', () => {
  it('renders the checkout application heading in its main landmark', async () => {
    await TestBed.configureTestingModule({ imports: [App] }).compileComponents();
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('main h1')?.textContent).toBe('Supermarket Checkout');
  });
});
