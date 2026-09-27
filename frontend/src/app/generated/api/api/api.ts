export * from './catalogManagement.service';
import { CatalogManagementService } from './catalogManagement.service';
export * from './checkout.service';
import { CheckoutService } from './checkout.service';
export * from './products.service';
import { ProductsService } from './products.service';
export const APIS = [CatalogManagementService, CheckoutService, ProductsService];
