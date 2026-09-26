import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProductCatalog, ProductsService } from '../../../../generated/api';
import { ProductList } from '../../components/product-list/product-list';

type CatalogState =
  { status: 'loading' } | { status: 'ready'; catalog: ProductCatalog } | { status: 'error' };

@Component({
  selector: 'app-checkout-page',
  imports: [ProductList],
  templateUrl: './checkout-page.html',
  styleUrl: './checkout-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CheckoutPage {
  private readonly products = inject(ProductsService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly state = signal<CatalogState>({ status: 'loading' });

  constructor() {
    this.loadCatalog();
  }

  protected retry(): void {
    if (this.state().status === 'error') {
      this.loadCatalog();
    }
  }

  private loadCatalog(): void {
    this.state.set({ status: 'loading' });

    this.products
      .getProducts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (catalog) => this.state.set({ status: 'ready', catalog }),
        error: () => this.state.set({ status: 'error' }),
      });
  }
}
