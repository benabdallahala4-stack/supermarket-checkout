import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CheckoutPage } from './features/checkout/pages/checkout-page/checkout-page';

@Component({
  selector: 'app-root',
  imports: [CheckoutPage],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {}
