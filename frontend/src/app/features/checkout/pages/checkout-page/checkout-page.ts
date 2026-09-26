import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProductCatalog, ProductsService } from '../../../../generated/api';
import { Cart, CartLine } from '../../components/cart/cart';
import { CartState, MAX_QUANTITY } from '../../state/cart-state';
import { ProductList } from '../../components/product-list/product-list';

type CatalogState =
  { status: 'loading' } | { status: 'ready'; catalog: ProductCatalog } | { status: 'error' };

@Component({
  selector: 'app-checkout-page',
  imports: [ProductList, Cart],
  providers: [CartState],
  templateUrl: './checkout-page.html',
  styleUrl: './checkout-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CheckoutPage {
  private readonly products = inject(ProductsService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly cart = inject(CartState);

  protected readonly state = signal<CatalogState>({ status: 'loading' });

  protected readonly limitProductIds = computed(
    () =>
      new Set(
        this.cart
          .items()
          .filter((item) => item.quantity === MAX_QUANTITY)
          .map((item) => item.productId),
      ),
  );

  protected readonly cartLines = computed<readonly CartLine[]>(() => {
    const current = this.state();
    const products = current.status === 'ready' ? current.catalog.items : [];
    const names = new Map(products.map((product) => [product.id, product.name]));

    return this.cart
      .items()
      .map((item) => ({ ...item, name: names.get(item.productId) ?? item.productId }));
  });

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
