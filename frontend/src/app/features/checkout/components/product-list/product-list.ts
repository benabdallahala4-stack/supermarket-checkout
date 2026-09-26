import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { Currency, Product } from '../../../../generated/api';

@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.html',
  styleUrl: './product-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductList {
  readonly disabled = input(false);

  readonly add = output<string>();

  readonly products = input.required<readonly Product[]>();
  readonly limitProductIds = input<ReadonlySet<string>>(new Set());
  readonly currency = input.required<Currency>();
}
