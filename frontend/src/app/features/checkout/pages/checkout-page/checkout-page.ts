import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  viewChild,
  inject,
  signal,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
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
  readonly catalogRevision: string;
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
  private readonly productsHeading = viewChild.required<ElementRef<HTMLElement>>('productsHeading');
  private readonly checkoutStatus = viewChild.required<ElementRef<HTMLElement>>('checkoutStatus');
  private readonly products = inject(ProductsService);
  private readonly checkoutService = inject(CheckoutService);
  private readonly checkoutAttempt = signal<CheckoutAttempt | null>(null);
  private nextRequestId = 0;
  private catalogRequestId = 0;

  private readonly destroyRef = inject(DestroyRef);

  protected readonly cart = inject(CartState);

  protected readonly state = signal<CatalogState>({ status: 'loading' });
  protected readonly catalogNotice = signal('');

  protected readonly checkout = computed<CheckoutResult>(() => {
    const attempt = this.checkoutAttempt();

    const catalog = this.state();

    return attempt?.cartSnapshot === this.cart.items() &&
      catalog.status === 'ready' &&
      attempt.catalogRevision === catalog.catalog.catalogRevision
      ? attempt.result
      : { status: 'idle' };
  });

  protected readonly pending = computed(() => this.checkout().status === 'pending');
  protected readonly busy = computed(() => this.pending() || this.state().status !== 'ready');

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
    this.loadCatalog('initial');
  }

  protected calculate(): void {
    const catalog = this.state();
    if (this.pending() || catalog.status !== 'ready') {
      return;
    }

    this.checkoutStatus().nativeElement.focus();
    this.catalogNotice.set('');

    const attempt: CheckoutAttempt = {
      requestId: ++this.nextRequestId,
      catalogRevision: catalog.catalog.catalogRevision,
      cartSnapshot: this.cart.items(),
      result: { status: 'pending' },
    };
    this.checkoutAttempt.set(attempt);

    this.checkoutService
      .calculateCheckout(this.cart.toCheckoutRequest())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (receipt) => this.completeCheckout(attempt, { status: 'ready', receipt }),
        error: (error: unknown) => {
          if (this.isCurrent(attempt) && this.isUnknownProduct(error)) {
            this.loadCatalog('changed');
          } else {
            this.completeCheckout(attempt, { status: 'error' });
          }
        },
      });
  }

  protected increment(productId: string): void {
    if (!this.busy()) {
      this.cart.increment(productId);
    }
  }

  protected decrement(productId: string): void {
    if (!this.busy()) {
      this.cart.decrement(productId);
    }
  }

  protected remove(productId: string): void {
    if (!this.busy()) {
      this.cart.remove(productId);
    }
  }

  protected clear(): void {
    if (!this.busy()) {
      this.cart.clear();
    }
  }

  private isCurrent(attempt: CheckoutAttempt): boolean {
    const catalog = this.state();
    return (
      this.checkoutAttempt()?.requestId === attempt.requestId &&
      this.cart.items() === attempt.cartSnapshot &&
      catalog.status === 'ready' &&
      catalog.catalog.catalogRevision === attempt.catalogRevision
    );
  }

  private completeCheckout(attempt: CheckoutAttempt, result: CheckoutResult): void {
    if (!this.isCurrent(attempt)) {
      return;
    }

    if (result.status === 'ready' && result.receipt.catalogRevision !== attempt.catalogRevision) {
      this.loadCatalog('changed');
      return;
    }

    this.checkoutAttempt.set({ ...attempt, result });
  }

  private isUnknownProduct(error: unknown): boolean {
    if (!(error instanceof HttpErrorResponse) || error.status !== 400) {
      return false;
    }
    const problem: unknown = error.error;
    return (
      typeof problem === 'object' &&
      problem !== null &&
      'code' in problem &&
      problem.code === 'UNKNOWN_PRODUCT'
    );
  }

  protected refresh(): void {
    if (!this.busy()) {
      this.productsHeading().nativeElement.focus();
      this.loadCatalog('refresh');
    }
  }

  protected retry(): void {
    if (this.state().status === 'error') {
      this.productsHeading().nativeElement.focus();
      this.loadCatalog('refresh');
    }
  }

  private loadCatalog(reason: 'initial' | 'refresh' | 'changed'): void {
    const requestId = ++this.catalogRequestId;
    this.checkoutAttempt.set(null);
    this.catalogNotice.set(reason === 'changed' ? 'Catalog changed. Refreshing products…' : '');
    this.state.set({ status: 'loading' });

    this.products
      .getProducts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (catalog) => {
          if (requestId !== this.catalogRequestId) {
            return;
          }

          const available = new Set(catalog.items.map((product) => product.id));
          const removed = this.cart.items().filter((item) => !available.has(item.productId));
          for (const item of removed) {
            this.cart.remove(item.productId);
          }

          this.state.set({ status: 'ready', catalog });
          const refreshed =
            reason === 'initial' ? '' : 'Products refreshed. Calculate checkout again.';
          const removal =
            removed.length === 0
              ? ''
              : ` Removed unavailable products: ${removed.map((item) => item.productId).join(', ')}.`;
          this.catalogNotice.set(refreshed + removal);
        },
        error: () => {
          if (requestId !== this.catalogRequestId) {
            return;
          }

          this.state.set({ status: 'error' });
          this.catalogNotice.set(
            reason === 'changed' ? 'Catalog changed. Refresh failed. Try again.' : '',
          );
        },
      });
  }
}
