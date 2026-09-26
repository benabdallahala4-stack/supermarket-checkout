import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  CheckoutItem,
  CheckoutReceipt,
  CheckoutService,
  ProductCatalog,
  ProductsService,
} from '../../../../generated/api';
import { Receipt } from '../../components/receipt/receipt';
import { Cart, CartLine } from '../../components/cart/cart';
import { CartState, MAX_QUANTITY } from '../../state/cart-state';
import { ProductList } from '../../components/product-list/product-list';

type CatalogState =
  { status: 'loading' } | { status: 'ready'; catalog: ProductCatalog } | { status: 'error' };

type CheckoutResult =
  | { status: 'idle' }
  | { status: 'pending' }
  | { status: 'ready'; receipt: CheckoutReceipt }
  | { status: 'error' };

interface CheckoutAttempt {
  readonly requestId: number;
  readonly cartSnapshot: readonly Readonly<CheckoutItem>[];
  readonly result: CheckoutResult;
}

@Component({
  selector: 'app-checkout-page',
  imports: [ProductList, Cart, Receipt],
  providers: [CartState],
  templateUrl: './checkout-page.html',
  styleUrl: './checkout-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CheckoutPage {
  private readonly products = inject(ProductsService);
  private readonly checkoutService = inject(CheckoutService);
  private readonly checkoutAttempt = signal<CheckoutAttempt | null>(null);
  private nextRequestId = 0;

  private readonly destroyRef = inject(DestroyRef);

  protected readonly cart = inject(CartState);

  protected readonly state = signal<CatalogState>({ status: 'loading' });

  protected readonly checkout = computed<CheckoutResult>(() => {
    const attempt = this.checkoutAttempt();

    return attempt?.cartSnapshot === this.cart.items() ? attempt.result : { status: 'idle' };
  });

  protected readonly pending = computed(() => this.checkout().status === 'pending');

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

  protected calculate(): void {
    if (this.pending() || this.state().status !== 'ready') {
      return;
    }

    const attempt: CheckoutAttempt = {
      requestId: ++this.nextRequestId,
      cartSnapshot: this.cart.items(),
      result: { status: 'pending' },
    };
    this.checkoutAttempt.set(attempt);

    this.checkoutService
      .calculateCheckout(this.cart.toCheckoutRequest())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (receipt) => this.completeCheckout(attempt, { status: 'ready', receipt }),
        error: () => this.completeCheckout(attempt, { status: 'error' }),
      });
  }

  protected increment(productId: string): void {
    if (!this.pending()) {
      this.cart.increment(productId);
    }
  }

  protected decrement(productId: string): void {
    if (!this.pending()) {
      this.cart.decrement(productId);
    }
  }

  protected remove(productId: string): void {
    if (!this.pending()) {
      this.cart.remove(productId);
    }
  }

  protected clear(): void {
    if (!this.pending()) {
      this.cart.clear();
    }
  }

  private completeCheckout(attempt: CheckoutAttempt, result: CheckoutResult): void {
    if (
      this.checkoutAttempt()?.requestId !== attempt.requestId ||
      this.cart.items() !== attempt.cartSnapshot
    ) {
      return;
    }

    this.checkoutAttempt.set({ ...attempt, result });
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
