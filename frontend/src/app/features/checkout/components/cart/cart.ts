import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  output,
  viewChild,
} from '@angular/core';
import { MAX_QUANTITY } from '../../state/cart-state';

export interface CartLine {
  readonly productId: string;
  readonly name: string;
  readonly quantity: number;
}

@Component({
  selector: 'app-cart',
  templateUrl: './cart.html',
  styleUrl: './cart.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Cart {
  private readonly heading = viewChild.required<ElementRef<HTMLElement>>('heading');
  readonly disabled = input(false);

  readonly items = input.required<readonly CartLine[]>();
  readonly itemCount = input.required<number>();

  readonly increment = output<string>();
  readonly decrement = output<string>();
  readonly remove = output<string>();
  readonly clear = output<void>();

  protected readonly maxQuantity = MAX_QUANTITY;

  protected removeItem(productId: string): void {
    if (this.disabled()) {
      return;
    }

    this.heading().nativeElement.focus();
    this.remove.emit(productId);
  }

  protected decrease(item: CartLine): void {
    if (this.disabled()) {
      return;
    }

    if (item.quantity === 1) {
      this.heading().nativeElement.focus();
    }
    this.decrement.emit(item.productId);
  }

  protected clearItems(): void {
    if (this.disabled() || this.items().length === 0) {
      return;
    }

    this.heading().nativeElement.focus();
    this.clear.emit();
  }
}
