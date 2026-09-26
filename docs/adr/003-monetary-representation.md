# ADR 003: exact cents in the domain

Status: accepted

## Context

The checkout uses EUR prices and integer quantities. Binary floating point cannot represent every cent exactly, and silently rounding input would change the configured price.

## Decision

Use immutable `BigDecimal` values constructed from decimal strings. Product and offer constructors reject null, negative and fractional-cent prices, then normalize to scale two with `RoundingMode.UNNECESSARY`. Extra trailing zeroes are accepted because their value is exact. Zero prices are valid.

Compare amounts numerically with `compareTo`. A quantity offer has at least two units and its bundle price must be strictly less than the corresponding regular price; the catalog snapshot checks that relationship. It also rejects duplicate products, multiple offers per product, missing product references and null entries, and defensively copies its maps.

The REST contract represents money as two-decimal strings. Later REST mappers will preserve that representation. Pricing remains in the Java domain, independent of Spring and generated models.

## Consequences

Arithmetic remains decimal throughout the backend. There is no implicit rounding policy and no frontend price calculation. Supporting fractional quantities, other currencies or taxes would require an explicit precision and rounding decision.

An immutable snapshot preserves an already obtained catalog view. It does not by itself guarantee a consistent read from a future database; a database provider would need an explicit consistency strategy.
