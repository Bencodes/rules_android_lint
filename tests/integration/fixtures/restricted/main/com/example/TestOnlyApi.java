package com.example;

import androidx.annotation.RestrictTo;

public class TestOnlyApi {
  @RestrictTo(RestrictTo.Scope.TESTS)
  public static int fixture() {
    return 1;
  }
}
