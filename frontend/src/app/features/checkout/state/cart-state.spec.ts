import { CartState, MAX_QUANTITY } from './cart-state';

describe('CartState', () => {
  let cart: CartState;

  beforeEach(() => {
    cart = new CartState();
  });

  it('starts empty with a valid empty checkout request', () => {
    expect(cart.isEmpty()).toBe(true);
    expect(cart.itemCount()).toBe(0);
    expect(cart.toCheckoutRequest()).toEqual({ items: [] });
  });

  it('adds and increments one quantity per product', () => {
    cart.increment('APPLE');
    cart.increment('APPLE');
    cart.increment('ORANGE');

    expect(cart.items()).toEqual([
      { productId: 'APPLE', quantity: 2 },
      { productId: 'ORANGE', quantity: 1 },
    ]);
    expect(cart.itemCount()).toBe(3);
    expect(cart.isEmpty()).toBe(false);
  });

  it('decrements a quantity and removes the last unit', () => {
    cart.setQuantity('APPLE', 2);
    cart.decrement('APPLE');
    expect(cart.items()).toEqual([{ productId: 'APPLE', quantity: 1 }]);

    cart.decrement('APPLE');
    cart.decrement('MISSING');
    expect(cart.items()).toEqual([]);
    expect(cart.itemCount()).toBe(0);
    expect(cart.isEmpty()).toBe(true);
  });

  it('removes an entire product without changing other quantities', () => {
    cart.setQuantity('APPLE', 3);
    cart.increment('ORANGE');
    cart.remove('APPLE');
    cart.remove('MISSING');

    expect(cart.items()).toEqual([{ productId: 'ORANGE', quantity: 1 }]);
    expect(cart.itemCount()).toBe(1);
  });

  it('clears the cart and can start a new cart', () => {
    cart.increment('APPLE');
    cart.increment('ORANGE');
    cart.clear();

    expect(cart.isEmpty()).toBe(true);
    expect(cart.itemCount()).toBe(0);
    expect(cart.toCheckoutRequest()).toEqual({ items: [] });

    cart.increment('ORANGE');
    expect(cart.itemCount()).toBe(1);
  });

  it('projects sorted request items without prices or display data', () => {
    cart.increment('ORANGE');
    cart.increment('APPLE');

    expect(cart.toCheckoutRequest()).toEqual({
      items: [
        { productId: 'APPLE', quantity: 1 },
        { productId: 'ORANGE', quantity: 1 },
      ],
    });
  });

  it('preserves previous snapshots and isolates request objects from state', () => {
    cart.increment('APPLE');
    const previous = cart.items();
    const request = cart.toCheckoutRequest();
    request.items[0].quantity = 99;
    request.items.push({ productId: 'ORANGE', quantity: 4 });
    cart.increment('APPLE');

    expect(previous).toEqual([{ productId: 'APPLE', quantity: 1 }]);
    expect(cart.items()).toEqual([{ productId: 'APPLE', quantity: 2 }]);
  });

  it('does not expose mutable item snapshots', () => {
    cart.increment('APPLE');
    const snapshot = cart.items();
    Reflect.set(snapshot[0], 'quantity', 99);
    Reflect.set(snapshot, 'length', 0);

    expect(cart.items()).toEqual([{ productId: 'APPLE', quantity: 1 }]);
    expect(cart.itemCount()).toBe(1);
    expect(cart.toCheckoutRequest()).toEqual({ items: [{ productId: 'APPLE', quantity: 1 }] });
  });

  it('keeps quantities within the int32 bound even when incremented at the limit', () => {
    cart.setQuantity('APPLE', MAX_QUANTITY);
    cart.increment('APPLE');

    expect(cart.items()).toEqual([{ productId: 'APPLE', quantity: MAX_QUANTITY }]);
    cart.decrement('APPLE');
    expect(cart.itemCount()).toBe(MAX_QUANTITY - 1);
  });

  it('treats zero as removal', () => {
    cart.increment('APPLE');
    cart.setQuantity('APPLE', 0);

    expect(cart.isEmpty()).toBe(true);
  });

  it.each([-1, 1.5, Number.NaN, Number.POSITIVE_INFINITY, MAX_QUANTITY + 1])(
    'rejects invalid quantity %s without changing the cart',
    (quantity) => {
      cart.increment('APPLE');
      expect(() => cart.setQuantity('APPLE', quantity)).toThrow(RangeError);
      expect(cart.items()).toEqual([{ productId: 'APPLE', quantity: 1 }]);
    },
  );

  it.each(['', '  '])('rejects blank product ID %j', (productId) => {
    expect(() => cart.increment(productId)).toThrow(Error);
    expect(cart.isEmpty()).toBe(true);
  });
});
