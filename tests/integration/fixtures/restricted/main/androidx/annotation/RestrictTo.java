package androidx.annotation;

public @interface RestrictTo {
  Scope[] value();

  enum Scope { TESTS }
}
