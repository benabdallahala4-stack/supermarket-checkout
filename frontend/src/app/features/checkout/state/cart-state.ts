import { computed, Injectable, signal } from '@angular/core';
import { CheckoutRequest } from '../../../generated/api';

export const MAX_QUANTITY = 2_147_483_647;

@Injectable()
export class CartState {
  private readonly quantities = signal<ReadonlyMap<string, number>>(new Map());

  readonly items = computed(() =>
    Object.freeze(
      Array.from(this.quantities().entries())
        .sort(([left], [right]) => (left < right ? -1 : 1))
        .map(([productId, quantity]) => Object.freeze({ productId, quantity })),
    ),
  );

  readonly itemCount = computed(() =>
    this.items().reduce((count, item) => count + item.quantity, 0),
  );
  readonly isEmpty = computed(() => this.quantities().size === 0);

  setQuantity(productId: string, quantity: number): void {
    if (productId.trim().length === 0) {
      throw new Error('Product ID must not be blank');
    }

    if (!Number.isInteger(quantity) || quantity < 0 || quantity > MAX_QUANTITY) {
      throw new RangeError('Quantity must be an integer between zero and the int32 limit');
    }

    if (quantity === 0) {
      this.remove(productId);
      return;
    }

    const updated = new Map(this.quantities());
    updated.set(productId, quantity);
    this.quantities.set(updated);
  }

  increment(productId: string): void {
    const quantity = this.quantities().get(productId) ?? 0;

    if (quantity < MAX_QUANTITY) {
      this.setQuantity(productId, quantity + 1);
    }
  }

  decrement(productId: string): void {
    const quantity = this.quantities().get(productId);

    if (quantity !== undefined) {
      this.setQuantity(productId, quantity - 1);
    }
  }

  remove(productId: string): void {
    const updated = new Map(this.quantities());
    updated.delete(productId);
    this.quantities.set(updated);
  }

  clear(): void {
    this.quantities.set(new Map());
  }

  toCheckoutRequest(): CheckoutRequest {
    return { items: this.items().map((item) => ({ ...item })) };
  }
}
