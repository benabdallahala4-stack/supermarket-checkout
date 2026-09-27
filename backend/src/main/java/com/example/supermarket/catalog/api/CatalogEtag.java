package com.example.supermarket.catalog.api;

import java.util.regex.Pattern;

final class CatalogEtag {
  private static final Pattern STRONG_REVISION =
      Pattern.compile("^\"([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\"$");

  private CatalogEtag() {}

  static String format(String revision) {
    return "\"" + revision + "\"";
  }

  static String revision(String ifMatch) {
    if (ifMatch == null) {
      throw new PreconditionRequired();
    }

    var match = STRONG_REVISION.matcher(ifMatch);
    if (!match.matches()) {
      throw new InvalidPrecondition();
    }

    return match.group(1);
  }

  static final class PreconditionRequired extends RuntimeException {}

  static final class InvalidPrecondition extends RuntimeException {}
}
