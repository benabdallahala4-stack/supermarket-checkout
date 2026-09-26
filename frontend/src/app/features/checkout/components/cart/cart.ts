import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
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
  readonly disabled = input(false);

  readonly items = input.required<readonly CartLine[]>();
  readonly itemCount = input.required<number>();

  readonly increment = output<string>();
  readonly decrement = output<string>();
  readonly remove = output<string>();
  readonly clear = output<void>();

  protected readonly maxQuantity = MAX_QUANTITY;
}
