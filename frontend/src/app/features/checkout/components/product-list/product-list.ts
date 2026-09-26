import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Currency, Product } from '../../../../generated/api';

@Component({
  selector: 'app-product-list',
  templateUrl: './product-list.html',
  styleUrl: './product-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductList {
  readonly products = input.required<readonly Product[]>();
  readonly currency = input.required<Currency>();
}
