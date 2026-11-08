package com.example;

public class TestOnlyApiTest {
  public String usesFixture(String name) {
    return name.toUpperCase() + TestOnlyApi.fixture();
  }
}
