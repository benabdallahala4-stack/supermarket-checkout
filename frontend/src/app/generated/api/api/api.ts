export * from './checkout.service';
import { CheckoutService } from './checkout.service';
export * from './products.service';
import { ProductsService } from './products.service';
export const APIS = [CheckoutService, ProductsService];
