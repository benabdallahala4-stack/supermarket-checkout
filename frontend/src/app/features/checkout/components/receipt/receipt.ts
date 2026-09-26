import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CheckoutReceipt } from '../../../../generated/api';

@Component({
  selector: 'app-receipt',
  templateUrl: './receipt.html',
  styleUrl: './receipt.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Receipt {
  readonly receipt = input.required<CheckoutReceipt>();
}
